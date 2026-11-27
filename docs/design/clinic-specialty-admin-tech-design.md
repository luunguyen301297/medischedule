# TECHNICAL DESIGN: Menu Chuyên khoa (quản lý chuyên khoa và phòng khám)

BA spec: [`docs/tasks/clinic-specialty-admin-ba-spec.md`](../tasks/clinic-specialty-admin-ba-spec.md) (đã duyệt).

- **Target Component/Layer:** Fullstack — backend module `clinic` (lần đầu có repository/service/web/dto), web
  `features/admin` (trang mới) và `features/landing` (đổi nguồn danh sách chuyên khoa).

## 1. Database Schema Changes

Bảng `specialty`, `room` đã có từ V6, không thêm cột. Một migration mới
`V8__specialty_room_ci_unique_and_seed.sql` (trước khi đặt số: `git fetch`, lấy version kế tiếp trên `origin/main`):

```sql
-- Trùng tên không phân biệt hoa thường (GĐ-02); UNIQUE(name) cũ giữ nguyên.
CREATE UNIQUE INDEX uq_specialty_name_ci ON specialty (lower(name));
CREATE UNIQUE INDEX uq_room_name_ci      ON room (lower(name));

-- Danh mục ban đầu = 10 chuyên khoa đang hiện ở trang chủ (GĐ-01).
INSERT INTO specialty (name) VALUES
  ('Nội tổng quát'), ('Nhi'), ('Tai Mũi Họng'), ('Da liễu'), ('Sản phụ khoa'),
  ('Tim mạch'), ('Tiêu hóa'), ('Cơ xương khớp'), ('Mắt'), ('Răng Hàm Mặt')
ON CONFLICT DO NOTHING;
```

`ON CONFLICT DO NOTHING` (không chỉ định cột) bắt cả hai ràng buộc unique, nên DB nào đã có sẵn tên trùng (hoa/thường
khác) vẫn chạy được. Ràng buộc FK hiện có là cơ sở cho rule "đang được dùng" (mục 3):
`room.specialty_id`, `staff.specialty_id`, `service.specialty_id` → `specialty`; `shift.room_id` → `room`. Không FK nào
có `ON DELETE CASCADE` (đã kiểm V6).

`needs_db_detail: false` — migration đủ đơn giản, đã ghi trọn ở đây.

## 2. Backend (package `vn.medischedule.clinic`)

| File | Nội dung |
|---|---|
| `repository/SpecialtyRepository` | `JpaRepository<Specialty, Long>`; `existsByNameIgnoreCase(String)`, `existsByNameIgnoreCaseAndIdNot(String, Long)`, `findByActiveTrue()` |
| `repository/RoomRepository` | `existsByNameIgnoreCase`, `existsByNameIgnoreCaseAndIdNot`, `findBySpecialtyIdOrderByName(Long)`, `existsBySpecialtyIdAndActiveTrue(Long)`; một query đếm phòng theo chuyên khoa: `select r.specialty.id, count(r) from Room r where r.specialty is not null group by r.specialty.id` |
| `dto/ClinicDtos` | record: `SpecialtyRequest(@NotBlank @Size(max=100) name)`, `SpecialtyResponse(id, name, active, roomCount)`, `RoomRequest(@NotBlank @Size(max=50) name, @NotNull specialtyId)`, `RoomResponse(id, name, specialtyId, active)`, `SpecialtyOption(id, name)` |
| `service/ClinicCatalogService` | Toàn bộ rule (mục 3), `@Transactional` |
| `web/AdminSpecialtyController` | `/api/admin/specialties`, `/api/admin/rooms`, `@PreAuthorize("hasRole('ADMIN')")` ở mức class (theo `AdminUserController`) |
| `web/PublicSpecialtyController` | `GET /api/public/specialties` |

Một service cho cả hai entity vì rule chéo (ngừng chuyên khoa phụ thuộc phòng, thêm phòng phụ thuộc chuyên khoa).
Controller chỉ gọi service, không chạm repository (khác `AdminUserController` hiện tại — đúng quy ước AGENTS.md).

## 3. Processing Flow / Rule

Tên luôn `trim()` trước khi kiểm và lưu (GĐ-02).

| Thao tác | Rule | Lỗi (`ApiException`) |
|---|---|---|
| Tạo/sửa chuyên khoa | tên trùng (ignore case, trừ chính nó) | 409 `SPECIALTY_EXISTS` "Chuyên khoa đã tồn tại" |
| Ngừng chuyên khoa (`active=false`) | `existsBySpecialtyIdAndActiveTrue` | 409 `SPECIALTY_HAS_ACTIVE_ROOMS` "Hãy ngừng hoặc chuyển hết phòng khám của chuyên khoa trước" |
| Xóa chuyên khoa | `delete` + `flush()`; bắt `DataIntegrityViolationException` | 409 `SPECIALTY_IN_USE` "Chuyên khoa đang được dùng, không thể xóa. Hãy ngừng hoạt động." |
| Tạo/sửa phòng | tên trùng toàn bảng `room` (gồm phòng dùng chung, GĐ-04) | 409 `ROOM_EXISTS` "Tên phòng đã tồn tại" |
| Tạo phòng, chuyển phòng sang chuyên khoa khác, mở lại phòng | chuyên khoa đích phải `active` | 409 `SPECIALTY_INACTIVE` "Hãy mở lại chuyên khoa trước" |
| Xóa phòng | `delete` + `flush()`; bắt `DataIntegrityViolationException` | 409 `ROOM_IN_USE` "Phòng đã có ca trực, không thể xóa. Hãy ngừng hoạt động." |
| id không có | — | 404 `SPECIALTY_NOT_FOUND` / `ROOM_NOT_FOUND` |
| Sửa/xóa/bật tắt phòng có `specialty = NULL` qua API này | phòng dùng chung ngoài phạm vi | 404 `ROOM_NOT_FOUND` |

**Vì sao dựa vào FK khi xóa:** rule "đang được dùng" chạm bảng của module khác (`staff` cùng module, nhưng `service`
thuộc `billing`, `shift` thuộc `schedule`). Gọi repository module khác bị cấm, còn các module đó chưa có service. FK đã
là nguồn sự thật, nên thử xóa rồi bắt vi phạm FK là đúng và không thêm phụ thuộc. Phải `flush()` ngay trong
`try` để lỗi nổ tại đó, không nổ lúc commit ngoài tầm bắt. Transaction rollback sau lỗi là đúng ý (không có thay đổi
nào khác cần giữ).

**Trùng tên đồng thời:** kiểm `exists…` cho thông báo đẹp; hai request cùng lúc lọt qua thì index `lower(name)` chặn.
Bọc `saveAndFlush` trong `try`, bắt `DataIntegrityViolationException` → cùng mã `*_EXISTS`.

**Public:** `findByActiveTrue()`, sắp theo tên bằng `java.text.Collator.getInstance(Locale.forLanguageTag("vi"))`
(không phụ thuộc collation của DB) → `List<SpecialtyOption>`.

## 4. API Contract Overview

Body lỗi theo chuẩn `{code, message, fields}`; lỗi validate → 400 với `fields`.

| Method | Path | Body / Param | Trả về |
|---|---|---|---|
| GET | `/api/admin/specialties` | — | `SpecialtyResponse[]`, sắp theo tên (Collator vi), gồm cả đang ngừng |
| POST | `/api/admin/specialties` | `{name}` | 201 `SpecialtyResponse` |
| PUT | `/api/admin/specialties/{id}` | `{name}` | `SpecialtyResponse` |
| PATCH | `/api/admin/specialties/{id}/active?value=` | — | `SpecialtyResponse` |
| DELETE | `/api/admin/specialties/{id}` | — | 204 |
| GET | `/api/admin/specialties/{id}/rooms` | — | `RoomResponse[]` sắp theo tên |
| POST | `/api/admin/rooms` | `{name, specialtyId}` | 201 `RoomResponse` |
| PUT | `/api/admin/rooms/{id}` | `{name, specialtyId}` | `RoomResponse` |
| PATCH | `/api/admin/rooms/{id}/active?value=` | — | `RoomResponse` |
| DELETE | `/api/admin/rooms/{id}` | — | 204 |
| GET | `/api/public/specialties` | — | `[{id, name}]` chỉ đang hoạt động, A→Z |

Không phân trang (GĐ-05). `needs_api_detail: false`.

## 5. Frontend

**Khu admin** — thư mục mới `web/src/features/admin/specialty/`:

- `api.ts`: `specialtyApi` theo kiểu `usersApi` (`http` từ `@/shared/api/http`).
- `SpecialtyPage.tsx`: hai cột (GĐ-05). Trái: danh sách chuyên khoa (tên, số phòng, Tag "Đang ngừng"), nút Thêm; mỗi
  dòng có Sửa tên, Switch hoạt động, Xóa (`Popconfirm`). Phải: bảng phòng của chuyên khoa đang chọn, nút Thêm phòng;
  mỗi dòng có Sửa (Modal: tên + Select chuyên khoa đang hoạt động), Switch, Xóa. Chuyên khoa đang ngừng: tắt nút Thêm
  phòng và Switch mở lại phòng (backend vẫn chặn, AC-17).
- React Query: key `['admin','specialties']` và `['admin','rooms',specialtyId]`; sau mỗi mutation invalidate
  `['admin','specialties']` (đếm phòng đổi), `['admin','rooms']` và `['public','specialties']`.
- Lỗi: `message.error(errorMessage(e))`; lỗi validate gắn vào ô bằng `fieldErrors`.
- `AdminRoutes.tsx`: `<Route path="specialties" …>`. `layout/AdminLayout.tsx`: thêm
  `{ key: '/admin/specialties', icon: <ApartmentOutlined />, label: 'Chuyên khoa' }` vào `children` của `settings`, sau
  Trang chủ (AC-01). Màu chỉ dùng `var(--ms-*)` / token Ant Design.

**Trang chủ** (`features/landing`):

- `api.ts`: thêm `contentApi.specialties` → `GET /public/specialties`; bỏ `specialties` khỏi kiểu `booking`, khỏi
  `DEFAULT_HOMEPAGE` và `LIMITS` (`specialties`, `specialty`).
- `LandingLayout.tsx`: `useQuery(['public','specialties'])`, truyền tên vào `BookingWidget`.
- `BookingWidget.tsx`: danh sách rỗng → hiện "Chưa có chuyên khoa" và khóa nút gửi (AC-24).
- `admin/homepage/HomepageSettingsPage.tsx`: thay section "Chuyên khoa trong form đặt lịch" bằng một dòng + `Link` tới
  `/admin/specialties` (AC-23). Trường `booking.specialties` cũ trong JSON `site_content` không còn ai đọc; lần lưu
  kế tiếp của trang Thiết lập sẽ ghi lại JSON không có trường này. Không cần migration dọn dữ liệu.

## 6. Permission Model

- `/api/admin/**`: `SecurityConfig` đã chặn ở tầng URL cho ADMIN; controller thêm `@PreAuthorize("hasRole('ADMIN')")`
  như mẫu (AC-02).
- `/api/public/specialties`: nằm trong `/api/public/**` đã public; chỉ trả `id`, `name` của chuyên khoa đang hoạt
  động.

## 7. Risks & Edge Cases

| Rủi ro | Xử lý |
|---|---|
| Xóa khi đang được dùng (AC-11, AC-20) | FK + `flush()` trong `try` → 409, mục 3 |
| Trùng tên khác hoa thường, hoặc hai request đồng thời (AC-06, AC-15) | `exists…IgnoreCase` + index `lower(name)` |
| Chuyên khoa ngừng nhưng phòng vẫn hoạt động | Chặn ở mọi đường vào: ngừng chuyên khoa, tạo phòng, chuyển phòng, mở lại phòng (AC-08, AC-17) |
| Phòng dùng chung bị sửa nhầm qua API admin | 404 khi `specialty = NULL` |
| Thứ tự tên tiếng Việt sai (Đ, dấu) | `Collator` vi ở Java |
| Trang chủ trống sau khi đổi nguồn | Seed 10 chuyên khoa trong V8 (GĐ-01); khi rỗng thật thì AC-24 |
| Migration chạy trên DB dev đã có dữ liệu tay | `ON CONFLICT DO NOTHING`; tạo index `lower(name)` sẽ lỗi nếu DB đã có hai tên chỉ khác hoa thường. Bảng hiện chưa có code ghi vào nên rủi ro thấp; dev gặp thì dọn tay |
| Seed dev `R__seed_dev_data.sql` | Hiện không chèn `specialty` (đã grep), không xung đột |

## 8. Test

- Unit (Mockito, theo `AuthServiceTest`): `ClinicCatalogServiceTest` — trùng tên, ngừng chuyên khoa còn phòng hoạt
  động, thêm/chuyển/mở phòng vào chuyên khoa đang ngừng, xóa bị FK chặn → mã lỗi đúng, phòng dùng chung → 404.
- Tích hợp trên DB `qa_*` (nếu QA chọn): migration V8 chạy được, index `lower(name)` chặn trùng, xóa chuyên khoa có
  phòng → 409.
- Web: `npm run typecheck`; kiểm tay/Playwright trang `/admin/specialties` và form đặt lịch trang chủ.

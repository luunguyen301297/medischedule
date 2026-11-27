# TECHNICAL DESIGN: Ca làm việc và Lịch làm việc nhân viên

Spec: `docs/tasks/work-shift-ba-spec.md` (AC-01…AC-20).

- **Target Component/Layer:** Fullstack — backend module `schedule` (đang trống, chỉ có entity `Shift`), web
  `features/admin`.

## Database Schema Changes — `V10__work_shift_and_assignment.sql`

```
work_shift
  id          BIGINT IDENTITY PK
  name        VARCHAR(50)  NOT NULL
  start_time  TIME         NOT NULL
  end_time    TIME         NOT NULL   CHECK (end_time > start_time)      -- GĐ-02 không qua đêm
  color       VARCHAR(7)   NOT NULL   CHECK (color ~ '^#[0-9a-fA-F]{6}$')
  active      BOOLEAN      NOT NULL DEFAULT TRUE
  UNIQUE INDEX uq_work_shift_name_ci ON lower(name)                      -- AC-03, giống V8

work_assignment
  id             BIGINT IDENTITY PK
  work_date      DATE   NOT NULL
  work_shift_id  BIGINT NOT NULL REFERENCES work_shift(id)               -- FK chặn xoá ca đã dùng (AC-06)
  user_id        BIGINT NOT NULL REFERENCES users(id)
  UNIQUE (work_date, work_shift_id, user_id)                             -- AC-09; cũng phục vụ truy vấn theo khoảng ngày
```

- Gắn `users.id`, không gắn `staff.id`: tài khoản nhân viên có thể chưa có hồ sơ `staff` (STAFF_PROFILE_NOT_FOUND), mà
  vẫn cần phân ca.
- Seed (memory "Mock data vào Flyway"): 3 ca Sáng 07:00–11:30, Chiều 13:00–17:00, Tối 17:30–21:00, `ON CONFLICT DO
  NOTHING`; không seed phân ca.
- Không đụng `shift` / `appointment`.

## Processing Flow

`WorkShiftController` / `WorkScheduleController` (`/api/admin/...`, `@PreAuthorize ADMIN`) → `WorkScheduleService`
(module schedule) → `WorkShiftRepository`, `WorkAssignmentRepository`; tên/vai trò nhân viên lấy qua
`AuthService.staffAccounts()` (method mới, trả `StaffAccount(userId, fullName, role, enabled)` cho user role ≠
PATIENT) — không gọi `UserRepository` từ module khác.

Quy tắc trong service:
- Tạo/sửa ca: trim tên, `existsByNameIgnoreCase(AndIdNot)` rồi `saveAndFlush` bắt lỗi unique (pattern
  `ClinicCatalogService.saveUniqueName`) → 409 `WORK_SHIFT_EXISTS`. `end <= start` → 400 `INVALID_TIME_RANGE`.
- Xoá ca: `deleteAndFlush` bắt `DataIntegrityViolationException` → 409 `WORK_SHIFT_IN_USE` ("Ca đã được phân lịch, hãy
  tắt ca").
- Phân ca `{date, shiftId, userIds[]}`:
  1. `date < today` (giờ `Asia/Ho_Chi_Minh`) → 400 `PAST_DATE` (AC-14).
  2. Ca phải tồn tại và `active` → 409 `WORK_SHIFT_INACTIVE` (AC-05).
  3. Mỗi userId phải là nhân viên `enabled` → 400 `INVALID_STAFF` (AC-12).
  4. Bỏ người đã có trong (date, shift) (AC-09).
  5. Chồng giờ: lấy các phân ca cùng ngày của những userId đó, so `a.start < b.end && b.start < a.end` → 409
     `SHIFT_OVERLAP` kèm tên người (AC-10, AC-11).
  6. `saveAll`.
- Gỡ: ngày đã qua → 400 `PAST_DATE`; không tồn tại → 404.
- ponytail: kiểm chồng giờ không khoá dòng (chỉ admin thao tác); cần chặn tuyệt đối thì exclusion constraint.

## API Contract Overview

| Method | Path | Body / Query | Trả |
|---|---|---|---|
| GET | `/api/admin/work-shifts` | — | `WorkShiftResponse[]` sắp theo `startTime` (gồm cả ca tắt) |
| POST | `/api/admin/work-shifts` | `{name, startTime "HH:mm", endTime, color}` | 201 `WorkShiftResponse` |
| PUT | `/api/admin/work-shifts/{id}` | như trên | `WorkShiftResponse` |
| PATCH | `/api/admin/work-shifts/{id}/active?value=` | — | `WorkShiftResponse` |
| DELETE | `/api/admin/work-shifts/{id}` | — | 204 |
| GET | `/api/admin/work-schedule?from=&to=` | ngày ISO, `to - from ≤ 42` ngày (lưới tháng 6 tuần), ngược → 400 `INVALID_RANGE` | `AssignmentResponse[]` `{id, date, shiftId, userId, fullName, role}` |
| POST | `/api/admin/work-schedule` | `{date, shiftId, userIds[1..]}` | 201 `AssignmentResponse[]` (bản ghi mới) |
| DELETE | `/api/admin/work-schedule/{id}` | — | 204 |
| GET | `/api/admin/work-schedule/staff` | — | `StaffOption[] {userId, fullName, role}` nhân viên đang hoạt động, A→Z |

`WorkShiftResponse = {id, name, startTime, endTime, color, active}`. Lỗi theo `{code, message, fields}` hiện có.

## Frontend

- `features/admin/workshift/` — `api.ts` (ca + phân ca + staff), `WorkShiftPage.tsx`: Table + Modal form (Input,
  `TimePicker.RangePicker` format HH:mm, `ColorPicker` của antd, Switch active, xoá có confirm) — theo mẫu
  `SpecialtyPage`.
- `features/admin/workshift/WorkSchedulePage.tsx` — `Segmented` Ngày/Tuần/Tháng, nút Trước/Hôm nay/Sau, `DatePicker`
  (picker theo chế độ), `Select` lọc vai trò (dùng `ROLE_LABEL` từ `@/features/auth`). Một query `['work-schedule',
  from, to]` theo khoảng hiển thị; nhóm theo ngày → ca ở client (dayjs).
  - Ngày: mỗi ca một khối (vạch màu ca, giờ), danh sách nhân viên (Tag vai trò, nút gỡ nếu ngày chưa qua), nút
    "Thêm nhân viên" mở Modal `Select mode=multiple` nhân viên.
  - Tuần: lưới 7 cột T2→CN, mỗi ô liệt kê ca + tên nhân viên; bấm ô → chế độ Ngày.
  - Tháng: antd `Calendar` với `cellRender` hiện mỗi ca "Tên ca · n người" (chấm màu ca); chọn ngày → chế độ Ngày.
- Menu: "Ca làm việc" (`/admin/work-shifts`) trong Cài đặt; "Lịch làm việc" (`/admin/work-schedule`) trong Quản trị.
- Màu ca là dữ liệu (inline style từ DB), không vi phạm quy tắc màu CSS; CSS mới dùng `var(--ms-*)` trong `admin.css`.

## Permission Model

Chỉ ADMIN: `@PreAuthorize("hasRole('ADMIN')")` trên cả hai controller, thêm lớp `/api/admin/**` sẵn có ở
SecurityConfig (AC-07). Web: nằm trong `AdminLayout` (đã chặn role ≠ ADMIN).

## Risks & Edge Cases

| Rủi ro | Xử lý |
|---|---|
| Sửa giờ ca làm phát sinh chồng giờ với phân ca sẵn có | Chấp nhận (AC-04 chỉ yêu cầu hiển thị giờ mới); chỉ kiểm khi phân ca mới. |
| Nhân viên bị khoá sau khi đã phân | Lịch cũ vẫn trả tên/vai trò (AC-12): `staffAccounts()` trả cả enabled=false. |
| Lệch ngày do múi giờ | Backend dùng `LocalDate.now(Asia/Ho_Chi_Minh)`; web gửi ngày dạng `YYYY-MM-DD`. |
| Khoảng truy vấn quá lớn | Giới hạn 42 ngày. |
| Trùng tên do race | Unique index `lower(name)`. Trùng phân ca do race → unique (date, shift, user) → 409. |

## Test

Unit `WorkScheduleServiceTest` (Mockito, theo mẫu test sẵn có): trùng tên, giờ ngược, ngày đã qua, ca tắt, bỏ người
đã có, chồng giờ, nhân viên khoá. Web: `npm run typecheck` + chạy thử trên trình duyệt.

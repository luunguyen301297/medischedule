# MediSchedule

Hệ thống đặt lịch và quản lý phòng khám. Trả lời bằng tiếng Việt.

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `backend/` | Java 21, Spring Boot 3.3, modular monolith, package gốc `vn.medischedule` |
| `web/` | React 18 + Vite + Ant Design 5 + React Query, chia theo feature (xem bên dưới) |
| `mobile/` | Flutter cho bệnh nhân (chưa khởi tạo) |
| `ai-service/` | Python FastAPI (chưa khởi tạo) |
| `infra/` | Script khởi tạo Postgres |
| `docs/` | Link use case, thiết kế CSDL |

## Lệnh

- Hạ tầng: `docker compose up -d` (Postgres :5435, Redis :6390, MinIO :9010 / console :9011)
- Backend trên máy (cần JDK 21): `powershell -File scripts\run-backend.ps1` (nạp `.env` rồi `mvnw spring-boot:run`)
- Web: `npm run dev` trong `web/` (:5175, proxy `/api` → :8085); kiểm kiểu: `npm run typecheck`
- VS Code: Run and Debug → **Run Full Stack**
- Swagger: http://localhost:8085/swagger-ui.html
- Cập nhật dữ liệu seed sau khi sửa nội dung trang chủ: `python scripts/export_seed.py`

## Quy ước backend

- Mỗi module một package: `domain` (entity) / `repository` / `service` / `web` (controller) / `dto` (record). Module mẫu: `auth`.
- Schema chỉ đổi qua Flyway (`backend/src/main/resources/db/migration/V{n}__mo_ta.sql`); Hibernate để `ddl-auto: validate`. Không sửa migration đã chạy.
- Phân quyền theo vai trò: mỗi user một role trong bảng `roles` (ADMIN, DOCTOR, RECEPTIONIST, PHARMACIST, PATIENT). Kiểm bằng `@PreAuthorize("hasRole('DOCTOR')")`; không có bảng permission.
- Người đang đăng nhập: `@AuthenticationPrincipal AuthUser me` (id, email, role lấy từ JWT).
- Lỗi nghiệp vụ: ném `ApiException` (mã lỗi + thông báo tiếng Việt); body lỗi `{code, message, fields}`.
- Không gọi chéo repository của module khác; gọi qua service của module đó.

## Quy ước web (`web/src`)

```
app/        main.tsx (entry), providers.tsx, router.tsx (route + lazy load từng khu), guards.tsx
shared/     dùng chung, KHÔNG import từ features/
  api/        http.ts (axios + JWT + lỗi), files.ts
  theme/      theme.ts (bảng màu duy nhất → biến --ms-* + theme Ant Design), base.css
  ui/         Reveal, ImageField...
features/   mỗi khu tự chứa api + type + trang + css, khớp module backend / vai trò
  auth/       đăng nhập, đăng ký, ROLE_LABEL
  landing/    trang giới thiệu, BookingWidget, nội dung trang chủ, landing.css
  home/       trang sau đăng nhập
  admin/      /admin/*: layout, staff, homepage (tải lười), admin.css
```

- Import bằng alias `@/` (`@/shared/api/http`). Trong cùng feature dùng đường dẫn tương đối.
- Feature khác chỉ import qua `index.ts` của feature (`@/features/auth`), không import sâu vào file bên trong.
- Khu mới theo vai trò (`doctor/`, `reception/`, `pharmacy/`, `patient/`...): tạo thư mục trong `features/`, export bộ route từ `index.ts`, gắn vào `app/router.tsx` bằng `lazy()`.
- Màu chỉ khai báo ở `shared/theme/theme.ts`; CSS dùng `var(--ms-*)`, không viết mã màu cứng. Không box-shadow, một màu xanh chủ đạo.
- CSS riêng của khu đặt cạnh feature và import trong layout/trang của khu đó.
- Control tự viết dùng trong `Form.Item` phải nhận `id` / `value` / `onChange` và gắn `id` vào phần tử nhập thật (label trỏ đúng).

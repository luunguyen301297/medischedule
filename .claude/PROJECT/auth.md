# Auth Flow

> Đọc khi: task đụng tới login, token, permission. Bỏ qua nếu task không liên quan auth/session.

MediSchedule dùng 1 hệ thống auth (chung cho nhân viên và bệnh nhân, phân biệt bằng role).

|                          | Tài khoản MediSchedule (nhân viên + bệnh nhân)               |
|--------------------------|--------------------------------------------------------------|
| User model               | `User` (bảng `users`, mỗi user đúng một `Role` — bảng `roles`) |
| Login method             | email **hoặc** tên đăng nhập (`username`) + mật khẩu (bcrypt); bệnh nhân tự đăng ký, nhân viên do admin tạo |
| Endpoint                 | `POST /api/auth/login` (cùng `POST /api/auth/register`, `GET /api/auth/me`) |
| Security module          | `vn.medischedule.auth.security` (`SecurityConfig`, `JwtAuthFilter`, `JwtService`, `AuthUser`) |
| JWT payload              | `sub` (user id), `email`, `role`, `iat`, `exp` — HS256, hạn `JWT_TTL_MINUTES` (mặc định 480) |
| Refresh token delivery   | Không có refresh token — chỉ access token trong body đăng nhập (`TokenResponse`) |
| Token storage (frontend) | `localStorage` key `ms_token` (`tokenStore` trong `web/src/shared/api/http.ts`) |
| Frontend state           | Không có context/store auth: `RequireAuth` (`web/src/app/guards.tsx`) chỉ kiểm có token; thông tin user lấy bằng query `['me']` (`authApi.me`) |
| Refresh rotation         | Không có — token hết hạn thì interceptor 401 xoá token, chuyển `/login` |
| RBAC                     | Role-only: 5 role `ADMIN`, `DOCTOR`, `RECEPTIONIST`, `PHARMACIST`, `PATIENT` (hằng `RoleCode`); không có bảng permission |
| Auth dependency / middleware (Spring Boot 3) | @AuthenticationPrincipal AuthUser, @PreAuthorize (`@PreAuthorize("hasRole('DOCTOR')")`; `/api/admin/**` chặn theo URL cho `ADMIN`) |

## Kiểm trạng thái tài khoản

| Cách | Hành vi | Hệ quả |
|---|---|---|
| **Stateless JWT** | Request chỉ xác thực chữ ký + hạn của token; không tra DB | Khoá tài khoản / đổi role chỉ có hiệu lực khi token hết hạn (hoặc khi refresh bị từ chối). Muốn hiệu lực sớm hơn: access token ngắn hạn, kiểm DB ở bước refresh |

Dự án đang dùng: **Stateless JWT** — `AuthUser` dựng từ claim JWT, không truy vấn DB mỗi request; `enabled = false` chỉ
chặn ở bước đăng nhập (`ACCOUNT_DISABLED`). Khoá tài khoản / đổi role có hiệu lực khi token hết hạn (tối đa
`JWT_TTL_MINUTES`). Đổi sang kiểm DB mỗi request là thay đổi kiến trúc (cần design + Tech Lead duyệt).

## Ghi chú bắt buộc

- Mọi request API của web đi qua `http` (`web/src/shared/api/http.ts`) — không tạo axios instance riêng.
- Lỗi 401/403 ở tầng filter do `SecurityConfig` ghi body `{code, message}` (`UNAUTHORIZED` / `FORBIDDEN`), không qua
  `GlobalExceptionHandler`; giữ cùng định dạng khi thêm handler.
- Endpoint mới: có `@PreAuthorize` đúng role (hoặc nằm trong danh sách `PUBLIC` có chủ đích); test có ca 401 (không
  token) và 403 (sai role).
- Guard FE chỉ để điều hướng; kiểm role của khu trong layout khu (vd `AdminLayout`) — phân quyền thật ở backend.
- Tài khoản admin tạo một lần lúc backend khởi động theo `ADMIN_*` trong `.env` (`AdminInitializer`); tài khoản demo
  theo vai trò nằm trong seed dev — không ghi mật khẩu vào tài liệu `.claude/`.

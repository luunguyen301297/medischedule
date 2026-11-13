# MediSchedule

## Chạy lần đầu

1. Bật Docker Desktop.
2. `copy .env.example .env` rồi đổi mật khẩu (bản dev đã có sẵn `.env`).
3. `docker compose up -d` — chạy Postgres, Redis, MinIO và tạo bucket `medical-files`.
4. Backend: `docker compose --profile app up -d --build backend`, hoặc mở `backend/` bằng IntelliJ (JDK 21), nạp biến từ `.env`, chạy `MediScheduleApplication`.
5. Mở http://localhost:5175/login, đăng nhập quản trị bằng `admin` / `admin@123` (đặt ở `ADMIN_USERNAME` / `ADMIN_PASSWORD` trong `.env`), vào `/admin`.
   Swagger: http://localhost:8085/swagger-ui.html, gọi `POST /api/auth/login` với `{"login": "admin", "password": "..."}`, bấm Authorize dán `accessToken`.

## API auth

| Method | Đường dẫn | Quyền |
|---|---|---|
| POST | `/api/auth/register` | Công khai, tạo tài khoản PATIENT |
| POST | `/api/auth/login` | Công khai, `login` là email hoặc tên đăng nhập, trả JWT |
| GET | `/api/public/site-content/homepage` | Công khai, nội dung trang giới thiệu |
| PUT | `/api/admin/site-content/homepage` | ADMIN, lưu nội dung trang giới thiệu |
| GET | `/api/auth/me` | Đã đăng nhập |
| GET | `/api/admin/users?role=DOCTOR&q=nguyen` | ADMIN |
| POST | `/api/admin/users` | ADMIN, tạo nhân viên với role chỉ định |
| PATCH | `/api/admin/users/{id}/role` | ADMIN |
| PATCH | `/api/admin/users/{id}/enabled?value=false` | ADMIN, khóa / mở tài khoản |

## Cổng

| Dịch vụ | Cổng |
|---|---|
| Postgres | 5435 |
| Redis | 6390 |
| MinIO API / Console | 9010 / 9011 |
| Backend | 8085 |
| Web (sau này) | 5175 |

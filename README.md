# MediSchedule

## Chạy lần đầu

1. Bật Docker Desktop.
2. `copy .env.example .env` rồi đổi mật khẩu (bản dev đã có sẵn `.env`).
3. `docker compose up -d` — chạy Postgres, Redis, MinIO và tạo bucket `medical-files`.
4. Backend: `docker compose --profile app up -d --build backend`, hoặc mở `backend/` bằng IntelliJ (JDK 21), nạp biến từ `.env`, chạy `MediScheduleApplication`.
5. Mở http://localhost:5175/login, đăng nhập quản trị bằng `admin` / `admin@123` (đặt ở `ADMIN_USERNAME` / `ADMIN_PASSWORD` trong `.env`), vào `/admin`.
   Swagger: http://localhost:8085/swagger-ui.html, gọi `POST /api/auth/login` với `{"login": "admin", "password": "..."}`, bấm Authorize dán `accessToken`.

Hoặc trong VS Code: mở thư mục gốc, chọn **Run Full Stack** (F5).

## Dữ liệu khởi tạo

Backend khởi động là Flyway tự dựng DB, không cần import gì thêm:

| Thư mục | Nội dung |
|---|---|
| `backend/src/main/resources/db/migration` (`V1`…`V4`) | Bảng, 5 vai trò, nội dung trang chủ mẫu |
| `backend/src/main/resources/db/seed` (`R__seed_dev_data.sql`) | Nội dung trang chủ hiện tại + tài khoản demo. Chạy sau `V*`, chạy lại mỗi khi file đổi |

Tài khoản có sẵn (đăng nhập bằng tên đăng nhập hoặc email):

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Quản trị viên | `admin` | `admin@123` (tạo bởi `AdminInitializer`) |
| Bác sĩ | `bacsi` | `demo@123` |
| Lễ tân | `letan` | `demo@123` |
| Dược sĩ | `duocsi` | `demo@123` |
| Bệnh nhân | `benhnhan` | `demo@123` |

Cập nhật seed sau khi sửa nội dung ở `/admin/homepage`:

```bash
python scripts/export_seed.py
```

Script đọc DB dev và ghi lại `R__seed_dev_data.sql`; commit file đó để người khác nhận bản mới (backend của họ tự nạp lại khi khởi động).
Tài khoản thật không được xuất ra seed. Ảnh upload lên MinIO cũng chưa nằm trong seed: dùng link ảnh ngoài nếu cần người khác thấy.

Môi trường thật: đặt `FLYWAY_LOCATIONS=classpath:db/migration` để không nạp dữ liệu demo.

Xóa sạch dữ liệu dev để dựng lại từ đầu: `docker compose down -v` rồi `docker compose up -d`.

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

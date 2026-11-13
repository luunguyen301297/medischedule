# MediSchedule

Hệ thống đặt lịch và quản lý phòng khám. Trả lời bằng tiếng Việt.

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `backend/` | Java 21, Spring Boot 3.3, modular monolith, package gốc `vn.medischedule` |
| `web/` | React + Vite + Ant Design (chưa khởi tạo) |
| `mobile/` | Flutter cho bệnh nhân (chưa khởi tạo) |
| `ai-service/` | Python FastAPI (chưa khởi tạo) |
| `infra/` | Script khởi tạo Postgres |
| `docs/` | Link use case, thiết kế CSDL |

## Lệnh

- Hạ tầng: `docker compose up -d` (Postgres :5435, Redis :6390, MinIO :9010 / console :9011)
- Backend trong Docker: `docker compose --profile app up -d --build backend` (API :8085)
- Backend trên máy (cần JDK 21 + Maven): nạp `.env` rồi `mvn spring-boot:run` trong `backend/`
- Swagger: http://localhost:8085/swagger-ui.html

## Quy ước backend

- Mỗi module một package: `domain` (entity) / `repository` / `service` / `web` (controller) / `dto` (record). Module mẫu: `auth`.
- Schema chỉ đổi qua Flyway (`backend/src/main/resources/db/migration/V{n}__mo_ta.sql`); Hibernate để `ddl-auto: validate`. Không sửa migration đã chạy.
- Phân quyền theo vai trò: mỗi user một role trong bảng `roles` (ADMIN, DOCTOR, RECEPTIONIST, PHARMACIST, PATIENT). Kiểm bằng `@PreAuthorize("hasRole('DOCTOR')")`; không có bảng permission.
- Người đang đăng nhập: `@AuthenticationPrincipal AuthUser me` (id, email, role lấy từ JWT).
- Lỗi nghiệp vụ: ném `ApiException` (mã lỗi + thông báo tiếng Việt); body lỗi `{code, message, fields}`.
- Không gọi chéo repository của module khác; gọi qua service của module đó.

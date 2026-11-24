# Domain Context (Shared)

> Mọi skill (BA, SA, TL, engineer, QA) đọc file này để nắm bối cảnh domain trước khi đọc KB. Giữ file ngắn (≤ 80
> dòng): chỉ những gì agent cần biết ngay ở đầu task; chi tiết rule/workflow nằm trong KB `docs/knowledge/medischedule/`.

Dự án **MediSchedule** — domain **Đặt lịch và quản lý phòng khám**.

## 1. Tóm tắt domain

- Mục đích: hệ thống đặt lịch và quản lý phòng khám — bệnh nhân đặt / huỷ / đổi lịch khám, lễ tân check-in và điều phối
  hàng đợi, bác sĩ khám và ghi bệnh án, dược sĩ cấp phát thuốc, quản trị quản lý nhân viên, bảng giá, nội dung trang chủ.
- Ngoài phạm vi repo: chưa ghi nhận hệ thống ngoài nào ngoài `ai-service` (phân tích ảnh y tế, nằm cùng repo, chưa khởi
  tạo) và kênh thông báo Push/Email/SMS (module `notification`, chưa làm) — **chờ xác nhận** với user.
- Nguồn nghiệp vụ: sơ đồ use case, thiết kế CSDL (link ở `docs/README.md`); mã use case (`G1`, `P1`, `D1`…) ở
  `package-info.java` từng module.

## 2. Glossary (thuật ngữ cốt lõi)

| Thuật ngữ | Tên trong code | Định nghĩa ngắn |
|-----------|----------------|-----------------|
| Vai trò | `Role` / `RoleCode` | Mỗi tài khoản đúng một vai trò: `ADMIN`, `DOCTOR`, `RECEPTIONIST`, `PHARMACIST`, `PATIENT` |
| Tài khoản | `User` | Người đăng nhập (nhân viên hoặc bệnh nhân); đăng nhập bằng email hoặc `username` |
| Nội dung trang chủ | `SiteContent` (bảng `site_content`) | Khối nội dung công khai do admin sửa ở `/admin/homepage`, lưu JSON theo khoá |
| Seed dev | `R__seed_dev_data.sql` | Dữ liệu demo dùng chung (nội dung trang chủ, tài khoản demo); không ghi đè dữ liệu đã sửa |
Thuật ngữ khám chữa bệnh (slot, ca trực, bệnh án, đơn thuốc…) thêm khi làm module — **chờ xác nhận** với user.

## 3. Actor & vai trò

| Actor     | Kênh     | Việc chính     | Quyền / role |
|-----------|----------|----------------|--------------|
| Quản trị viên | Web, khu `/admin` | quản lý nhân viên (tạo, đổi role, khoá), nội dung trang chủ, bảng giá | `ADMIN` |
| Bác sĩ | Web (khu theo vai trò, chưa làm) | khám, bệnh án, chỉ định, ca trực | `DOCTOR` |
| Lễ tân | Web (chưa làm) | đặt lịch hộ, check-in, hàng đợi, thu tiền | `RECEPTIONIST` |
| Dược sĩ | Web (chưa làm) | đơn thuốc, cấp phát, kho theo lô | `PHARMACIST` |
| Bệnh nhân | Web (trang giới thiệu, đăng ký, đặt lịch); app Flutter (dự kiến) | đặt / huỷ / đổi lịch, xem kết quả | `PATIENT` |

## 4. Quy trình chính

1. Đăng ký / đăng nhập: bệnh nhân tự đăng ký (`PATIENT`) → đăng nhập bằng email/username + mật khẩu → nhận JWT.
2. Quản trị nhân viên: admin tạo tài khoản nhân viên với role → hệ thống hiện thông tin đăng nhập một lần → nhân viên
   đăng nhập.
3. Nội dung trang chủ: admin sửa ở `/admin/homepage` → trang giới thiệu hiển thị → (dev) `scripts/export_seed.py` ghi
   lại seed.
4. Luồng khám (đặt lịch → check-in → khám → kê đơn → cấp phát → thanh toán): **chờ xác nhận** — chưa có code.

## 5. Ràng buộc nghiệp vụ xuyên suốt

- Múi giờ `Asia/Ho_Chi_Minh` (`hibernate.jdbc.time_zone`, container Postgres); thời điểm lưu `TIMESTAMPTZ`.
- Thông báo lỗi và chữ trên giao diện bằng tiếng Việt; mã lỗi ổn định (`EMAIL_EXISTS`…) cho FE xử lý.
- Dữ liệu y tế (bệnh án, file y tế) là dữ liệu cá nhân nhạy cảm — ràng buộc lưu trữ / quyền xem **chờ xác nhận**.

## 6. Auth & phân quyền

- User model: `User`
- Auth: email hoặc username + mật khẩu → JWT (HS256, stateless) → @AuthenticationPrincipal AuthUser, @PreAuthorize
- Frontend: token trong `localStorage` (`ms_token`), guard `RequireAuth`, user hiện tại qua query `['me']`
- RBAC: một role mỗi user, kiểm bằng `@PreAuthorize("hasRole('...')")`; không có bảng permission
- Refresh token: không có — hết hạn thì đăng nhập lại

Chi tiết auth: [PROJECT/auth.md](../../PROJECT/auth.md)
Danh mục module/endpoint (auto-gen, nếu có): `docs/_ref/capabilities.md` (cùng đường dẫn `CAPABILITIES_DOC` của
`.claude/scripts/check-docs-drift.sh`; đổi tên file thì đổi cả hai)
Knowledge base: `docs/knowledge/medischedule/README.md`

## 7. Stack (tóm tắt)

- Backend: Java 21, Spring Boot 3, Spring Data JPA (Hibernate), DTO record + Jakarta Bean Validation, PostgreSQL 16, migration Flyway
- Frontend: React 18, TypeScript (strict), Ant Design 5, state TanStack React Query 5
- Mobile: Flutter (chưa khởi tạo)
- Service `ai-service`: Python FastAPI (chưa khởi tạo)
- Stack pack: `.claude/stack-packs/backend-spring-boot-flyway.md`, `.claude/stack-packs/frontend-react-antd-react-query.md`, `.claude/stack-packs/mobile-flutter.md`, `.claude/stack-packs/backend-fastapi-sqlalchemy.md` (ai-service)

Chi tiết: [PROJECT/stack-and-commands.md](../../PROJECT/stack-and-commands.md)

## 8. Quy tắc chung

- Error handling: Service ném ApiException (HttpStatus + mã lỗi + thông báo tiếng Việt), GlobalExceptionHandler trả body {code, message, fields} — xem [PROJECT/conventions.md](../../PROJECT/conventions.md)
- Feature impl order: theo `PROJECT/conventions.md` § Feature Implementation Order (kiểu `SQL versioned`: entity và
  migration đổi trong cùng commit)
- Output artifacts: `docs/` only
- Tìm code: theo CLAUDE.md §Tìm code.

# Bản đồ module

Module nào nằm ở đâu, giữ bảng nào và phục vụ use case nào. Cập nhật file này khi thêm module, bảng hoặc khu web mới.

- Use case: [sơ đồ use case bản 2](https://claude.ai/artifact/1Dh4EEN2xN14GKx4vt8pcV). Mã use case theo vai trò:
  `G`/`P` khách & bệnh nhân, `D` bác sĩ, `R` lễ tân, `DS` dược sĩ, `A` quản trị.
- Bảng và cột: [thiết kế CSDL](https://claude.ai/artifact/MyPagR729Fb9aD5ZvVPP2g) (29 bảng), migration
  `backend/src/main/resources/db/migration`, ý nghĩa từng cột ở Javadoc của entity.

## Backend (`backend/src/main/java/vn/medischedule/<module>`)

Mỗi module là một package có `domain` (entity) / `repository` / `service` / `web` (controller) / `dto` (record); phạm
vi ghi ở `package-info.java`. Module khác chỉ gọi qua service, không dùng repository của nhau (`AGENTS.md`).

| Module | Bảng (entity trong `<module>/domain`) | Use case | Hiện trạng |
|---|---|---|---|
| `auth` | `roles` (Role), `users` (User) | P0-A, P0-B, D0-A, D0-B, A0, A1a, A1b (tài khoản) | Có code: đăng nhập, đăng ký, JWT, quản lý tài khoản |
| `cms` | `site_content` (SiteContent) | Trang chủ, trang đăng nhập | Có code |
| `storage` | Không có bảng (file ở MinIO, bucket `medical-files`) | Upload, đọc file | Có code |
| `clinic` | `specialty`, `room`, `staff` | A1b, A2 | Có code: danh mục chuyên khoa, phòng khám (admin + API public) |
| `patient` | `patient` | G1, P0-A, P1, R1, A1a | Mới có entity |
| `schedule` | `shift`, `shift_request` | A3, D4 | Mới có entity |
| `booking` | `appointment`, `voucher` | G1, P1, P2, P4, R1, R3, R4, D3 | Mới có entity |
| `emr` | `medical_record`, `lab_order` | D1, D5, P3 | Mới có entity |
| `pharmacy` | `drug`, `drug_batch`, `prescription`, `prescription_item` | D2, DS1–DS3, A2 | Mới có entity |
| `billing` | `service` (MedicalService), `invoice`, `invoice_item` | R2, A2, DS3 | Mới có entity |
| `aftercare` | `feedback`, `care_plan`, `health_measurement` | P5, P6, D6, A6 | Mới có entity |
| `commission` | `commission_policy`, `doctor_commission` | A5, D7 | Mới có entity |
| `notification` | `notification` | P1, P4, R3 (gửi Push / Zalo / SMS) | Mới có entity |
| `system` | `system_setting`, `audit_log` | A4 | Mới có entity |
| `ai` | `ai_analysis` | Luồng AI: ảnh da liễu, chỉ số xét nghiệm, lời khuyên LLM | Mới có entity; gọi `ai-service/` |
| `common` | Không có bảng | `config` (AppProperties, MinIO, OpenAPI), `exception` (ApiException) | Có code |

## Web (`web/src`)

Chia theo vai trò, mỗi khu một thư mục `features/<khu>` export route từ `index.ts`, gắn vào `app/router.tsx` bằng
`lazy()`. Dùng chung ở `shared/` (api, theme, ui).

| Vai trò (tab use case) | Khu web | Gọi module backend | Hiện trạng |
|---|---|---|---|
| Khách (chưa đăng nhập) | `features/landing` | cms, booking, clinic | Có code: trang giới thiệu, BookingWidget |
| Mọi vai trò | `features/auth`, `features/home` | auth | Có code: đăng nhập, đăng ký, trang sau đăng nhập |
| Khách & Bệnh nhân | `features/patient` | patient, booking, emr, aftercare, ai, notification | Chưa tạo |
| Bác sĩ | `features/doctor` | schedule, booking, emr, pharmacy, aftercare, commission, ai | Chưa tạo |
| Lễ tân | `features/reception` | booking, patient, billing, schedule | Chưa tạo |
| Dược sĩ | `features/pharmacy` | pharmacy, billing | Chưa tạo |
| Quản trị | `features/admin` (`/admin/*`) | auth, cms, clinic, schedule, billing, commission, aftercare, system | Có code: layout, tài khoản (`staff`), trang chủ (`homepage`), chuyên khoa (`specialty`) |

## Ngoài backend và web

| Thư mục | Nội dung | Hiện trạng |
|---|---|---|
| `mobile/` | App Flutter cho bệnh nhân | Chưa khởi tạo |
| `ai-service/` | Python FastAPI: Image AI, Data AI, LLM | Chưa khởi tạo |
| `infra/` | Script khởi tạo Postgres | Có |
| `backend/src/main/resources/db/migration` | Flyway `V*`: V1–V4 tài khoản và trang chủ, V5 users (Google, quên mật khẩu), V6 26 bảng nghiệp vụ | Có |

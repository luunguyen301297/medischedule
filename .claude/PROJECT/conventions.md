# Conventions

> Đọc khi: implement code mới (backend hoặc frontend), cần biết error handling pattern hoặc thứ tự làm feature.
> Bỏ qua nếu chỉ đọc/phân tích code không viết mới.

Quy ước code của dự án ghi ở `AGENTS.md` (mục "Quy ước backend", "Quy ước web") — đó là nguồn sự thật, ưu tiên hơn
file này; file này giữ khung quy ước chung của quy trình. Lệnh, ví dụ code và bẫy riêng của công cụ: chi tiết theo
stack: xem `.claude/stack-packs/backend-spring-boot-flyway.md` (backend) và
`.claude/stack-packs/frontend-react-antd-react-query.md` (frontend).

## Error Handling

**Convention**: Service ném ApiException (HttpStatus + mã lỗi + thông báo tiếng Việt), GlobalExceptionHandler trả body {code, message, fields}

- Lỗi nghiệp vụ ném `ApiException` ở **service** (`ApiException.badRequest/notFound/unauthorized(...)` hoặc
  `new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "...")`); controller mỏng, để exception lan lên.
- `GlobalExceptionHandler` (`vn.medischedule.common.exception`) trả `{code, message, fields}`: `ApiException` → status
  của nó; `@Valid` sai → 400 `VALIDATION_ERROR` + `fields`; `AccessDeniedException` → 403 `FORBIDDEN`. Mã lỗi (`code`)
  là chuỗi ổn định cho FE xử lý theo trường hợp (`errorCode` trong `http.ts`).
- Lỗi nghiệp vụ phải ra mã 4xx kèm lý do; không để exception chưa bắt lọt thành 500 (exception không có handler —
  `IllegalArgumentException`, `DataIntegrityViolationException`… — là 500: kiểm trước rồi ném `ApiException`).

## Backend (Java 21 / Spring Boot 3)

- Full type hints / kiểu tường minh ở mọi chỗ ngôn ngữ cho phép.
- Business logic trong service ở `backend/src/main/java/vn/medischedule/<module>/service`; controller (`web`) giữ mỏng.
- Format / lint: chưa có công cụ — giữ style file đang sửa (thụt lề, import tường minh, Lombok như code hiện có); không
  tự thêm plugin format.
- Type check / compile: `(cd backend && ./mvnw -q -DskipTests compile)`
- Dùng entry point settings duy nhất (`AppProperties`) — không tự khởi tạo object settings ở chỗ khác.
- **Session / transaction của `Spring Data JPA (Hibernate)`:** `open-in-view: false` ⇒ không đọc quan hệ LAZY ngoài
  transaction (vd khi map DTO trong controller) — nạp trong service `@Transactional` (fetch join / `@EntityGraph`) hoặc
  map sang DTO ngay trong transaction. Cách làm cụ thể và lỗi đi kèm: stack pack backend §3.
- Response chỉ trả field đã khai báo trong schema DTO record + Jakarta Bean Validation (record không có field thì JSON
  không có) — kiểm record backend, không tin type FE.

## Frontend (TypeScript (strict) / React 18)

- Strict mode của TypeScript luôn bật (`web/tsconfig.json` `strict: true`).
- UI kit chuẩn: `Ant Design 5`; không có CSS utility — CSS riêng của khu đặt cạnh feature, dùng `var(--ms-*)`.
- Mọi API call đi qua module trong `web/src/features/<khu>/api.ts` (dựa trên `http` của `@/shared/api/http`), không gọi
  inline trong component/page.
- Quy ước giao diện (màu chữ, shadow, nền…): `.claude/skills/ui-ux-pro-max/references/project-ux-spec.md` § "Quy ước UI mặc định" — sửa ở đó, không ghi lại
  ở đây.

## Feature Implementation Order

1. Define schemas / DTO (`backend/src/main/java/vn/medischedule/**/dto`)
2. Define models / entity (`backend/src/main/java/vn/medischedule/**/domain`).
   - **Kiểu `SQL versioned`:** entity không sinh migration — đổi entity và viết SQL ở bước 3 trong cùng commit.
3. Viết migration bằng `Flyway` vào `backend/src/main/resources/db/migration`:
   - **Kiểu `SQL versioned`** (Flyway): viết SQL tay đúng với entity vừa đổi (đọc entity rồi viết, review
     như code); không dùng chế độ "ORM tự tạo/sửa bảng" để lấy DDL. Entity và migration đổi trong **cùng commit** —
     app kiểm schema lúc khởi động (`ddl-auto: validate`), lệch nhau ⇒ app không chạy.
   - Cột NOT NULL thêm vào bảng có dữ liệu cần default hoặc bước backfill; dữ liệu bắt buộc (danh mục, vai trò) đi trong
     migration `V*`, dữ liệu demo tách riêng ở seed `R__*.sql` (`db/seed`, phải idempotent và không ghi đè dữ liệu user
     đã sửa); migration đã áp ở DB dùng chung / đã lên nhánh gốc thì không sửa — viết migration mới.
   - **Đánh số revision/version:** `V<n>__mo_ta.sql`, n = cao nhất trên `origin/main` + 1 (kiểm lại ngay trước commit).
     Đổi id/số của migration đã áp trên DB nào thì phải ghi nhận lại trong lịch sử migration của mọi DB đang trỏ tới —
     kiểm `DB_URL` trước (HARD-STOPS).
   - Index/constraint đặc thù `PostgreSQL 16` (vd partial unique index): ghi trong SQL và chú thích ở entity.
     Extension cần cho migration: `CREATE EXTENSION IF NOT EXISTS` ngay trong `V*` (script `infra/postgres/init.sql`
     chỉ chạy khi tạo volume lần đầu).
   - Không strip drift "unrelated" im lặng — include trong PR hoặc tách PR reconciliation riêng.
   - Kiểm drift: `spring.jpa.hibernate.ddl-auto: validate`
   - Lệnh, cờ và bẫy của `Flyway`: `.claude/stack-packs/backend-spring-boot-flyway.md`.
4. Implement service (`backend/src/main/java/vn/medischedule/<module>/service`)
5. Add route / controller (`backend/src/main/java/vn/medischedule/<module>/web`)
6. Wire frontend service (`web/src/features/<khu>/api.ts`)
7. Connect page to live data

## Vùng rủi ro cao

**Cách lập danh sách:** agent **đề xuất** dòng dựa trên code (thư mục/file xử lý tiền, giá, thanh toán, phân quyền,
migration…), ghi `chờ xác nhận` ở cột Trạng thái, rồi **hỏi user xác nhận** (thêm/bớt/sửa). Chỉ dòng `đã xác nhận`
mới là chính thức: reviewer và QA chỉ áp quy tắc RỦI RO CAO bắt buộc cho các dòng đó; dòng `chờ xác nhận` thì nêu
trong báo cáo để user quyết.

Diff chạm vào vùng trong bảng dưới là **RỦI RO CAO**: code review phải có thêm reviewer độc lập (subagent
`code-reviewer` không biết design — ORCHESTRATION §2) và QA bắt buộc có test tự động cho vùng đó. Dự án có script
chọn test theo diff hỗ trợ cờ RỦI RO CAO (xem `.claude/scripts/README.md`) thì cấu hình nó theo danh sách này; không
có thì người review tự đối chiếu diff với danh sách.

| Vùng (glob / từ khoá path) | Vì sao rủi ro | Trạng thái |
|---|---|---|
| `backend/src/main/java/vn/medischedule/billing/**`, `**/commission/**` | tiền / giá: bảng giá, hoá đơn, thanh toán, hoa hồng bác sĩ | chờ xác nhận |
| `backend/src/main/java/vn/medischedule/auth/**`, `**/*Controller.java` (`@PreAuthorize`), `web/src/app/guards.tsx` | phân quyền: JWT, `SecurityConfig`, role | chờ xác nhận |
| `backend/src/main/java/vn/medischedule/booking/**`, `**/schedule/**` | đặt lịch / slot / hàng đợi: đọc-rồi-ghi cùng slot (race) | chờ xác nhận |
| `backend/src/main/java/vn/medischedule/emr/**`, `**/pharmacy/**`, `**/patient/**`, `**/storage/**` | dữ liệu y tế cá nhân, đơn thuốc / kho theo lô, file y tế | chờ xác nhận |
| `backend/src/main/resources/db/seed/**`, `scripts/export_seed.py` | seed dev chạy lại khi checksum đổi — ghi đè dữ liệu đã sửa ở máy dev | chờ xác nhận |
| `backend/src/main/resources/db/migration/**` | migration / schema | đã xác nhận (mặc định template) |

# Stack pack — Backend Spring Boot 3 + Spring Data JPA + Flyway + PostgreSQL + JUnit 5 (Maven)

Dùng khi backend là: Java 17/21, Spring Boot 3 (web, security, data-jpa, validation), Hibernate, Flyway, PostgreSQL,
JUnit 5 qua `spring-boot-starter-test`, build bằng Maven wrapper (Gradle: đổi lệnh tương ứng). Bổ sung cho
`backend-engineer`, `database-engineer`, `qa-engineer`, `team-code-reviewer`. File chung (`PROJECT/*.md`, `WORKFLOW.md`,
`HARD-STOPS.md`, `LESSONS.md`) ghi quy tắc trung lập; pack này giữ phần **cụ thể cho stack**.

Mục ghi **[kiểm dự án]** là chỗ các dự án Spring hay làm khác nhau — đọc code/cấu hình thật rồi ghi quy ước đã chốt vào
`PROJECT/*.md`, đừng coi giá trị ở đây là mặc định.

Giá trị khoá placeholder cho stack này (khoá = tên placeholder trong `PROJECT/stack-and-commands.md`, bỏ ngoặc nhọn kép).
Khi pack này dùng cho **service phụ**: thay các placeholder còn trong pack bằng giá trị của service đó trước — xem
`stack-packs/README.md` § "Pack cho service phụ".

Cột "Giá trị" là **giá trị thuần** — chép thẳng vào cột "Giá trị dự án" của bảng stack; cột "Ghi chú" giải thích, không
chép vào giá trị. Mọi giá trị `*_COMMAND` chạy được từ gốc repo: đã gồm `cd backend` và bước nạp `.env` khi
cần (Spring Boot không tự đọc — §1).

| Khoá | Giá trị | Ghi chú |
|---|---|---|
| `BACKEND_LANG` | Java 21 | |
| `BACKEND_FRAMEWORK` | Spring Boot 3 | |
| `SCHEMA_LIB` | DTO record + Jakarta Bean Validation | |
| `ORM` | Spring Data JPA (Hibernate) | |
| `DB_ENGINE` | PostgreSQL | |
| `DB_URL_ENV_VAR` | `DB_URL` | **[kiểm dự án]** biến mà `application.yml` đọc cho `spring.datasource.url`; không đặt riêng thì Spring đọc `SPRING_DATASOURCE_URL` |
| `MIGRATION_TOOL` | Flyway | |
| `MIGRATION_STYLE` | SQL versioned | |
| `MIGRATION_ON_STARTUP` | có | Flyway chạy khi Spring context khởi động — cả app lẫn `@SpringBootTest` |
| `MIGRATION_COMMAND` | `(set -a; . ./.env; set +a; cd backend && SPRING_FLYWAY_TARGET=<n> ./mvnw spring-boot:run)` | Flyway không có lệnh riêng — áp khi khởi động. `spring-boot:run` **không tự thoát**: chạy nền (`run_in_background`), chờ log Flyway `Successfully applied …` / `Schema … is up to date` rồi dừng app. Dự án có wrapper (vd `scripts/run-backend.*`) thì dùng wrapper |
| `MIGRATION_DIR` | `backend/src/main/resources/db/migration` | |
| `MIGRATION_DRIFT_CHECK` | `spring.jpa.hibernate.ddl-auto: validate` | app không khởi động khi entity lệch schema |
| `MIGRATION_CHAIN_TEST` | `chưa có` | không cần — Flyway tự báo version trùng / out-of-order khi khởi động |
| `TEST_FRAMEWORK` | JUnit 5 + Spring Boot Test | kèm Mockito, AssertJ, spring-security-test |
| `TEST_COMMAND` | `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test)` | unit. Test tích hợp: `INTEGRATION_TEST_COMMAND`, không ghép biến DB vào lệnh này |
| `TEST_SINGLE_COMMAND` | `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test#<hàm>)` | |
| `INTEGRATION_TEST_COMMAND` | `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)` | nạp `.env` trước, đặt biến DB test sau (§7); bản PowerShell ở §7 |
| `TEST_SELECT_COMMAND` | chọn thủ công theo package/module bị sửa | lớp test cùng package + lớp gọi nó |
| `UNIT_TEST_DIR` | `backend/src/test/java/<package>/*Test.java` | |
| `INTEGRATION_TEST_DIR` | `backend/src/test/java/<package>/*IntegrationTest.java` | |
| `KNOWN_FLAKY_FILE` | `backend/src/test/KNOWN_FLAKY.txt` | |
| `TEST_DB_ENV_VAR` | `DB_URL` | **[kiểm dự án]** giai đoạn 1 (chưa có profile test): bằng giá trị `DB_URL_ENV_VAR`; dự án đã có / vừa dựng profile test đọc `TEST_DB_URL` không default (giai đoạn 2) thì điền `TEST_DB_URL` — §7 "Biến URL DB test theo giai đoạn". Ở giai đoạn 1 giá trị trùng biến của app: khi lên giai đoạn 2 làm theo `PROJECT/testing.md` (tìm theo tên biến, xét từng dòng) |
| `BE_LINT_COMMAND` | `(cd backend && ./mvnw spotless:apply)` | Checkstyle nếu pom có; không có plugin nào thì `chưa có` |
| `BE_TYPECHECK_COMMAND` | `(cd backend && ./mvnw -q -DskipTests compile)` | |
| `SERVICE_LAYER_GLOB` | `backend/src/main/java/**/service/*.java` | theo module: `**/<module>/service/*.java` |
| `API_LAYER_GLOB` | `backend/src/main/java/**/controller/*.java` | hoặc `**/web/*.java` |

---

## 1. Cấu trúc & cấu hình (← `PROJECT/architecture.md`, `PROJECT/conventions.md`)

- **[kiểm dự án]** Package theo tầng (`controller/service/repository/entity`) hay theo module
  (`<base>.<module>.{domain,repository,service,web,dto}`). Theo module thì thường cấm gọi chéo repository giữa module —
  ghi vào `PROJECT/architecture.md` § Module boundary.
- Route khai báo bằng annotation (`@RestController` + `@RequestMapping`), Spring tự quét theo package — không có file
  đăng ký router; controller nằm ngoài package được quét thì endpoint không tồn tại.
- Cấu hình typed: `@ConfigurationProperties(prefix = "app")` (record lồng). Thêm khoá mới: field record + khoá trong
  `application.yml` dạng `${ENV_VAR:default}` + dòng trong `.env.example`.
- `application.yml` đọc biến môi trường; biến **không có default** (mật khẩu DB, secret JWT) ⇒ chạy app/test khi chưa
  nạp `.env` sẽ lỗi khởi động. Spring Boot **không tự đọc `.env`** — `mvnw spring-boot:run` / `mvnw test` chạy trần sẽ
  thiếu biến hoặc rơi về default. Cách nạp (chọn một, ghi lệnh đã chốt vào `PROJECT/stack-and-commands.md` § Backend):
  - **Nạp `.env` inline trong cùng lệnh** (không cần file script). Bash, chạy từ gốc repo:
    ```bash
    (set -a; . ./.env; set +a; cd backend && ./mvnw spring-boot:run)
    ```
    PowerShell (biến đặt vào `$env:` còn lại trong phiên shell — đặt lại mỗi lệnh):
    ```powershell
    Get-Content .\.env | Where-Object { $_ -match '^\s*[^#=\s][^=]*=' } | ForEach-Object {
      $k, $v = $_ -split '=', 2; Set-Item "env:$($k.Trim())" $v.Trim().Trim('"') }
    Set-Location backend; .\mvnw spring-boot:run
    ```
  - **Script wrapper sẵn có của dự án** (vd `scripts/run-backend.*`, target `make`): dùng đúng script đó, ghi tên vào
    `PROJECT/stack-and-commands.md`. Repo chưa có wrapper thì tạo từ mẫu `scripts/run-backend.ps1` ở `.claude/scripts/README.md` (mục launch.json);
    đừng trỏ tới file chưa có trong repo.
  - `spring.config.import=optional:file:../.env[.properties]`: không cần nạp ngoài, nhưng `.env` phải đúng cú pháp
    properties (nháy kép không bị bỏ, không `export`) và đường dẫn tính từ thư mục chạy.
  - Plugin IDE (EnvFile…) chỉ có tác dụng khi chạy trong IDE — agent chạy bằng shell vẫn cần một trong các cách trên.
- **[kiểm dự án]** `application.yml` hay có default URL trỏ DB dev (`url: ${DB_URL:jdbc:postgresql://<dev-host>/app}`):
  chạy không đặt biến ⇒ app/test kết nối DB dev **và Flyway áp migration lên DB dev**. Kiểm theo HARD-STOPS trước mỗi
  lần chạy; với test xem §7.
- Nên đặt `spring.jpa.open-in-view: false` và `ddl-auto: validate` (bẫy ở §3).
- Không đặt logic trong `@Configuration`; bean phụ thuộc vòng → tách service.
- Không có `spring-boot-devtools` ⇒ chạy dev không auto-reload: sửa BE xong nhắc user restart.

## 2. Error handling (← `PROJECT/conventions.md` § Error Handling)

Mẫu phổ biến: một exception nghiệp vụ mang `HttpStatus` + mã lỗi, và một `@RestControllerAdvice` chuyển thành body
lỗi thống nhất (vd `{code, message, fields}`); `@Valid` sai → 400 kèm danh sách field.

```java
// service
if (users.existsByEmailIgnoreCase(email)) {
    throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email đã được sử dụng");
}
return users.findById(id).orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Không tìm thấy tài khoản"));
```

Bẫy:
- Exception không có handler (`IllegalArgumentException`, `IllegalStateException`, `DataIntegrityViolationException`,
  `EntityNotFoundException`…) ⇒ 500. Kiểm trước (`existsBy…`) rồi ném exception nghiệp vụ, hoặc thêm handler có chủ
  đích (đổi contract lỗi chung → ghi trong design).
- Lỗi 401/403 ở tầng filter của Spring Security **không** đi qua `@RestControllerAdvice` — phải cấu hình
  `AuthenticationEntryPoint` / `AccessDeniedHandler` ghi cùng định dạng body.
- `@Valid` trên `@RequestParam`/`@PathVariable` cần `@Validated` ở controller và ném `ConstraintViolationException` —
  không có handler thì thành 500. Ưu tiên validate qua DTO body, hoặc thêm handler.

## 3. JPA / Hibernate — bẫy (← `PROJECT/conventions.md`, `PROJECT/qa-process.md` cổng mục 5)

- **`open-in-view: false`** ⇒ truy cập quan hệ `LAZY` sau khi transaction đóng (vd trong controller khi map DTO) ném
  `LazyInitializationException` → 500. Nạp trong service `@Transactional` bằng fetch join / `@EntityGraph`, hoặc map
  sang DTO ngay trong transaction. Quan hệ `EAGER` che lỗi này nhưng gây N+1 — đừng đổi sang `EAGER` để "sửa".
- **`ddl-auto: validate`**: entity lệch schema (thiếu bảng/cột, sai kiểu cột) ⇒ app **không khởi động**. Entity và
  migration đổi cùng commit. Không dùng `ddl-auto: update/create` ngoài môi trường thử nghiệm.
- N+1: list có quan hệ ⇒ `@EntityGraph` / `join fetch`; phân trang + fetch join collection ⇒ Hibernate phân trang trong
  bộ nhớ (cảnh báo HHH90003004) — dùng hai bước (trang id rồi fetch).
- Đọc-rồi-ghi trên cùng bản ghi (slot, tồn kho, số thứ tự, số dư) cần khoá: `@Lock(PESSIMISTIC_WRITE)` trên query
  repository, hoặc `@Version` (optimistic) + bắt `OptimisticLockException` → lỗi 409.
- `@Transactional` chỉ có hiệu lực qua proxy: gọi hàm `@Transactional` từ chính lớp đó (self-call) không mở
  transaction mới; hàm `private` không được proxy.
- Thời điểm: `OffsetDateTime`/`Instant` ↔ `TIMESTAMPTZ`; đặt `hibernate.jdbc.time_zone` tường minh.
- JSONB: map bằng `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6) — xem cột JSONB có sẵn trong code trước khi thêm mới.

## 4. DTO & validation (← `LESSONS.md` § Frontend)

- DTO là `record`, response dựng bằng factory `of(entity)`; không trả entity ra controller.
- Response chỉ có field khai trong record ⇒ type FE có field mà record không có thì JSON không có — kiểm record backend.
- `Page<T>` của Spring Data: dạng JSON phụ thuộc version/cấu hình (Spring Data 3.3+ khuyến nghị `PagedModel` /
  `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`) — FE đọc theo dạng nào thì giữ dạng đó; đổi là đổi
  contract.

## 5. Flyway & migration (← `PROJECT/conventions.md` § Feature Implementation Order, `WORKFLOW.md` §3.6,
`HARD-STOPS.md`, `LESSONS.md`)

**Tạo migration**
- File `backend/src/main/resources/db/migration/V<n>__<mo_ta>.sql` (hai dấu gạch dưới, mô tả snake_case). Không sửa migration đã chạy.
- SQL viết tay (Flyway không sinh từ entity): đọc entity rồi viết DDL, review như code; không dùng `ddl-auto: update`
  để "lấy DDL".
- `n` = version cao nhất trên `origin/<nhánh dev>` + 1, kiểm lại ngay trước commit: phiên song song cùng lấy `V5` ⇒
  Flyway báo "Found more than one migration with version 5"; `V6` của phiên khác đã áp trước `V5` của bạn ⇒ lỗi
  out-of-order (`outOfOrder` mặc định false).
- Dữ liệu bắt buộc (danh mục, vai trò…) trong `V*`; dữ liệu demo dev tách thành repeatable `R__*.sql` ở location riêng
  chỉ nạp ở dev. `R__` chạy lại mỗi khi checksum đổi ⇒ phải idempotent (`ON CONFLICT`, `WHERE NOT EXISTS`).
- Môi trường thật: `spring.flyway.locations` chỉ trỏ thư mục `V*` (không nạp seed dev).
- Extension (`pg_trgm`, `unaccent`, `pgcrypto`…): `CREATE EXTENSION IF NOT EXISTS` ngay trong `V*` cần nó (script init
  của container chỉ chạy lần đầu tạo volume).

**Chạy migration**
- Không có lệnh riêng: Spring context khởi động (`spring-boot:run`, `@SpringBootTest`) ⇒ Flyway áp **mọi** `V*` trong
  checkout (kể cả file chưa commit của phiên khác) lên DB của datasource. Áp tới version cụ thể: `SPRING_FLYWAY_TARGET=<n>`
  inline.
- Checksum: sửa file `V*` đã áp ⇒ "Migration checksum mismatch" ⇒ app không khởi động. Không `flyway repair` khi chưa
  hỏi user.
- Flyway community **không có undo**; rollback = migration mới đảo ngược.
- Kiểm DB thật: `flyway_schema_history` có dòng `success = true` chưa đủ — kiểm `information_schema.columns`/`tables`.
- `./mvnw flyway:migrate` chỉ chạy được khi pom có `flyway-maven-plugin`; thêm plugin là quyết định riêng.

**Hook**
- `context_guard.py` (cấu hình trong `.claude/settings.json` / biến môi trường, xem `.claude/scripts/README.md`): đặt
  mẫu lệnh khớp `mvnw flyway:migrate|clean|baseline|repair|undo`, `flyway migrate|…` (migration guard — **chặn** khi DB
  đích không phải DB tạm) và biến URL DB của stack. Lệnh khởi động app / test (`spring-boot:run`, `mvnw test|verify`,
  wrapper `run-backend.*` — Flyway chạy ngầm khi khởi động) do **startup guard** (`GUARD_STARTUP_CMD_REGEX`) bắt: chỉ
  **cảnh báo**, không chặn, khi DB đích ở máy khác danh sách cho phép; **test-db guard** (`GUARD_TEST_CMD_REGEX`) cảnh
  báo lệnh test có DB đích không tên `qa_*`, kể cả localhost (DB dev và `qa_*` cùng Postgres Docker local). Guard đọc
  URL từ đối số (`-Dflyway.url=`, `--spring.datasource.url=`…), gán trong lệnh, biến môi trường rồi `.env` — lệnh tự
  nạp `.env` (`. ./.env`, wrapper khai ở `GUARD_ENV_WRAPPER_REGEX`) thì `.env` thắng môi trường và phép gán đứng trước
  bước nạp. Cảnh báo không thay được việc tự kiểm biến URL DB theo HARD-STOPS trước mỗi lần chạy app/test.

## 6. Spring Security (← `PROJECT/auth.md`)

- `@EnableMethodSecurity` + `@PreAuthorize("hasRole('X')")` (role trong token là `X`, Spring thêm tiền tố `ROLE_`); dùng
  hằng thay chuỗi rải rác.
- **[kiểm dự án]** Endpoint public: danh sách path `permitAll()` trong `SecurityFilterChain` hoặc prefix riêng (vd
  `/api/public/**`). Path đã chặn theo URL vẫn nên ghi `@PreAuthorize` trên controller cho rõ.
- Người đăng nhập: `@AuthenticationPrincipal`. **[kiểm dự án]** Principal dựng từ claim JWT (không tra DB) ⇒ khoá tài
  khoản / đổi role chưa có hiệu lực tới khi token hết hạn — lựa chọn ghi ở `PROJECT/auth.md`.
- CORS: danh sách origin từ biến môi trường, khớp cổng dev server FE; header cho phép tối thiểu (`Authorization`,
  `Content-Type`).
- Test phân quyền: `MockMvc` + `@WithMockUser(roles = "X")` hoặc token thật từ service JWT; mỗi endpoint mới có ca 401
  (không token) và 403 (sai role).

## 7. Test JUnit / Maven (← `PROJECT/testing.md`, `PROJECT/qa-process.md`, `CODING-GUIDELINES.md` §6)

- Lệnh (trong `backend/`, sau khi nạp `.env` — §1): `./mvnw -q test -Dtest=FooServiceTest`, nhiều lớp `-Dtest='FooTest,Bar*Test'`, một hàm
  `-Dtest=FooTest#tenHam`. PowerShell: bọc nháy `"-Dtest=FooTest"`. Gom mọi lớp vào **một** lệnh (mỗi lần gọi trả chi
  phí JVM + compile + Spring context).
- Không có test khớp `-Dtest` ⇒ Surefire báo "No tests were executed" — thêm `-Dsurefire.failIfNoSpecifiedTests=false`
  chỉ khi chủ đích.
- Tên lớp: unit `*Test`, integration `*IntegrationTest` (Surefire mặc định chạy `*Test`, `*Tests`, `Test*`, `*TestCase`;
  hậu tố `*IT` cần `maven-failsafe-plugin` và chạy bằng `verify`).
- Chạy lại test đỏ: Surefire không có cờ kiểu "last failed" — liệt kê đúng lớp/hàm đỏ trong `-Dtest`.
- Unit: JUnit 5 + AssertJ, không Spring context; kiểm lỗi bằng
  `assertThatThrownBy(...).isInstanceOf(ApiException.class).extracting("code").isEqualTo("EMAIL_EXISTS")`.
- Integration: `@SpringBootTest` + `@AutoConfigureMockMvc`, gọi endpoint qua `MockMvc` theo đúng luồng thật (cổng kịch
  bản mục 5). **Không đặt `@Transactional` trên lớp integration test gọi HTTP**: transaction của test bao luôn request
  ⇒ lazy-load "chạy được" trong test nhưng 500 khi chạy thật; dọn dữ liệu bằng DB `qa_*` riêng.
- DB: URL lấy bằng `python .claude/scripts/qa_db.py url <task>` (chạy từ gốc repo; `qa_db.py` cần cấu hình in JDBC URL —
  xem `.claude/scripts/README.md`). Lệnh hoàn chỉnh = giá trị `INTEGRATION_TEST_COMMAND`; thứ tự bắt buộc: **nạp `.env`
  trước** (user/mật khẩu DB, secret), **đặt URL test sau** (ghi đè URL dev trong `.env`), trong **cùng một lệnh**:
  ```bash
  (set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest='FooIntegrationTest,BarIntegrationTest')
  ```
  PowerShell (biến `$env:` còn lại trong phiên shell — chạy lại cả lệnh mỗi lần):
  ```powershell
  Get-Content .\.env | Where-Object { $_ -match '^\s*[^#=\s][^=]*=' } | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item "env:$($k.Trim())" $v.Trim().Trim('"') }; $env:DB_URL = (python .claude/scripts/qa_db.py url <task>); Push-Location backend; .\mvnw -q test "-Dtest=FooIntegrationTest,BarIntegrationTest"; Pop-Location
  ```
  **Cấm:** `DB_URL=$(…) (set -a; . ./.env; …)` (gán tiền tố trước subshell — lỗi cú pháp) và
  `export DB_URL=…` / `$env:DB_URL = …` **trước** bước nạp `.env` (`.env` ghi đè lại thành URL
  dev). Dự án có script wrapper (§1) tự nạp `.env` thì wrapper phải nhận biến test **sau** bước nạp của nó (vd tham số
  `-TestDbUrl`), không đặt biến trước khi gọi wrapper.
- **Biến URL DB test theo giai đoạn** (giá trị `TEST_DB_ENV_VAR`; cấu hình hook tương ứng ở `.claude/scripts/README.md`
  § Java):
  - **Giai đoạn 1 — chưa có profile test** riêng (dự án mới, chưa có test): app chỉ đọc `DB_URL_ENV_VAR`, nên
    `TEST_DB_ENV_VAR` = chính biến đó (vd `DB_URL`): `INTEGRATION_TEST_COMMAND` có `export DB_URL="$(python
    .claude/scripts/qa_db.py url <task>)"` sau bước nạp `.env`. **Không** đặt `TEST_DB_URL=…` ở giai đoạn này: app
    không đọc biến đó ⇒ test và Flyway chạy lên `DB_URL` trong `.env` (DB dev). `GUARD_MIGRATION_DB_URL_VAR` **không**
    chứa `TEST_DB_URL` ⇒ lệnh như vậy bị guard cảnh báo khi `DB_URL` (theo thứ tự bộ nạp `.env` của guard) trỏ máy khác
    hoặc (test-db guard) DB không phải `qa_*`. Quên đặt biến thì test cũng chạy lên DB
    trong `.env` — kiểm biến theo HARD-STOPS trước **mỗi** lệnh `mvnw test`.
  - **Giai đoạn 2 — đã dựng profile test** (`src/test/resources/application-test.yml` + `@ActiveProfiles("test")`,
    hoặc `src/test/resources/application.yml`) đọc URL từ `TEST_DB_URL` **không default** (`url: ${TEST_DB_URL}`) ⇒
    thiếu biến thì context không khởi động. Khi đó: đổi `TEST_DB_ENV_VAR` thành `TEST_DB_URL` (bảng stack + mọi lệnh
    test đã điền), và đưa `TEST_DB_URL` lên **đầu** `GUARD_MIGRATION_DB_URL_VAR` trong `.claude/settings.json`
    (`TEST_DB_URL,FLYWAY_URL,DB_URL,SPRING_DATASOURCE_URL`), sửa ca tương ứng trong `hook_cases.json`.
- **Cảnh báo DB dev:** `application.yml` có default URL trỏ DB dev, hoặc `.env` đặt cùng biến tới DB dev ⇒ quên đặt
  biến test (hoặc đặt sai thứ tự) là `@SpringBootTest` chạy lên DB dev và Flyway áp mọi `V*` trong checkout lên đó.
  Phòng lâu dài: chuyển sang giai đoạn 2 (profile test không default).
- Vì Flyway chạy khi context khởi động, test tích hợp tự dựng schema trên `qa_<task>` — đúng ý.
- Testcontainers là lựa chọn thay `qa_db.py` khi máy có Docker (mỗi lớp/suite một container) — quyết định ở design.
- Output gọn: `-q`; stack trace dài ⇒ `| tail -60` hoặc đọc `backend/target/surefire-reports/<Lớp>.txt`.
- Hook Stop nhận lệnh test qua cấu hình regex (vd `mvnw … test|verify`, xem `.claude/scripts/README.md`) — chạy test
  bằng IDE thì hook vẫn nhắc; khi đó nói rõ đã chạy ở đâu.

## 8. Skill tham khảo & công cụ

- Template không kèm skill tham khảo Java/Spring — kiến thức stack nằm ở pack này. SQL/PostgreSQL và contract REST:
  skill tham khảo tương ứng trong `.claude/skills/_shared/` nếu có (danh sách: `.claude/skills/README.md`).
- Không có formatter/linter Java trong pom ⇒ giữ style file đang sửa (thụt lề, import tường minh, Lombok như code hiện
  có); không tự thêm plugin format.

## Rule Pack bổ sung (team-code-reviewer)

Bổ sung cho `.claude/skills/team-code-reviewer/references/rule-pack.md`. Reviewer kết luận từng dòng áp dụng cho file
trong diff. Sửa glob theo cấu trúc package thật.

| Vùng | Rule | Mức khi vi phạm |
|---|---|---|
| controller (`*/web/*`, `*/controller/*`) | Endpoint mới có `@PreAuthorize` đúng vai trò (hoặc nằm trong danh sách public có chủ đích); body có `@Valid` | **Critical** (thiếu phân quyền) / Major |
| controller, DTO | Controller/DTO đọc quan hệ LAZY ngoài transaction (`open-in-view: false`) ⇒ `LazyInitializationException` 500 | **Critical** |
| service | Ném exception không có handler cho lỗi nghiệp vụ dự kiến ⇒ 500 | Major |
| service | Đọc-rồi-ghi trên cùng bản ghi (slot, tồn kho, STT, số dư) không khoá dòng / không `@Version` | Major |
| service | Gọi repository của module khác (khi dự án cấm — `PROJECT/architecture.md`) | Major |
| entity + `db/migration/*` | Entity đổi mà không có migration tương ứng (hoặc ngược lại) ⇒ `validate` chặn khởi động | **Critical** |
| `db/migration/V*` | Sửa file `V*` đã có trên nhánh gốc; version trùng / không tăng | **Critical** |
| `R__*` (seed) | Seed không idempotent hoặc ghi đè dữ liệu user đã sửa | Major |

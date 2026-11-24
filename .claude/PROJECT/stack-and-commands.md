# Stack & Commands

> Đọc khi: cần setup môi trường, chạy dev/Docker, hoặc xác nhận version/thư viện trong stack. Bỏ qua nếu chỉ sửa
> logic nghiệp vụ trong code đã chạy sẵn.

## Stack

**Cách điền:**

- **Giá trị thô** (đường dẫn, tên công cụ, biến môi trường, nhánh): điền **không** backtick — tài liệu có thể đặt
  placeholder trong backtick hoặc ghép thêm đường dẫn phía sau (vd thư mục backend nối `/…`).
- **Giá trị markdown** — mọi placeholder có hậu tố `_COMMAND`, cùng `UNIT_TEST_DIR`, `INTEGRATION_TEST_DIR`,
  `MIGRATION_DRIFT_CHECK`, `MIGRATION_CHAIN_TEST`: điền **kèm** backtick (vd `` `./mvnw -q test -Dtest=<Lớp>` ``). Tài
  liệu luôn đặt các placeholder này **ngoài** backtick và ngoài khối code (mục "Lệnh" bên dưới là danh sách, không
  phải khối code). Giá trị thô không chứa backtick (vd `DTO record + Bean Validation`, không bọc `record`).
- **Giá trị thuần, chú thích để cột "Ghi chú":** cột "Giá trị dự án" chỉ chứa giá trị (một lệnh / đường dẫn / tên,
  hoặc `chưa có`) — không ngoặc giải thích, không "— §1", không "(trong `backend/`)". Giá trị chảy vào giữa câu, ngoặc
  và tiêu đề ở file khác; chú thích kèm theo sẽ thành ngoặc lồng hoặc trỏ sai mục. Lý do, điều kiện, biến thể… ghi ở
  cột "Ghi chú". Bảng khoá ở đầu stack pack theo cùng quy tắc (cột "Giá trị" thuần, cột "Ghi chú" riêng).
- **Mọi giá trị `*_COMMAND` là lệnh tự chạy được từ gốc repo:** tự vào đúng thư mục (`(cd <thư mục> && …)`,
  `npm --prefix <thư mục> …`, `-f <thư mục>/pom.xml`…) và tự nạp `.env` nếu runner không tự đọc (vd Spring Boot) —
  thư mục chạy và bước nạp `.env` nằm **trong** giá trị, không ở cột "Ghi chú". Tài liệu dùng placeholder như một lệnh
  trọn vẹn, không ghép thêm tiền tố/hậu tố (không `VAR=… <lệnh>`, không `cd x && <lệnh>`). Lệnh test tích hợp trên DB
  `qa_<task>` là placeholder riêng `INTEGRATION_TEST_COMMAND` (đã gồm bước đặt biến DB test, đúng thứ tự sau bước nạp
  `.env`) — không ghép `TEST_DB_ENV_VAR` vào `TEST_COMMAND`.
- **Tầng / công cụ chưa có** (không có mobile, chưa có linter, chưa có script kiểm FE, chưa có test runner…): ghi
  `chưa có` ở cột "Giá trị dự án" (luôn đúng chữ này, không viết "không có" / "không dùng") và **xoá cả bước/câu** dùng
  placeholder đó ở các file khác (các bước lệnh được viết thành gạch đầu dòng hoặc dòng riêng để xoá gọn) — không
  điền `chưa có` vào giữa câu. Có công cụ sau này thì thêm lại bước.
- **Tầng dự kiến, chưa khởi tạo:** placeholder của tầng vẫn điền **tên công nghệ** (vd Flutter) — trạng thái chỉ ghi ở
  bảng "Trạng thái tầng" dưới, không chèn vào giá trị (giá trị chảy vào câu văn ở file khác).
- **Chưa có môi trường prod:** `PROD_DB_HOST`, `PROD_SERVER` ghi `chưa có`; xoá câu dùng chúng (HARD-STOPS, comment
  hook) hoặc viết lại thành điều kiện "khi có prod: …".
- **Số đo chưa đo được** (vd `APP_IMPORT_SECONDS` khi repo chưa có test): ghi `chưa đo — đo ở task test đầu tiên`.
- `TEST_SELECT_COMMAND` chỉ nhận hai dạng: lệnh trong backtick, hoặc đúng chuỗi `chọn thủ công theo package/module bị
  sửa`. Mọi câu dùng nó có dạng "chọn test theo diff: …" nên đọc trôi với cả hai.
- Placeholder theo **từng module/capability** không nằm trong bảng này mà viết `<PREFIX>` (mã rule KB — bảng prefix ở
  `PROJECT/kdd.md`); đừng thay bằng một giá trị chung.
- Sau khi điền, lệnh dưới phải sạch — trừ cột Placeholder của các bảng trong file này. Lệnh bỏ qua thư mục skill tham
  khảo `.claude/skills/_shared/<tên-skill>/` và dữ liệu `.claude/skills/ui-ux-pro-max/data/` (chứa cặp ngoặc nhọn kép
  của JSX/template) nhưng vẫn quét `_shared/domain-context.md`, `_shared/project-conventions.md`; mẫu chỉ khớp
  placeholder có ký tự đầu tiên sau cặp ngoặc mở không phải khoảng trắng; bộ lọc `grep -v` chỉ so phần đường dẫn ở
  đầu dòng output (neo `^`, không vượt dấu `:`) nên dòng có `/` trong nội dung vẫn được giữ:
  ```bash
  grep -rnE '\{\{[^ {}][^{}]*\}\}' CLAUDE.md .claude docs | grep -vE '^(\./)?\.claude/skills/(_shared/[^/:]+/|ui-ux-pro-max/data/)'
  ```
- Mọi comment hướng dẫn điền là khối comment HTML mở đầu bằng chữ TEMPLATE và dấu hai chấm (kể cả comment ví dụ trong
  ô bảng): điền xong thì xoá; tìm còn sót bằng `grep -rn 'TEMPLATE[:]' CLAUDE.md .claude docs` (phải ra rỗng).

**Trạng thái tầng** (tầng không có thì xoá dòng; dự kiến mà chưa có code thì `chưa khởi tạo`):

| Tầng | Trạng thái | Ghi chú |
|---|---|---|
| Backend | đang dùng | `backend/` (Spring Boot); repo chưa có test |
| Frontend | đang dùng | `web/` |
| Mobile | chưa khởi tạo | thư mục `mobile/` mới có `README.md` (app Flutter cho bệnh nhân, client API sinh từ OpenAPI bằng `openapi-generator` `dart-dio`); chưa cấu hình hook |

| Placeholder | Ý nghĩa | Giá trị dự án | Ghi chú | Ví dụ |
|---|---|---|---|---|
| `{{BACKEND_LANG}}` | Ngôn ngữ backend + version | Java 21 | `pom.xml` `java.version` | Py: Python 3.11+ · Java: Java 21 |
| `{{BACKEND_FRAMEWORK}}` | Web framework backend | Spring Boot 3 | 3.3.5 (parent trong `backend/pom.xml`); modular monolith, package gốc `vn.medischedule` | Py: FastAPI · Java: Spring Boot 3 |
| `{{SCHEMA_LIB}}` | Thư viện validate / serialize request-response | DTO record + Jakarta Bean Validation | | Py: Pydantic v2 · Java: DTO record + Bean Validation |
| `{{ORM}}` | ORM / data access | Spring Data JPA (Hibernate) | `open-in-view: false` | Py: SQLAlchemy 2 async · Java: Spring Data JPA (Hibernate) |
| `{{DB_ENGINE}}` | Hệ quản trị CSDL | PostgreSQL 16 | image `postgres:16-alpine` (docker-compose); extension `unaccent`, `pg_trgm` tạo ở `infra/postgres/init.sql` | PostgreSQL 17 |
| `{{DB_URL_ENV_VAR}}` | Biến môi trường chứa URL DB mà backend (và công cụ migration) đọc | DB_URL | `application.yml`: `${DB_URL:jdbc:postgresql://localhost:5435/medischedule}` — thiếu biến thì rơi về DB dev local; user/mật khẩu DB đọc từ `POSTGRES_USER` / `POSTGRES_PASSWORD` | Py: `DATABASE_URL` · Java: `DB_URL` / `SPRING_DATASOURCE_URL` |
| `{{MIGRATION_TOOL}}` | Công cụ migration schema | Flyway | | Py: Alembic · Java: Flyway |
| `{{MIGRATION_STYLE}}` | Cách viết migration: `autogenerate` (sinh từ model rồi review) hoặc `SQL versioned` (SQL viết tay, đánh số version) | SQL versioned | seed dev là repeatable `R__*.sql` ở `backend/src/main/resources/db/seed` (nạp theo `FLYWAY_LOCATIONS`) | Py: autogenerate · Java: SQL versioned |
| `{{MIGRATION_ON_STARTUP}}` | Migration có tự chạy khi app/test khởi động không (`có` / `không`) | có | Flyway chạy khi Spring context khởi động — cả app lẫn `@SpringBootTest` | Py: không · Java: có (Flyway trong Spring Boot) |
| `{{MIGRATION_COMMAND}}` | Lệnh áp migration tới **một revision/version cụ thể** (markdown) | `(set -a; . ./.env; set +a; cd backend && SPRING_FLYWAY_TARGET=<n> ./mvnw spring-boot:run)` | Flyway không có lệnh riêng (pom không có `flyway-maven-plugin`) — áp khi khởi động; app không tự thoát: chạy nền, chờ log `Successfully applied …` / `Schema … is up to date` rồi dừng. PowerShell: `$env:SPRING_FLYWAY_TARGET='<n>'; powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run-backend.ps1` (wrapper nạp `.env`, `.env` không có biến target) | Py: `` `(cd backend && alembic upgrade <revision>)` `` · Java: `` `(set -a; . ./.env; set +a; cd backend && SPRING_FLYWAY_TARGET=<n> ./mvnw spring-boot:run)` `` (ghi chú: Flyway áp khi khởi động; app không tự thoát — chạy nền, dừng sau khi log Flyway báo xong) |
| `{{MIGRATION_DIR}}` | Thư mục file migration | backend/src/main/resources/db/migration | | Py: `backend/alembic/versions` · Java: `backend/src/main/resources/db/migration` |
| `{{MIGRATION_DRIFT_CHECK}}` | Kiểm model/entity lệch DB (markdown; `chưa có` nếu chưa có) | `spring.jpa.hibernate.ddl-auto: validate` | app không khởi động khi entity lệch schema | Py: lệnh chạy script so model với DB (nếu dự án có) · Java: `` `spring.jpa.hibernate.ddl-auto: validate` `` |
| `{{MIGRATION_CHAIN_TEST}}` | Test bất biến chuỗi migration (markdown; `chưa có` nếu chưa có) | chưa có | không cần — Flyway tự báo version trùng / out-of-order khi khởi động | Py: `` `tests/integration/test_alembic_chain.py` `` · Java: chưa có (ghi chú: không cần — Flyway tự báo version trùng) |
| `{{TEST_FRAMEWORK}}` | Test runner backend | JUnit 5 + Spring Boot Test | `spring-boot-starter-test`, `spring-security-test` đã có trong pom; repo **chưa có test nào** (`backend/src/test/java` rỗng) | Py: pytest + pytest-xdist · Java: JUnit 5 + Spring Boot Test |
| `{{TEST_COMMAND}}` | Lệnh chạy test (output gọn, markdown) — unit / test không cần DB `qa_<task>` | `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test)` | unit; test tích hợp dùng `INTEGRATION_TEST_COMMAND` | Py: `` `(cd backend && python -m pytest -q --tb=short <file…>)` `` · Java: `` `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>)` `` |
| `{{TEST_SINGLE_COMMAND}}` | Lệnh chạy một test (markdown) | `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test#<hàm>)` | | Py: `` `(cd backend && python -m pytest -q <file>::<test>)` `` · Java: `` `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>#<hàm>)` `` |
| `{{INTEGRATION_TEST_COMMAND}}` | Lệnh **hoàn chỉnh** chạy test tích hợp trên DB `qa_<task>` (markdown): nạp `.env` trước (nếu runner không tự đọc), **rồi** đặt `TEST_DB_ENV_VAR` = URL từ `qa_db.py url <task>`, rồi chạy runner trong đúng thư mục. Không ghép `VAR=… (subshell)`, không `export` trước bước nạp `.env` (`.env` đè lại) | `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)` | giai đoạn 1 (chưa có profile test): nạp `.env` trước, đặt `DB_URL` sau; bản PowerShell ở `.claude/scripts/README.md` § Java | Py: `` `(export TEST_POSTGRES_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && python -m pytest -q --tb=short <file…>)` `` · Java: `` `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)` `` |
| `{{TEST_SELECT_COMMAND}}` | Cách chọn test theo diff. **Một trong hai giá trị:** lệnh (markdown, kèm backtick), hoặc đúng chuỗi `chọn thủ công theo package/module bị sửa` (không backtick) | chọn thủ công theo package/module bị sửa | lớp test cùng package + lớp gọi nó | Py: `` `python .claude/scripts/select_tests.py --base <nhánh>` `` · Java: chọn thủ công theo package/module bị sửa |
| `{{UNIT_TEST_DIR}}` | Nơi đặt unit test + mẫu tên file, gộp thành **một** đường dẫn glob (markdown) | `backend/src/test/java/vn/medischedule/<module>/*Test.java` | | Py: `` `backend/tests/test_*.py` `` · Java: `` `backend/src/test/java/<pkg>/*Test.java` `` |
| `{{INTEGRATION_TEST_DIR}}` | Nơi đặt integration test + mẫu tên file, gộp thành **một** đường dẫn glob (markdown) | `backend/src/test/java/vn/medischedule/<module>/*IntegrationTest.java` | | Py: `` `backend/tests/integration/test_*.py` `` · Java: `` `backend/src/test/java/<pkg>/*IntegrationTest.java` `` |
| `{{KNOWN_FLAKY_FILE}}` | File danh sách test đỏ sẵn / chập chờn | backend/src/test/KNOWN_FLAKY.txt | chưa tạo — tạo khi có test đỏ sẵn đầu tiên | Py: `backend/tests/integration/KNOWN_FLAKY.txt` · Java: `backend/src/test/KNOWN_FLAKY.txt` |
| `{{TEST_DB_ENV_VAR}}` | Biến môi trường mà `INTEGRATION_TEST_COMMAND` đặt (sau bước nạp `.env`) để trỏ DB test; mọi tài liệu và pack dùng placeholder này, không ghi tên biến cứng. Đích: biến riêng mà hạ tầng test (fixture / profile test) đọc, **không default**. Hạ tầng test **chưa** đọc biến riêng nào (vd Spring chưa có profile test) ⇒ điền đúng giá trị `DB_URL_ENV_VAR` (biến app đang đọc), đổi khi dựng xong — `PROJECT/testing.md` mục "Repo chưa có test" bước 5 | DB_URL | giai đoạn 1: chưa có profile test đọc biến riêng ⇒ bằng `DB_URL_ENV_VAR`; khi dựng profile test đọc `TEST_DB_URL` không default thì đổi theo `PROJECT/testing.md` bước 5 | Py: `TEST_DATABASE_URL` (tên conftest đang đọc, vd `TEST_POSTGRES_URL`) · Java chưa có profile test: `DB_URL` · Java đã có profile test đọc `TEST_DB_URL`: `TEST_DB_URL` (pack Spring §7) |
| `{{BE_LINT_COMMAND}}` | Format / lint backend (markdown) | chưa có | pom không có Spotless / Checkstyle — giữ style file đang sửa | Py: `` `(cd backend && python -m black . && python -m isort .)` `` · Java: `chưa có` / `` `(cd backend && ./mvnw spotless:apply)` `` |
| `{{BE_TYPECHECK_COMMAND}}` | Type check / compile backend (markdown) | `(cd backend && ./mvnw -q -DskipTests compile)` | | Py: `` `(cd backend && python -m mypy app/)` `` · Java: `` `(cd backend && ./mvnw -q -DskipTests compile)` `` |
| `{{SERVICE_LAYER_GLOB}}` | Glob file tầng service / business logic (tính từ gốc repo; rule pack review) | backend/src/main/java/vn/medischedule/**/service/*.java | | Py: `backend/app/services/*.py` · Java: `backend/**/service/*.java` |
| `{{API_LAYER_GLOB}}` | Glob file tầng API (route / controller) (tính từ gốc repo; rule pack review) | backend/src/main/java/vn/medischedule/**/web/*.java | controller nằm trong package `web` của từng module | Py: `backend/app/api/**/*.py` · Java: `backend/**/controller/*.java` (hoặc `**/web/*.java`) |
| `{{FRONTEND_FRAMEWORK}}` | Framework frontend web | React 18 | React Router 6 | React 18 |
| `{{FE_LANG}}` | Ngôn ngữ frontend | TypeScript (strict) | | TypeScript (strict) |
| `{{UI_LIBRARY}}` | UI kit | Ant Design 5 | | Ant Design 5 |
| `{{STATE_LIBRARY}}` | Global state, hoặc server state khi không có store global | TanStack React Query 5 | không có store global | Zustand · TanStack React Query (ghi chú: không có store global) |
| `{{FE_BUILD_TOOL}}` | Build tool / dev server | Vite | | Vite |
| `{{PACKAGE_MANAGER}}` | Trình quản lý package FE | npm | | npm |
| `{{FE_TYPECHECK_COMMAND}}` | Type check frontend (markdown) | `npm --prefix web run typecheck` | `tsc --noEmit` | `` `npm --prefix frontend run typecheck` `` (ghi chú: `tsc --noEmit`) |
| `{{FE_LINT_COMMAND}}` | Lint frontend (markdown) | chưa có | `web/package.json` không có ESLint | `` `npm --prefix frontend run lint` `` (ghi chú: ESLint) |
| `{{FE_CHECK_COMMAND}}` | Script kiểm FE bổ sung ngoài type check / lint, vd kiểm hàm thuần không cần test runner (markdown) | chưa có | | `` `npm --prefix frontend run check` `` (ghi chú: `*.check.ts` chạy bằng esbuild + node) · `chưa có` |
| `{{MOBILE_FRAMEWORK}}` | Framework app mobile (tên công nghệ, kể cả khi tầng chưa khởi tạo) | Flutter | tầng chưa khởi tạo | React Native + Expo · Android Kotlin + Compose · Flutter |
| `{{MOBILE_APP_DIR}}` | Thư mục app mobile (chưa khởi tạo: thư mục dự kiến) | mobile | hiện chỉ có `README.md` | `apps/<actor>/mobile` · `mobile` |
| `{{MOBILE_LANG}}` | Ngôn ngữ app mobile | Dart | | TypeScript · Kotlin · Dart |
| `{{MOBILE_LINT_COMMAND}}` | Lint app mobile (markdown) | chưa có | chưa khởi tạo; khi có: `(cd mobile && flutter analyze)` | `` `(cd mobile && npx expo lint)` `` · `` `(cd mobile && ./gradlew lint)` `` · `` `(cd mobile && flutter analyze)` `` · chưa khởi tạo: `chưa có` |
| `{{MOBILE_TYPECHECK_COMMAND}}` | Type check / compile kiểm app mobile (markdown) | chưa có | chưa khởi tạo; khi có: `(cd mobile && flutter analyze)` | `` `(cd mobile && npx tsc --noEmit)` `` · `` `(cd mobile && ./gradlew compileDebugKotlin)` `` · `` `(cd mobile && flutter analyze)` `` · chưa khởi tạo: `chưa có` |
| `{{E2E_TOOL}}` | Công cụ E2E (`chưa có` nếu chưa dùng) | chưa có | | Playwright |
| `{{MAIN_BRANCH}}` | Nhánh chính (phát hành / prod) | main | | `main` |
| `{{DEV_BRANCH}}` | Nhánh tích hợp hằng ngày; repo chỉ có một nhánh thì điền **giống dòng `MAIN_BRANCH`** (xem ghi chú dưới bảng) | main | repo chỉ có nhánh `main` (xem ghi chú dưới bảng) | `develop` · `main` |
| `{{BACKEND_PACK}}` | Đường dẫn stack pack backend | .claude/stack-packs/backend-spring-boot-flyway.md | | `.claude/stack-packs/backend-spring-boot-flyway.md` |
| `{{FRONTEND_PACK}}` | Đường dẫn stack pack frontend (`chưa có` nếu chưa có tầng này) | .claude/stack-packs/frontend-react-antd-react-query.md | | `.claude/stack-packs/frontend-react-antd-react-query.md` |
| `{{MOBILE_PACK}}` | Đường dẫn stack pack mobile (`chưa có` nếu chưa có tầng này; tầng chưa khởi tạo vẫn ghi pack) | .claude/stack-packs/mobile-flutter.md | tầng chưa khởi tạo vẫn ghi pack | `.claude/stack-packs/mobile-flutter.md` |

**Repo không có nhánh dev riêng (chỉ có nhánh chính):** điền dòng `DEV_BRANCH` giống `MAIN_BRANCH`. Hệ quả: mọi push
"lên nhánh dev" là push thẳng nhánh chính ⇒ luôn hỏi user trước (HARD-STOPS); "gộp lô vào nhánh dev" (WORKFLOW §3.7)
thành gộp vào nhánh chính, nên mỗi lô phải xanh test trước khi gộp; migration nối vào điểm cuối của nhánh chính trên
remote; hotfix không cherry-pick (chỉ có một nhánh). Có pipeline deploy tự chạy khi merge vào nhánh chính thì coi mỗi
lần gộp là phát hành. Sau khi thay placeholder, chỉ hai file cần sửa tay:

- `.claude/skills/hotfix-engineer/SKILL.md` — mục "Không có tác dụng phụ ngoài code": giữ dòng *Chỉ có nhánh chính*,
  xoá dòng *Có nhánh dev riêng* (nếu không sẽ thành "commit trên main rồi cherry-pick sang main").
- `.claude/HARD-STOPS.md` — dòng "Push": bỏ cụm "(kể cả `<nhánh dev>`)" ở câu đầu (sau khi thay thành tên nhánh chính,
  trùng ý câu sau), giữ câu "repo không có nhánh dev riêng (dev = nhánh chính) thì mọi push đều thuộc dòng này".

`.claude/LESSONS.md` (mục git: push chỉ commit của task) và `.claude/WORKFLOW.md` (§3.6 migration nối sau điểm cuối
nhánh, §3.7 "Giao từng lô") đã có sẵn câu cho trường hợp một nhánh và vẫn đọc đúng khi hai placeholder cùng giá trị —
không cần sửa.

**Migration — chọn theo kiểu công cụ** (quy tắc chung ở `PROJECT/conventions.md`, `WORKFLOW.md` §3.6, `HARD-STOPS.md`):

| | `autogenerate` (Alembic, Prisma, EF Core…) | `SQL versioned` (Flyway, Liquibase SQL…) |
|---|---|---|
| Tạo | sinh từ model, review, patch tối thiểu | viết SQL tay theo entity; số version = cao nhất trên nhánh gốc + 1 |
| Áp tới một điểm | lệnh áp tới revision | target version (vd biến cấu hình target) |
| Ghi nhận SQL chạy tay | đánh dấu revision (vd `stamp`) | tránh; nếu buộc phải: ghi bảng lịch sử bằng công cụ (vd `baseline`/`repair`) sau khi hỏi user |
| Chạy khi khởi động app (`MIGRATION_ON_STARTUP` = có) | hiếm | thường gặp: mọi lần chạy app/test **là** lệnh migration lên DB mà `DB_URL` trỏ tới |

## Placeholder dự án & môi trường

Placeholder **dùng ở nhiều file** nhưng không thuộc stack. Điền giá trị không backtick (trừ `E2E_POLICY` là một câu văn
xuôi). Quy tắc: placeholder xuất hiện ở **≥ 2 file** thì có dòng trong bảng (stack ở bảng trên, còn lại ở bảng dưới);
placeholder chỉ xuất hiện trong **một** file thì **điền tại chỗ** trong file đó, không có dòng ở bảng — vd phần lớn
placeholder của `PROJECT/auth.md`, `architecture.md`, `environment.md`, `frontend-state.md`, `conventions.md`,
`DOMAIN_OVERVIEW` ở `PROJECT/README.md`, các lệnh và port ở cuối file này. Gặp placeholder có dòng trong bảng (kể cả
trong các file vừa kể) thì dùng giá trị bảng.

| Placeholder | Ý nghĩa | Nơi dùng | Giá trị dự án | Ví dụ |
|---|---|---|---|---|
| `{{PROJECT_NAME}}` | Tên dự án / sản phẩm | `CLAUDE.md`, skill, agent, `PROJECT/*` | MediSchedule | Acme Shop |
| `{{DOMAIN}}` | Slug domain nghiệp vụ = tên thư mục KB `docs/knowledge/<slug>/` (cách chọn: dưới bảng) | `CLAUDE.md`, `PROJECT/kdd.md`, `_shared/domain-context.md`, spec KDD | medischedule | `acme-shop` |
| `{{DOMAIN_NAME}}` | Tên domain đọc được | `_shared/domain-context.md`, `_shared/project-conventions.md` | Đặt lịch và quản lý phòng khám | Bán hàng trực tuyến |
| `{{BACKEND_DIR}}` | Thư mục code backend (tính từ gốc repo) | lệnh ở file này, `launch.json`, rule pack review, stack pack, `mobile-screen-loop` | backend | `backend` |
| `{{FRONTEND_DIR}}` | Thư mục code frontend web | lệnh ở file này, `launch.json`, rule pack review, stack pack, `ui-ux-pro-max` | web | `frontend` |
| `{{FRONTEND_DEV_PORT}}` | Cổng dev server FE | file này, `PROJECT/qa-process.md` | 5175 | 5173 |
| `{{BACKEND_PORT}}` | Cổng backend API trên máy dev | file này, stack pack | 8085 | 8000 |
| `{{SETTINGS_MODULE}}` | Entry point cấu hình backend duy nhất (module / hàm / lớp) | `PROJECT/architecture.md`, `PROJECT/conventions.md` | AppProperties | Py: `backend/app/core/config.py` · Java: `AppProperties` |
| `{{MODELS_DIR}}` | Thư mục model / entity | `PROJECT/architecture.md`, `PROJECT/conventions.md` | backend/src/main/java/vn/medischedule/**/domain | Py: `backend/app/models` · Java: `backend/src/main/java/<pkg>/**/entity` |
| `{{SCHEMAS_DIR}}` | Thư mục schema request-response / DTO | `PROJECT/architecture.md`, `PROJECT/conventions.md` | backend/src/main/java/vn/medischedule/**/dto | Py: `backend/app/schemas` · Java: `backend/src/main/java/<pkg>/**/dto` |
| `{{FRONTEND_SERVICES_DIR}}` | Thư mục module gọi API của frontend | `PROJECT/architecture.md`, `PROJECT/conventions.md` | web/src/features/<khu>/api.ts | `frontend/src/services` |
| `{{API_CLIENT}}` | Tên HTTP client frontend dùng chung (instance có interceptor auth) | `PROJECT/architecture.md`, `PROJECT/auth.md` | http | `apiClient` |
| `{{API_PREFIX}}` | Tiền tố URL API | `PROJECT/architecture.md`, `team-api-designer`, `mobile-screen-loop`, stack pack | /api | `/api/v1` |
| `{{USER_MODEL}}` | Model/entity user đăng nhập | `PROJECT/auth.md`, `_shared/domain-context.md` | User | `User` |
| `{{AUTH_DEPENDENCIES}}` | Dependency / middleware / annotation lấy user hiện tại và kiểm quyền (nhiều tên cách dấu phẩy) | `PROJECT/auth.md`, `_shared/domain-context.md` | @AuthenticationPrincipal AuthUser, @PreAuthorize | Py: get_current_user, require_permission(...) · Java: @AuthenticationPrincipal, @PreAuthorize |
| `{{ERROR_HANDLING_CONVENTION}}` | Một câu tóm tắt cách xử lý lỗi | `PROJECT/conventions.md`, `_shared/domain-context.md` | Service ném ApiException (HttpStatus + mã lỗi + thông báo tiếng Việt), GlobalExceptionHandler trả body {code, message, fields} | Service ném ApiException kèm mã lỗi, handler chung trả code + message |
| `{{DEV_DB_HOST}}` | Host(:port) **kèm tên DB** dev dùng chung — DB duy nhất ngoài `qa_*` được phép chạy migration/backend (không phải đích của test tích hợp). Trùng host với `QA_DB_HOST` (vd Docker local) thì chỉ **tên DB** phân biệt dev với `qa_*` — ghi cả tên DB | `HARD-STOPS.md`, `WORKFLOW.md`, `PROJECT/qa-process.md` | localhost:5435 (DB medischedule) | `10.0.0.12:5432` (DB `app_dev`) · cùng Docker local: `localhost:5435` (DB `app_dev`) |
| `{{QA_DB_HOST}}` | Host(:port) nơi tạo DB test `qa_<task>`; có thể **trùng** host DB dev (một Postgres Docker local chứa cả DB dev lẫn `qa_*`) — khi đó kiểm DB đích bằng tên (`qa_*`), không bằng host | `HARD-STOPS.md`, `PROJECT/qa-process.md`, `PROJECT/testing.md` | localhost:5435 | `localhost:5432` · `localhost:5435` (cùng Docker với DB dev) |
| `{{PROD_DB_HOST}}` | Host DB prod (chỉ đọc khi user yêu cầu) | `HARD-STOPS.md` | chưa có | `10.0.1.5:5432` · chưa có prod: `chưa có` (xoá câu dùng nó) |
| `{{PROD_SERVER}}` | Server prod (ssh/restart) | `HARD-STOPS.md` | chưa có | `deploy@prod-1` · chưa có prod: `chưa có` (xoá câu dùng nó) |
| `{{APP_IMPORT_SECONDS}}` | Thời gian khởi động app trước test đầu tiên (đo trên máy dev) | `PROJECT/qa-process.md` | chưa đo — đo ở task test đầu tiên | ~15 s · repo chưa có test: `chưa đo — đo ở task test đầu tiên` |
| `{{E2E_POLICY}}` | Một câu chính sách E2E | `PROJECT/qa-process.md` | Chưa dùng E2E. | "Không dùng E2E." / "E2E Playwright chỉ cho luồng thanh toán." |
| `{{UI_LANGUAGE}}` | Ngôn ngữ chữ trên giao diện | `mobile-screen-loop` | tiếng Việt | tiếng Việt |

**Chọn slug `DOMAIN`:** kebab-case, chữ thường không dấu (`acme-shop`, không `AcmeShop` / `bán-hàng`); **không trùng tên
module/capability** (capability nằm dưới `docs/knowledge/<slug>/capabilities/<tên>/` — slug `order` mà có capability
`order` sẽ rối); nên dùng tên sản phẩm. Một repo thường một domain; repo nhiều sản phẩm tách hẳn thì mỗi sản phẩm một slug.

## Service khác & tầng chưa khởi tạo

Bảng stack chỉ có ba tầng backend / frontend / mobile.

- **Service thứ 4 trở đi** (vd service AI/worker FastAPI cạnh backend Java): không nhồi vào placeholder của tầng có sẵn.
  Ghi một dòng vào bảng dưới; pack áp dụng cho service đó (vd `backend-fastapi-sqlalchemy.md`) ghi ở cột Pack và ở dòng
  "Stack pack" của `CLAUDE.md` (kèm thư mục). Chỉ khi file chung cần lệnh của service đó mới thêm placeholder dạng
  `SERVICE_<TÊN>_<KHOÁ>` trong ngoặc nhọn kép (vd khoá `SERVICE_AI_TEST_COMMAND`) vào bảng này và dùng ở file chung.
- **Pack của service phụ:** pack dùng chung tên placeholder với tầng chính (`BACKEND_DIR`, `MIGRATION_DIR`,
  `TEST_DB_ENV_VAR`… trong ngoặc nhọn kép). Trong file pack của service, thay các placeholder đó bằng **giá trị của
  service** (hoặc đổi thành `SERVICE_<TÊN>_<KHOÁ>` rồi thêm vào bảng này) **trước** khi replace hàng loạt cho cả repo —
  nếu không, giá trị backend chính chảy vào pack của service. Bảng khoá ở đầu pack (lệnh test, thư mục migration…) đọc
  là giá trị của service, ghi lại vào các cột bảng dưới. Chi tiết: `.claude/stack-packs/README.md` § "Pack cho service phụ".
- **Tầng / service dự kiến nhưng chưa khởi tạo** (chưa có thư mục code, vd app Flutter sắp làm): giữ pack của nó; các
  dòng của tầng trong bảng stack vẫn điền **tên công nghệ / thư mục dự kiến** (để câu văn ở file khác đọc đúng), lệnh
  của tầng ghi `chưa có` (theo "Cách điền"); trạng thái `chưa khởi tạo` chỉ ghi ở bảng "Trạng thái tầng" (và cột
  Trạng thái bảng dưới với service); **chưa cấu hình hook** cho tầng đó (`.claude/scripts/README.md`). Khi khởi tạo:
  điền lệnh, đổi trạng thái thành `đang dùng`, cấu hình hook.

| Service | Thư mục | Ngôn ngữ / framework | Lệnh chạy | Lệnh test | Lint / type check | Pack | Trạng thái |
|---|---|---|---|---|---|---|---|
| ai-service | `ai-service` | Python + FastAPI + PyTorch | chưa có | chưa có | chưa có | `.claude/stack-packs/backend-fastapi-sqlalchemy.md` | chưa khởi tạo |

`ai-service` (phân tích ảnh y tế): backend gọi qua REST (`AI_SERVICE_URL`, module `vn.medischedule.ai`), ảnh lấy từ MinIO
bucket `medical-files` qua presigned URL — không có DB riêng. Pack của service đã bỏ phần DB và thay thư mục bằng
`ai-service`.

## Stack pack đang dùng

- Backend: `.claude/stack-packs/backend-spring-boot-flyway.md`
- Frontend: `.claude/stack-packs/frontend-react-antd-react-query.md`
- Mobile: `.claude/stack-packs/mobile-flutter.md` (tầng chưa khởi tạo)
- Service `ai-service`: `.claude/stack-packs/backend-fastapi-sqlalchemy.md` (chưa khởi tạo)

Bẫy, lệnh và ví dụ code riêng của từng công cụ nằm trong pack — chi tiết theo stack: xem pack ứng với tầng đang làm.

## Storage

- **Storage**: PostgreSQL 16 + Redis 7 (cache) + MinIO (object storage, bucket `medical-files`) — cả ba chạy bằng
  Docker Compose (`docker-compose.yml`), backend và web chạy trực tiếp trên máy.

## Lệnh

Các placeholder lệnh dưới đây là **giá trị markdown** (điền kèm backtick, như ở văn xuôi), nên ghi thành danh sách,
không đặt trong khối code. Lệnh chưa có thì xoá cả dòng.

### Backend (code ở `backend`)

Mỗi lệnh tự chạy được từ gốc repo (tự `cd` / `--prefix`, tự nạp `.env` nếu runner không tự đọc — "Cách điền") —
đừng giả định đã `cd` sẵn, đừng ghép thêm tiền tố.

- Cài dependency: không có bước riêng — `mvnw` tự tải Maven và dependency ở lần chạy đầu (không cần cài Maven).
- Chạy dev server: `(set -a; . ./.env; set +a; cd backend && ./mvnw spring-boot:run)` — PowerShell / VS Code dùng
  wrapper của dự án: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run-backend.ps1` (nạp `.env` rồi
  `mvnw spring-boot:run`).
- Type check: `(cd backend && ./mvnw -q -DskipTests compile)`
- Chạy test: `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test)`
- Chạy một test: `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test#<hàm>)`
- Chạy test tích hợp trên DB `qa_<task>`: `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)`
- Migration (chỉ trên DB dev / `qa_*` — xem HARD-STOPS "An toàn môi trường"): `(set -a; . ./.env; set +a; cd backend && SPRING_FLYWAY_TARGET=<n> ./mvnw spring-boot:run)`
- Cập nhật file seed sau khi sửa nội dung trang chủ ở `/admin/homepage`: `python scripts/export_seed.py` (đọc DB dev qua
  `docker compose exec postgres`, ghi `backend/src/main/resources/db/seed/R__seed_dev_data.sql`).

Migration chạy lúc khởi động app (Flyway trong Spring Boot): lệnh chạy backend / test ở trên cũng áp migration — áp
mọi `V*` và `R__*` (seed dev, theo `FLYWAY_LOCATIONS`) có trong checkout lên DB mà `DB_URL` trỏ tới. Spring Boot không
tự đọc `.env`: mọi lệnh trên đã gồm bước nạp `.env` — xem pack backend §1. Lệnh khởi động app (kể cả lệnh migration
kiểu "áp lúc khởi động") không tự thoát: chạy nền, dừng app khi log migration báo xong. Backend không có
`spring-boot-devtools` (không auto-reload): sửa BE xong nhắc user restart.

### Frontend (code ở `web`)

Như backend: mỗi lệnh tự chạy được từ gốc repo (vd `npm --prefix <thư mục> run dev` hoặc `(cd <thư mục> && …)`).

- Cài dependency: `npm --prefix web install`
- Dev server (http://localhost:5175, proxy `/api` → `http://localhost:8085`): `npm --prefix web run dev`
- Type check: `npm --prefix web run typecheck`
- Build: `npm --prefix web run build` (`tsc --noEmit && vite build`)

Script trong `web/package.json`: `dev`, `build`, `preview`, `typecheck` — không có lint / formatter; code hiện có chưa
format bằng prettier, đừng chạy formatter ghi đè lên file có sẵn.

### Docker

- Lần đầu: `cp .env.example .env`
- Dựng hạ tầng dev (Postgres, Redis, MinIO + job `minio-init` tạo bucket rồi thoát): `docker compose up -d`
- Xem trạng thái: `docker compose ps`

Docker Compose chỉ chạy hạ tầng; backend và web chạy trực tiếp trên máy. `docker compose down -v` **xoá sạch** dữ liệu
dev (volume) — chỉ chạy khi user yêu cầu. Không tự khởi động Docker Desktop (`LESSONS.md`).

## Port Map

| Service       | Host port                   |
|---------------|-----------------------------|
| Backend API   | 8085 (Swagger `/swagger-ui.html`) |
| Frontend      | 5175 (Vite dev, `strictPort`) |
| Database      | 5435 (Postgres trong Docker)  |
| Cache         | 6390 (Redis trong Docker)     |
| Object storage| 9010 (MinIO API) · 9011 (console) |

Frontend **local dev** chạy ở `:5175` (khớp `.claude/launch.json` và `CORS_ORIGINS` của backend; `strictPort: true` nên
cổng bận thì Vite thoát — worktree chạy song song phải đổi cả cổng lẫn `CORS_ORIGINS`).

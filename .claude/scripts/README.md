# Hook scripts

Lõi hook viết bằng Python (máy cần có Python, dù dự án dùng stack gì). Mọi giả định stack nằm trong khối
`# CONFIG — chỉnh theo stack dự án` ở đầu từng file, đè được bằng biến môi trường (shell hoặc khối `env` của
`.claude/settings.json`). Không đặt biến nào thì hành vi mặc định là stack ví dụ: Alembic/PostgreSQL/pytest/black/FastAPI/React.

Kiểm sau khi chỉnh:

- `python -I -B .claude/scripts/test_hooks.py` — hành vi mặc định của script (bỏ qua env của dự án). Ca phụ thuộc
  file dự án được phép xoá (`qa_db.py`, `select_tests.py`, `codegraph_nudge.py`…) in `SKIP`, không tính FAIL.
- `python -I -B .claude/scripts/test_hooks.py --project` — cấu hình `env` của dự án với lệnh thật của stack (mục
  [Chế độ `--project`](#chế-độ---project)).

## Script

| Script | Vai trò | Gắn stack? |
|---|---|---|
| `context_guard.py` | (1) **Giới hạn đọc**: chặn `sed -n A,Bp` một khúc > 200 dòng (tổng các khúc > 300) và `cat <file>` không pipe/redirect > 400 dòng; cảnh báo khi Write ghi đè file có sẵn > 200 dòng. Dấu bỏ qua: `# context-guard: allow`. (2) Chặn lệnh migration ghi vào DB không phải DB tạm; tuỳ chọn cảnh báo khi khởi động app tự chạy migration lên DB máy khác. Dấu bỏ qua: `# migration-guard: allow` | Không (cấu hình được) |
| `handoff.py` | Quản lý `docs/HANDOFF.<task>.local.md`, phase, đánh dấu đã chạy test, Stop hook | Không (cấu hình được) |
| `edit_hooks.py` | Nhắc file lớn, kiểm format sau Edit, nhắc cập nhật tài liệu API | Không (cấu hình được) |
| `handoff-context.sh` | Nạp HANDOFF vào context | Không |
| `check-docs-drift.sh` | So route/store thật với tài liệu (chạy tay) | Không (cấu hình được; chưa cấu hình route thì bỏ mục đó) |
| `qa_db.py` | **Adapter PostgreSQL**: tạo/xoá DB test riêng mỗi task. Dùng psycopg2 → psycopg → `psql`, không cần SQLAlchemy | Có — thay bằng script cùng CLI |
| `test_hooks.py`, `hook_cases.example.json` | Tự kiểm hook; bảng ca mẫu cho chế độ `--project` | Không |

### Hợp đồng CLI của adapter

Mỗi adapter có mục **"Giao diện CLI"** trong docstring đầu file (đối số, output, exit code). Dự án stack khác thay
script bằng bản cùng giao diện, hoặc xoá đi: hook không bao giờ gọi trực tiếp các adapter này, và khi file không
có thì mọi chỗ tham chiếu im lặng —

- `qa_db.py` không có → `handoff.py close` bỏ bước dọn DB tạm.

Tóm tắt: `qa_db.py url <task>` in đúng một dòng URL (stdout), `list`, `drop <task>`, `workers` (dòng đầu là số).

MediSchedule (backend Java) đã xoá các adapter Python `select_tests.py`, `route_table_dump.py`, `refactor_move_*.py` và
hook `codegraph_nudge.py` (repo không index CodeGraph): Stop hook không gợi ý script chọn test, skill Flow D kiểm route /
dời code bằng tay.

## Biến môi trường

Cột **Mới/Đổi** đánh dấu thay đổi so với bản trước.

### Giới hạn đọc (`context_guard.py`)

Hook `PreToolUse` Bash chặn (exit 2) lệnh đổ khúc file lớn vào context; lệnh có pipe (`| grep`, `| head`) hoặc
redirect ra file thì không tính. Bỏ qua cho một lệnh: thêm `# context-guard: allow` cuối lệnh. Hook `PostToolUse`
Write chỉ cảnh báo, không chặn.

| Biến | Mặc định | Mới/Đổi | Ý nghĩa |
|---|---|---|---|
| `GUARD_SED_MAX_LINES` | `200` | Mới | Một khúc `sed -n A,Bp` dài hơn → chặn (`A,$p` luôn tính là quá) |
| `GUARD_SED_TOTAL_MAX_LINES` | `300` | Mới | Tổng các khúc `sed -n` trong một lệnh dài hơn → chặn |
| `GUARD_CAT_MAX_LINES` | `400` | Mới | Tổng số dòng các file `cat` (không pipe/redirect) dài hơn → chặn |
| `GUARD_WRITE_WARN_LINES` | `200` | Mới | Write ghi đè file có sẵn dài hơn → cảnh báo dùng Edit |
| `GUARD_SEARCH_HINT` | "Định vị trước bằng Grep / `grep -n` (hoặc công cụ index code nếu repo có)" | Mới | Câu gợi ý định vị code in trong thông báo chặn; repo có công cụ index code thì nêu lệnh của nó |

### Migration guard (`context_guard.py`)

| Biến | Mặc định | Mới/Đổi | Ý nghĩa |
|---|---|---|---|
| `GUARD_MIGRATION_CMD_REGEX` | regex khớp `alembic upgrade\|downgrade\|stamp` | | Nhận lệnh migration ghi DB; group đầu tiên khác rỗng = hành động in ra. `''` = tắt guard |
| `GUARD_MIGRATION_DRYRUN_REGEX` | `--sql\b` | | Lệnh chỉ in SQL thì cho qua (`''` = không có chế độ này) |
| `GUARD_MIGRATION_DB_URL_VAR` | `DATABASE_URL` | Đổi | Biến chứa URL DB. **Nhận danh sách phẩy**, biến trước ưu tiên (vd `FLYWAY_URL,DB_URL,SPRING_DATASOURCE_URL`) |
| `GUARD_DB_URL_ARG_REGEX` | nhận `-Dflyway.url=…`, `-Dspring.datasource.url=…`, `-url=…`, `--url …`, `--url=…`, `--database-url …`, `--spring.datasource.url=…` | Mới | Đối số dòng lệnh mang URL (group 1 = giá trị); `''` = không đọc đối số |
| `GUARD_DB_URL_REGEX` | `//(?:[^/@\s]*@)?([^/?\s]+)/([^/?\s;]+)` | | Tách host (g1) và tên DB (g2), nhận cả jdbc |
| `GUARD_SCRATCH_URL_HELPER_REGEX` | `qa_db\.py\s+url\b` | | URL lấy từ script DB tạm thì cho qua |
| `GUARD_SCRATCH_URL_HINT` | `$(python .claude/scripts/qa_db.py url <task>)` | | Gợi ý in khi chặn |
| `GUARD_ENV_FILE` | `.env` | | File env đọc khi lệnh không đặt URL (dòng `VAR=…` hoặc `export VAR=…`) |
| `GUARD_STARTUP_CMD_REGEX` | `''` (tắt) | Mới | Lệnh khởi động app mà migration tự chạy lúc start (Spring Boot + Flyway/Liquibase, Rails…) → **cảnh báo**, không chặn |
| `GUARD_STARTUP_ALLOWED_HOSTS` | `localhost,127.0.0.1,::1` | Mới | Host coi là DB cục bộ khi khởi động app (so không kèm port) |
| `GUARD_ENV_LOADER_REGEX` | nhận `. ./.env`, `. .env`, `source ../.env`, `set -a; .`, PowerShell `Get-Content .env` | Mới | Bộ nạp `.env` **trong lệnh** (ghi đè biến môi trường). `''` = không nhận bộ nạp nào |
| `GUARD_ENV_WRAPPER_REGEX` | `''` | Mới | Script wrapper của dự án tự nạp `.env` lúc bắt đầu (vd `\brun-backend\.(?:ps1\|sh)\b`): tính như bộ nạp đặt tại chỗ gọi wrapper |
| `GUARD_TEST_CMD_REGEX` | `''` (tắt) | Mới | Lệnh chạy test (Java `mvnw test\|verify`, `gradlew test\|check`; Python `pytest`). DB đích có tên **không** bắt đầu bằng `SCRATCH_DB_PREFIXES` ⇒ **cảnh báo**, kể cả host localhost. DB đích không xác định được ⇒ im |
| `GUARD_TEST_DB_URL_VAR` | = `GUARD_MIGRATION_DB_URL_VAR` | Mới | Biến URL DB mà lệnh test dùng (danh sách phẩy, biến trước ưu tiên), vd Python `TEST_POSTGRES_URL` |
| `SCRATCH_DB_PREFIXES` | `qa_,tmp_itest_` | | Tiền tố DB tạm |

DB đích lấy theo thứ tự: **đối số URL trong lệnh** → phép gán `VAR=…` / `export VAR=…` / `$env:VAR = …` trong lệnh
(kể cả `VAR=$OTHER` trỏ phép gán khác; chỉ phép gán đứng trước lệnh được xét) → biến môi trường → dòng `VAR=` trong
`.env` (gốc repo hoặc thư mục cha của cwd).

**Lệnh tự nạp `.env`** (khớp `GUARD_ENV_LOADER_REGEX` hoặc `GUARD_ENV_WRAPPER_REGEX`, vd lệnh mẫu của pack Spring
`(set -a; . ./.env; set +a; cd backend && ./mvnw …)`, wrapper `run-backend.ps1`): bộ nạp ghi đè môi trường, nên thứ tự
đổi thành **phép gán đứng SAU bộ nạp** → `.env` → phép gán đứng TRƯỚC bộ nạp → biến môi trường (nhiều bộ nạp: tính bộ
nạp cuối cùng đứng trước lệnh). Wrapper tính như bộ nạp đặt tại chỗ gọi nó — thường không có phép gán nào sau nó ⇒
`.env` thắng. Ví dụ (`.env` trỏ máy khác):

| Lệnh | DB guard thấy |
|---|---|
| môi trường `DB_URL=…localhost…`; `(set -a; . ./.env; set +a; ./mvnw spring-boot:run)` | DB trong `.env` |
| `export DB_URL=$(… qa_db.py url t1); (set -a; . ./.env; set +a; ./mvnw test)` | DB trong `.env` (`.env` đè lại) |
| môi trường `DB_URL=…localhost…`; `powershell -File scripts/run-backend.ps1` (wrapper đã khai) | DB trong `.env` |
| `(set -a; . ./.env; set +a; export DB_URL="$(… qa_db.py url t1)"; ./mvnw test)` | DB tạm `qa_t1` — đúng |

**Test-db guard** (`GUARD_TEST_CMD_REGEX`, chỉ cảnh báo): startup guard chỉ so **host**, nên khi DB dev và DB test
`qa_*` cùng một Postgres (vd Docker local `localhost:5435`) nó không phân biệt được test chạy lên DB dev. Bật
`GUARD_TEST_CMD_REGEX` để mọi lệnh chạy test bị so **tên DB**: tên không bắt đầu bằng `SCRATCH_DB_PREFIXES` ⇒ cảnh
báo, dù host là localhost. Lệnh test không trỏ DB nào xác định được (unit thuần) thì im.

**Startup guard — giới hạn.** Chạy app ở máy dev là việc bình thường nên guard chỉ *cảnh báo* (Claude và user đều
thấy), không bao giờ chặn. Nó chỉ biết DB đích qua đối số / phép gán trong lệnh / biến môi trường / `.env`. URL nằm
trong `application.yml`/`application-<profile>.properties`, profile Spring (`-Dspring.profiles.active=prod`),
`config/database.yml` của Rails, Docker Compose, secret manager… thì guard **không thấy** và im lặng. Host không tách
được (URL lạ) cũng im. Nếu dự án có profile trỏ DB dùng chung, ghi luật đó trong `HARD-STOPS.md` của dự án.

### HANDOFF / Stop hook (`handoff.py`, `settings.json`)

| Biến | Mặc định | Mới/Đổi | Ý nghĩa |
|---|---|---|---|
| `HANDOFF_BASE_BRANCH` | tự dò | Đổi | Branch gốc để diff. Không đặt/rỗng: `develop` nếu có (local hoặc `origin/`), không thì `main`, rồi `master` (dò lười, chỉ khi cần) |
| `HANDOFF_CODE_PATHS`, `HANDOFF_BACKEND_SEGS` | xem đầu file | | Đường dẫn tính là code sản phẩm / phải chạy test sau khi sửa |
| `HANDOFF_TEST_PATH_REGEX` | `/tests/\|\.check\.\|/src/test/\|\.test\.[jt]sx?$\|_test\.go$` | Đổi | File test (QA được sửa): thêm Maven/Gradle `src/test/`, JS/TS `*.test.*`, Go `*_test.go` |
| `HANDOFF_TEST_CMD_REGEX` | `-m\s+pytest\b` | | Nhận lệnh chạy test ở bất kỳ đâu trong câu lệnh |
| `HANDOFF_TEST_RUNNER_REGEX` | khớp `pytest` đầu đoạn lệnh | | Nhận runner test ở đầu từng đoạn lệnh (tách tại `&&` `\|\|` `;` `\|` xuống dòng ngoài nháy/`$(…)`), sau khi bỏ tiền tố `VAR=…` (giá trị có nháy/khoảng trắng/`$(…)`/backtick), `env`, `&` PowerShell |
| `HANDOFF_TEST_LABEL` | `pytest` | | Tên bộ test trong lời nhắc |
| `HANDOFF_TEST_SELECT_HINT` | `select_tests.py --base {base}` **chỉ khi file có**, không thì `''` | Đổi | Gợi ý chọn test trong lời nhắc (`''` = không in) |
| `HANDOFF_TEST_PREFILTER` | `pytest` | Đổi | Lọc nhanh bằng shell trong `settings.json` trước khi gọi Python. **Danh sách phẩy** chuỗi con, vd `mvn,gradle`, `test,vitest`, `go test`; `''` = luôn gọi |
| `HANDOFF_SCOPE_EXAMPLE` | `backend/app/services/foo*.py,...` | | Ví dụ glob cho `--scope` |
| `HANDOFF_MIGRATION_DIR` | `backend/alembic/versions/` | | Thư mục migration ghi vào HANDOFF |
| `HANDOFF_QA_DB_SCRIPT` | `.claude/scripts/qa_db.py` | | Script dọn DB tạm khi `close`; không có thì bỏ qua |

`HANDOFF_TEST_PREFILTER` chỉ là bộ lọc rẻ (thuần shell, không khởi động Python) cho hook PostToolUse chạy sau **mọi**
lệnh Bash; quyết định thật vẫn là `HANDOFF_TEST_RUNNER_REGEX`. Đặt chuỗi con xuất hiện trong mọi lệnh test của dự án.

### Edit hooks (`edit_hooks.py`)

| Biến | Mặc định | Mới/Đổi | Ý nghĩa |
|---|---|---|---|
| `EDIT_LARGE_FILE_LINES` | `1000` | | Ngưỡng cảnh báo file lớn |
| `EDIT_FORMAT_ROOT` / `EDIT_FORMAT_SUBDIR` | `backend` / `app/` | | cwd chạy formatter / thư mục cần kiểm |
| `EDIT_FORMAT_EXTS` | `.py` | | Đuôi file kiểm format |
| `EDIT_FORMAT_CHECK_CMD` | `{python} -m black --check -q {file}` | | Lệnh kiểm (`''` = tắt); formatter chưa cài thì im |
| `EDIT_FORMAT_FIX_CMD` / `EDIT_FORMAT_NAME` | `python -m black {file}` / `black` | | Lời nhắc sửa |
| `EDIT_FORMAT_FAIL_CODES` | `1` | | Mã thoát = sai format |
| `EDIT_API_SEG` | `backend/app/api/v1/` | Đổi | Sửa file API thì nhắc cập nhật tài liệu (`''` = tắt). **Danh sách phẩy**; mỗi mục: đoạn đường dẫn (chuỗi con), glob có `* ? [` (vd `*Controller.java`, `*/web/*`, `*/routes/*.ts`) hoặc regex tiền tố `re:` |
| `EDIT_API_DOC` | `.claude/PROJECT/architecture.md` | | Tài liệu cần cập nhật |

### DB tạm (`qa_db.py`)

| Biến | Mặc định | Mới/Đổi | Ý nghĩa |
|---|---|---|---|
| `QA_DB_ADMIN_URL` | — | Đổi | URL admin (biến môi trường hoặc `.env`); nhận cả `jdbc:postgresql://h:p/db?user=&password=` |
| `QA_DB_ADMIN_URL_VAR` | `QA_DB_ADMIN_URL` | Mới | Tên biến chứa URL admin |
| `QA_DB_PG_VARS` | `POSTGRES_HOST,POSTGRES_PORT,POSTGRES_USER,POSTGRES_PASSWORD,POSTGRES_DB` | Mới | Không có URL admin thì ghép từ các biến này (cần USER; host mặc định localhost, port 5432) |
| `QA_DB_FALLBACK_URL_VARS` | `DATABASE_URL` | Mới | Cuối cùng mới dùng URL DB dev (danh sách phẩy) |
| `QA_DB_ENV_FILE` | `.env` | Mới | File env ở gốc repo |
| `QA_DB_ALLOWED_HOSTS` | (thêm vào) `localhost,127.0.0.1,::1` mọi port | Đổi | Host được tạo DB tạm. Mục `host` = mọi port, `host:port` = đúng port. Host ngoài danh sách luôn bị từ chối; KHÔNG BAO GIỜ thêm prod |
| `QA_DB_PREFIX` | `qa_` | | Tiền tố DB tạm của task |
| `QA_DB_URL_DRIVER` | `postgresql+asyncpg` | Đổi | Scheme URL in ra; `postgresql` cho libpq/Prisma/node/Go; bắt đầu `jdbc:` = chế độ JDBC |
| `QA_DB_JDBC_CREDENTIALS` | `none` | Mới | JDBC: `none` chỉ in URL (user ghi chú ra stderr, app đọc user/mật khẩu từ biến riêng); `query` gắn `?user=&password=` (pgjdbc, MySQL Connector/J — không dùng với SQL Server) |
| `QA_DB_PSQL` | `psql` | Mới | Lệnh `psql` khi không có psycopg2/psycopg |

### `check-docs-drift.sh` (chạy tay)

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `ROUTER_REGISTRY_FILE` | `''` (bỏ mục route) | File **hoặc thư mục** khai báo route; thư mục thì `grep -r` (bỏ `node_modules`, `.git`, `target`, `build`, `dist`) |
| `ROUTER_PREFIX_REGEX` | nhận chung Spring `@…Mapping("/…")`, FastAPI `prefix="…"`, Express `.get('/…')` | Regex `grep -E` lấy route |
| `API_MODULES_DIR` / `CAPABILITIES_DOC` | `''` (bỏ mục) / `docs/_ref/capabilities.md` | Thư mục module API / tài liệu module |
| `FE_SRC_DIR` / `FE_STORE_PATTERNS` | `frontend/src` / `*.store.ts *Store.ts` | Store frontend (thư mục không có thì bỏ) |
| `ARCH_DOC` / `FE_STATE_DOC` | `.claude/PROJECT/architecture.md` / `.claude/PROJECT/frontend-state.md` | Tài liệu so sánh |

## Ví dụ cấu hình theo stack

Đặt trong khối `env` của `.claude/settings.json` (giá trị là chuỗi JSON — backslash viết đôi). Sau khi đặt, chép
`hook_cases.example.json` thành `hook_cases.json`, sửa ca theo lệnh thật của dự án và chạy `test_hooks.py --project`.

- **Regex trong JSON:** mỗi `\` của regex viết thành `\\` (vd regex `\bflyway\b` → chuỗi JSON `"\\bflyway\\b"`; `\s` →
  `"\\s"`; dấu `"` trong regex → `\"`). Chép nguyên khối ví dụ dưới (đã escape sẵn) thay vì gõ lại từ regex thô.
- **Sửa `settings.json` bằng Write/Edit** của Claude Code (hoặc trình soạn thảo), **không** bằng heredoc / `echo` /
  `Set-Content` trong shell: shell tự xử lý `\`, `$`, backtick và nháy nên chuỗi regex bị đổi âm thầm (thiếu một lớp
  escape là regex khác nghĩa, JSON vẫn hợp lệ). Sửa xong kiểm `python -c "import json; json.load(open('.claude/settings.json', encoding='utf-8'))"`
  rồi `test_hooks.py --project`.

### Python + Alembic + pytest (mặc định)

Không cần đặt biến nào. Thường chỉ chỉnh đường dẫn: `HANDOFF_CODE_PATHS`, `HANDOFF_BACKEND_SEGS`, `EDIT_FORMAT_ROOT`,
`EDIT_API_SEG`, `PY_BACKEND_DIR`; và `QA_DB_ADMIN_URL` trong `.env` (Postgres test trên localhost). Nên bật test-db
guard (tên biến = `TEST_DB_ENV_VAR` của dự án):

```json
{
  "env": {
    "GUARD_TEST_CMD_REGEX": "\\bpytest\\b",
    "GUARD_TEST_DB_URL_VAR": "TEST_POSTGRES_URL"
  }
}
```

Lệnh test tích hợp hoàn chỉnh (giá trị `INTEGRATION_TEST_COMMAND`, chạy từ gốc repo):
`(export TEST_POSTGRES_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && python -m pytest -q --tb=short tests/integration/<file…>)`.

### Java / Spring Boot + Flyway + Maven

```json
{
  "env": {
    "GUARD_MIGRATION_CMD_REGEX": "\\bflyway(?:\\b[:\\s][^|;&]*?\\b(migrate|clean|baseline|repair|undo)\\b|(Migrate|Clean|Baseline|Repair|Undo)\\b)",
    "GUARD_MIGRATION_DRYRUN_REGEX": "",
    "GUARD_MIGRATION_DB_URL_VAR": "FLYWAY_URL,DB_URL,SPRING_DATASOURCE_URL",
    "GUARD_STARTUP_CMD_REGEX": "\\bmvnw?(?:\\.cmd)?\\b[^|;&]*(?:\\bspring-boot:run\\b|\\s(?:test|verify)(?=[\\s)'\"]|$))|\\bgradlew?(?:\\.bat)?\\b[^|;&]*(?:\\bbootRun\\b|\\s(?:test|check)(?=[\\s)'\"]|$))|\\bjava\\b[^|;&]*\\s-jar\\s|\\brun-backend\\.(?:ps1|sh)\\b",
    "GUARD_TEST_CMD_REGEX": "\\bmvnw?(?:\\.cmd)?\\b[^|;&]*\\s(?:test|verify)(?=[\\s)'\"]|$)|\\bgradlew?(?:\\.bat)?\\b[^|;&]*\\s(?:test|check)(?=[\\s)'\"]|$)",
    "GUARD_ENV_WRAPPER_REGEX": "\\brun-backend\\.(?:ps1|sh)\\b",
    "HANDOFF_CODE_PATHS": "backend/src/,web/src/",
    "HANDOFF_BACKEND_SEGS": "backend/src/main/,backend/src/test/",
    "HANDOFF_TEST_CMD_REGEX": "\\b(?:bash|sh|cmd|powershell|pwsh|timeout)(?:\\.exe)?\\s[^|\\n]*?(?:\\S*[/\\\\])?(?:mvnw?|gradlew?)(?:\\.cmd|\\.bat)?\\s+(?:\\S+\\s+)*?(?:test|verify|check)(?=[\\s'\"]|$)",
    "HANDOFF_TEST_RUNNER_REGEX": "(?:&\\s+)?(?:\\S*[/\\\\])?(?:mvnw?(?:\\.cmd)?|gradlew?(?:\\.bat)?)\\s+(?:\\S+\\s+)*?(?:test|verify|check)(?:\\s|$)",
    "HANDOFF_TEST_LABEL": "JUnit (mvnw test)",
    "HANDOFF_TEST_PREFILTER": "mvn,gradle",
    "HANDOFF_TEST_SELECT_HINT": "test cùng package của file đã sửa, trên DB qa_<task>: (set -a; . ./.env; set +a; export DB_URL=\"$(python .claude/scripts/qa_db.py url <task>)\"; cd backend && ./mvnw -q test -Dtest=<Lớp>Test,<Lớp>IntegrationTest)",
    "HANDOFF_SCOPE_EXAMPLE": "backend/src/main/java/com/acme/order/**,web/src/features/order/",
    "HANDOFF_MIGRATION_DIR": "backend/src/main/resources/db/migration/",
    "EDIT_FORMAT_CHECK_CMD": "",
    "EDIT_API_SEG": "*Controller.java",
    "QA_DB_URL_DRIVER": "jdbc:postgresql",
    "ROUTER_REGISTRY_FILE": "backend/src/main/java",
    "API_MODULES_DIR": "backend/src/main/java/com/acme"
  }
}
```

Xoá `select_tests.py`, `route_table_dump.py`, `refactor_move_*.py` (adapter Python) hoặc thay bằng bản Java cùng CLI
(`test_hooks.py` tự SKIP ca của adapter đã xoá).

- `HANDOFF_TEST_RUNNER_REGEX` so với **đầu từng đoạn lệnh** sau khi bỏ tiền tố gán `VAR=…` (giá trị có nháy chứa
  khoảng trắng, `$(…)`, backtick đều được), `env`, toán tử gọi PowerShell `&` — nên `DB_URL=$(python … url t1)
  ./mvnw -q test`, `& .\mvnw.cmd test` đều được tính là đã chạy test.
- `HANDOFF_TEST_CMD_REGEX` (tìm ở bất kỳ đâu) bắt lệnh test bọc trong shell khác mà runner không thấy: `bash -c 'set
  -a; . ./.env; ./mvnw -q test'`, `cmd /c backend\mvnw.cmd verify`, `timeout 900 ./mvnw test`.
- `GUARD_STARTUP_CMD_REGEX` bắt cả `mvnw test|verify` / `gradlew test|check`: test `@SpringBootTest` khởi động context
  nên Flyway cũng tự chạy migration lên DB đích — DB đó ở máy khác thì guard **cảnh báo**.
- `GUARD_TEST_CMD_REGEX` so **tên DB** của lệnh test: DB dev và `qa_*` cùng một Postgres Docker local (`localhost:5435`)
  thì chỉ tên DB phân biệt được ⇒ test trỏ DB không phải `qa_*` bị cảnh báo dù host là localhost.
- `GUARD_ENV_WRAPPER_REGEX`: wrapper `run-backend.*` tự nạp `.env` ⇒ guard đọc DB từ `.env`, không từ biến môi trường
  đặt trước khi gọi wrapper.

**Test tích hợp lên DB tạm.** Spring Boot không tự đọc `.env`; lệnh test phải nạp `.env` (biến bắt buộc như JWT
secret, mật khẩu DB) **trước**, rồi đặt URL DB test **sau** (ghi đè URL dev trong `.env`), cùng một lệnh — đó là giá
trị `INTEGRATION_TEST_COMMAND` ở `PROJECT/stack-and-commands.md`. Biến đặt URL test tuỳ giai đoạn của dự án —
`GUARD_MIGRATION_DB_URL_VAR` phải khớp giai đoạn đó:

| Giai đoạn | Biến đặt URL test (`TEST_DB_ENV_VAR`) | `GUARD_MIGRATION_DB_URL_VAR` |
|---|---|---|
| **1. Chưa có profile test** riêng (mặc định của ví dụ trên) | đúng biến app đang đọc (`DB_URL_ENV_VAR`, vd `DB_URL`) | `FLYWAY_URL,DB_URL,SPRING_DATASOURCE_URL` |
| **2. Đã dựng profile test** đọc `TEST_DB_URL` **không default** (pack Spring §7) | `TEST_DB_URL` | `TEST_DB_URL,FLYWAY_URL,DB_URL,SPRING_DATASOURCE_URL` (`TEST_DB_URL` **đứng đầu**) |

Giai đoạn 1 (lệnh hoàn chỉnh, chạy từ gốc repo):

```bash
# bash / Git Bash
(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)
```

```powershell
# PowerShell
Get-Content .env | Where-Object { $_ -match '^\s*([A-Za-z_]\w*)\s*=(.*)$' } | ForEach-Object { Set-Item "env:$($Matches[1])" $Matches[2].Trim().Trim('"') }; $env:DB_URL = (python .claude/scripts/qa_db.py url <task>); & .\backend\mvnw.cmd -f backend/pom.xml -q test "-Dtest=<Lớp>IntegrationTest"
```

**Cấm hai kiểu ghép:** (1) `DB_URL=$(… qa_db.py url <task>) (set -a; . ./.env; …)` — gán tiền tố trước subshell là
lỗi cú pháp bash; (2) `export DB_URL=…` (hoặc `$env:DB_URL = …`) **trước** bước nạp `.env` — `.env` đè lại thành URL
dev. Guard tính đúng thứ tự này (mục [Migration guard](#migration-guard-context_guardpy)) và cảnh báo cả hai.

Giai đoạn 2: cùng lệnh, thay `DB_URL` bằng `TEST_DB_URL`. **Đừng dùng `TEST_DB_URL=… ./mvnw test` khi chưa có profile
test đọc biến đó:** app không đọc `TEST_DB_URL`, nên test — và Flyway migrate — chạy lên `DB_URL` trong `.env` (DB dev).
Với cấu hình giai đoạn 1, guard không coi `TEST_DB_URL` là URL DB, đọc `DB_URL` (lệnh → môi trường → `.env`, theo thứ tự
bộ nạp `.env` ở trên) và **cảnh báo** khi nó trỏ máy khác hoặc (test-db guard) DB không phải `qa_*`; chuyển sang giai đoạn 2 mà quên đưa `TEST_DB_URL` lên đầu danh sách thì ngược lại:
lệnh đúng bị cảnh báo nhầm theo `DB_URL` của `.env`.

User/mật khẩu DB tạm lấy từ `SPRING_DATASOURCE_USERNAME/PASSWORD` (hoặc đặt `QA_DB_JDBC_CREDENTIALS=query`).
**Cảnh báo:** nếu cấu hình profile test có giá trị mặc định trỏ DB dev (vd `url: ${TEST_DB_URL:jdbc:postgresql://dev-db:5432/app}`,
hoặc rơi về `${DB_URL}`) thì quên đặt `TEST_DB_URL` là test chạy — và Flyway migrate — thẳng lên DB dev. Guard chỉ thấy URL trong lệnh / biến môi
trường / `.env`, không đọc `application.yml`; nên bỏ default trỏ DB dùng chung khỏi `application.yml` (để thiếu biến
thì app dừng ngay) hoặc chỉ giữ default `localhost`.

### Node / Prisma + vitest

```json
{
  "env": {
    "GUARD_MIGRATION_CMD_REGEX": "\\bprisma\\s+(migrate\\s+(?:deploy|dev|reset)|db\\s+push)\\b",
    "GUARD_MIGRATION_DRYRUN_REGEX": "",
    "HANDOFF_CODE_PATHS": "apps/api/src/,apps/web/src/",
    "HANDOFF_BACKEND_SEGS": "apps/api/src/,apps/web/src/",
    "HANDOFF_TEST_CMD_REGEX": "",
    "HANDOFF_TEST_RUNNER_REGEX": "(?:npm|pnpm|yarn)\\s+(?:--prefix\\s+\\S+\\s+)?(?:run\\s+)?test\\b|npx\\s+(?:vitest|jest)\\b|(?:\\S*[/\\\\])?(?:vitest|jest)(?:\\s|$)",
    "HANDOFF_TEST_LABEL": "vitest",
    "HANDOFF_TEST_PREFILTER": "test,vitest",
    "HANDOFF_TEST_SELECT_HINT": "npx vitest related <file đã sửa>",
    "HANDOFF_MIGRATION_DIR": "prisma/migrations/",
    "EDIT_FORMAT_ROOT": "apps/web",
    "EDIT_FORMAT_SUBDIR": "src/",
    "EDIT_FORMAT_EXTS": ".ts,.tsx",
    "EDIT_FORMAT_CHECK_CMD": "npx --no-install prettier --check {file}",
    "EDIT_FORMAT_FIX_CMD": "npx prettier --write {file}",
    "EDIT_FORMAT_NAME": "prettier",
    "EDIT_API_SEG": "*/routes/*,*/controllers/*",
    "QA_DB_URL_DRIVER": "postgresql",
    "ROUTER_REGISTRY_FILE": "apps/api/src"
  }
}
```

Prisma đọc `DATABASE_URL` (mặc định của guard). DB test (lệnh hoàn chỉnh; tiền tố `VAR=…` chỉ áp cho lệnh đầu nên
dùng `export` trong subshell): `(export DATABASE_URL="$(python .claude/scripts/qa_db.py url <task>)"; npx prisma migrate deploy && npx vitest run)`.

## Khi xoá adapter/script

Dự án xoá một adapter hay script (vd `select_tests.py`, `route_table_dump.py`, `refactor_move_*.py`,
`codegraph_nudge.py`):

- Xoá dòng của nó ở bảng [Script](#script) (và hook gọi nó trong `settings.json`, nếu có).
- Bảng biến môi trường của nó: **xoá cho gọn** (không còn script đọc các biến đó; giữ lại dễ tưởng còn hiệu lực).
- Mục [Hợp đồng CLI của adapter](#hợp-đồng-cli-của-adapter): xoá dòng "`<script>` không có → …" và phần của nó trong
  đoạn "Tóm tắt" (mô tả giao diện CLI của script đã xoá). Thay bằng bản cùng giao diện thì giữ, sửa cho khớp bản mới.
- `test_hooks.py` tự `SKIP` ca của file đã xoá, không cần sửa.
- `.claude/scripts/hook_cases.json` của dự án **nên commit**: đó là test cấu hình hook của dự án, chạy lại bằng
  `test_hooks.py --project` mỗi khi đổi `env` trong `settings.json`.

## launch.json (Claude Preview)

`.claude/launch.json` là ví dụ JSON hợp lệ cho stack mặc định (Vite + FastAPI). Trường harness đọc: `name`,
`runtimeExecutable`, `runtimeArgs` (mảng, mỗi đối số một phần tử), `port` (**số**), `url` (tuỳ chọn; localhost thì
chỉ là origin `http://localhost:<port>`, không path). Không dùng `cwd` (không chắc harness hỗ trợ) — chạy trong thư
mục con bằng cờ của chính công cụ. Điền: thay hai placeholder thư mục frontend / backend trong `runtimeArgs` của
`launch.json` bằng thư mục thật (vd `frontend`, `backend` — cùng giá trị điền ở `PROJECT/stack-and-commands.md`),
sửa `port` khớp dev server.

| Stack | `runtimeExecutable` | `runtimeArgs` | `port` |
|---|---|---|---|
| Vite / npm (mặc định) | `npm` | `["--prefix", "frontend", "run", "dev"]` | `5173` |
| FastAPI (mặc định) | `python` | `["-m", "uvicorn", "app.main:app", "--app-dir", "backend", "--port", "8000"]` | `8000` |
| Spring Boot + Maven (bash) | `bash` | `["-c", "set -a; . ./.env; set +a; exec ./backend/mvnw -f backend/pom.xml spring-boot:run"]` | `8080` |
| Spring Boot + Maven (Windows) | `powershell` | `["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "scripts/run-backend.ps1"]` | `8080` |
| Spring Boot + Gradle (bash) | `bash` | `["-c", "set -a; . ./.env; set +a; exec ./backend/gradlew -p backend bootRun"]` | `8080` |
| Next.js | `npm` | `["--prefix", "web", "run", "dev", "--", "-p", "3000"]` | `3000` |

**Spring Boot không tự đọc `.env`.** Gọi thẳng `mvnw spring-boot:run` thì biến chỉ có trong `.env` (JWT secret, mật
khẩu DB, `DB_URL`…) không tới app → app dừng lúc khởi động vì thiếu property bắt buộc, hoặc tệ hơn: lặng lẽ dùng
default trong `application.yml` (có thể là DB dev dùng chung). Vì vậy mẫu trên chạy qua wrapper nạp `.env` trước.
`.env` phải là dòng `KEY=value` thuần (giá trị có khoảng trắng thì đặt trong nháy) để `.` của bash đọc được.
Wrapper PowerShell mẫu `scripts/run-backend.ps1` (đặt ở gốc repo, dự án tự tạo):

```powershell
# scripts/run-backend.ps1 — nạp .env rồi chạy Spring Boot (Claude Preview / chạy tay)
$root = Split-Path -Parent $PSScriptRoot
Get-Content (Join-Path $root '.env') | Where-Object { $_ -match '^\s*([A-Za-z_]\w*)\s*=(.*)$' } |
  ForEach-Object { Set-Item "env:$($Matches[1])" $Matches[2].Trim().Trim('"') }
& (Join-Path $root 'backend\mvnw.cmd') -f (Join-Path $root 'backend\pom.xml') spring-boot:run
```

Đặt `GUARD_STARTUP_CMD_REGEX` (ví dụ Java ở trên) để guard cảnh báo khi lệnh khởi động trỏ DB máy khác — guard chỉ
thấy lệnh `mvnw … spring-boot:run` khi Claude chạy qua Bash/PowerShell, không thấy lệnh harness Preview gọi.

Chỉ giữ cấu hình server thật sự cần xem trước; dự án không có giao diện web thì xoá file.

## Chế độ `--project`

```
python -I -B .claude/scripts/test_hooks.py --project [--settings <file>] [--cases <file>] [--root <dir>]
TEST_HOOKS_PROJECT=1 python -I -B .claude/scripts/test_hooks.py
```

- Không xoá biến `GUARD_*` / `HANDOFF_*` / `EDIT_*` khỏi môi trường; nạp khối `env` của `--settings` (mặc định
  `<root>/.claude/settings.json`; truyền `--settings` thì file nằm đâu cũng được, `--root` không cần có
  `.claude/settings.json`) đè lên.
- Chạy bảng ca `--cases` (mặc định `.claude/scripts/hook_cases.json`; mẫu: `hook_cases.example.json` cho
  Spring Boot + Flyway + Maven). Không lệnh nào được chạy thật — chỉ đưa cho hook đánh giá.
- `--root` (mặc định `CLAUDE_PROJECT_DIR` hoặc thư mục chứa `.claude/`): cwd của ca `bash`, nơi guard đọc `.env`.
  Ca `edit` chạy trong project tạm (không có HANDOFF task nào).

Định dạng ca (mỗi ca một khoá chủ thể + `expect`; tuỳ chọn `name`, `contains`, `env`, `shell` = `powershell` cho
ca `bash`/`test_cmd` là lệnh PowerShell — backtick là escape, đọc `$env:VAR = …`; `dotenv` (chuỗi hoặc mảng dòng) cho
ca `bash` = nội dung `.env` riêng của ca: hook chạy trong thư mục tạm chỉ có file đó, không đọc `.env` của `--root`):

| Khoá | Hook được kiểm | `expect` |
|---|---|---|
| `bash` | `context_guard.py bash` | `block` (exit 2) / `allow` / `warn` (cảnh báo startup) |
| `edit` | `edit_hooks.py` | `remind` / `silent` (post: nhắc tài liệu API) hoặc `block` / `allow` (pre) |
| `test_cmd` | `handoff._runs_tests` | `test` / `not-test` |
| `path` | `HANDOFF_CODE_PATHS` + `HANDOFF_TEST_PATH_REGEX` | `product` / `test` / `other` |

Exit 1 nếu có ca FAIL; dòng FAIL in kết quả thực tế và output hook để sửa regex.

# Test Strategy

> Đọc khi: viết test mới cho backend. Bỏ qua nếu chỉ implement code không kèm test, hoặc test frontend.

- Test runner backend: JUnit 5 + Spring Boot Test
- Lệnh chạy test: `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test)`
- Lệnh chạy test tích hợp trên DB `qa_<task>` (hoàn chỉnh, không ghép thêm): `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)`
- Ví dụ code, fixture, cờ dòng lệnh, thư mục chạy: chi tiết theo stack ở `.claude/stack-packs/backend-spring-boot-flyway.md`.

**Style (unit)**: pure unit tests against service/domain logic — no database, no HTTP client, no mocks. Tests instantiate
model objects directly and assert on return values or raised exceptions.

**Location:**

- Unit: `backend/src/test/java/vn/medischedule/<module>/*Test.java`
- Integration (DB thật): `backend/src/test/java/vn/medischedule/<module>/*IntegrationTest.java`

Fixture / cấu hình test dùng chung: **chưa có** (repo chưa có test — `backend/src/test/java` rỗng, chưa có profile
test). Task test đầu tiên dựng theo mục "Repo chưa có test nào" bên dưới và ghi lại ở đây (lớp test cơ sở, profile
`test`, helper gọi HTTP).

**What to test (unit)**: pure computation in service methods (date logic, validation, state derivations). Avoid testing
queries or HTTP routes in unit tests; those belong in integration (section below).

## Repo chưa có test nào: bắt đầu từ đâu

> Mục này dành cho repo chưa có test: xoá khi repo đã có bộ test và các mục dưới đã điền theo thực tế.

Task đầu tiên cần test (thường là task QA đầu tiên) dựng khung tối thiểu — ghi quyết định vào file này và
`stack-and-commands.md`, không dựng hạ tầng lớn trước khi có ca cần:

1. **Công cụ:** dùng test runner chuẩn của stack (vd pytest; JUnit 5 qua starter test của framework; Vitest/Jest cho
   TS) — kiểm manifest (`pyproject`/`requirements-dev`, `pom.xml`/`build.gradle`, `package.json`) xem đã có dependency
   test chưa; thêm dependency là thay đổi build ⇒ nêu trong design/HANDOFF. Điền các dòng `TEST_FRAMEWORK`, `TEST_COMMAND`,
   `TEST_SINGLE_COMMAND`, `INTEGRATION_TEST_COMMAND` ở bảng stack.
2. **Tách unit / integration ngay từ đầu** bằng thư mục hoặc hậu tố tên, để chạy riêng được: unit không DB, không HTTP,
   chạy vài giây; integration dùng DB thật `qa_<task>` (mục dưới). Điền các dòng `UNIT_TEST_DIR`, `INTEGRATION_TEST_DIR` ở bảng stack.
3. **Đặt tên:** theo quy ước runner tự nhận (vd `test_<module>.py`; `<Lớp>Test` cho unit và `<Lớp>IntegrationTest`
   hoặc `*IT` cho integration — kiểm plugin chạy test có nhận hậu tố đó không); một file/lớp test cho một service hoặc
   một luồng endpoint; tên hàm test mô tả hành vi (`tao_lich_trung_gio_bi_tu_choi`), không mô tả cài đặt.
4. **Hạ tầng tối thiểu cho integration:** một chỗ đọc biến DB test riêng, dừng ngay kèm lệnh đúng nếu thiếu (không rơi
   về DB dev — bước 5); dựng schema bằng chính `Flyway` (không bằng chế độ ORM tự tạo bảng — sẽ không bắt được
   migration hỏng); một helper tạo client HTTP gọi app thật.
5. **Dựng cấu hình test đọc biến DB test riêng _trước_ khi dùng biến đó.** Chừng nào hạ tầng test chưa có chỗ đọc biến
   riêng không default (vd Spring chưa có profile test đọc `TEST_DB_URL`), app chỉ đọc `DB_URL`: đặt biến
   riêng lúc đó là vô tác dụng — test và migration lúc khởi động chạy lên DB trong `.env`. Vì vậy:
   - **Chưa dựng:** `TEST_DB_ENV_VAR` ở bảng stack = giá trị `DB_URL_ENV_VAR`; `INTEGRATION_TEST_COMMAND` đặt đúng biến
     app đang đọc, sau bước nạp `.env` (vd `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py
     url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)`); cấu hình hook giữ danh sách biến URL
     mặc định của stack (`.claude/scripts/README.md`).
   - **Dựng xong** (cấu hình test đọc biến riêng, không default, thiếu thì dừng): đổi `TEST_DB_ENV_VAR` sang biến riêng
     ở bảng stack và ở mọi chỗ đã điền giá trị cũ. Giá trị cũ trùng tên biến của app nên **không** thay hàng loạt: tìm
     `grep -rnw '<biến cũ>' AGENTS.md CLAUDE.md .claude docs` rồi xét từng dòng — đổi các chỗ nói về DB **test** (giá trị
     `INTEGRATION_TEST_COMMAND`, lệnh `export VAR=…`, `$env:VAR = …`, câu văn "đặt / thiếu biến DB test" trong file
     này, `qa-process.md`, pack backend §7), giữ các chỗ nói về DB của **app**; thêm biến đó vào **đầu** `GUARD_MIGRATION_DB_URL_VAR` trong `.claude/settings.json`, sửa ca
     trong `hook_cases.json` rồi chạy `test_hooks.py --project`. Chi tiết Spring: pack backend §7.
6. **Ca đầu tiên nên viết:** một unit cho hàm tính toán thuần của task và một integration gọi endpoint qua HTTP theo
   luồng thật (cổng kịch bản mục 5, `PROJECT/qa-process.md`) — đủ để kiểm hạ tầng chạy được trước khi viết thêm.
7. **Chưa có script chọn test theo diff:** dòng `TEST_SELECT_COMMAND` ở bảng stack điền đúng chuỗi `chọn thủ công theo
   package/module bị sửa`; tạo file danh sách test đỏ sẵn (dòng `KNOWN_FLAKY_FILE`) rỗng khi có test đỏ sẵn đầu tiên.

## Integration test

- DB: mỗi task một DB rỗng `qa_<task>`, lấy URL bằng `python .claude/scripts/qa_db.py url <task>` (từ thư mục gốc repo)
  và đặt vào biến `DB_URL` — mọi lần chạy test tích hợp dùng lệnh hoàn chỉnh `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)`
  (đã gồm bước này, đúng thứ tự), không tự ghép biến vào `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test)`. DB tạo trên `localhost:5435`; kết nối
  admin `qa_db.py` lấy theo thứ tự `QA_DB_ADMIN_URL` → ghép từ `POSTGRES_*` → URL DB dev dự phòng (biến liệt kê ở `QA_DB_FALLBACK_URL_VARS`, mặc định `DATABASE_URL`),
  mỗi bước xét biến môi trường trước rồi `.env` gốc repo, host phải nằm trong danh sách cho phép (chi tiết:
  `.claude/scripts/README.md`). Không bao giờ trỏ vào DB dev/prod: hạ tầng test chỉ tự dựng lại schema
  và chạy song song trên DB tên `qa_*`. Kiểm DB đích bằng **host và tên DB** — DB dev có thể cùng host
  `localhost:5435` (Postgres Docker local chứa cả hai), khi đó chỉ tên `qa_*` chứng minh đúng DB test. `qa_db.py` đọc cấu hình (engine, host cho phép, dạng URL in ra…) ở đầu file /
  biến môi trường — xem `.claude/scripts/`.
- **Runner không tự đọc `.env`** (vd Spring Boot): `INTEGRATION_TEST_COMMAND`, trong **cùng một lệnh**, nạp `.env`
  **trước** (vd bash `set -a; . ./.env; set +a`, hoặc script wrapper sẵn có của dự án), đặt `DB_URL`
  **sau** (để ghi đè giá trị dev trong `.env`). Cấm: `VAR=… (subshell nạp .env …)` (lỗi cú pháp) và `export VAR=…`
  đứng trước bước nạp `.env` (`.env` đè lại thành DB dev). Đích cần đạt: cấu hình test (profile / file cấu hình của test) đọc URL DB test từ biến riêng
  **không có giá trị mặc định**; chưa dựng thì `DB_URL` chính là biến app đang đọc (bước 5 mục "Repo chưa
  có test"). Nếu cấu hình chính có default trỏ DB dev (vd `${DB_URL:jdbc:postgresql://<dev>/…}`) hoặc `.env` đặt biến
  đó tới DB dev, thì quên đặt biến = test chạy (và migration chạy lúc khởi động áp) lên DB dev. Lệnh cụ thể:
  `.claude/stack-packs/backend-spring-boot-flyway.md`.
- **Máy không vào được DB test chung**, chọn một:
  - `PostgreSQL 16` local: đặt `QA_DB_ADMIN_URL` trỏ tới instance local trong `.env` gốc repo, rồi dùng `qa_db.py url`
    như trên (host local phải nằm trong danh sách host cho phép của `qa_db.py`).
  - Docker / container test: **chỉ** khi hạ tầng test đã được dựng để tự tạo container (vd Testcontainers) **và** cấu
    hình test không có giá trị mặc định trỏ DB dev — ghi rõ ở mục trên lệnh chạy tương ứng. Chưa có hạ tầng đó thì
    **không** bỏ trống `DB_URL`: thiếu biến, test (và migration chạy lúc khởi động) sẽ rơi về URL trong
    `.env` / mặc định — tức DB dev. Mỗi worker song song một container.
- Cache: chưa có cấu hình test riêng — `@SpringBootTest` dùng Redis theo `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`
  của `.env` (Redis dev trong Docker, `localhost:6390`). Test có ghi cache thì dùng key riêng của test và dọn sau khi
  chạy; cần tách hẳn thì dựng cấu hình cache test ở task test đầu tiên.
- **Test migration:** chỉ viết khi migration có biến đổi dữ liệu đáng ngờ (backfill, tách/gộp cột). Kiểm dữ liệu sau
  khi áp migration trên DB `qa_*` riêng; không để test để lại schema dở dang cho test khác (vd test chạy migration
  ngược / migration lỗi giữa chừng phải nằm trong transaction rollback hoặc DB riêng, nếu không các test sau đỏ dây
  chuyền). **Xoá test migration khi migration đã lên `main`**. Bất biến chung của chuỗi migration (tuyến
  tính, không trùng version) do Flyway tự kiểm khi khởi động — không có test chuỗi riêng.
- Song song (chỉ khi ≥ 8 file; khởi động test có nạp app tốn thời gian nên gom file vào một lệnh —
  `PROJECT/qa-process.md` "Tốc độ test"): số worker lấy từ `python .claude/scripts/qa_db.py workers`, mỗi worker một DB
  `qa_<task>_gwN`. Runner không hỗ trợ chạy song song theo worker thì chạy một process.
- **Thế giới đắt dùng chung (tuỳ chọn):** hàm dựng dữ liệu tốn vài giây bọc bằng snapshot DB mẫu (vd tạo DB từ template
  ~0,3 s mỗi test) thay vì dựng lại mỗi test. Dự án chưa dùng.
- Chọn test theo diff: chọn thủ công theo package/module bị sửa; so baseline (HEAD sạch) chỉ cho test đỏ.
- Dọn: `python .claude/scripts/qa_db.py drop <task>` khi QA được chấp nhận; `qa_db.py list` xem DB sót.
- **DB sạch là mặc định, mọi lần chạy test tích hợp dùng `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)`** (đặt `DB_URL`
  ngay trong lệnh, không dựa vào URL DB sẵn có của máy / `.env`, không đánh dấu/hạ revision migration trên DB dev). Test đỏ → chạy lại đúng file đó trên `qa_<task>` mới **trước
  khi điều tra**: DB bẩn (dữ liệu test khác, bảng `bak_*` sót) là nguyên nhân thường gặp nhất của "đỏ có sẵn".
- **Test đỏ có sẵn / chập chờn:** nguồn duy nhất là `backend/src/test/KNOWN_FLAKY.txt` — test nằm trong đó đỏ thì không điều tra
  lại, chỉ báo khi có test KHÁC đỏ. Sửa xong hoặc phát hiện thêm thì cập nhật file đó.

## Frontend — kiểm không cần test runner

Frontend chưa có test runner, lint hay script kiểm hàm thuần; chi tiết theo stack: xem
`.claude/stack-packs/frontend-react-antd-react-query.md`.

- Type check: `npm --prefix web run typecheck`. Lưu ý type checker thường KHÔNG bắt lỗi dùng biến trước khi khai báo trong
  component (lỗi chỉ lộ lúc render).
- Chưa có ESLint: rà tay lỗi dùng biến trước khai báo ở component lớn; thêm ESLint là quyết định riêng (task riêng).
- Thay đổi UI: chạy type check rồi báo; user tự kiểm UI (`LESSONS.md` § Frontend).

# QA — hai giai đoạn, cổng kịch bản, tốc độ test, chạy thật

Tách từ `WORKFLOW.md` §3.4. Đọc khi ở phase `qa-plan`/`qa-run` hoặc khi duyệt kịch bản.

1. **`qa-plan`:** đọc AC từ BA Spec, viết `docs/tests/[feature]-test-cases.md` kèm bảng ánh xạ `AC → tầng test → file spec`
   (mục 2b của template), rồi qua cổng kịch bản.
2. **`qa-run`:** chỉ chạy khi kịch bản đã qua cổng. Viết test `JUnit 5 + Spring Boot Test` (unit/integration), chạy, dán output thật, ra verdict.

**QA gộp một lượt — Flow B và C-nhỏ** (C, G, F, Full vẫn hai giai đoạn). `code-review` LGTM → thẳng
`qa-run`, không có phase `qa-plan`, không file kịch bản duyệt trước, không vòng `REVIEW-REQUEST` kịch bản:
- AC lấy từ delta ghi ở mục "Báo cáo" của HANDOFF (C-nhỏ) hoặc mô tả task (B).
- Viết test + chạy luôn; `docs/tests/[feature]-test-cases.md` chỉ gồm bảng `AC → tầng test → file::test`, output
  test thật và mục Chạy thật (khi bắt buộc). Không viết test case văn xuôi.
- Mục 2 và 5 của cổng bên dưới vẫn phải đúng (AC có logic backend có test tự động; endpoint đổi có test qua HTTP client) —
  ghi một dòng xác nhận trong báo cáo thay cho bảng tự chấm kèm bằng chứng. AC mơ hồ → dừng hỏi như thường.
- Task hoá ra chạm > 5 file / schema / contract / quyền → quay về đường hai giai đoạn (WORKFLOW §3.2).

**Cổng kịch bản — đạt cả 5 mục thì tự duyệt, đi tiếp, không dừng:**

1. Mọi AC trong BA Spec có đúng một dòng ánh xạ tầng test.
2. AC có logic backend / tiền / race / migration / phân quyền ánh xạ Integration hoặc Unit, không chỉ Thủ công.
3. AC Thủ công có steps + expected rõ ràng.
4. Không còn AC mơ hồ cần hỏi lại nghiệp vụ.
5. Endpoint mới/đổi trong diff: có ít nhất một integration test **gọi qua HTTP client** (không chỉ gọi service) theo
   đúng luồng thật — dữ liệu tạo bằng các bước trước của luồng, đã nằm sẵn trong session, không seed object mới ngay
   trước lệnh gọi. (Bài học: lỗi lazy-load của ORM trên object có sẵn trong session chỉ lộ ở prod khi test seed object
   mới — ví dụ cụ thể theo stack: xem `.claude/stack-packs/<pack đang dùng>`.)

Đạt → ghi `Kịch bản tự duyệt theo checklist §3.4 (ngày)` vào file test-cases và HANDOFF, đổi `Phase` sang `qa-run`;
báo cáo cuối dẫn đường dẫn kịch bản để user xem lại. Không đạt (hoặc QA không chắc) → dừng, nêu mục không đạt, hỏi user
qua `AskUserQuestion` (Phiên task/Điều hành: phiên task duyệt, điều hành được duyệt thay nếu user uỷ quyền, không gồm
push/deploy/đọc prod/ghi DB staging-prod).

**Ai làm gì** (theo mô hình, ORCHESTRATION §2):
- *Phiên task / Điều hành:* session QA riêng (Sonnet, vai `qa`) viết kịch bản → `REVIEW-REQUEST`; **phiên task duyệt**
  (tự đếm AC so với spec, kiểm ca tiền/race/migration/phân quyền có test thật) → "DUYỆT" → session QA viết + chạy test →
  `REVIEW-REQUEST` kèm báo cáo → phiên task review, chạy lại vài test then chốt nếu nghi ngờ. Lỗi code → session dev
  sửa; test sai → session QA sửa.
- *Solo:* giao cho subagent `qa-writer`, session là bên duyệt.

**Phạm vi test — chỉ chức năng đã sửa và chỗ bị ảnh hưởng.** Không chạy cả bộ. Chọn test theo diff:
chọn thủ công theo package/module bị sửa (chọn thủ công = lớp/file test cùng package/module với code bị sửa và nơi gọi nó; script
chọn test thì xem cách nó lan theo import ở `.claude/scripts/README.md`), rồi QA bỏ bớt file không liên quan AC. Hồi quy toàn bộ chỉ cho Flow F hoặc khi
phiên task yêu cầu. So baseline (HEAD sạch) **chỉ cho test đỏ**, không chạy cả bộ hai lần.

**Không lặp test dev:** AC đã có test dev kiểm cùng hành vi ⇒ bảng ánh xạ AC trỏ tới test đó, không viết lại. Test QA
mới chỉ phủ AC/nhánh/biên dev chưa có và viết vào file/lớp test dev của task — không tạo file/lớp test "bản QA" riêng
(file QA trùng lặp làm bộ test phình và chậm).

**Tốc độ test:**
- **Mỗi lệnh test có test nạp app (router / entrypoint / application context) tốn thời gian khởi động**
  (đo trên máy dev: chưa đo — đo ở task test đầu tiên) trước khi test đầu tiên chạy. Vì vậy **gom mọi file/lớp cần chạy vào MỘT
  lệnh**, không chạy từng file một; vòng sửa thì chỉ chạy lại test đỏ lần trước trên đúng lệnh đó (cờ "chạy lại test
  đỏ" của runner nếu có, không thì liệt kê đúng file/lớp đỏ). Test thuần service/model (không nạp app) khởi động nhanh
  hơn nhiều — AC tính toán thuần ưu tiên tầng này.
- **Không vòng lặp chờ** (`until … sleep`, `for … sleep`) để canh log test: lệnh dưới 10 phút chạy foreground; lâu hơn
  thì `run_in_background` và chờ thông báo của harness.
- Unit (không DB) và integration chạy **song song** trong cùng lượt (bộ chọn test in sẵn lệnh mẫu nếu có), chờ cả hai.
- DB test: chạy test tích hợp bằng lệnh hoàn chỉnh `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)` (thay `<task>`, file/lớp test) —
  không tự ghép `DB_URL=…` vào `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test)` (lệnh nạp `.env` sẽ đè lại, hoặc lỗi cú pháp). `qa_db.py
  url <task>` tạo `qa_<task>` trên `localhost:5435` (kết nối admin theo thứ tự `QA_DB_ADMIN_URL` → `POSTGRES_*` → URL DB dev dự phòng, biến môi trường
  trước `.env` — `.claude/scripts/README.md`), tên thống nhất để dọn. Hạ tầng test nên bỏ qua bước dựng schema
  khi schema không đổi, tự dựng lại khi model/migration đổi. **Không có DB test chung mặc định** (dữ liệu cũ gây đỏ
  giả): thiếu `DB_URL` thì hạ tầng test dừng ngay kèm lệnh đúng (không rơi về DB dev) — khi đã có cấu hình test
  đọc biến riêng; chưa có (`PROJECT/testing.md`, mục "Repo chưa có test" bước 5) thì kiểm DB đích theo HARD-STOPS
  trước mỗi lệnh test — **cả host lẫn tên DB**: DB test phải tên `qa_*` (DB dev có thể cùng host `localhost:5435`).
  Hook/script đọc
  cấu hình ở đầu file / biến môi trường — xem `.claude/scripts/`.
- Song song — **chỉ khi ≥ 8 file integration** (ít hơn thì một process nhanh hơn, vì mỗi worker trả lại chi phí import
  và tự dựng schema DB riêng): số worker lấy từ `python .claude/scripts/qa_db.py workers` (tự chia theo số task đang
  `qa-run` để không quá tải máy DB); mỗi worker một DB `qa_<task>_gwN`, chỉ chạy trên DB `qa_*`. Cờ chạy song song của
  `JUnit 5 + Spring Boot Test` (nếu có): `.claude/stack-packs/backend-spring-boot-flyway.md`.
- Integration: dùng lại DB `qa_<task>` đã có schema thay vì tạo mới mỗi lượt; chạy theo nhóm file liên quan trước, đỏ
  thì sửa và chạy lại đúng file/lớp đỏ, cuối cùng chạy lại cả danh sách đã chọn một lần.
- Không chạy integration từ nhiều session cùng lúc trên cùng DB `qa_*`.
- **Test có vấn đề sẵn:** `backend/src/test/KNOWN_FLAKY.txt`. Test đỏ nằm trong mục [chập chờn] → chạy lại đúng test đó một
  lần; nằm trong [đỏ sẵn] → so baseline HEAD sạch rồi mới kết luận. Chỉ báo "lỗi mới" cho test không có trong
  danh sách hoặc đỏ khác baseline. Sửa xong / phát hiện thêm thì cập nhật file.
- **Dọn DB test:** ngay khi phiên task **chấp nhận báo cáo QA** (test đạt, không yêu cầu chạy lại):
  `python .claude/scripts/qa_db.py drop <task>` (xoá `qa_<task>` và `qa_<task>_gw*`). `handoff.py close` cũng tự dọn
  (lưới an toàn). Xem DB còn sót: `qa_db.py list`.

| Tầng | AC loại nào |
|------|-------------|
| Integration | Cần DB thật: race condition, transaction, migration, phân quyền endpoint |
| Unit | Tính toán thuần: ngày giờ, tiền, làm tròn, validate, state derivation |
| Thủ công | Chỉ quan sát được qua UI: điều hướng, form, hiển thị — QA ghi steps + expected, user tự kiểm |

**Chạy thật trước khi báo PASSED** (bắt buộc khi diff đổi frontend hoặc endpoint; bỏ cho Flow D và task chỉ BE nội
bộ). Session QA (Solo: chính session) chạy app — preview `backend` + `frontend` (`:5175`) — đăng nhập
tài khoản test, đi đúng luồng của từng AC Thủ công và AC có endpoint đổi bằng trình duyệt dựng sẵn (`get_page_text` /
`read_page`, ảnh `scale: 0.5` khi cần nhìn layout). Ghi mỗi AC: đạt / lỗi + ảnh vào mục "Chạy thật" của file test-cases.
- **Trước khi chạy backend:** kiểm `DB_URL` (inline / shell / `.env` gốc repo — vd
  `grep DB_URL .env`; lệnh tự nạp `.env` thì `.env` thắng shell) — chỉ chạy khi trỏ DB dev
  `localhost:5435 (DB medischedule)` hoặc `qa_<task>` (so host **và** tên DB); khác (prod) → dừng, báo phiên task.
- Lỗi tìm thấy = FAILED như test đỏ → dev sửa. Không chạy được (thiếu dữ liệu, cần phần cứng thật) → ghi rõ phần
  chưa kiểm vào báo cáo cho user, không bỏ qua im lặng.
- Lý do: phần lớn việc sửa lại sau giao là lỗi chỉ lộ khi chạy thật trên trình duyệt.

**PASSED = đủ cả 3:** test tự động xanh, mọi AC ánh xạ tới ít nhất 1 test (tự động, hoặc thủ công với steps rõ
ràng), **và** bước chạy thật không có lỗi mở (khi bắt buộc).
AC có logic backend mà chỉ ánh xạ thủ công → FAILED vì thiếu coverage. Chưa dùng E2E.

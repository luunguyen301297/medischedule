# Mô hình làm việc: Solo, Phiên task, Điều hành

Cả ba dùng chung flow (skill `route-task`, WORKFLOW.md), chung HANDOFF theo task (§4), chung quy tắc dừng (HARD-STOPS.md).
Khác nhau ở chỗ ai làm phase nào.

## 1. Chọn mô hình

| Mô hình | Khi nào | Tầng |
|---|---|---|
| **Solo** | Việc rất nhỏ: hotfix (Flow A), Flow B vài dòng, C-nhỏ; hoặc user muốn làm trong một session | 1 session làm hết |
| **Phiên task** (mặc định cho một task) | Mọi task còn lại | Phiên task (Opus) → session triển khai (Sonnet) |
| **Điều hành** | Từ 2 task trở lên **liên quan nhau** (chung yêu cầu, chung file/quy tắc, cần rà chéo hoặc gộp theo thứ tự) | Phiên điều hành → mỗi task một phiên task (Opus) → session triển khai (Sonnet) |
| Nhiều **Phiên task** song song | Nhiều task **độc lập, nhỏ** | Mỗi task một phiên task, user nói chuyện trực tiếp — rẻ và nhanh hơn 3 tầng |

Phiên task trong mô hình Điều hành làm **y hệt** phiên task đơn lẻ; khác duy nhất: hỏi user qua phiên điều hành (§5).
Chỉ giao tiếp giữa hai tầng liền kề: triển khai ↔ phiên task ↔ điều hành ↔ user.

## 2. Phân vai theo phase

| Phase | Ai làm | Ghi chú |
|---|---|---|
| `ba` | Phiên task (Opus) | Hỏi user trực tiếp (2 tầng) hoặc qua điều hành (3 tầng, §5.4) |
| `design`, `tl-review`, `contract` | Phiên task | TL review: phiên task tự review design của mình bằng checklist TL; design lớn thì giao subagent độc lập |
| `dev` | **Session dev** (Sonnet, vai `dev`) | Code + unit test của phần mình sửa; xong `set-phase code-review`, báo `DONE` |
| `code-review` | Phiên task | Không viết code nên khách quan với cách code, nhưng có thể cùng điểm mù với design của chính mình: nếu diff **RỦI RO CAO** — có file khớp danh sách "Vùng rủi ro cao" trong `PROJECT/conventions.md` (tiền/giá, phân quyền, migration; script chọn test theo diff in cờ này nếu dự án có và đã cấu hình, không thì tự đối chiếu diff với danh sách) thì giao thêm subagent `code-reviewer` (không biết design) và gộp hai kết quả. REQUEST CHANGES → nhắn session dev sửa (auto-loop) |
| `qa-plan` | **Session QA** (Sonnet, vai `qa`) viết kịch bản từ BA spec → `REVIEW-REQUEST`. Mở **cùng lượt với session dev** (§3.1) nên viết kịch bản song song lúc dev code; chỉ chạy test sau khi phiên task nhắn `DUYỆT` kèm diff (dev `DONE` + code review LGTM). QA chỉ ghi thư mục test (`backend/src/test/java/vn/medischedule/<module>/*Test.java`, `backend/src/test/java/vn/medischedule/<module>/*IntegrationTest.java`) và `docs/tests/` (hook chặn session QA sửa code sản phẩm) | Phiên task duyệt theo checklist `PROJECT/qa-process.md` → nhắn "DUYỆT, chạy" hoặc yêu cầu sửa |
| `qa-run` | Session QA chạy test → `REVIEW-REQUEST` kèm báo cáo | Phiên task review báo cáo. Lỗi code → session dev sửa; test sai → session QA sửa; rồi chạy lại phần liên quan. **Chấp nhận** → `qa_db.py drop <task>` ngay |
| `distill` | Phiên task | KB (PROJECT/kdd.md) khi `knowledge_impact != none` |
| đóng | Phiên task (2 tầng) / điều hành xác nhận (3 tầng) | Kiểm diff + test → `handoff.py close` → commit gồm cả `docs/HANDOFF.history.md` + `docs/HANDOFF.timing.jsonl` (WORKFLOW §1) → push / merge |

**Giữ ngữ cảnh phiên task gọn:** HANDOFF là nguồn sự thật — cuối mỗi phase ghi 2–3 dòng vào mục Báo cáo (quyết định,
commit, test) để sau compact vẫn nối được. Review diff theo file (`git diff --stat` rồi từng file), không đọc cả diff một
lần; báo cáo của dev/QA gửi lên ngắn, chi tiết nằm trong file (`docs/tests/…`, `docs/reviews/…`).

**Gộp nhiều task trước khi push:** khi từ 2 nhánh task trở lên vào `main` cùng đợt, gộp vào worktree tạm và chạy hợp các
danh sách test đã chọn của từng task (chọn test theo diff: chọn thủ công theo package/module bị sửa) trên cây đã gộp (DB `qa_merge_<đợt>`), xanh mới push; xong thì dọn DB và worktree.

**Phiên điều hành** (3 tầng) không làm BA chi tiết: phân tích, chia task, brief, chuyển câu hỏi user, rà spec chéo, giám sát. Chi tiết 3 tầng (vai điều hành, brief, hỏi user qua điều hành, rà spec): [PROJECT/orchestration-3tier.md](./PROJECT/orchestration-3tier.md).

**Tăng tốc, model, chi phí:** WORKFLOW §3.7 (điều hành chỉ khi ≥ 2 task phụ thuộc; tối đa 2 phiên Opus; hạn mức gần cạn thì không mở task mới). Dev/QA Sonnet, phiên task/điều hành Opus, cùng permission mode với phiên mở nó. Phiên task không đọc thẳng diff lớn (> ~400 dòng hoặc > 10 file): giao subagent `code-reviewer`. Một phiên điều hành cho một đợt việc.

## 3. Brief

Brief tự chứa đủ để làm — session mới không thấy hội thoại của phiên mở nó. Dòng `[[handoff-role <task> <vai>]]` bắt
buộc: hook ghi lại vai trò session để nhắc nó không dừng sai chỗ (§6).

*Phiên điều hành → phiên task:* xem file 3 tầng ở trên.

**Phiên task → session dev:**
```
[[handoff-role <task> dev]]
Task: <tên> | HANDOFF: docs/HANDOFF.<task>.local.md | Phiên task: <id>
Spec + design đã duyệt: <đường dẫn>  |  Phạm vi: <glob>  |  Ngoài phạm vi: <glob>
Làm: implement + unit test cho phần sửa; chạy test liên quan (`PROJECT/qa-process.md` "Phạm vi test") và CHỜ kết quả trong lượt.
Xong: `handoff.py set-phase <task> code-review`, báo `DONE | <task> | commit … | test …`.
Cây chung: chỉ git add file của task; không stash/restore cả file; migration mới nối sau điểm cuối (head / version cao nhất) của `origin/main`.
Lệch spec / thiếu thông tin: `handoff.py raise`, báo phiên task, chờ `RE Q<n>`. SendMessage nạp trễ: ToolSearch "select:SendMessage".
```

**Phiên task → session QA:**
```
[[handoff-role <task> qa]]
Task: <tên> | HANDOFF: … | Phiên task: <id> | BA spec: <đường dẫn> | Diff: <base>..<head> (hoặc "gửi kèm DUYỆT" khi mở cùng dev)
Bước 1: viết kịch bản + bảng AC → tầng test, CHỈ cho chức năng đã sửa và chỗ bị ảnh hưởng (PROJECT/qa-process.md), tự chấm
checklist; báo `REVIEW-REQUEST | <task> | kịch bản`. Không `set-phase` khi dev còn đang code (phiên task đổi phase).
Chờ "DUYỆT" (kèm diff) — bổ sung ca hồi quy cho diff đó (chọn test theo diff: chọn thủ công theo package/module bị sửa) trước khi viết test.
Bước 2 (sau khi duyệt): `set-phase qa-run`; viết test `JUnit 5 + Spring Boot Test`; chạy theo "Tốc độ test" (PROJECT/qa-process.md) và chờ kết quả trong
lượt; diff đổi FE/endpoint thì làm bước "Chạy thật" (PROJECT/qa-process.md) trên app dev; báo `REVIEW-REQUEST | <task> | báo cáo
test` kèm số pass/fail, lệnh đã chạy và kết quả chạy thật.
```

### 3.1 Khi nào mở được session con (giới hạn của app)

App **chỉ cho `start_session` trong lượt do user gõ trực tiếp** ở session đó. Lượt do note session con, tin nhắn của
session khác, hoặc cả session được session khác mở (phiên task 3 tầng) → bị từ chối ("Start sessions only on the user's
own words…"). Lỗi hay gặp: dev xong rồi mới mở QA → bị từ chối → thẻ đề xuất nằm chờ user bấm.

- **Phiên task 2 tầng:** ngay lượt user duyệt design/plan, mở **cả session dev lẫn session QA** (QA viết kịch bản song
  song). Sau đó chỉ dùng `SendMessage` (luôn được phép) để giao tiếp, không cần mở thêm.
- Bị từ chối giữa chừng: không thử lại; đề xuất thẻ (`initiation: "own_initiative"`) **và** báo user một dòng "bấm thẻ
  <tên> để chạy tiếp", hoặc chuyển sang subagent nếu việc gọn.

## 4. HANDOFF theo task

- Mỗi task một file `docs/HANDOFF.<task>.local.md` (không git-tracked), kiểm trùng tên trước. Tạo:
  `python .claude/scripts/handoff.py new <task> --flow C --title "…" --scope "<glob>" --orchestrator <id phiên mở>`.
  Chưa biết Scope (task mới ở BA): `--no-scope`, phiên task điền sau khi design xong (`scope-suggest` gợi ý từ commit).
- Scope dùng cho hook `guard`: sửa file chỉ thuộc Scope của task chưa duyệt thiết kế → bị chặn.
- Đổi phase: `handoff.py set-phase <task> <phase>` ngay lúc đổi (sửa cả block lẫn yaml).
- Đóng: `handoff.py close <task> --summary "…"` (append history + xoá file). `handoff.py stale` liệt kê file idle sót.
- Phiên điều hành giữ `docs/HANDOFF.<đợt>-orchestrator.local.md` (chi tiết ở file 3 tầng).

## 5. Giao tiếp

### 5.1 Kênh

- Id phiên: `get_session("self")` (dạng `local_…`); id session con có trong kết quả `start_session`. Ghi vào HANDOFF/bảng.
- **Lên tầng trên:** báo cáo cuối lượt (app chuyển về phiên mở nó kèm khoảng 400 ký tự đầu) với **dòng đầu**
  `<TRẠNG THÁI> [Q<n>] | <task> | <1 câu>`; việc gấp giữa lượt thì `SendMessage` (`to` = id, hoặc `"parent"`).
- **Xuống tầng dưới:** `SendMessage` (`to` = id session), mở đầu `RE Q<n>:` hoặc `DUYỆT:` / `SỬA:`. Trả lời tin đến thì
  dùng `from` của nó làm `to`. Chờ thì để app đánh thức khi session con xong lượt, không hỏi dò lặp lại.
- **App chỉ đánh thức tầng trên khi mọi session con của nó đều rảnh** → con báo `DONE`/`REVIEW-REQUEST` lúc anh em còn
  chạy thì note nằm chờ (phiên task có thể bỏ sót DONE tới khi user hỏi). Vì vậy session con kết thúc lượt bằng trạng
  thái dừng thì **cũng `SendMessage` `"parent"` đúng dòng trạng thái đó**. Tầng trên xử lý idempotent: đọc Phase trong
  HANDOFF trước khi làm, note đến sau cho việc đã xử lý thì bỏ qua.
- Cùng permission mode giữa các tầng, nếu không tin nhắn bị giữ chờ user duyệt.
- **Nguồn sự thật** là HANDOFF (Phase, mục `Q<n>`), không phải tin nhắn.

### 5.2 Trạng thái

| Trạng thái | Ai dùng | Nghĩa |
|---|---|---|
| `DONE` | dev, QA, phiên task | Xong phần việc của vai mình (dev: đã `set-phase code-review`; phiên task: phase `idle`) |
| `REVIEW-REQUEST` | QA | Gửi kịch bản hoặc báo cáo test cho phiên task duyệt |
| `WAITING-CHILD` | phiên task, điều hành | Đang chờ session con của mình (app sẽ đánh thức) |
| `SPEC-DEVIATION` / `QUESTION` / `BLOCKED` | mọi vai | Cần quyết định / bị chặn; đã `handoff.py raise` |
| `PROGRESS` | mọi vai | **Chỉ** trong `SendMessage` giữa lượt; không dùng để kết thúc lượt |

### 5.3 Gặp lệch spec / thiếu thông tin

Dừng phần bị ảnh hưởng (không đoán, không "sửa spec cho khớp code"); `handoff.py raise <task> --kind
spec-deviation|blocked|question --text "<spec nói gì / thực tế> | tác động | phương án | khuyến nghị>"`; báo tầng trên;
chờ `RE Q<n>`. Tầng trên đánh giá theo tiêu chí dưới; quá quyền thì chuyển tiếp lên nữa.

**Tự quyết** khi đủ cả ba: (a) suy ra được từ nguồn đã chốt (spec đã duyệt, design TL duyệt, KB, quyết định user đã
ghi, ADR, convention); (b) không đổi hành vi user thấy, tiền/giá, phân quyền, dữ liệu prod ngoài spec; (c) hoàn tác được.
**Hỏi user** khi: hai nguồn đã chốt mâu thuẫn; cần quyết định nghiệp vụ mới; chạm tiền/giá/phân quyền/dữ liệu prod; đổi
AC, phạm vi, API contract hay schema đã duyệt; hoặc không chắc. Tự quyết thì `handoff.py resolve` + ghi nhật ký + báo
user một dòng ở lần báo kế tiếp.

### 5.4 / 5.5 Hỏi user và rà spec qua điều hành (3 tầng)

Xem [PROJECT/orchestration-3tier.md](./PROJECT/orchestration-3tier.md): phiên task gom mọi câu GATE 1 vào một tin; điều hành phân loại (tự trả lời / trả về / hỏi user) và rà spec trước GATE 3.

### 5.6 Báo user

Mỗi lần trạng thái đổi đáng kể: **task – trạng thái – user cần làm gì**, một dòng mỗi task. Không dán nguyên báo cáo con,
không dự đoán kết quả con chưa báo.

## 6. Giám sát: không để task dừng giữa chừng

Hook Stop nhắc session nếu nó định dừng mà dòng đầu không phải trạng thái dừng hợp lệ của vai nó (theo dòng
`[[handoff-role …]]`) — vd. "đang chạy nền", PROGRESS, DONE khi chưa đổi phase. Hook không thấy session **chết giữa
lượt**; phần đó tầng trên kiểm mỗi khi nhận note hoặc khi user hỏi tiến độ:

| Tình trạng | Dấu hiệu | Làm ngay |
|---|---|---|
| Chờ quyết định | Có Q OPEN, dòng đầu SPEC-DEVIATION/QUESTION/BLOCKED | §5.3 |
| Kẹt | Session idle, phần việc chưa xong, không Q OPEN | `SendMessage`: "Tiếp tục `<task>` từ `<phase>`: <bước kế>. Chờ việc nền trong lượt." |
| Chết giữa lượt | `list_events` dừng ở tool call không có câu trả lời cuối, `isRunning: false` | `SendMessage`: "Lượt trước bị ngắt ở <bước>. Kiểm git log, HANDOFF, test rồi làm tiếp." |
| HANDOFF lệch | Phase khác việc đang làm | Nhắc `set-phase` |

Reject của TL / code review / QA vẫn tự lặp không giới hạn (HARD-STOPS). Sau compact, đọc lại HANDOFF (Phase, Q OPEN),
file điều hành và `git log` theo Scope để nối lại; không giả định session con đã làm điều chưa báo.

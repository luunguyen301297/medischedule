# Workflow — Flow Routing & Pipelines

Định nghĩa flows, skill pipelines và quy trình điều phối qua file cấu hình `docs/HANDOFF.<task>.local.md`. Quy tắc chặn
nghiêm ngặt xem tại `.claude/HARD-STOPS.md`.

## 1. Cơ chế Kích hoạt Cục bộ (Handoff Lifecycle)

Mỗi task có state riêng để nhiều task chạy song song không đụng nhau. Cách chọn Solo / Phiên task / Điều hành và vai trò
session: [ORCHESTRATION.md](./ORCHESTRATION.md).

| File                           |   Git-tracked?   | Vai trò                                                                                                                                                  |
|--------------------------------|:----------------:|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `docs/HANDOFF.<task>.local.md` | ❌ (`.gitignore`) | State đang chạy của **một** task — mọi lần chuyển `phase` ghi vào đây. Mỗi task/máy/branch một file nên không conflict. Tạo bằng `handoff.py new`.         |
| `docs/HANDOFF.history.md`      |        ✅         | Lịch sử task đã đóng — chỉ **append**, không sửa entry cũ (`merge=union`). `handoff.py close` ghi vào đây rồi xoá file của task.                         |

**Entry point chuẩn:** hook đầu mỗi turn liệt kê các task đang chạy. Mở `docs/HANDOFF.<task>.local.md` của task mình,
xác định phase, tra bảng Phase → skill (WORKFLOW §3.1), invoke skill bằng `Skill` tool.

**Auto-proceed / stop rules:** xem [HARD-STOPS.md](./HARD-STOPS.md) — nguồn duy nhất, không lặp lại ở đây.

* **Khởi tạo:** `python .claude/scripts/handoff.py new <task> --flow <X> --title "…"` (kiểm tên trùng trước). Nếu task
  thuộc domain có KB, điền `domain`, `capability`, `feature`, `knowledge_impact` trong khối yaml.
* **Kết thúc:** khi flow đạt `idle` (và đã qua `distill` nếu có, ghi `knowledge_distill_status`), session sở
  hữu task chạy `python .claude/scripts/handoff.py close <task> --summary "…"`: append vào history rồi xoá file.
  `handoff.py stale` liệt kê file đã idle còn sót.
* **Đóng trước, commit sau:** chạy `handoff.py close` **trước** commit cuối của task, rồi `git add` thêm
  `docs/HANDOFF.history.md` và `docs/HANDOFF.timing.jsonl` vào cùng commit với code. Một commit, một lần push; không
  tách commit riêng cho HANDOFF. Hai file này chỉ append nên gộp nhánh không conflict.

## 2. Chọn flow

Bảng trigger → flow → phase bắt đầu nằm ở skill [`route-task`](./skills/route-task/SKILL.md) (nguồn duy nhất). Khớp nhiều
hơn một flow hoặc không rõ → hỏi user, không đoán. Chọn xong → xin xác nhận bằng `AskUserQuestion` (miễn cho câu hỏi,
đọc–tìm code, chỉnh `.claude/`/memory — xem HARD-STOPS).

---

## 3. Pipeline

### 3.1 Pipeline gốc (Full Pipeline)

Các bước chạy tuần tự; mỗi bước đổi `Phase` trong HANDOFF của task. Quy tắc dừng và thứ tự skill: [HARD-STOPS.md](./HARD-STOPS.md).

| # | Phase | Skill | Đầu ra | Chuyển tiếp |
|---|-------|-------|--------|-------------|
| 1 | `ba` | `team-business-analyst` | `docs/tasks/[name]-ba-spec.md` (Approved) | → `design` |
| 2 | `design` | `solution-architect` | `docs/design/[name]-tech-design.md` (API Contract Overview + DB Schema Changes sơ bộ) | → `tl-review` |
| 3 | `tl-review` | `tech-lead` (gate bắt buộc) | `docs/reviews/design-review-[name].md` chỉ khi có action item cho dev; không thì confirm inline | APPROVED → `contract` nếu đủ phức tạp (§3.3), không thì `dev`. REVISION → quay về `design` (SA revise), auto-loop |
| 4 | `contract` (conditional) | `team-api-designer` / `database-engineer` — chỉ skill có flag `needs_api_detail` / `needs_db_detail` = true | `docs/api/[name]-api-spec.md`, Models + migration `Flyway` | → `dev` khi mọi flag đã có artifact |
| 5 | `dev` | `backend-engineer` / `frontend-engineer` / `mobile-engineer` | Code + unit test. Có API Spec/DB Migration từ bước 4 thì dùng lại; chưa có thì tự chi tiết hoá inline. Có thể gọi thủ công `ui-ux-pro-max` (tư vấn UX / UX Spec). | → `code-review` |
| 6 | `code-review` | `team-code-reviewer` (subagent `code-reviewer`, §3.5) | §3.5 | LGTM → `qa-plan`. REQUEST CHANGES → `dev`, auto-loop |
| 7 | `qa-plan` | `qa-engineer` (subagent `qa-writer`, §3.4) | `docs/tests/[name]-test-cases.md` + bảng ánh xạ AC → tầng test | qua cổng kịch bản → `qa-run` |
| 8 | `qa-run` | `qa-engineer` (subagent `qa-writer`) | test `JUnit 5 + Spring Boot Test` + báo cáo 100% Passed | PASSED → `distill` nếu `knowledge_impact != none`, không thì `idle`. FAILED → `dev`, auto-loop |
| 9 | `distill` | `tech-lead` | Cập nhật KB (§5) | → `idle` |

Khi TL yêu cầu REVISION: SA sửa design (Full / C / G); ở Flow D–F engineer là tác giả draft nên tự sửa. Đóng task: `handoff.py close` (§1).

### 3.2 Các flow khác = pipeline gốc trừ một số bước

Flow nào không liệt kê thì đi đúng pipeline gốc. Cột "Bỏ" tính theo số bước ở §3.1.

| Flow | Dùng khi | Bắt đầu ở | Bỏ | Khác biệt |
|------|----------|-----------|----|-----------|
| **Full** | Feature mới, có DB/API mới, chưa có pattern | `ba` | — | Pipeline gốc. `knowledge_impact` mặc định `full` |
| **A — Hotfix** | Bug production, nguyên nhân đã rõ, không đổi logic, không migration | `dev` | 1–4, 7–9 | Bước 5 do `hotfix-engineer` làm: vá (báo cáo `[PROBLEM] → [CAUSE] → [FIX]`, `docs/bugs/hotfix-<date>-<slug>.md`, chạy lại suite hiện có) → `code-review` quick pass chỉ file vừa sửa → `idle` |
| **B — Mini** | ≤ 3 file, reuse pattern/API/schema, không bảng mới | `dev` | 1–4, 7 | QA gộp một lượt: `code-review` → `qa-run` (§3.4). KB `none` |
| **C — Modify** | Đổi behavior của feature đang có | `ba` | 2 | BA viết impact analysis + change spec chỉ ghi delta (`docs/tasks/modify-<slug>.md`). TL review scope — **không đổi API contract / schema thì gộp TL vào GATE 3 của BA**: BA tự chấm checklist TL (pattern, quyền, rủi ro) và ghi `TL: gộp vào GATE 3` trong spec, phase đi thẳng `dev`; có đổi contract/schema thì giữ `tl-review` riêng. QA tập trung regression. KB mặc định `partial`. Có **C-nhỏ** bên dưới |
| **D — Refactor** | Cải thiện cấu trúc, **không đổi behavior** (đổi behavior → Flow C) | `tl-review` | 1, 2, 4, 7, 8 | TL define scope + risk, duyệt plan (`docs/reviews/refactor-<slug>.md` nếu có action item). Sau `code-review` chỉ chạy lại suite hiện có, không viết test mới. Rename symbol → `distill` partial (chỉ traceability + `drift.symbols`) |
| **E — Performance** | Chậm, slow query, memory leak, timeout | `dev` | 1–3, 7–9 | DB-eng/BE đo before → fix → đo after (`docs/bugs/perf-<date>-<slug>.md`). Có migration hoặc đổi index → thêm `tl-review` trước `code-review`. Xong `code-review` → `idle` |
| **F — Upgrade** | Bump dependency, CVE | `design` | 1, 4 | Engineer lập draft breaking changes + impact (`docs/reviews/upgrade-<slug>.md`) → `tl-review` approve/reject (reject = go/no-go → `idle`, hỏi user). QA **full regression, bắt buộc**. KB `none` |
| **G — Bug Investigation** | Có lỗi nhưng root cause chưa rõ / nhiều hướng fix | `ba` | 2 (trừ khi > 1 hướng fix), 4 (hiếm) | BA làm root cause (repro, expected vs actual) + impact: `dependency-graph.yaml` lấy `depended_by`, tìm caller trong code (AGENTS.md §Tìm code) lấy blast radius → `docs/tasks/bug-<slug>.md`. > 1 hướng fix → `design` so trade-off (`docs/design/bug-<slug>-solution.md`). QA bắt buộc có test tái hiện đúng bug (chỉ lộ ở UI → steps thủ công). KB `none` trừ khi fix hé lộ rule chưa document |

**C-nhỏ:** ≤ ~5 file, không đổi schema/contract/quyền → bỏ BA và TL riêng, ghi delta vào "Báo cáo" HANDOFF rồi `dev`; bỏ `qa-plan`, QA gộp một lượt ở `qa-run` (§3.4); chạm > 5 file hoặc cần schema → quay lại đường đầy đủ; chi tiết ở `route-task`.

### 3.3 Khi nào cần `contract`

TL (ở `tl-review`) set flag khi APPROVED:

* `needs_db_detail: true` — nhiều bảng/quan hệ chồng chéo, migration ảnh hưởng dữ liệu hiện có (không chỉ thêm cột),
  hoặc cần tối ưu index/constraint kỹ.
* `needs_api_detail: true` — nhiều endpoint, nhiều client/team consume, hoặc error schema/pagination phức tạp hơn CRUD.

Feature đơn giản (1–2 bảng thêm cột, vài endpoint CRUD theo pattern có sẵn) → không set flag, đi thẳng `dev`.

### 3.4 QA — hai giai đoạn, qua cổng kịch bản

Chi tiết (cổng 5 mục, phạm vi test, tốc độ test, dọn DB, chạy thật, PASSED): [PROJECT/qa-process.md](./PROJECT/qa-process.md).

- `qa-plan`: viết `docs/tests/[feature]-test-cases.md` + bảng `AC → tầng test`, tự chấm **cổng 5 mục**; đạt thì tự duyệt sang `qa-run`, không đạt thì dừng hỏi user.
- `qa-run`: test chỉ cho chức năng đã sửa (chọn test theo diff: chọn thủ công theo package/module bị sửa) — unit bằng
  `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test)`, tích hợp bằng lệnh hoàn chỉnh `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)` (đã đặt DB `qa_<task>` sau bước nạp
  `.env`; không tự ghép biến DB vào lệnh test); diff đổi FE/endpoint thì **chạy thật** trên app dev.
- **Flow B và C-nhỏ: gộp một lượt** — bỏ `qa-plan`, viết + chạy test luôn ở `qa-run`, báo cáo chỉ gồm bảng ánh xạ AC và output.
- PASSED = test tự động xanh + mọi AC có test + chạy thật không còn lỗi mở. Chính sách E2E: xem `PROJECT/qa-process.md`.

### 3.5 Code review

- *Phiên task / Điều hành:* phiên task review diff do session dev làm (nó không viết code nên là reviewer độc lập), theo
  skill `team-code-reviewer`. REQUEST CHANGES → ghi `docs/reviews/code-review-[name].md`, nhắn session dev sửa,
  review lại (auto-loop). LGTM → không tạo file, Nit in inline.
- *Solo:* giao subagent chỉ đọc `code-reviewer` (`.claude/agents/code-reviewer.md`) vì session tự viết code.

### 3.6 Checklist phát hành — chạy khi task có migration

Làm trước khi báo "xong" (đã có nhiều sự cố vì thiếu bước này):

0. Trước **mọi** lệnh áp / hạ / đánh dấu revision của `Flyway` (`(set -a; . ./.env; set +a; cd backend && SPRING_FLYWAY_TARGET=<n> ./mvnw spring-boot:run)`…) — và, nếu
   migration chạy lúc khởi động app, trước mọi lần chạy backend/test: kiểm
   `DB_URL` (inline / shell / `.env` gốc repo; lệnh tự nạp `.env` thì `.env` thắng shell), so host **và**
   tên DB. Không phải DB dev `localhost:5435 (DB medischedule)` / `qa_*` / DB tạm → dừng, hỏi user (HARD-STOPS "An toàn môi trường").
1. Chuỗi migration **tuyến tính, không rẽ nhánh**, migration mới nối sau điểm cuối hiện tại của `origin/main`
   (fetch trước — nhánh local có thể lùi): kiểu `autogenerate` ⇒ chỉ **một** head, revision mới trỏ đúng head đó; kiểu
   `SQL versioned` ⇒ số version = cao nhất trên `origin/main` + 1, không trùng, kiểm lại ngay trước commit
   (phiên song song có thể lấy cùng số).
2. Kiểm DB thật có đủ cột/bảng của migration (không chỉ bảng lịch sử version của `Flyway`); DB dev dùng
   chung → kiểm trước khi chạy. Nhiều phiên song song: áp tới revision/version cụ thể, không áp "tới mới nhất" (phiên
   khác có thể đã nối migration sau của bạn, hoặc file chưa commit của họ nằm trong checkout).
3. Đã áp migration bằng `Flyway` (hoặc bằng lần khởi động app, nếu migration chạy lúc khởi động), không
   bằng SQL tay. Lỡ chạy SQL tay ở môi trường nào → ghi nhận vào lịch sử migration của công cụ ngay ở môi trường đó
   (HARD-STOPS).
4. Ghi vào HANDOFF: migration nào đã áp ở môi trường nào (dev/staging/prod), cái nào còn chờ.

Lệnh cụ thể (kiểm head/version, xem SQL trước, đánh dấu revision, áp tới target…): chi tiết theo stack: xem
`.claude/stack-packs/backend-spring-boot-flyway.md`.

### 3.7 Tăng tốc — giữ task không phình (bài học: task Full dễ mất hàng giờ tới dòng code đầu và trả lại nhiều vòng)

1. **Đường ngắn nhất đủ an toàn:** Solo/B → C-nhỏ → C → Full (`route-task`); lên bậc phải có lý do cụ thể.
2. **Một lượt hỏi nghiệp vụ:** BA gom mọi câu vào một `AskUserQuestion`, khuyến nghị đứng đầu; câu phụ thuộc → giả định
   mặc định ghi trong spec, user duyệt ở GATE 3.
3. **Khóa phạm vi sau GATE 3:** yêu cầu thêm = task riêng (HANDOFF mới), trừ khi sửa lỗi spec; phiên task hỏi user trước.
4. **DB sạch, danh sách đỏ có sẵn:** `PROJECT/testing.md` mục Integration — đỏ thì chạy lại trên `qa_<task>` mới trước
   khi điều tra; test đỏ có sẵn (danh sách `KNOWN_FLAKY`) không cần điều tra lại.
5. **Giao từng lô:** task lớn tách lô độc lập; lô xong → review → gộp `main` ngay, lô sau rebase lên
   (repo không có nhánh dev riêng thì mỗi lô phải xanh test trước khi gộp và push vẫn hỏi user).
6. **Review không soi trùng:** TL chỉ soi thiết kế; code review chỉ soi diff theo rule pack; QA không lặp code review.
   Reviewer được chạy lệnh chỉ-đọc để xác nhận nghi vấn. Flow nhỏ: một lượt review gộp.
7. **Điều hành chỉ khi ≥ 2 task thật sự phụ thuộc.** Một task = Phiên task.
8. **Model:** dev/QA Sonnet; phiên task/điều hành Opus; tối đa 2 phiên Opus cùng lúc; hạn mức 5 giờ gần cạn
   (`get_usage`) thì không mở task mới.
9. **Ngữ cảnh gọn:** brief subagent chỉ trích mục liên quan (số mục); memory > 150 file → `consolidate-memory`.
10. **Đo bằng số:** `python .claude/scripts/handoff.py timing [<task>]` — trung vị từng phase và thời gian chờ user.

---

## 4. Phase

Chỉ dùng các giá trị này trong `Phase` của HANDOFF. Phase theo **vai**; flow nằm ở trường `Flow`. Có thể thêm chú thích
sau tên phase (vd `dev (BE xong, còn FE)`).

| Phase | Ý nghĩa |
|-------|---------|
| `ba` | BA phân tích / viết spec |
| `design` | SA thiết kế; Flow F: engineer lập draft; vòng sửa theo action item của TL |
| `tl-review` | Tech Lead duyệt design/plan trước khi code |
| `contract` | API Designer / DB Engineer chi tiết hoá (conditional) |
| `dev` | Engineer implement, kể cả vòng sửa sau review/QA |
| `code-review` | Reviewer duyệt diff |
| `qa-plan` | QA viết kịch bản + ánh xạ AC (bước 1/2) |
| `qa-run` | QA viết test tự động + chạy (bước 2/2) |
| `distill` | Tech Lead distill KB |
| `idle` | Không có việc / đã đóng |

Đổi phase: `python .claude/scripts/handoff.py set-phase <task> <phase> [--note "…"]`. Tên phase cũ (`dev-ready`,
`qa-ready`…) vẫn được `handoff.py` chấp nhận (bảng `ALIAS` trong script); gặp thì quy đổi sang tên mới.

---


## 5. Knowledge Lifecycle (KDD)

Spec đầy đủ: [`docs/knowledge/rules/knowledge_building.md`](../docs/knowledge/rules/knowledge_building.md).

### 5.1 / 5.3–5.5 Phân tầng, distill, trường HANDOFF

Chuyển sang [PROJECT/kdd.md](./PROJECT/kdd.md). Tóm tắt: chỉ distill khi `knowledge_impact != none` và QA PASS; owner `tech-lead` @ `distill`; Full=`full`, C/D-rename=`partial`, còn lại `none`. Dev/QA không ghi KB.

### 5.2 Task Start — Load KB (read-only)

Skill đầu tiên có code impact (BA cho Full/Modify, hoặc engineer cho Mini+) **trước khi viết spec/code**:

1. Đọc `docs/knowledge/{domain}/registry.yaml` → match `capability` / `feature`
2. Load `meta.yaml` → `load_policy[intent]`
3. Filter business rules theo `applies_to`
4. Nếu `drift.*: true` hoặc `confidence.*: generated|stale` → verify symbol trong code (cách tìm: AGENTS.md §Tìm code)
5. **Flow C — Modify và Flow G — Bug Investigation:** đọc `docs/knowledge/{domain}/dependency-graph.yaml` → lấy
   `depended_by` của capability đang sửa; nếu đã xác định symbol/function cụ thể → bổ sung blast radius code-level (caller
   thật, AGENTS.md §Tìm code) → BA ghi vào BA spec mục 4 "Cross-Capability Impact" — QA dùng để scope regression test

Ghi `domain`, `capability`, `feature` vào `docs/HANDOFF.<task>.local.md` lúc khởi tạo task.

# Hard Stop Rules

Quy tắc dừng và thứ tự của workflow, áp dụng cho request liên quan code/feature/bug. Pipeline và phase: [WORKFLOW.md](./WORKFLOW.md).

**Được hook thực thi (không chỉ là lời dặn):** sửa code thuộc `Scope` của task chưa duyệt thiết kế bị chặn
(`handoff.py guard`); lệnh áp / hạ / đánh dấu revision của `Flyway` lên DB không phải DB tạm `qa_*` bị
chặn (`context_guard.py`, kể cả DB dev — user đồng ý rồi mới thêm comment cho phép của hook cuối lệnh, vd
`# migration-guard: allow`; mẫu lệnh và token hook đọc ở đầu file / biến môi trường, xem `.claude/scripts/`);
`docs/_ref/` chỉ đọc; session sửa backend mà chưa chạy test, hoặc session con dừng sai chỗ theo vai, được
nhắc và chạy tiếp (`handoff.py stopcheck`, Stop hook `additionalContext`). Phần còn lại dựa vào việc làm đúng theo văn bản.

## Trước khi làm task

Task đổi code sản phẩm: chọn flow qua skill `route-task`, trình bày và xin xác nhận bằng `AskUserQuestion` (không in text
rồi kết thúc turn). Câu hỏi, đọc–tìm code, đo đạc, chỉnh `CLAUDE.md` / `.claude/` / memory thì làm thẳng. Task đang chạy
hiện ở đầu mỗi turn qua hook `[HANDOFF]`; đang giữa chừng thì tiếp tục từ phase đó. Chỉ làm đúng bước hiện tại, không
gộp, không nhảy bước (trừ khi user nói "skip [bước]").

## Thứ tự skill

Skill trước phải DONE (artifact đầy đủ, không còn câu hỏi mở hay issue chưa giải quyết) thì skill sau mới bắt đầu:
BA → SA → TL duyệt → engineer → code review → QA. Không implement khi flow cần Tech Lead duyệt mà chưa có.
Không ghi vào `docs/_ref/`; ghi `docs/knowledge/` chỉ ở phase `distill` (tech-lead). Output tạm đi vào `docs/`.

## Khi nào dừng hỏi user

Pipeline tự chạy sau khi BA/SA được duyệt. Mọi câu hỏi đặt qua `AskUserQuestion`, trong cùng turn.

| Điều kiện | Hành động |
|---|---|
| BA cần làm rõ nghiệp vụ (GATE 1) | Dừng, hỏi; lặp đến khi rõ |
| BA GATE 3 (duyệt spec cuối) | Dừng, hỏi duyệt / cần sửa |
| SA thấy spec mơ hồ | Dừng, trả về BA, BA hỏi user |
| SA gặp quyết định kỹ thuật vượt scope | Dừng, SA hỏi user |
| Kịch bản test (`qa-plan`) không đạt checklist `PROJECT/qa-process.md` | Dừng, hỏi user (Phiên task/Điều hành: phiên task duyệt; điều hành được duyệt thay, không gồm push/deploy/đọc prod/ghi DB staging-prod). Đạt checklist thì tự duyệt, đi tiếp |
| TL APPROVED (Solo) / CR LGTM / QA PASSED | Tự đi tiếp, không hỏi |
| TL APPROVED (mô hình Phiên task) | Trình user 1–2 câu (design + rủi ro); user duyệt thì mở session dev **và** QA ngay trong lượt đó — app chỉ cho mở session trong lượt user gõ (ORCHESTRATION §3.1) |
| TL REVISION / CR REQUEST CHANGES / QA FAILED (mọi lần) | Tác giả sửa rồi review lại, không giới hạn số lần, không hỏi |
| Flow F: TL reject go/no-go (huỷ upgrade) | Dừng, hỏi user |
| Blocking issue thật (thiếu input/file bắt buộc, DB xung đột, môi trường lỗi) | Dừng, hỏi user |

**Mô hình Phiên task / Điều hành:** session dev/QA không hỏi user; mọi điểm "dừng hỏi user" đi lên tầng trên
(`handoff.py raise`, ORCHESTRATION.md §5): phiên task tự quyết hoặc hỏi user (3 tầng: qua phiên điều hành).

Khi pipeline chạy tự động, không output trung gian; chỉ báo khi xong hoặc gặp blocking issue.

## An toàn môi trường (mọi mô hình)

- Trước mọi lệnh migration (`(set -a; . ./.env; set +a; cd backend && SPRING_FLYWAY_TARGET=<n> ./mvnw spring-boot:run)`, hạ / đánh dấu revision), chạy backend, chạy test, hay script ghi
  DB: kiểm biến `DB_URL` (inline, môi trường shell, rồi `.env` gốc repo — vd `grep DB_URL .env`;
  lệnh tự nạp `.env` — `. ./.env`, wrapper — thì `.env` thắng môi trường shell và mọi phép gán đứng trước bước nạp).
  Kiểm **host và tên DB**: không phải DB dev `localhost:5435 (DB medischedule)`, `qa_*` trên `localhost:5435` hoặc DB tạm → dừng, hỏi
  user. **Test tích hợp chỉ chạy trên DB tên `qa_*`** — host localhost không chứng minh gì khi DB dev và `qa_*` cùng một
  máy (vd Postgres Docker local); chạy bằng `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url <task>)"; cd backend && ./mvnw -q test -Dtest=<Lớp>IntegrationTest)`, không tự ghép biến DB vào lệnh test.
- **Nếu migration chạy lúc khởi động app** (vd Flyway trong Spring Boot): mọi lệnh chạy backend hoặc test khởi động app
  **là** lệnh migration — áp mọi file migration có trong checkout (kể cả file chưa commit của phiên khác) lên DB mà
  `DB_URL` trỏ tới. Hook migration thường không bắt được các lệnh này, nên kiểm như dòng trên trước mỗi lần.
- Khi có môi trường prod: đọc DB prod, ssh server prod, restart container prod chỉ khi user yêu cầu rõ trong lượt đó
  (hiện chưa có prod).
- Push: không push khi user chưa yêu cầu; khi được yêu cầu chỉ push commit của task (xem
  `LESSONS.md`). `main`/prod luôn hỏi user — repo không có nhánh dev riêng (dev = nhánh
  chính) thì mọi push đều thuộc dòng này. Không `git stash`/`reset --hard` trên cây chung; dùng worktree tạm.
- Chạy SQL tay lên DB do `Flyway` quản lý: tránh. Buộc phải làm thì ghi nhận ngay vào lịch sử migration
  của công cụ ở đúng DB đó (kiểu `autogenerate`: đánh dấu revision; kiểu `SQL versioned`: hỏi user trước khi sửa bảng
  lịch sử) — nếu không, lần áp sau sẽ chạy lại hoặc báo lệch.
- Không sửa file migration đã áp ở DB dùng chung hoặc đã lên nhánh gốc (công cụ kiểu `SQL versioned` kiểm checksum ⇒
  app không khởi động); cần đổi → migration mới.
- Không tự chạy migration trên prod; deploy/merge chạy.

Lệnh cụ thể của `Flyway`: chi tiết theo stack: xem `.claude/stack-packs/backend-spring-boot-flyway.md`.


## Khi skill gặp vấn đề

Dừng và hỏi user với: cần làm rõ nghiệp vụ (chỉ BA/SA), quyết định vượt scope (chỉ SA), hoặc blocking issue thật
(không tự sửa và lặp được). Reject của quality gate thông thường không thuộc nhóm này. Mẫu:

```
⚠️ [Tên skill] — CẦN CONFIRM
🔍 Vấn đề: [mô tả ngắn / thay đổi / câu hỏi]
```

rồi gọi `AskUserQuestion` với câu hỏi cụ thể + option. Không ghi câu hỏi vào file doc rồi dừng turn chờ user tự đọc, và
không tự đoán đáp án rồi tiếp tục.

## Liên quan

[WORKFLOW.md](./WORKFLOW.md) · [ORCHESTRATION.md](./ORCHESTRATION.md) · [skills/README.md](./skills/README.md)

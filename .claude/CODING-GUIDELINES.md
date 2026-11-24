# Coding Guidelines

Áp dụng cho mọi coding task. Skills đọc file này trước khi implement.

**Tradeoff:** Những hướng dẫn này nghiêng về thận trọng hơn tốc độ. Với task đơn giản, dùng judgment.

---

## §1. Think Before Coding

Đừng assume. Đừng giấu sự mơ hồ. Surface tradeoffs.

Trước khi implement:

- Nêu rõ assumptions. Nếu không chắc, hỏi.
- Nếu có nhiều cách hiểu, trình bày hết — đừng chọn im lặng.
- Nếu có cách đơn giản hơn, nói ra. Push back khi cần.
- Nếu có điều gì không rõ, dừng lại. Nêu tên điều đó. Hỏi theo format
  `⚠️ CẦN CONFIRM` quy định tại [HARD-STOPS.md](./HARD-STOPS.md).

---

## §2. Simplicity First

Code tối thiểu giải quyết vấn đề. Không có gì suy đoán.

- Không thêm feature ngoài yêu cầu.
- Không abstraction cho code chỉ dùng 1 lần.
- Không "flexibility" hay "configurability" nếu không được yêu cầu.
- Không error handling cho scenario không thể xảy ra.
- Nếu viết 200 dòng mà có thể là 50, viết lại.

Tự hỏi: "Senior engineer có nói cái này overcomplicated không?" Nếu có, đơn giản hóa.

---

## §3. Surgical Changes

Chỉ chạm vào những gì cần. Dọn dẹp mess của chính mình.

Khi edit code có sẵn:

- Không "cải thiện" code/comment/format xung quanh.
- Không refactor thứ không bị broken.
- Match style hiện có, dù bạn sẽ làm khác.
- Nếu thấy dead code không liên quan, đề cập — đừng xóa.

Khi changes của bạn tạo ra orphans:

- Xóa imports/variables/functions mà **changes của bạn** làm thành unused.
- Không xóa dead code có sẵn trừ khi được yêu cầu.

**Test:** Mỗi dòng thay đổi phải trace trực tiếp đến yêu cầu của user.

---

## §4. Goal-Driven Execution

Định nghĩa success criteria trong phạm vi bước hiện tại. Verify trước khi bàn giao.

Biến tasks thành verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

Với sub-steps **trong phạm vi một agent**, nêu brief plan:

```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

> **Scope boundary:** "Loop cho đến khi verified" chỉ áp dụng trong bước của agent hiện tại.
> Sang bước kế tiếp theo HARD-STOPS: đã được duyệt (APPROVED / LGTM / PASSED) thì tự chạy tiếp, chỉ dừng ở các điểm
> HARD-STOPS liệt kê.
> Plan toàn pipeline đã được định nghĩa tại [WORKFLOW.md](./WORKFLOW.md) — không tự thiết kế lại.

---

## §5. File Safety

Ưu tiên Edit tool cho mọi thay đổi file có sẵn, bất kể kích thước. Write tool chỉ dùng khi tạo file mới.
Không rewrite toàn bộ file chỉ để sửa vài dòng — §3 Surgical Changes áp dụng. Áp dụng cả cho **mockup HTML,
BA spec / design doc, test-cases, script trong scratchpad**: sửa vòng 2, 3 bằng Edit, không Write lại nguyên file
(mỗi lần Write cả nội dung nằm lại trong context — một mockup 50k ký tự ghi 3 lần = 150k). Hook
`context_guard.py write` cảnh báo khi Write ghi đè file có sẵn > 200 dòng.

Nếu Edit tool báo lỗi (pattern mismatch): đọc lại đoạn liên quan bằng `Read`, sửa `old_string` cho khớp chính xác,
không dùng Write tool để ghi đè toàn file thay thế.

**Không nhét script/nội dung dài vào lệnh Bash** (heredoc, `python - <<EOF`, `git commit -m "…"` nhiều đoạn): trên
Windows dễ vỡ quote (`unexpected EOF while looking for matching '`) hoặc vượt giới hạn độ dài lệnh (`ENAMETOOLONG`) —
lỗi rất hay gặp. Quá ~30 dòng: Write ra scratchpad rồi chạy file; commit message dài: `git commit -F <file>`.

Encoding contract của repo (UTF-8 no BOM, LF) — xem [`.claude/PROJECT/environment.md`](./PROJECT/environment.md)
nếu cần thao tác file bằng script ngoài Edit/Write tool.

---

## §6. Context Budget — đọc code

Mọi thứ đã đọc nằm lại trong context đến hết session. Đo thực tế ở các session dài: phần lớn output Bash là
`sed -n`/`cat` đọc code, cùng một file lớn bị đọc lại hàng chục lần.

1. **Định vị trước, đọc sau.** Tìm symbol/caller bằng công cụ index code nếu repo có (CLAUDE.md §Tìm code), không thì Grep/`grep -n`, rồi mới đọc đúng khúc cần — không mở file "để xem tổng quan".
2. **Khúc ≤ 200 dòng, ≤ 300 dòng mỗi lệnh.** Cần nhiều hơn thường là dấu hiệu chưa định vị đủ hẹp. Muốn lọc trong
   khúc lớn thì pipe: `sed -n A,Bp f | grep -n …`. Với `Read` file > 400 dòng luôn kèm `offset`/`limit`.
3. **Không đọc lại vùng đã đọc** nếu chưa sửa nó — dùng lại những gì đã có trong context. Sau Edit chỉ đọc lại vài
   dòng quanh chỗ sửa nếu cần kiểm.
4. **Output lệnh phải gọn:** test runner ở chế độ gọn (`(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test)` — cờ quiet / traceback ngắn của `JUnit 5 + Spring Boot Test`), `git diff --stat` trước rồi diff từng file, `| tail -40` cho log dài.
5. **Trình duyệt:** ưu tiên `get_page_text` / `find` / `read_page` có `ref_id`; chụp ảnh khi thật sự cần nhìn layout,
   kèm `scale: 0.5`.

Hook `context_guard.py bash` (PreToolUse Bash) **chặn** `sed -n` một khúc > 200 dòng (hoặc tổng > 300 dòng/lệnh) và
`cat` không pipe file > 400 dòng. Trường hợp thật sự cần đọc nhiều (vd. spec ngắn cần đọc trọn): thêm
`# context-guard: allow` cuối lệnh.

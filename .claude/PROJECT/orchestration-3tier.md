# Mô hình Điều hành (3 tầng)

Tách từ `ORCHESTRATION.md`. Chỉ đọc khi làm vai phiên điều hành hoặc phiên task do điều hành mở.

## Vai phiên điều hành

**Phiên điều hành** (3 tầng) không làm BA chi tiết. Việc của nó: phân tích yêu cầu + hiện trạng code/KB, chia task, viết
brief trước BA cho từng phiên task, chuyển và phân loại câu hỏi user (§5.4), rà spec chéo giữa các task trước khi trình
user duyệt (§5.5), giám sát tới khi mọi task đóng (§6), giữ nhật ký quyết định.

## Giữ chi phí

- **Một phiên điều hành cho một đợt việc.** Mọi task của đợt đóng → `close <đợt>-orchestrator --no-history`, đợt sau mở
  phiên điều hành **mới** (chỉ mang theo HANDOFF/nhật ký quyết định). Bài học: một phiên điều hành sống cả ngày tích
  hàng trăm triệu token cache-read và hàng trăm tool call.

## Brief điều hành → phiên task

**Phiên điều hành → phiên task (3 tầng):**
```
[[handoff-role <task> lead]]
Task: <tên> | Flow: <X> | HANDOFF: docs/HANDOFF.<task>.local.md | Điều hành: <id>
Yêu cầu gốc của user (nguyên văn) + phần thuộc task này.
Hiện trạng đã phân tích: file/luồng liên quan, rule KB, quyết định đã có, task khác chạm cùng chỗ.
Làm: toàn bộ pipeline theo ORCHESTRATION §2 (mở session dev/QA Sonnet). Hỏi user: gom câu hỏi mỗi vòng thành MỘT tin
gửi điều hành (§5.4). Trình spec cho điều hành rà trước GATE 3 (§5.5). Xong: phase idle, báo DONE.
```

## Giới hạn mở session (3 tầng)

- **Phiên task do điều hành mở (3 tầng):** không mở được session. Dev/QA chạy bằng subagent (`Agent`, `model: sonnet`,
  `isolation: worktree`; QA dùng subagent `qa-writer`) — hoặc điều hành mở sẵn session dev/QA cho task trong lượt user.

## HANDOFF của phiên điều hành

- Phiên điều hành giữ `docs/HANDOFF.<đợt>-orchestrator.local.md`: bảng task | id phiên task | phase | trạng thái | Q OPEN,
  và **Nhật ký quyết định** (Q-ID, quyết định, căn cứ). Tên file phải kết thúc `-orchestrator` (hook `status` bỏ qua
  file này). Xong đợt (mọi task đã đóng): `handoff.py close <đợt>-orchestrator --no-history`.

## 5.4 Câu hỏi BA qua phiên điều hành

Phiên task gom **mọi** câu hỏi của một vòng GATE 1 vào một tin, mỗi câu kèm phương án + khuyến nghị. Điều hành phân loại
từng câu: **tự trả lời** (có trong nguồn đã chốt — ghi căn cứ), **trả về** (câu hỏi sai trọng tâm hoặc code đã trả lời —
kèm chỉ dẫn), **hỏi user** (`AskUserQuestion`, kèm phân tích của điều hành: xung đột với code, KB, task khác). Trả lời
cho phiên task một tin, câu trả lời của user chép **nguyên văn**; phiên task ghi vào HANDOFF.

## 5.5 Điều hành rà spec trước GATE 3

Trước khi trình user duyệt spec, điều hành kiểm: AC phản ánh đúng câu trả lời của user và không mâu thuẫn nhau; giả định
về luồng/dữ liệu khớp code thật (tìm theo AGENTS.md §Tìm code); không vi phạm rule KB hay đảo quyết định cũ; không đụng cùng quy tắc/file
với task khác (nếu có thì chốt thứ tự gộp); AC đo được. Có vấn đề → trả phiên task sửa. Ổn → trình user kèm tóm tắt
và danh sách rủi ro.

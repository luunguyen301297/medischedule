---
name: route-task
description: Dùng khi user giao việc thay đổi code sản phẩm (feature, sửa bug, đổi behavior, refactor, tối ưu hiệu năng, nâng dependency, hotfix) và trước khi làm bất kỳ bước nào, để chọn flow, chọn mô hình Solo, Phiên task hoặc Điều hành, khai báo và xin user xác nhận. Không dùng cho câu hỏi, đọc/tìm code, đo đạc hay chỉnh CLAUDE.md, .claude/, memory.
---

# Chọn flow cho task

## 1. Chọn flow

| Trigger                                                                | Flow                  | Phase bắt đầu    | Skill đầu tiên              |
|------------------------------------------------------------------------|-----------------------|------------------|-----------------------------|
| hotfix, bug production, urgent, critical, patch, sửa gấp               | A — Hotfix            | `dev`   | `hotfix-engineer`           |
| thêm field nhỏ, filter mới, reuse pattern sẵn, ≤3 files                | B — Mini              | `dev`      | BE / FE / Mobile            |
| đổi behavior nhỏ: ước lượng ≤ 5 file, không đổi schema / API contract / quyền | **C-nhỏ** (mặc định cho đổi behavior nhỏ) | `dev` | BE / FE (delta ghi vào HANDOFF) |
| đổi behavior, thay logic, update feature, modify                       | C — Modify            | `ba`      | `team-business-analyst` |
| refactor, cleanup, tách service, rename                                | D — Refactor          | `tl-review` | `tech-lead`                 |
| chậm, slow query, latency, timeout, memory leak                        | E — Performance       | `dev`     | DB-eng / BE                 |
| upgrade, bump version, CVE, update dependency                          | F — Upgrade           | `design`  | BE / FE                     |
| bug phức tạp, root cause chưa rõ, cần đánh giá giải pháp trước khi fix | G — Bug Investigation | `ba`      | `team-business-analyst` |
| Feature mới + DB/API mới, không có pattern sẵn                         | Full Pipeline         | `ba`       | `team-business-analyst` |

Khớp nhiều hơn một flow hoặc không rõ → hỏi user, không đoán.

**Mặc định là đường NGẮN NHẤT đủ an toàn** (WORKFLOW §3.7). Thứ tự thử: Solo/Flow B → **C-nhỏ** → C → Full. Chỉ đi lên
bậc trên khi có một trong: đổi schema / API contract / quyền; > 5 file (ước lượng sau khi định vị symbol chính theo CLAUDE.md §Tìm
code, và chọn test theo diff: chọn thủ công theo package/module bị sửa); nghiệp vụ chưa rõ tới mức cần hỏi user. Chọn C hoặc Full thì phải ghi **một dòng "vì sao không
C-nhỏ"** trong câu khai báo — không có lý do cụ thể thì chọn C-nhỏ. Xu hướng tự nhiên là đẩy mọi việc lên Flow C dù
C-nhỏ đủ; "an toàn" không phải lý do. Việc nhỏ mà user vẫn xin Full thì làm theo user (mục "Full Pipeline over Reuse").

**Mô hình:** 1 task → Phiên task, không dùng Điều hành. Điều hành chỉ khi ≥ 2 task thật sự phụ thuộc nhau (ORCHESTRATION §1).

**Flow G / A / C:** Hotfix (A) = nguyên nhân đã rõ, sửa ngay; chỉ code review nhanh phần vừa sửa, bỏ BA/SA/TL/QA (WORKFLOW §3.2). Modify (C) = đổi behavior có chủ đích,
không do lỗi. Bug Investigation (G) = có lỗi nhưng root cause chưa rõ hoặc có nhiều hướng fix, cần đánh giá trước khi
động vào code. Chi tiết từng pipeline: `.claude/WORKFLOW.md` §3. Bảng phase → skill: §3.1.

## 2. Chọn mô hình

- **Solo**: một session làm hết. Chỉ việc rất nhỏ (Flow A, Flow B vài dòng, C-nhỏ) hoặc user muốn.
- **Phiên task** (mặc định): session này (Opus) làm BA → SA → TL → code review → duyệt QA → KB; mở session dev và
  session QA (Sonnet) để code và test.
- **Điều hành**: từ 2 task trở lên — session này phân tích, chia task, mở mỗi task một phiên task (Opus), chuyển câu hỏi
  user, rà spec chéo, giám sát. Chi tiết: `.claude/ORCHESTRATION.md`.

## 3. Khai báo và xin xác nhận

Trình bày `📋 Flow: [X] — [lý do 1 câu] | 📍 Skill: [Y] | 🧭 Mô hình: Solo|Phiên task|Điều hành`, rồi hỏi bằng `AskUserQuestion`
(không in text rồi kết thúc turn). Sau khi user xác nhận: tạo HANDOFF
(`python .claude/scripts/handoff.py new <task> --flow <X> --scope "<glob>" --title "…"`) và gọi skill đầu tiên.
Quy tắc dừng, thứ tự skill: `.claude/HARD-STOPS.md`.

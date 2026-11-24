# MediSchedule — Quy ước chung cho skill

Không phải skill vendor. File này gom các khối lặp lại nguyên văn ở nhiều skill `team-*` và skill vai trò, để sửa
một chỗ thay vì sửa ở từng skill. Skill tham chiếu bằng path, kèm phần override riêng nếu có.

Cùng họ với [`domain-context.md`](domain-context.md) (bối cảnh domain Đặt lịch và quản lý phòng khám).

---

## 0. Tìm code

Theo CLAUDE.md §Tìm code (nguồn duy nhất: công cụ index nếu repo có, không thì Grep/Glob) và CODING-GUIDELINES §6.

---

## 0b. Phase trong HANDOFF

Phase theo vai (`ba`, `design`, `tl-review`, `contract`, `dev`, `code-review`, `qa-plan`, `qa-run`, `distill`, `idle`).
Gặp tên cũ (`dev-ready`, `qa-ready`…) thì quy đổi theo bảng `ALIAS` trong `.claude/scripts/handoff.py`. Đổi phase bằng
`python .claude/scripts/handoff.py set-phase <task> <phase>` (sửa cả block lẫn yaml) — không sửa tay một chỗ.

---

## 1. Artifact Cross-Check — GATE bắt buộc trước handoff

Áp dụng cho mọi skill dev (`backend-engineer`, `frontend-engineer`, `mobile-engineer`).

Sau khi code xong, **trước khi chuyển phase**, đọc lại toàn bộ artifact ✅ trong mục **Artifacts Registry** của HANDOFF
(mẫu do `handoff.py new` tạo) và tick từng item theo bảng dưới. HANDOFF cũ chưa có mục này → coi artifact là ✅ khi file
ở đường dẫn chuẩn (`docs/tasks/<task>-ba-spec.md`, `docs/design/<task>-tech-design.md`…) tồn tại; không dừng vì thiếu bảng. **Không được bỏ qua bất kỳ row nào**, kể cả khi nghĩ đã làm đủ.

| Artifact        | Cách extract items                                                                                                     | Yêu cầu tick                                  | Áp dụng          |
|-----------------|------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|------------------|
| **BA Spec**     | Liệt kê từng `AC-XX` trong §3                                                                                          | Mỗi AC phải có code tương ứng                 | BE · FE · Mobile |
| **TL Review**   | Từng Action Item đánh số (#1, #2, …)                                                                                   | 100% action items                             | BE · FE · Mobile |
| **Tech Design** | Từng endpoint trong "API Contract Overview"; từng bảng trong "Database Schema Changes"                                 | BE: route + service + schema + migration đầy đủ. FE/Mobile: từng endpoint đã wire đúng | BE · FE · Mobile |
| **API Spec**    | Từng endpoint: method, path, request body, response schema, HTTP status codes                                          | BE: không thiếu field, không sai status code. FE/Mobile: đã gọi đúng | BE · FE · Mobile |
| **DB Design**   | Từng bảng/cột/index/constraint được chỉ định                                                                           | Migration tạo đúng, không thiếu index         | **BE only**      |
| **UX Spec**     | §2: từng component/column; **§3: đọc từng ROW trong bảng States** (loading/empty/error/success/…); §4: từng button và style | Tất cả rows §3 phải được implement — đây là mục hay bị bỏ sót nhất | **FE · Mobile only** |

**Report checklist inline** (không tạo file):

```
## Artifact Cross-Check
### BA Spec
- [x] AC-01: ...
- [x] AC-02: ...
### UX Spec §3 — Feedback States      (FE/Mobile)
- [x] Loading: Table loading={loading} prop ✅
- [x] Empty parent: locale.emptyText ✅
### TL Review
- [x] #1 — ...
### Tech Design — API Contract
- [x] POST /endpoint → service → schema ✅
### DB Design                          (BE)
- [x] Bảng X: cột Y, index Z ✅
```

Nếu phát hiện item chưa implement → **fix ngay**, sau đó mới report. **Không handoff khi còn item `[ ]`.**

---

## 2. Self-Review Pass — xử lý theo kết quả review

Áp dụng cho skill sinh artifact tài liệu (`team-business-analyst`, `solution-architect`,
`team-api-designer`, `database-engineer`). Mỗi skill tự định nghĩa **bullet đối chiếu riêng theo domain của
nó**; ba nhánh xử lý dưới đây là chung, không skill nào được đổi:

* **Gap tự sửa được** (thiếu field/AC/edge case/index theo convention có sẵn) → tự `Edit` artifact ngay, không
  hỏi user, không log riêng.
* **Thiếu thông tin cần thiết** để quyết định (không tự suy ra được từ spec/codebase) → **DỪNG**, hỏi user qua
  `AskUserQuestion` — không tự bịa, không im lặng bỏ qua.
* **Không phát hiện gap** → tiếp tục bước kế tiếp, không hỏi user (đúng tinh thần Auto-proceed,
  [`.claude/HARD-STOPS.md`](../../HARD-STOPS.md)).

Không tạo file review riêng cho bước này — sửa/log ngay trong artifact hoặc chat.

---

## 3. Communication styles

Skill ghi tên style ở dòng `* **Communication:**` của nó; định nghĩa đầy đủ ở đây.

| Style                | Format                                                       | Dùng ở                                                 |
|----------------------|--------------------------------------------------------------|--------------------------------------------------------|
| **Military**         | Trực tiếp, không preamble, không filler. Output: facts only. | backend · frontend · mobile · hotfix                   |
| **First Principles** | Core requirements → tradeoffs → decision. Không nhận convention mà không kiểm tra. | solution-architect · api-designer · database-engineer · ui-ux-pro-max (UX Spec) |
| **Reality Check**    | `[what works] → [real risk] → [verdict]`. Honest, balanced, không block vì style preference. | tech-lead · team-code-reviewer                     |
| **Socratic**         | Đặt câu hỏi dẫn dắt để lộ edge case, không đưa answer trước. | qa-engineer                                            |

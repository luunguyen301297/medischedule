# .claude — Skill & Project Hub

Reference hub cho các file trong `.claude/`. Thứ tự đọc khi bắt đầu task xem tại `../AGENTS.md`.

## Bootstrap

Xem `../AGENTS.md` § Làm task — đó là nguồn duy nhất cho thứ tự đọc file khi bắt đầu task. Không lặp lại
ở đây để tránh lệch.

## Tài liệu

| File                                           | Nội dung                                                            |
|------------------------------------------------|---------------------------------------------------------------------|
| [HARD-STOPS.md](./HARD-STOPS.md)               | Hard stops, gates, tuần tự skill                                    |
| [WORKFLOW.md](./WORKFLOW.md)                   | Flow routing (Full, A–G), pipelines                                 |
| [ORCHESTRATION.md](./ORCHESTRATION.md)         | Mô hình Solo / Phiên task / Điều hành, HANDOFF                      |
| [COMSTYLE.md](./COMSTYLE.md)                   | Communication tags [DOCS]/[CODE]/[TEST]/[REVIEW]                    |
| [CODING-GUIDELINES.md](./CODING-GUIDELINES.md) | Nguyên tắc code §1–§6                                               |
| [LESSONS.md](./LESSONS.md)                     | Bài học git / Windows / phiên song song                             |
| [PROJECT/](./PROJECT/)                         | Stack, architecture, auth, conventions, testing (điền theo dự án)   |
| [stack-packs/](./stack-packs/README.md)        | Bẫy, lệnh, ví dụ code riêng từng stack — chọn pack khớp dự án       |
| [scripts/](./scripts/README.md)                | Hook scripts + biến môi trường cấu hình theo stack                  |
| `../docs/claude-workflow/improvement-log.md`   | (tạo khi cần) Nhật ký cải tiến workflow/skills/context, việc treo   |
| [skills/README.md](./skills/README.md)         | Registry project skills                                             |

## Cấu trúc

```
.claude/
├── README.md            ← file này
├── HARD-STOPS.md        ← gates bắt buộc (tách từ WORKFLOW)
├── WORKFLOW.md          ← flow routing & pipelines
├── ORCHESTRATION.md     ← mô hình làm việc nhiều session
├── COMSTYLE.md
├── CODING-GUIDELINES.md
├── LESSONS.md           ← bài học vận hành
├── PROJECT/             ← project knowledge (khung mẫu, điền theo dự án)
├── stack-packs/         ← kiến thức/bẫy riêng theo stack (backend, frontend, mobile)
├── skills/              ← project role skills (SKILL.md per role)
├── agents/              ← subagent (code-reviewer, qa-writer)
├── scripts/             ← handoff.py, hooks, adapter theo stack (danh sách: scripts/README.md)
└── settings.json        ← hooks & permissions
```

## Invoke skill

Dùng `Skill` tool với skill ID từ [skills/README.md](./skills/README.md). Skills chạy inline trong conversation context
hiện tại — không spawn sub-process riêng.
Xem phase → skill mapping tại `WORKFLOW.md §3.1`.

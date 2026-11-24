# Skills Registry

## Loại A — Skill vendor cục bộ (`_shared/`)

Bản copy tại chỗ (không phụ thuộc plugin/marketplace ngoài) — mọi capability bổ trợ đều phải có bản vendor local
tại `.claude/skills/_shared/`, KHÔNG tham chiếu skill bare-name từ marketplace (đã gỡ bỏ toàn bộ, xem lịch sử ở
`git log` nếu cần).
Skill file trong project trỏ trực tiếp bằng path, ví dụ `.claude/skills/_shared/api-designer/SKILL.md`.

> **Load khi cần:** Các skill vendor này KHÔNG được đọc mặc định — agent chỉ `Read` file `SKILL.md` tương ứng khi
> thực sự rơi vào tình huống ở cột "Dùng bởi"/"Khi nào dùng" của skill gọi nó. Không proactively load toàn bộ
> `_shared/` mỗi lần chạy.

Skill chung (không gắn stack):

| Skill                   | Path                                                                                                                | Dùng bởi                                                   |
|-------------------------|---------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| `architecture-designer` | `_shared/architecture-designer/SKILL.md`                                                                            | `tech-lead`, `solution-architect`                          |
| `api-designer`          | `_shared/api-designer/SKILL.md`                                                                                     | `solution-architect`, `team-api-designer`                  |
| `code-reviewer`         | `_shared/code-reviewer/SKILL.md`                                                                                    | `tech-lead`, `team-code-reviewer`                          |
| `database-optimizer`    | `_shared/database-optimizer/SKILL.md`                                                                               | `solution-architect`, `database-engineer`                  |
| `sql-pro`               | `_shared/sql-pro/SKILL.md`                                                                                          | `database-engineer`                                        |
| `ponytail`              | `_shared/ponytail/SKILL.md` (vendor từ [DietrichGebert/ponytail](https://github.com/DietrichGebert/ponytail), MIT)  | `backend-engineer`, `frontend-engineer`, `hotfix-engineer` |
| `ponytail-review`       | `_shared/ponytail-review/SKILL.md` (vendor cùng nguồn trên)                                                         | `tech-lead`, `team-code-reviewer`                          |
| `test-master`           | `_shared/test-master/SKILL.md` (vendor từ bộ skill cộng đồng — ghi nguồn ở đầu file)                                | `qa-engineer`                                              |
| `debugging-wizard`      | `_shared/debugging-wizard/SKILL.md` (vendor cùng nguồn trên)                                                        | `hotfix-engineer`                                          |
| `business-analyst`      | `_shared/business-analyst/ba-methodology.md` (methodology-only, không có `SKILL.md` riêng — không phải vendor từ marketplace) | `team-business-analyst` |
| `domain-context`        | `_shared/domain-context.md` (project-owned, không vendor) | mọi skill Loại B |
| `project-conventions`   | `_shared/project-conventions.md` (project-owned, không vendor — Artifact Cross-Check, Self-Review Pass, Communication styles) | dev skills + skill sinh artifact |

Skill tham khảo theo stack — **không bắt buộc**; engineer chỉ đọc skill khớp stack thật của dự án
(`.claude/PROJECT/stack-and-commands.md`), stack khác thì vendor skill tương ứng về `_shared/` hoặc bỏ qua. Bộ có sẵn
trong `_shared/` (mỗi dòng là ví dụ, path `_shared/<tên>/SKILL.md`):

| Skill                   | Stack (vd)                         | Dùng bởi                               |
|-------------------------|------------------------------------|----------------------------------------|
| `fastapi-expert`        | service `ai-service` (FastAPI, chưa khởi tạo) | `backend-engineer`          |
| `python-pro`            | service `ai-service` (Python, pytest) | `backend-engineer`                  |
| `postgres-pro`          | DB PostgreSQL 16                   | `database-engineer`                    |
| `react-expert`          | frontend React 18 (`web/`)         | `frontend-engineer`                    |
| `typescript-pro`        | TypeScript (`web/`)                | `frontend-engineer`                    |

Backend Java/Spring Boot và mobile Flutter chưa có skill tham khảo — kiến thức nằm trong stack pack tương ứng.

Xoá skill tham khảo không dùng thì xoá luôn dòng của nó ở bảng này; skill vai trò và pack chỉ trỏ chung "skill tham
khảo khớp stack trong `_shared/` nếu có", nên không cần sửa thêm (kiểm bằng lệnh grep ở README template, bước "Dọn
tham chiếu").

Kiến thức và bẫy riêng một stack (không phải skill) nằm ở `.claude/stack-packs/`.

> **Business analyst:** `ba-methodology.md` là lớp *phương pháp phân tích* tổng quát (root cause, traceability,
> completeness check…) làm reference cho `team-business-analyst`; quy trình 3-gate trong
> `team-business-analyst/SKILL.md` mô tả *vận hành* (khi nào hỏi, khi nào dừng) và có độ ưu tiên cao hơn khi mâu
> thuẫn (xem `ba-methodology.md` §12).

## Loại B — Project Role Skills (internal)

Được định nghĩa trong `.claude/skills/` của project này. Invoke bằng `Skill` tool, chạy inline trong context hiện tại.

> **Prefix `team-`:** `team-business-analyst`, `team-api-designer`, `team-code-reviewer` mang prefix để tránh trùng
> ID với skill generic cùng tên (`business-analyst`, `api-designer`, `code-reviewer` ở Loại A hoặc plugin cài sẵn) —
> trùng ID khiến `Skill` tool liệt kê hai bản giống nhau, không rõ bản nào được chọn khi invoke. Các skill role khác
> không trùng tên nên giữ bare ID.

| Skill ID                    | Định nghĩa tại                              | Phase áp dụng                                    |
|-----------------------------|---------------------------------------------|--------------------------------------------------|
| `team-business-analyst` | `skills/team-business-analyst/SKILL.md` | `ba`                                             |
| `solution-architect`        | `skills/solution-architect/SKILL.md`        | `design`                                         |
| `tech-lead`                 | `skills/tech-lead/SKILL.md`                 | `tl-review`, `distill`                           |
| `team-api-designer`     | `skills/team-api-designer/SKILL.md`     | `contract` (conditional, TL set `needs_api_detail`) hoặc manual |
| `database-engineer`         | `skills/database-engineer/SKILL.md`         | `contract` (conditional, TL set `needs_db_detail`), `dev`, hoặc manual |
| `backend-engineer`          | `skills/backend-engineer/SKILL.md`          | `dev`; `design` khi là tác giả draft (Flow D–F)  |
| `frontend-engineer`         | `skills/frontend-engineer/SKILL.md`         | `dev`                                            |
| `ui-ux-pro-max`             | `skills/ui-ux-pro-max/SKILL.md` (+ `references/project-ux-spec.md`) | Manual only — tư vấn UX / UX Spec / mockup |
| `mobile-engineer`           | `skills/mobile-engineer/SKILL.md`           | `dev`                                            |
| `mobile-screen-loop`        | `skills/mobile-screen-loop/SKILL.md`        | `dev` cho app mobile (Flutter): hiện trạng web/backend → thiết kế (Stitch hoặc công cụ khác) → dựng UI → build → nghiệm thu trên emulator/simulator, 1 màn/vòng |
| `team-code-reviewer`    | `skills/team-code-reviewer/SKILL.md`    | `code-review`                                    |
| `qa-engineer`               | `skills/qa-engineer/SKILL.md`               | `qa-plan`, `qa-run`                              |
| `hotfix-engineer`           | `skills/hotfix-engineer/SKILL.md`           | `dev` (Flow A)                                   |

> `security-engineer` đã bị xóa khỏi workflow (không còn security review tự động hay opt-in).

## Quy tắc khi thêm skill mới (Loại B — code impact)

Mọi skill Loại B có code impact (BA, SA, TL, engineer, reviewer, QA — tức có thể chạm task thuộc domain có KB) khi
tạo mới **bắt buộc** phải có 1 trong 2:

1. Section "Load KB" tham chiếu rõ `.claude/WORKFLOW.md` §5.2 (4 bước: registry match → `load_policy[intent]` →
   filter `applies_to` → check `confidence`/`drift`), hoặc
2. Restate ngắn gọn cả 4 bước inline (mẫu: `hotfix-engineer`, `backend-engineer` trong `.claude/skills/`).

Không được bỏ qua hoàn toàn. Review khi thêm skill mới, không có CI tự động check vì skill file không chạy được.

## Quy tắc khi plugin update

1. Với skill vendor cục bộ (`_shared/`): khi bản gốc trên GitHub/marketplace có update quan trọng, tải lại thủ công
   và ghi ngày vendor trong comment đầu file.
2. **Không thêm tham chiếu skill marketplace bare-name mới** (skill không có bản vendor tại `_shared/`) vào bất kỳ
   skill file nào — nếu cần capability đó, vendor bản copy về `_shared/` trước rồi mới tham chiếu bằng path.
3. Nếu phát hiện tham chiếu tới plugin không tồn tại hoặc marketplace bare-name không vendor
   (`anthropic-skills:*`, `dev-skills:*`, hoặc skill ID trần không có bản `_shared/`) → xóa khỏi skill file, không
   giữ dead reference.

## Skill điều hướng và subagent

- `route-task` — chọn flow + mô hình Solo / Phiên task / Điều hành trước khi làm task code (gọi từ CLAUDE.md).
- Subagent (`.claude/agents/`): `qa-writer` (QA), `code-reviewer` (chỉ đọc) — phiên task giao việc và duyệt kết quả; xem `.claude/PROJECT/qa-process.md` (QA), `.claude/WORKFLOW.md` §3.5.

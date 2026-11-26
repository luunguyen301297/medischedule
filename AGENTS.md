# MediSchedule

Hướng dẫn chung cho mọi AI coding agent và người làm trong repo. Đây là nguồn duy nhất: `CLAUDE.md`, `GEMINI.md`,
`.github/copilot-instructions.md` chỉ nạp hoặc trỏ về file này; sửa quy ước thì sửa ở đây.

Hệ thống đặt lịch và quản lý phòng khám. Trả lời bằng tiếng Việt.

Stack (backend Spring Boot 3 + Spring Data JPA (Hibernate) + PostgreSQL 16; frontend React 18 + Ant Design 5; mobile
Flutter; service `ai-service` Python FastAPI). Bảng stack đầy đủ: [`PROJECT/stack-and-commands.md`](.claude/PROJECT/stack-and-commands.md).

Stack pack đang dùng (bẫy và lệnh riêng của công cụ — đọc khi code/test tầng tương ứng): backend
`.claude/stack-packs/backend-spring-boot-flyway.md`, frontend `.claude/stack-packs/frontend-react-antd-react-query.md`,
mobile `.claude/stack-packs/mobile-flutter.md`, service ai-service `.claude/stack-packs/backend-fastapi-sqlalchemy.md`
(thư mục `ai-service`).

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `backend/` | Java 21, Spring Boot 3.3, modular monolith, package gốc `vn.medischedule` |
| `web/` | React 18 + Vite + Ant Design 5 + React Query, chia theo feature (xem bên dưới) |
| `mobile/` | Flutter cho bệnh nhân (chưa khởi tạo) |
| `ai-service/` | Python FastAPI (chưa khởi tạo) |
| `infra/` | Script khởi tạo Postgres |
| `docs/` | Link use case, thiết kế CSDL, bản đồ module (`docs/modules.md`) |

Luồng request, module boundary, settings: [`.claude/PROJECT/architecture.md`](.claude/PROJECT/architecture.md).

## Lệnh

- Hạ tầng: `docker compose up -d` (Postgres :5435, Redis :6390, MinIO :9010 / console :9011)
- Backend trên máy (cần JDK 21): `powershell -File scripts\run-backend.ps1` (nạp `.env` rồi `mvnw spring-boot:run`)
- Web: `npm run dev` trong `web/` (:5175, proxy `/api` → :8085); kiểm kiểu: `npm run typecheck`
- VS Code: Run and Debug → **Run Full Stack**
- Swagger: http://localhost:8085/swagger-ui.html
- Cập nhật dữ liệu seed sau khi sửa nội dung trang chủ: `python scripts/export_seed.py`

Lệnh đầy đủ cho agent (chạy từ gốc repo bằng Bash: test, test tích hợp trên DB `qa_<task>`, migration tới version cụ
thể, port): [`.claude/PROJECT/stack-and-commands.md`](.claude/PROJECT/stack-and-commands.md) § Lệnh. Spring Boot không
tự đọc `.env` và Flyway chạy lúc khởi động — mọi lần chạy backend/test là một lần áp migration lên DB mà `DB_URL` trỏ
tới (xem [HARD-STOPS](.claude/HARD-STOPS.md) "An toàn môi trường").

## Quy ước backend

- Mỗi module một package: `domain` (entity) / `repository` / `service` / `web` (controller) / `dto` (record). Module mẫu: `auth`.
- Schema chỉ đổi qua Flyway (`backend/src/main/resources/db/migration/V{n}__mo_ta.sql`); Hibernate để `ddl-auto: validate`. Không sửa migration đã chạy.
- Phân quyền theo vai trò: mỗi user một role trong bảng `roles` (ADMIN, DOCTOR, RECEPTIONIST, PHARMACIST, PATIENT). Kiểm bằng `@PreAuthorize("hasRole('DOCTOR')")`; không có bảng permission.
- Người đang đăng nhập: `@AuthenticationPrincipal AuthUser me` (id, email, role lấy từ JWT).
- Lỗi nghiệp vụ: ném `ApiException` (mã lỗi + thông báo tiếng Việt); body lỗi `{code, message, fields}`.
- Không gọi chéo repository của module khác; gọi qua service của module đó.

## Quy ước web (`web/src`)

```
app/        main.tsx (entry), providers.tsx, router.tsx (route + lazy load từng khu), guards.tsx
shared/     dùng chung, KHÔNG import từ features/
  api/        http.ts (axios + JWT + lỗi), files.ts
  theme/      theme.ts (bảng màu duy nhất → biến --ms-* + theme Ant Design), base.css
  ui/         Reveal, ImageField...
features/   mỗi khu tự chứa api + type + trang + css, khớp module backend / vai trò
  auth/       đăng nhập, đăng ký, ROLE_LABEL
  landing/    trang giới thiệu, BookingWidget, nội dung trang chủ, landing.css
  home/       trang sau đăng nhập
  admin/      /admin/*: layout, staff, homepage (tải lười), admin.css
```

- Import bằng alias `@/` (`@/shared/api/http`). Trong cùng feature dùng đường dẫn tương đối.
- Feature khác chỉ import qua `index.ts` của feature (`@/features/auth`), không import sâu vào file bên trong.
- Khu mới theo vai trò (`doctor/`, `reception/`, `pharmacy/`, `patient/`...): tạo thư mục trong `features/`, export bộ route từ `index.ts`, gắn vào `app/router.tsx` bằng `lazy()`.
- Màu chỉ khai báo ở `shared/theme/theme.ts`; CSS dùng `var(--ms-*)`, không viết mã màu cứng. Không box-shadow, một màu xanh chủ đạo.
- CSS riêng của khu đặt cạnh feature và import trong layout/trang của khu đó.
- Control tự viết dùng trong `Form.Item` phải nhận `id` / `value` / `onChange` và gắn `id` vào phần tử nhập thật (label trỏ đúng).

Hai mục quy ước trên là nguồn sự thật. Khung quy ước chung của quy trình (error handling, thứ tự làm feature, vùng rủi
ro cao): [`.claude/PROJECT/conventions.md`](.claude/PROJECT/conventions.md); quy ước UI agent đọc khi làm giao diện
(tóm tắt từ mục trên): `.claude/skills/ui-ux-pro-max/references/project-ux-spec.md` § "Quy ước UI mặc định".

## An toàn (bắt buộc với mọi AI)

Chi tiết: [HARD-STOPS](.claude/HARD-STOPS.md) "An toàn môi trường". Hook trong `.claude/settings.json` chỉ chắc chắn chạy ở
Claude Code; công cụ khác **không được coi là có hook chặn** và phải tự làm đủ các dòng dưới.

- Chạy backend hoặc test khởi động Spring **là** chạy migration lên DB mà `DB_URL` trỏ tới. Trước mỗi lần, kiểm cả host
  lẫn tên DB. Lệnh tự nạp `.env` thì giá trị trong `.env` thắng biến môi trường có sẵn.
- Test tích hợp chỉ chạy trên DB `qa_*`, bằng lệnh test tích hợp trong
  [stack-and-commands](.claude/PROJECT/stack-and-commands.md) § Lệnh. Không tự ghép biến DB vào lệnh test.
- Không sửa, xoá, đổi tên file migration `V*` đã lên nhánh gốc; cần đổi thì thêm version mới.
- Không in hoặc commit `.env`, mật khẩu, token.
- Không push, deploy, hay chạy `docker compose down -v` khi người dùng chưa yêu cầu rõ.
- Không `git stash` / `reset --hard` trên cây làm việc chung; chỉ `git add` đúng file của task.

Lớp kiểm không phụ thuộc AI: git hook (bật một lần mỗi bản clone) và CI `.github/workflows/ci.yml`.

```bash
git config core.hooksPath scripts/git-hooks
```

## Làm task

Task thay đổi code sản phẩm (feature, bug, refactor, perf, upgrade, hotfix): gọi skill **`route-task`** trước tiên để
chọn flow, chọn mô hình (Solo / Phiên task / Điều hành), và xin user xác nhận. Câu hỏi, đọc/tìm code, đo đạc, chỉnh `.claude/` hoặc
memory thì làm thẳng.

Ba quy tắc cốt lõi (chi tiết và bảng dừng ở [HARD-STOPS.md](.claude/HARD-STOPS.md)):

1. Skill trước xong (DONE) thì skill sau mới bắt đầu; không gộp bước.
2. Sau BA/SA được duyệt thì pipeline tự chạy. Chỉ dừng hỏi user khi cần làm rõ nghiệp vụ, có blocking issue thật, hoặc
   kịch bản test không đạt checklist. TL REVISION / CR REQUEST CHANGES / QA FAILED tự lặp lại, không hỏi.
3. Không implement khi flow cần Tech Lead duyệt mà chưa có.

Các task đang chạy trên máy này: `python .claude/scripts/handoff.py status` (Claude Code tự hiện `[HANDOFF]` ở đầu mỗi turn), mỗi task một file `docs/HANDOFF.<task>.local.md`
(không git-tracked). Lịch sử task đã đóng: `docs/HANDOFF.history.md`. Các file khác (`WORKFLOW.md`, `HARD-STOPS.md`,
`ORCHESTRATION.md`, `PROJECT/*.md`) chỉ đọc khi cần.

## Làm task khi không dùng Claude Code

Tài liệu trong `.claude/` là Markdown thường, đọc và làm theo được. Chỗ nào nhắc công cụ của Claude thì thay như sau:

| Tài liệu ghi | Làm thay |
|---|---|
| Gọi skill `<tên>` | Đọc `.claude/skills/<tên>/SKILL.md` rồi làm theo; đọc tiếp `references/` khi file đó nhắc tới |
| `AskUserQuestion` | Nêu câu hỏi kèm lựa chọn trong khung chat rồi **dừng chờ** trả lời |
| Mô hình Phiên task / Điều hành, `start_session`, `SendMessage` | Dùng mô hình **Solo** |
| Subagent `code-reviewer`, `qa-writer` | Mở phiên mới, đưa nội dung `.claude/agents/<tên>.md` và diff; không có thì nhờ người review |
| `[HANDOFF]` hiện ở đầu mỗi turn | Đầu phiên tự chạy `python .claude/scripts/handoff.py status` |
| Hook nhắc chạy test, chặn sửa code chưa duyệt | Tự kiểm phase trong HANDOFF trước khi sửa; chạy test liên quan trước khi báo xong |
| `EnterPlanMode`, `ToolSearch`, memory | Bỏ qua |

Bắt đầu một task đổi code: đọc `.claude/skills/route-task/SKILL.md` để chọn flow → nêu flow và chờ xác nhận → làm từng
phase theo [WORKFLOW](.claude/WORKFLOW.md). Việc chỉ đọc hoặc hỏi đáp thì làm thẳng.

## Phối hợp nhiều người

`docs/HANDOFF.*.local.md` chỉ nằm trên máy người làm, máy khác không thấy. Trạng thái dùng chung nằm ở GitHub:

- Mỗi task một issue và một nhánh; nhận việc thì assign issue, xong phase thì cập nhật issue hoặc PR.
- Gộp vào `main` qua PR theo mẫu `.github/pull_request_template.md`; người review không phải người (hoặc phiên AI) viết.
- Trước khi đặt số version migration, `git fetch` rồi lấy số kế tiếp version cao nhất trên `origin/main`; trùng thì CI báo lỗi.

## Quick refs

| Topic                                    | File                                                                                       |
|------------------------------------------|--------------------------------------------------------------------------------------------|
| Hard stops                               | [`.claude/HARD-STOPS.md`](.claude/HARD-STOPS.md)                                           |
| Solo / Phiên task / Điều hành, HANDOFF   | [`.claude/ORCHESTRATION.md`](.claude/ORCHESTRATION.md)                                     |
| Workflow                                 | [`.claude/WORKFLOW.md`](.claude/WORKFLOW.md)                                               |
| Stack & commands                         | [`.claude/PROJECT/stack-and-commands.md`](.claude/PROJECT/stack-and-commands.md)           |
| Stack packs (bẫy/lệnh theo stack)        | [`.claude/stack-packs/README.md`](.claude/stack-packs/README.md)                           |
| Architecture                             | [`.claude/PROJECT/architecture.md`](.claude/PROJECT/architecture.md)                       |
| Auth                                     | [`.claude/PROJECT/auth.md`](.claude/PROJECT/auth.md)                                       |
| Conventions                              | [`.claude/PROJECT/conventions.md`](.claude/PROJECT/conventions.md)                         |
| Testing                                  | [`.claude/PROJECT/testing.md`](.claude/PROJECT/testing.md)                                 |
| Frontend state                           | [`.claude/PROJECT/frontend-state.md`](.claude/PROJECT/frontend-state.md)                   |
| Knowledge base (L2/L3 semantic)          | `docs/knowledge/medischedule/README.md` (tạo khi có domain đầu tiên)                       |
| KDD spec                                 | [`docs/knowledge/rules/knowledge_building.md`](docs/knowledge/rules/knowledge_building.md) |
| Communication style                      | [`.claude/COMSTYLE.md`](.claude/COMSTYLE.md)                                               |
| Environment (OS, shell, paths, encoding) | [`.claude/PROJECT/environment.md`](.claude/PROJECT/environment.md)                         |
| Coding guidelines                        | [`.claude/CODING-GUIDELINES.md`](.claude/CODING-GUIDELINES.md) (§1–§6, engineering skills) |
| Bài học (git, Windows, phiên song song)  | [`.claude/LESSONS.md`](.claude/LESSONS.md)                                                 |
| Skills                                   | [`.claude/skills/README.md`](.claude/skills/README.md)                                     |
| Hub                                      | [`.claude/README.md`](.claude/README.md)                                                   |

## Communication

Default BLUF. Prefix tags `[DOCS]` `[CODE]` `[TEST]` `[REVIEW]` — xem [`.claude/COMSTYLE.md`](.claude/COMSTYLE.md).

## Tìm code

Định vị symbol/caller trước, rồi Read đúng khúc cần: dùng Grep/Glob/Read. Các file khác trong `.claude/` chỉ trỏ về mục
này, không nhắc tên công cụ.

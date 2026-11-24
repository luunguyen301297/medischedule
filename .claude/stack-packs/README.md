# Stack packs

**Stack pack** là một file gom kiến thức riêng của một stack công nghệ: lệnh chuẩn, bẫy hay gặp, ví dụ code, quy ước
chỉ đúng với framework/thư viện đó (vd `MissingGreenlet` của SQLAlchemy async, chuỗi revision của Alembic, response
schema của Pydantic bỏ field không khai báo…).

Các file quy trình (`CLAUDE.md`, `WORKFLOW.md`, `HARD-STOPS.md`, `LESSONS.md`, `PROJECT/*.md`) chỉ ghi quy tắc **trung
lập với stack** và dùng placeholder (`ORM`, `MIGRATION_TOOL`, `TEST_COMMAND`… viết trong cặp ngoặc nhọn kép; bảng đầy đủ ở
[`PROJECT/stack-and-commands.md`](../PROJECT/stack-and-commands.md)). Chỗ nào cần chi tiết theo công cụ, file chung trỏ
tới pack của tầng tương ứng (placeholder `BACKEND_PACK`, `FRONTEND_PACK`, `MOBILE_PACK`).

## Pack có sẵn

Pack đi kèm template (dự án xoá pack không dùng — pack đang áp dụng là pack ghi ở `PROJECT/stack-and-commands.md`
§ Stack pack đang dùng; bảng dưới chỉ là danh mục, không link để khỏi thành link chết):

| Pack | Phạm vi |
|---|---|
| `backend-fastapi-sqlalchemy.md` | Service phụ `ai-service` (Python FastAPI, không DB): đã bỏ phần SQLAlchemy / Alembic của bản gốc |
| `backend-spring-boot-flyway.md` | Backend Java: Spring Boot 3, Spring Data JPA, Flyway (SQL versioned, chạy lúc khởi động), PostgreSQL, JUnit 5 + Maven |
| `frontend-react-antd-react-query.md` | Frontend web: React + TypeScript, Ant Design, TanStack React Query (chỉ server state, không store), Vite |
| `mobile-flutter.md` | App mobile đa nền tảng: Flutter (Dart) |

## Chọn pack cho dự án

1. Điền bảng stack ở `PROJECT/stack-and-commands.md` (mọi placeholder chuẩn).
2. Mỗi tầng (backend / frontend / mobile) chọn **một** pack khớp; điền đường dẫn pack vào placeholder `BACKEND_PACK`,
   `FRONTEND_PACK`, `MOBILE_PACK` (bảng ở `stack-and-commands.md`; các placeholder này xuất hiện ở `CLAUDE.md`
   và trong skill). Tầng không có thì xoá các câu nhắc tới pack của tầng đó.
3. Xoá các pack không dùng (agent chỉ đọc pack được liệt kê), rồi dọn tham chiếu tới chúng — lệnh grep ở README
   template, bước "Dọn tham chiếu".
4. Không có pack khớp → viết pack mới (mục dưới), hoặc tạm ghi bẫy vào `PROJECT/conventions.md` rồi tách sau.
5. Tầng dự kiến nhưng chưa khởi tạo (vd app Flutter chưa có code): vẫn giữ pack và ghi đường dẫn pack; trạng thái ghi ở
   bảng "Trạng thái tầng" của `stack-and-commands.md`, không ghi vào placeholder.

## Pack cho service phụ

Service thứ 4 trở đi (vd service AI FastAPI cạnh backend Java) dùng một pack có sẵn, nhưng pack viết placeholder theo
**tầng chính** (thư mục backend, thư mục migration, biến DB test… trong ngoặc nhọn kép). Replace hàng loạt cho cả repo
sẽ điền giá trị của backend chính vào pack của service ⇒ sai. Quy ước:

- **Trước** khi replace hàng loạt, mở pack của service và thay mọi placeholder trong đó bằng **giá trị của service**
  (vd thư mục backend → `ai-service`, thư mục migration → `ai-service/alembic/versions`), hoặc đổi thành placeholder
  riêng dạng `SERVICE_<TÊN>_<KHOÁ>` (ngoặc nhọn kép) rồi thêm vào bảng "Service khác" của
  `PROJECT/stack-and-commands.md`.
- Bảng khoá ở đầu pack (lệnh test, thư mục test…) đọc là giá trị **của service**, ghi vào cột tương ứng bảng "Service
  khác" — không điền vào bảng stack của tầng chính.
- Tầng chính và service phụ cùng stack (cùng một pack) ⇒ chép pack thành file riêng cho service (vd
  `backend-fastapi-sqlalchemy.ai-service.md`) rồi thay như trên; pack gốc giữ cho tầng chính.
- Ghi pack của service vào cột Pack bảng "Service khác" và dòng "Stack pack" của `CLAUDE.md` (kèm thư mục service).

## Viết pack mới

- Tên file: `<tầng>-<công nghệ chính>.md` (vd `backend-nestjs-prisma.md`, `frontend-vue-pinia.md`).
- Mở đầu: stack áp dụng (version chính) + **bảng giá trị placeholder** cho stack đó, ba cột `Khoá | Giá trị | Ghi chú`:
  "Giá trị" là **giá trị thuần** (chép thẳng vào bảng stack — không ngoặc giải thích, không trỏ mục kiểu "— §1"), giải
  thích / điều kiện để ở "Ghi chú" (quy tắc ở `PROJECT/stack-and-commands.md` § Cách điền). Giá trị `*_COMMAND` là
  lệnh tự chạy được từ gốc repo (tự `cd` / `--prefix` vào thư mục placeholder của tầng, tự nạp `.env` nếu runner
  không tự đọc) — thư mục chạy không để ở "Ghi chú"; pack backend có DB ghi thêm `INTEGRATION_TEST_COMMAND`. Cột khoá ghi **tên trần trong
  backtick** (vd `` `TEST_COMMAND` ``), **không** bọc ngoặc nhọn kép — pack được giữ lại sau khi điền template và
  bước kiểm placeholder còn sót phải sạch; câu điều kiện "Áp dụng khi …" cũng dùng tên trần. Chỉ dùng ngoặc nhọn kép
  ở chỗ thật sự cần thay bằng giá trị dự án (vd đường dẫn bắt đầu bằng `FRONTEND_DIR` / `BACKEND_DIR` trong lệnh), và
  placeholder riêng pack thì liệt kê ở đầu pack để thay ngay khi chọn.
- Không ghi cứng thư mục (`backend/`, `frontend/`): dùng placeholder thư mục của tầng, để pack dùng được cho cả tầng
  chính lẫn service phụ (mục dưới).
- Không trỏ tới script/file không có trong template hay repo (vd script wrapper tự đặt tên): ghi lệnh inline, hoặc
  "script sẵn có của dự án (nếu có)".
- Mỗi mục ghi rõ nó bổ sung cho file chung nào (vd "← `PROJECT/conventions.md` § Feature Implementation Order") để
  khi đọc file chung biết tra đâu.
- Chỉ ghi điều agent hay làm sai nếu không biết: bẫy runtime, lệnh đúng/sai, thứ tự bắt buộc, cờ hook. Không chép lại
  tài liệu chính thức.
- Quy tắc vốn chung (test pyramid, DB test riêng mỗi task, không chạy migration trên prod, encoding LF…) để ở file
  chung, pack chỉ ghi cách làm bằng công cụ của stack.
- Hook và adapter trong `.claude/scripts/` (danh sách: `.claude/scripts/README.md`) đọc cấu hình ở đầu file / biến môi
  trường; pack ghi giá trị cấu hình cho stack nếu khác mặc định.
- Skill tham khảo theo stack (nếu có) nằm ở `.claude/skills/_shared/` — pack có thể trỏ tới skill của **chính stack
  đó**, không chép; skill nào bị xoá thì xoá dòng trỏ tới nó. Stack chưa có skill tham khảo: kiến thức nằm hẳn trong
  pack.

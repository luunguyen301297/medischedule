# Bài học làm việc (rút từ dự án trước)

Các quy tắc dưới đây sinh ra từ sự cố thật khi nhiều phiên Claude chạy song song trên cùng một checkout.
Áp dụng mặc định; bỏ mục nào không hợp với dự án. Quy ước của dự án (`CLAUDE.md`, `PROJECT/*.md`) ưu tiên hơn file này.

## Git trong checkout dùng chung

- **Không `git stash` / `stash pop`.** Stash cuốn luôn file chưa commit của phiên song song. Cần baseline thì
  `git show HEAD:<file> > <scratch>` để so.
- **Không để gì ở trạng thái staged.** Không `git mv` / `git add` trước khi sẵn sàng commit; phiên khác commit sẽ cuốn
  theo. Stage bằng danh sách path tường minh trong cùng lệnh với commit.
- **`git branch --show-current` ngay trước mỗi commit/push**: phiên khác có thể đã đổi branch của checkout.
- **Trước khi `reset` / dời ref**: kiểm `git log origin/main..HEAD` và `git rev-parse HEAD` ngay trong cùng
  lệnh; commit nào không phải của mình thì cherry-pick giữ lại. Ưu tiên `pull --rebase`.
- **Không push khi user chưa yêu cầu.** Khi được yêu cầu, chỉ push commit của task: tạo worktree tạm tại
  `origin/main`, cherry-pick commit của task, push `HEAD:main`. Rebase không conflict thì push
  thẳng, conflict thì hỏi. Repo không có nhánh dev riêng (dev = nhánh chính): push này là push
  lên nhánh chính — chỉ làm khi user yêu cầu rõ trong lượt đó (HARD-STOPS), và nếu merge vào nhánh chính kích hoạt deploy
  thì báo user trước.
- **File bị hai task sửa xen kẽ**: giữ edit của mình dưới dạng script thay chuỗi; replay trong worktree tạm, rồi
  `git diff --cached` ra patch và `git apply --cached` vào checkout chính để commit.
- **Worktree có junction/symlink tới thư mục dependency của checkout chính** (vd thư mục package của FE): xoá junction
  trước (`cmd /c rmdir <path>`), rồi mới `git worktree remove`. Nếu không, git xoá sạch thư mục thật. Tốt hơn: chạy
  type check / build của worktree bằng công cụ đã cài ở checkout chính (trỏ đường dẫn project sang worktree).

## Windows / encoding

- PowerShell 5.1: `git commit -m` có dấu `"` bị tách thành pathspec. Ghi message ra file rồi `git commit -F <file>`.
- Python ghi file ở text mode trên Windows sinh CRLF. Repo `eol=lf`: đọc bytes, `.replace(b'\r\n', b'\n')`, ghi bytes.
- Tiếng Việt trong heredoc bash pipe vào `python -` có thể vỡ dấu. Viết script bằng tool Write.

## Tài nguyên dùng chung giữa các phiên

- File trạng thái chung (HANDOFF…): không ghi đè `phase`/`task_name` của phiên khác; chỉ append ghi chú. Khi đóng
  task, chỉ chuyển dòng của mình (dòng phiên khác có thể chen vào section của mình).
- Bộ đếm id dùng chung (rule id, `knowledge_version`, revision migration): kiểm "id còn trống" và ghi file trong **một**
  bước nguyên tử; nếu Write báo file đã tồn tại thì dừng ngay.
- Migration: không bao giờ áp migration "tới mới nhất" khi nhiều phiên cùng thêm migration (phiên khác có thể đã nối
  migration sau của bạn, hoặc file chưa commit của họ nằm trong checkout); chỉ định revision/version cụ thể
  (`(set -a; . ./.env; set +a; cd backend && SPRING_FLYWAY_TARGET=<n> ./mvnw spring-boot:run)`), xem SQL sinh ra trước nếu công cụ hỗ trợ. Nếu migration chạy lúc khởi động
  app: mỗi lần chạy app/test trên DB dùng chung đều là một lần áp — dùng DB `qa_<task>`.
- Nối migration mới vào điểm cuối của `origin/main` (fetch trước; nhánh local có thể lùi): một head duy nhất
  (kiểu `autogenerate`) hoặc số version kế tiếp, kiểm lại ngay trước commit (kiểu `SQL versioned`). Lệnh cụ thể:
  `.claude/stack-packs/backend-spring-boot-flyway.md`.

## Môi trường & dữ liệu

- Không tự chạy migration trên prod; deploy/merge sẽ chạy. Không tạo DB test trên server prod.
- Không tự khởi động Docker Desktop. Không có DB test dùng được thì bỏ integration test và ghi rõ là chưa chạy.
- Integration test dùng DB riêng cho từng task; không dùng DB dev dùng chung.
- Ghi thẳng DB (ngoài app) thì phải tự invalidate cache (vd đổi version key của cache).
- Backend không auto-reload: sau khi sửa BE nhắc user restart.

## Frontend

- Không chạy formatter ở chế độ ghi (vd `prettier --write`) trên file FE có sẵn nếu repo chưa format bằng formatter đó;
  chỉ format file mới.
- Không tin type FE là bằng chứng field có trong response. Schema response (DTO record + Jakarta Bean Validation) có thể chỉ trả field khai
  báo; kiểm schema backend.
- Quy ước giao diện (shadow, nền, màu chữ…): nguồn duy nhất là `.claude/skills/ui-ux-pro-max/references/project-ux-spec.md` § "Quy ước UI mặc định" — không
  chép lại ở đây.
- Thay đổi UI: chạy `npm --prefix web run typecheck` rồi báo; user tự kiểm UI, không chụp màn hình của user.
- Tính năng có UI: làm mockup tương tác (Artifact) **trước** BA spec, sao chép đúng CSS/layout màn hiện tại, mở rộng
  điều hướng sẵn có (thêm tab) thay vì màn mới.

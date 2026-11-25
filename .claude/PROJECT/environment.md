# Environment — Shell, Paths & Encoding

Agent đọc file này trước khi chạy bất kỳ shell command hoặc file operation nào.

---

## OS & Shell

| Item        | Value                                                         |
|-------------|---------------------------------------------------------------|
| OS          | Windows 11                                                    |
| Repo (host) | `D:\medischedule`                                             |
| Shell       | PowerShell + Bash tool (Git Bash) — cả 2 đều sẵn có           |

PowerShell và Bash tool chạy trực tiếp trên Windows filesystem — dùng thẳng Windows path (`D:\...` hoặc `d:/...`), không
dịch sang mount path khác. Lệnh `*_COMMAND` trong `PROJECT/stack-and-commands.md` viết cho Bash (Git Bash); lệnh dự án
viết sẵn cho PowerShell: `scripts\run-backend.ps1` (nạp `.env` rồi `mvnw spring-boot:run`, dùng bởi VS Code "Run Full
Stack").

**Công cụ có / không có trên máy dev** (kiểm lại khi đổi máy):

| Cần | Dùng | Không có |
|---|---|---|
| Format / lint backend | giữ style file đang sửa (dự án chưa có formatter/linter Java) | Spotless, Checkstyle (pom không có plugin) |
| Build / chạy backend | JDK 21 (Temurin, `JAVA_HOME`) + Maven wrapper `backend/mvnw` | `mvn` cài sẵn (không cần) |
| Truy vấn DB | script ngắn trong scratchpad dùng driver DB sẵn có của dự án (hoặc ORM nếu chạy được ngoài app — với ORM gắn framework như JPA/Hibernate thì dùng driver/JDBC trực tiếp hoặc CLI client), đọc `DB_URL` từ `.env` sau khi kiểm host (HARD-STOPS). Python có `psycopg2`; hoặc `docker compose exec -T postgres psql …` như `scripts/export_seed.py` | `psql`, `pg_dump` trên máy host; module Python `psycopg` (v3) |
| Sao lưu bảng trước khi xoá | `CREATE TABLE <bảng>_bak_<ngày> AS SELECT …` hoặc xuất CSV bằng script | `pg_dump` trên host |
| Docker | Docker Desktop có cài; hạ tầng dev chạy bằng `docker compose up -d` — **không tự khởi động Docker Desktop**, Docker chưa chạy thì báo user | |
| Mobile | | Flutter SDK (tầng mobile chưa khởi tạo) |

---

## Tool Priority — File Search & Read

Ưu tiên theo thứ tự, từ trên xuống:

1. **Công cụ index code** — nếu repo có (xem `AGENTS.md` §Tìm code).
2. **Built-in tools: `Grep`, `Glob`, `Read`** — ưu tiên mặc định cho mọi tìm kiếm/đọc file.
3. **Bash/PowerShell** — chỉ dùng cho thao tác shell thuần túy (git, npm, docker...), không dùng thay
   cho Grep/Glob/Read khi các tool đó đủ dùng.

---

## Encoding Contract

| Item         | Value             |
|--------------|-------------------|
| Encoding     | UTF-8, **no BOM** |
| Line endings | LF (`\n`) — `.gitattributes`: `* text=auto eol=lf`; riêng `*.cmd`, `*.bat` là CRLF |

- Không thay đổi encoding hoặc line endings khi edit file, dù "vô tình".
- Khi ghi file bằng Python: luôn dùng `open(..., encoding="utf-8", newline="\n")` (trên Windows text-mode mặc định ghi
  CRLF).
- File đã fix BOM: không được tái tạo BOM dưới bất kỳ hình thức nào. Ngoại lệ đang có: `scripts/run-backend.ps1` có BOM
  sẵn — sửa file này thì giữ nguyên như hiện trạng (không thêm/bỏ BOM khi chưa hỏi user).

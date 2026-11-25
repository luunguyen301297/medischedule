# MediSchedule

Hệ thống đặt lịch và quản lý phòng khám.

- **Backend:** Java 21, Spring Boot 3.3, Flyway, JWT (`backend/`)
- **Web:** React 18, Vite, Ant Design 5 (`web/`)
- **Hạ tầng (Docker):** PostgreSQL 16, Redis 7, MinIO

Yêu cầu: Docker, JDK 21, Node 18+. Không cần cài Maven (có `mvnw`).

## Cài Docker

- **Windows:** cài [Docker Desktop](https://www.docker.com/products/docker-desktop/) (hoặc `winget install -e --id Docker.DockerDesktop`). Docker Desktop dùng WSL 2; nếu máy chưa có, chạy `wsl --install` trong PowerShell Administrator rồi khởi động lại máy.
- **macOS:** cài [Docker Desktop](https://www.docker.com/products/docker-desktop/) (hoặc `brew install --cask docker`).
- **Linux:** cài Docker Engine và plugin Compose theo [hướng dẫn chính thức](https://docs.docker.com/engine/install/).

Mở Docker Desktop, đợi trạng thái **Engine running**, rồi kiểm tra:

```bash
docker compose version
```

Nếu Docker Desktop trên Windows báo lỗi WSL ("distro installation failed"): Quit Docker, chạy `wsl --shutdown`, mở lại. Vẫn lỗi thì `wsl --update` (PowerShell Administrator).

## Chạy dự án

```bash
git clone https://github.com/luunguyen301297/medischedule.git
```

```bash
cd medischedule
```

```bash
cp .env.example .env
```

```bash
docker compose up -d
```

Sau đó mở bằng VS Code, chọn **Run Full Stack** (F5). Cấu hình này chạy cả backend lẫn web. Hoặc chạy tay:

- Backend (Windows):
```bash
powershell -ExecutionPolicy Bypass -File scripts\run-backend.ps1
```
- Backend (macOS/Linux):
```bash
set -a && . ./.env && set +a && cd backend && ./mvnw spring-boot:run
```
- Web:
```bash
cd web && npm install && npm run dev
```

Lần chạy đầu, backend tự dựng DB, **không cần import gì**: Flyway tạo schema, nạp nội dung trang chủ và 4 tài khoản demo (`db/migration`, rồi `db/seed`), sau đó backend tạo tài khoản admin theo `.env`. Log có dòng `Successfully applied 5 migrations` và `Đã tạo tài khoản quản trị 'admin'`. Mở http://localhost:5175/login là thấy trang chủ có dữ liệu (ảnh nền tải từ internet).

Seed **không ghi đè dữ liệu bạn đã sửa**. Nội dung trang chủ chỉ được cập nhật theo seed mới nếu bạn chưa từng lưu ở `/admin/homepage`; tài khoản demo chỉ thêm khi chưa có. Muốn lấy đúng bản seed mới nhất thì xóa dữ liệu (`docker compose down -v`) rồi chạy lại backend.

## Địa chỉ và tài khoản

| | |
|---|---|
| Web | http://localhost:5175 |
| API / Swagger | http://localhost:8085 · `/swagger-ui.html` |
| MinIO Console | http://localhost:9011 |
| Postgres / Redis | `localhost:5435` / `localhost:6390` |

| Vai trò | Đăng nhập | Mật khẩu |
|---|---|---|
| Quản trị | `admin` | `admin@123` |
| Bác sĩ / Lễ tân / Dược sĩ / Bệnh nhân | `bacsi` / `letan` / `duocsi` / `benhnhan` | `demo@123` |

## Ghi chú

- Quy ước code: [`AGENTS.md`](AGENTS.md), dùng chung cho mọi AI và người làm.
- Bật git hook kiểm migration và bí mật, một lần mỗi bản clone: `git config core.hooksPath scripts/git-hooks`.
- Muốn người mới clone về có nội dung trang chủ giống máy bạn: sửa ở `/admin/homepage`, chạy `python scripts/export_seed.py`, commit file seed.
- Không sửa migration `V*` đã chạy; thay đổi schema thì tạo file mới.
- Docker image MinIO dùng `pgsty/minio`, vì image `minio/minio` gốc đã ngừng phát hành.
- Xóa sạch dữ liệu dev: `docker compose down -v`. Cũng dùng khi đổi `POSTGRES_PASSWORD` sau lần chạy đầu (Postgres giữ mật khẩu cũ trong volume).
- Đăng nhập `admin` không được: tài khoản này chỉ tạo một lần. Đổi `ADMIN_PASSWORD` sau đó sẽ không có tác dụng, cần xóa sạch dữ liệu để tạo lại.

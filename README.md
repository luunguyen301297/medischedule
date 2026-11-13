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

Lần chạy đầu, Flyway tự tạo schema và nạp dữ liệu mẫu (`db/migration`, `db/seed`); tài khoản admin được tạo theo `.env`.

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

- Quy ước code: [`CLAUDE.md`](CLAUDE.md).
- Sửa nội dung trang chủ ở `/admin/homepage` rồi chạy `python scripts/export_seed.py` để cập nhật seed cho cả nhóm.
- Không sửa migration `V*` đã chạy; thay đổi schema thì tạo file mới.
- Docker image MinIO dùng `pgsty/minio`, vì image `minio/minio` gốc đã ngừng phát hành.
- Xóa sạch dữ liệu dev: `docker compose down -v`.

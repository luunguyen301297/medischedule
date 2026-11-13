"""
Xuất dữ liệu hiện có trong DB dev ra file seed Flyway để người khác fetch code về có cùng dữ liệu.

    python scripts/export_seed.py

- Đọc nội dung trang chủ (site_content) từ container Postgres của docker compose
- Ghi lại backend/src/main/resources/db/seed/R__seed_dev_data.sql (phần nội dung trang chủ + tài khoản demo)
- Không xuất tài khoản thật (email, mã băm mật khẩu cá nhân): chỉ tạo tài khoản demo theo từng vai trò

Flyway chạy lại file R__ mỗi khi nội dung file đổi, nhưng nội dung trang chủ chỉ được ghi đè ở máy
chưa từng sửa trang chủ trong /admin (site_content.updated_by trống). Máy đã sửa giữ nguyên nội dung của mình.
"""
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SEED = ROOT / "backend/src/main/resources/db/seed/R__seed_dev_data.sql"

# Tài khoản demo: (email, username, họ tên, vai trò). Mật khẩu chung DEMO_PASSWORD, băm bằng pgcrypto lúc nạp.
DEMO_PASSWORD = "demo@123"
DEMO_USERS = [
    ("bacsi.demo@medischedule.local", "bacsi", "BS. Trần Minh Khoa", "DOCTOR"),
    ("letan.demo@medischedule.local", "letan", "Nguyễn Thu Hà", "RECEPTIONIST"),
    ("duocsi.demo@medischedule.local", "duocsi", "DS. Lê Quang Huy", "PHARMACIST"),
    ("benhnhan.demo@medischedule.local", "benhnhan", "Phạm Ngọc Anh", "PATIENT"),
]


def psql(sql: str) -> str:
    cmd = ["docker", "compose", "exec", "-T", "postgres", "sh", "-c",
           'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tA -c "$0"', sql]
    return subprocess.run(cmd, cwd=ROOT, check=True, capture_output=True).stdout.decode("utf-8").strip()


def q(s: str) -> str:
    """Chuỗi SQL an toàn (nhân đôi dấu nháy đơn)."""
    return "'" + s.replace("'", "''") + "'"


def build(homepage: dict) -> str:
    content = json.dumps(homepage, ensure_ascii=False, indent=2)
    users = ",\n".join(
        f"    ({q(e)}, {q(u)}, {q(n)}, {q(r)})" for e, u, n, r in DEMO_USERS
    )
    return f"""-- ============================================================================
-- Dữ liệu dev dùng chung (Flyway repeatable migration). SINH TỰ ĐỘNG bởi scripts/export_seed.py
-- Sửa nội dung trang chủ ở /admin/homepage rồi chạy lại script để cập nhật file này.
--
-- Chạy sau các migration V*, và chạy lại mỗi khi file này thay đổi.
-- Không ghi đè dữ liệu người dùng đã sửa: trang chủ chỉ cập nhật khi chưa sửa ở /admin, tài khoản demo chỉ thêm khi chưa có.
-- Chỉ nạp khi FLYWAY_LOCATIONS có classpath:db/seed (mặc định ở môi trường dev, xem application.yml).
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Nội dung trang chủ (đặt lịch đầu trang, giới thiệu chung, các cụm ảnh nền)
INSERT INTO site_content (content_key, content, updated_at)
VALUES ('homepage', {q(content)}::jsonb, now())
ON CONFLICT (content_key) DO UPDATE SET content = EXCLUDED.content, updated_at = now()
-- Chỉ ghi đè nội dung chưa từng được sửa ở /admin/homepage (updated_by trống = do migration/seed tạo).
-- Máy đã sửa trang chủ giữ nguyên; muốn lấy bản seed mới nhất thì xóa dữ liệu: docker compose down -v
WHERE site_content.updated_by IS NULL;

-- 2. Tài khoản demo cho từng vai trò, mật khẩu: {DEMO_PASSWORD}
--    (tài khoản quản trị admin / admin@123 do backend tự tạo lúc khởi động, xem AdminInitializer)
INSERT INTO users (email, username, password_hash, full_name, role_id)
SELECT v.email, v.username, crypt({q(DEMO_PASSWORD)}, gen_salt('bf', 10)), v.full_name, r.id
FROM (VALUES
{users}
) AS v(email, username, full_name, role_code)
JOIN roles r ON r.code = v.role_code
ON CONFLICT DO NOTHING;   -- trùng email hoặc username thì bỏ qua
"""


def main() -> None:
    raw = psql("select content::text from site_content where content_key = 'homepage'")
    if not raw:
        sys.exit("Không có dòng site_content 'homepage' trong DB")
    homepage = json.loads(raw)
    SEED.parent.mkdir(parents=True, exist_ok=True)
    SEED.write_text(build(homepage), encoding="utf-8", newline="\n")
    print(f"Đã ghi {SEED.relative_to(ROOT)}: {len(homepage.get('sections', []))} cụm ảnh nền, {len(DEMO_USERS)} tài khoản demo")


if __name__ == "__main__":
    main()

#!/usr/bin/env python
"""qa_db.py — tạo / liệt kê / dọn DB test tạm `qa_<task>` trên Postgres dev (đặt tên thống nhất để dọn được).

  python .claude/scripts/qa_db.py url <task>        tạo qa_<task> nếu chưa có, in URL kết nối (TEST_POSTGRES_URL, DB_URL…)
  python .claude/scripts/qa_db.py list              liệt kê DB qa_* / tmp_itest_* kèm dung lượng
  python .claude/scripts/qa_db.py drop <task>       xoá qa_<task> và các DB worker song song qa_<task>_gw*
  python .claude/scripts/qa_db.py workers           số worker test song song nên dùng (chia theo số task đang qa-run)

Kết nối admin (tạo/xoá DB), lấy theo thứ tự — biến môi trường trước, rồi file `.env` ở gốc repo (QA_DB_ENV_FILE):
  1. QA_DB_ADMIN_URL (tên biến đổi qua QA_DB_ADMIN_URL_VAR) — Postgres riêng cho test, nhận cả `jdbc:postgresql://…`
  2. ghép từ POSTGRES_HOST / POSTGRES_PORT / POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_DB (kiểu docker-compose;
     tên biến đổi qua QA_DB_PG_VARS; cần ít nhất POSTGRES_USER; host mặc định localhost, port 5432)
  3. DATABASE_URL (danh sách biến qua QA_DB_FALLBACK_URL_VARS) — Postgres dev.
CHỈ chạy với host trong ALLOWED_HOSTS (mặc định localhost, 127.0.0.1, ::1 — mọi port; thêm host dev/test qua
QA_DB_ALLOWED_HOSTS="host1,host2:6543", mục không có port = mọi port). Host lạ luôn bị từ chối — .env có thể trỏ prod.
Chỉ đụng DB có tên bắt đầu `qa_` / `tmp_itest_` (SCRATCH_DB_PREFIXES).

Driver: psycopg2 nếu cài, rồi psycopg (v3), không có thì gọi `psql` (PATH hoặc QA_DB_PSQL). Không cần SQLAlchemy.

URL in ra (QA_DB_URL_DRIVER):
  - mặc định `postgresql+asyncpg` -> `postgresql+asyncpg://user:pass@host:port/qa_<task>` (SQLAlchemy/asyncpg). Công cụ
    cần URL libpq thường (psql, Prisma, node-postgres, Go pgx): đặt `postgresql`.
  - bắt đầu bằng `jdbc:` (vd `jdbc:postgresql`) -> URL JDBC `jdbc:postgresql://host:port/qa_<task>`. JDBC thường không
    mang user/mật khẩu trong URL; QA_DB_JDBC_CREDENTIALS chọn cách đưa:
      `none`  (mặc định) chỉ in URL; user ghi chú ra stderr — app đọc user/mật khẩu từ biến riêng
              (Spring: SPRING_DATASOURCE_USERNAME/PASSWORD hoặc POSTGRES_USER/PASSWORD trong .env).
      `query` gắn `?user=…&password=…` — pgjdbc (PostgreSQL) và MySQL Connector/J nhận; SQL Server dùng `;user=…`
              nên KHÔNG dùng chế độ này với mssql-jdbc.

Giao diện CLI (adapter DB khác — MySQL, SQL Server, SQLite, Mongo… — thay script này bằng script cùng giao diện):
  url <task>     tạo DB tạm (idempotent), in DUY NHẤT một dòng URL ra stdout (ghi chú ra stderr), exit 0
  list           in danh sách DB tạm kèm dung lượng, exit 0
  drop <task>    xoá DB tạm của task (kể cả DB worker song song), in một dòng tóm tắt; không có gì để xoá vẫn exit 0
  workers        in số worker test song song nên dùng (dòng đầu stdout là số nguyên), exit 0
  Lỗi cấu hình / host không được phép / không kết nối được: thông báo ra stderr, exit khác 0.
Giữ an toàn: chỉ đụng host trong danh sách cho phép và DB có tiền tố tạm. Stack không dùng DB tạm: xoá script này
(handoff.py close bỏ qua êm; đặt GUARD_MIGRATION_CMD_REGEX='' nếu cũng không có migration).
"""
import hashlib
import os
import re
import shutil
import subprocess
import sys
from urllib.parse import parse_qs, quote, unquote, urlsplit


def _env(name, default):
    v = os.environ.get(name)
    return default if v is None else v


def _csv(name, default):
    return [x.strip() for x in _env(name, default).split(",") if x.strip()]


def _parse_hosts(items):
    """'host' (mọi port) | 'host:port' | '[v6]:port' | 'v6' -> {(host, port hoặc None)}."""
    out = set()
    for item in items:
        m = re.fullmatch(r"\[([^\]]+)\](?::(\d+))?", item)
        if m:
            out.add((m.group(1).lower(), int(m.group(2)) if m.group(2) else None))
        elif item.count(":") == 1:
            host, _, port = item.partition(":")
            if host and port.isdigit():
                out.add((host.lower(), int(port)))
        else:
            out.add((item.lower(), None))
    return out


# ---------------------------------------------------------------------------------------------------------------
# CONFIG — chỉnh theo dự án (đè bằng biến môi trường)
# ---------------------------------------------------------------------------------------------------------------
# host[:port] Postgres dev/test được phép tạo/xoá DB tạm (KHÔNG BAO GIỜ thêm host prod); không ghi port = mọi port
ALLOWED_HOSTS = _parse_hosts(["localhost", "127.0.0.1", "::1"] + _csv("QA_DB_ALLOWED_HOSTS", ""))
# tiền tố DB tạm được phép xoá (cùng biến với context_guard.py)
SCRATCH = tuple(p for p in _env("SCRATCH_DB_PREFIXES", "qa_,tmp_itest_").split(",") if p)
# tiền tố tên DB tạm của task (phải nằm trong SCRATCH)
DB_PREFIX = _env("QA_DB_PREFIX", "qa_")
# driver ghi vào URL in ra (xem docstring: `postgresql+asyncpg` | `postgresql` | `jdbc:postgresql`…)
URL_DRIVER = _env("QA_DB_URL_DRIVER", "postgresql+asyncpg")
# chế độ JDBC: `none` | `query` (xem docstring)
JDBC_CREDENTIALS = _env("QA_DB_JDBC_CREDENTIALS", "none")
# file env ở gốc repo + tên biến đọc trong đó (và trong môi trường)
ENV_FILE = _env("QA_DB_ENV_FILE", ".env")
ADMIN_URL_VAR = _env("QA_DB_ADMIN_URL_VAR", "QA_DB_ADMIN_URL")
PG_VARS = (_csv("QA_DB_PG_VARS", "POSTGRES_HOST,POSTGRES_PORT,POSTGRES_USER,POSTGRES_PASSWORD,POSTGRES_DB") + [""] * 5)[:5]
FALLBACK_URL_VARS = _csv("QA_DB_FALLBACK_URL_VARS", "DATABASE_URL")
# psql dùng khi không có psycopg2/psycopg
PSQL = _env("QA_DB_PSQL", "psql")
# ---------------------------------------------------------------------------------------------------------------


class Conn:
    """Thông số kết nối admin (tách từ URL hoặc POSTGRES_*)."""

    def __init__(self, host, port, user, password, database, source):
        self.host, self.port, self.user = host or "localhost", int(port or 5432), user or ""
        self.password, self.database, self.source = password or "", database or "postgres", source


def _root():
    out = subprocess.run(["git", "rev-parse", "--path-format=absolute", "--git-common-dir"], capture_output=True,
                         text=True).stdout.strip()
    return out.rsplit("/.git", 1)[0] if out.endswith("/.git") else os.getcwd()


def _dotenv():
    path = os.path.join(_root(), ENV_FILE)
    vals = {}
    if os.path.exists(path):
        for line in open(path, encoding="utf-8", errors="ignore"):
            m = re.match(r"\s*(?:export\s+)?([A-Za-z_]\w*)\s*=(.*)$", line)
            if m and m.group(1) not in vals:
                vals[m.group(1)] = m.group(2).strip().strip("\"'")
    return vals


def parse_url(url, source):
    """postgresql[+driver]://u:p@h:port/db hoặc jdbc:postgresql://h:port/db?user=..&password=.. -> Conn."""
    raw = url[5:] if url.startswith("jdbc:") else url
    s = urlsplit(raw)
    q = {k: v[-1] for k, v in parse_qs(s.query).items()}
    return Conn(s.hostname, s.port, unquote(s.username or "") or q.get("user"),
                unquote(s.password or "") or q.get("password"), (s.path or "/").lstrip("/") or None, source)


def _admin():
    env, dot = os.environ, _dotenv()

    def get(name):
        return (env.get(name) or dot.get(name) or "") if name else ""

    url = get(ADMIN_URL_VAR)
    if url:
        c = parse_url(url, ADMIN_URL_VAR)
    elif get(PG_VARS[2]):
        c = Conn(get(PG_VARS[0]), get(PG_VARS[1]), get(PG_VARS[2]), get(PG_VARS[3]), get(PG_VARS[4]),
                 "/".join(v for v in PG_VARS if v))
    else:
        name = next((v for v in FALLBACK_URL_VARS if get(v)), None)
        if not name:
            sys.exit(f"Không thấy {ADMIN_URL_VAR}, {PG_VARS[2] or 'POSTGRES_USER'}… hay {'/'.join(FALLBACK_URL_VARS)} "
                     f"(biến môi trường hoặc {ENV_FILE} ở gốc repo). Thêm vào {ENV_FILE}: "
                     f"{ADMIN_URL_VAR}=postgresql://<user>:<mật khẩu>@localhost:5432/postgres")
        c = parse_url(get(name), name)
    h = c.host.lower()
    if (h, c.port) not in ALLOWED_HOSTS and (h, None) not in ALLOWED_HOSTS:
        sys.exit(f"Từ chối: {c.source} trỏ {c.host}:{c.port} — không nằm trong ALLOWED_HOSTS (xem QA_DB_ALLOWED_HOSTS).")
    return c


def _lit(v):
    return str(v) if isinstance(v, int) else "'" + str(v).replace("'", "''") + "'"


def sql(c, query, params=()):
    """Chạy một câu SQL autocommit (CREATE/DROP DATABASE không chạy trong transaction) -> list tuple."""
    kw = dict(host=c.host, port=c.port, user=c.user, password=c.password, dbname=c.database)
    for mod in ("psycopg2", "psycopg"):
        try:
            drv = __import__(mod)
        except ImportError:
            continue
        conn = drv.connect(**kw)
        try:
            conn.autocommit = True
            cur = conn.cursor()
            cur.execute(query, params or None)
            return [tuple(r) for r in cur.fetchall()] if cur.description else []
        finally:
            conn.close()
    exe = shutil.which(PSQL)
    if not exe:
        sys.exit("Không có psycopg2 / psycopg (pip install psycopg2-binary) và không thấy `psql` trong PATH (QA_DB_PSQL).")
    parts = query.split("%s")
    text = parts[0] + "".join(_lit(p) + rest for p, rest in zip(params, parts[1:]))
    r = subprocess.run([exe, "-X", "-q", "-A", "-t", "-F", "\t", "-v", "ON_ERROR_STOP=1", "-h", c.host, "-p",
                        str(c.port), "-U", c.user, "-d", c.database, "-c", text], capture_output=True, text=True,
                       encoding="utf-8", env=dict(os.environ, PGPASSWORD=c.password, PGCONNECT_TIMEOUT="10"))
    if r.returncode != 0:
        sys.exit(f"psql lỗi: {r.stderr.strip()[:300]}")
    return [tuple(line.split("\t")) for line in r.stdout.splitlines() if line.strip()]


def db_name(task):
    n = DB_PREFIX + re.sub(r"[^a-z0-9_]", "_", task.lower())
    if len(n) > 50:  # chừa chỗ cho hậu tố _gwN, giới hạn 63 của Postgres
        n = n[:41] + "_" + hashlib.sha1(task.encode()).hexdigest()[:8]
    return n


def render_url(c, name):
    if URL_DRIVER.startswith("jdbc:"):
        url = f"{URL_DRIVER}://{c.host}:{c.port}/{name}"
        if JDBC_CREDENTIALS == "query":
            return url + f"?user={quote(c.user, safe='')}&password={quote(c.password, safe='')}"
        print(f"# JDBC: user={c.user} — mật khẩu lấy từ {c.source}; app đọc user/mật khẩu từ biến riêng "
              f"(QA_DB_JDBC_CREDENTIALS=query để gắn vào URL)", file=sys.stderr)
        return url
    auth = quote(c.user, safe="") + (":" + quote(c.password, safe="") if c.password else "")
    return f"{URL_DRIVER}://{auth + '@' if auth else ''}{c.host}:{c.port}/{name}"


def cmd_url(task):
    c = _admin()
    name = db_name(task)
    if not sql(c, "SELECT 1 FROM pg_database WHERE datname=%s", (name,)):
        sql(c, f'CREATE DATABASE "{name}"')
        print(f"# đã tạo {name}", file=sys.stderr)
    print(render_url(c, name))


def cmd_list():
    c = _admin()
    rows = [(r[0], int(r[1])) for r in sql(c, "SELECT datname, pg_database_size(datname) FROM pg_database ORDER BY 2 DESC")
            if r[0].startswith(SCRATCH)]
    for name, size in rows:
        print(f"{size / 1048576:>8.1f} MB  {name}")
    print(f"{len(rows)} DB, tổng {sum(r[1] for r in rows) / 1048576:.0f} MB")


def cmd_drop(task):
    c = _admin()
    base = db_name(task)
    names = [r[0] for r in sql(c, "SELECT datname FROM pg_database WHERE datname = %s OR datname LIKE %s",
                                (base, base.replace("_", "\\_") + "\\_gw%")) if r[0].startswith(SCRATCH)]
    for name in names:
        sql(c, "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname=%s AND pid<>pg_backend_pid()",
            (name,))
        sql(c, f'DROP DATABASE IF EXISTS "{name}"')
    print(f"Đã xoá {len(names)} DB: {', '.join(names) or '(không có)'}")


def cmd_workers():
    """Chia tải máy DB test: tổng khoảng 8 worker cho mọi task đang chạy QA cùng lúc."""
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import handoff
    active = 0
    for p in handoff.task_files():
        try:
            if handoff.canon(handoff.field(handoff.current_block(handoff.read(p)), "Phase")) == "qa-run":
                active += 1
        except OSError:
            pass
    n = 4 if active <= 2 else (2 if active <= 4 else 1)
    print(n)
    print(f"# {active} task đang qa-run -> -n {n}", file=sys.stderr)


def main():
    if len(sys.argv) < 2 or sys.argv[1] not in ("url", "list", "drop", "workers"):
        sys.exit(__doc__)
    cmd = sys.argv[1]
    if cmd == "list":
        return cmd_list()
    if cmd == "workers":
        return cmd_workers()
    if len(sys.argv) < 3:
        sys.exit(f"Thiếu <task>: qa_db.py {cmd} <task>")
    (cmd_url if cmd == "url" else cmd_drop)(sys.argv[2])


if __name__ == "__main__":
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8")
        except Exception:
            pass
    main()

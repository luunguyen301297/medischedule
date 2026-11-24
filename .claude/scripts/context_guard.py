#!/usr/bin/env python
"""context_guard.py — giữ context session gọn (xem CODING-GUIDELINES.md §6).

Hai chế độ, đọc JSON hook từ stdin:
  bash  (PreToolUse Bash)  — CHẶN (exit 2) lệnh đổ khúc file lớn vào context:
          `sed -n A,Bp` một khúc > SED_MAX dòng (hoặc tổng các khúc > SED_TOTAL_MAX),
          `cat <file>` không pipe mà file > CAT_MAX dòng. Ngưỡng + câu gợi ý: khối CONFIG (GUARD_*_LINES,
          GUARD_SEARCH_HINT). Bỏ qua khi lệnh có comment `# context-guard: allow`.
          + migration-guard: CHẶN lệnh migration ghi DB (mặc định `alembic upgrade|downgrade|stamp`) khi DB đích
          không phải DB tạm (SCRATCH_DB_PREFIXES). Lệnh/biến URL/tiền tố chỉnh ở khối CONFIG bên dưới (biến môi
          trường GUARD_*); stack không có migration: GUARD_MIGRATION_CMD_REGEX=''. Bỏ qua: `# migration-guard: allow`.
          DB đích lấy theo thứ tự: đối số URL trong lệnh (`-Dflyway.url=…`, `-url=…`, `--url …`,
          `--spring.datasource.url=…`) → phép gán `VAR=…` trong lệnh → biến môi trường → dòng `VAR=` trong `.env`.
          Lệnh có bộ nạp `.env` (GUARD_ENV_LOADER_REGEX: `. ./.env`, `source .env`, `set -a; .`, `Get-Content .env`;
          wrapper của dự án qua GUARD_ENV_WRAPPER_REGEX, vd `run-backend.ps1`): phép gán đứng SAU bộ nạp → `.env` →
          phép gán đứng TRƯỚC bộ nạp → biến môi trường (bộ nạp ghi đè môi trường).
          + startup-guard (tuỳ chọn, GUARD_STARTUP_CMD_REGEX): migration chạy lúc app khởi động (Spring Boot + Flyway,
          Rails…) — lệnh khởi động app trỏ DB ngoài máy (không phải localhost / DB tạm) thì CẢNH BÁO, KHÔNG chặn.
          + test-db-guard (tuỳ chọn, GUARD_TEST_CMD_REGEX): lệnh chạy test mà DB đích không phải DB tạm (tên không bắt
          đầu bằng SCRATCH_DB_PREFIXES) thì CẢNH BÁO — kể cả localhost (DB dev và qa_* cùng một Postgres).
  write (PostToolUse Write) — CẢNH BÁO (additionalContext) khi Write ghi đè file có sẵn > WRITE_WARN dòng.

Lỗi bất kỳ trong script → cho qua (exit 0), không bao giờ chặn nhầm vì bug của guard.
"""
import json
import os
import re
import shlex
import sys

BYPASS = "context-guard: allow"


def _strip_heredocs(cmd):
    """Bỏ thân heredoc (script python/sql nhúng) để không quét nhầm nội dung bên trong."""
    out, lines, i = [], cmd.split("\n"), 0
    while i < len(lines):
        line = lines[i]
        out.append(line)
        m = re.search(r"<<-?\s*['\"]?(\w+)['\"]?", line)
        i += 1
        if m:
            end = m.group(1)
            while i < len(lines) and lines[i].strip() != end:
                i += 1
            i += 1
    return "\n".join(out)


def _to_native(path, cwd):
    m = re.match(r"^/([a-zA-Z])/(.*)$", path)  # Git Bash /d/foo → D:/foo
    if m:
        path = f"{m.group(1).upper()}:/{m.group(2)}"
    path = os.path.expanduser(path)
    return path if os.path.isabs(path) else os.path.join(cwd, path)


def _count_lines(path):
    try:
        with open(path, "rb") as f:
            return sum(1 for _ in f)
    except OSError:
        return 0


# ---------------------------------------------------------------------------------------------------------------
# CONFIG — chỉnh theo stack dự án (đè bằng biến môi trường; mặc định = Alembic + DATABASE_URL dạng URL Postgres)
# Migration-guard chặn lệnh migration GHI lên DB (upgrade/downgrade/stamp…) khi DB đích không phải DB tạm.
# Ví dụ stack khác:
#   Prisma : GUARD_MIGRATION_CMD_REGEX='\bprisma\s+(migrate\s+(?:deploy|dev|reset)|db\s+push)\b'  GUARD_MIGRATION_DRYRUN_REGEX=''
#   Flyway (CLI `flyway … migrate`, Maven `./mvnw flyway:migrate`, Gradle `./gradlew flywayMigrate`):
#     GUARD_MIGRATION_CMD_REGEX='\bflyway(?:\b[:\s][^|;&]*?\b(migrate|clean|baseline|repair|undo)\b|(Migrate|Clean|Baseline|Repair|Undo)\b)'
#     GUARD_MIGRATION_DRYRUN_REGEX=''  GUARD_MIGRATION_DB_URL_VAR=FLYWAY_URL,DB_URL,SPRING_DATASOURCE_URL
#     (URL trong đối số `-url=jdbc:…` / `-Dflyway.url=jdbc:…` được đọc sẵn; `flyway info|validate` không bị chặn)
#   Spring Boot chạy Flyway lúc khởi động (chỉ CẢNH BÁO khi DB đích ở máy khác):
#     GUARD_STARTUP_CMD_REGEX='\bmvnw?(?:\.cmd)?\b[^|;&]*\bspring-boot:run\b|\bgradlew?(?:\.bat)?\b[^|;&]*\bbootRun\b|\bjava\b[^|;&]*\s-jar\s'
#   Rails (db:prepare trong entrypoint): GUARD_STARTUP_CMD_REGEX='\b(?:bin/)?rails\s+(?:s|server)\b|\bbin/dev\b'
#   Tắt hẳn: GUARD_MIGRATION_CMD_REGEX=''
# ---------------------------------------------------------------------------------------------------------------
def _env(name, default):
    v = os.environ.get(name)
    return default if v is None else v


def _env_int(name, default):
    try:
        return int(_env(name, str(default)))
    except ValueError:
        return default


# Giới hạn đọc (chế độ bash) và cảnh báo Write (chế độ write); bỏ qua giới hạn đọc bằng `# context-guard: allow`
SED_MAX = _env_int("GUARD_SED_MAX_LINES", 200)  # một khúc `sed -n A,Bp`
SED_TOTAL_MAX = _env_int("GUARD_SED_TOTAL_MAX_LINES", 300)  # tổng các khúc `sed -n` trong một lệnh
CAT_MAX = _env_int("GUARD_CAT_MAX_LINES", 400)  # tổng số dòng các file `cat` không pipe/redirect
WRITE_WARN = _env_int("GUARD_WRITE_WARN_LINES", 200)  # Write ghi đè file có sẵn dài hơn -> cảnh báo
# cách định vị code trước khi đọc, in trong thông báo chặn (repo có công cụ index code thì nêu lệnh của nó ở đây)
SEARCH_HINT = _env("GUARD_SEARCH_HINT", "Định vị trước bằng Grep / `grep -n` (hoặc công cụ index code nếu repo có)")
HINT = (
    f"{SEARCH_HINT}, rồi đọc khúc <= {SED_MAX} dòng; không đọc lại vùng đã đọc nếu chưa sửa "
    f"(CODING-GUIDELINES.md §6). Thật sự cần đọc nhiều: thêm `# {BYPASS}` cuối lệnh."
)


# regex (re.search trên từng đoạn lệnh) nhận ra lệnh migration ghi DB; group(1) nếu có = hành động, để in thông báo
MIGRATION_CMD_REGEX = _env("GUARD_MIGRATION_CMD_REGEX",
                           r"(?:^|[\s(])(?:\S*python\S*\s+(?:-\S+\s+)*-m\s+)?alembic\b[^|]*?\b(upgrade|downgrade|stamp)\b")
# đoạn lệnh khớp regex này = chế độ chỉ in SQL/dry-run, không ghi DB -> cho qua ('' = không có chế độ dry-run)
MIGRATION_DRYRUN_REGEX = _env("GUARD_MIGRATION_DRYRUN_REGEX", r"--sql\b")
# biến chứa URL DB mà công cụ migration dùng (đặt inline trong lệnh, biến môi trường, hoặc dòng VAR= trong .env);
# nhiều biến cách nhau dấu phẩy, biến đứng trước được ưu tiên (vd `FLYWAY_URL,DB_URL,SPRING_DATASOURCE_URL`)
MIGRATION_DB_URL_VARS = tuple(v.strip() for v in _env("GUARD_MIGRATION_DB_URL_VAR", "DATABASE_URL").split(",") if v.strip())
MIGRATION_DB_URL_VAR = "/".join(MIGRATION_DB_URL_VARS) or "DATABASE_URL"  # tên hiển thị trong thông báo
# đối số dòng lệnh mang URL DB (group 1 = giá trị): `-Dflyway.url=…`, `-Dspring.datasource.url=…`, `-url=…`,
# `--url …`, `--url=…`, `--database-url …`, `--spring.datasource.url=…`; '' = không đọc đối số
DB_URL_ARG_REGEX = _env("GUARD_DB_URL_ARG_REGEX",
                        r"""(?:^|\s)(?:-D[\w.-]*?\burl|--?(?:[\w.]*[.-])?url)(?:=|\s+)("[^"]*"|'[^']*'|\$\([^)]*\)|[^\s;&|]+)""")
# tách host + tên DB từ URL (group 1 = host[:port], group 2 = tên DB) — khớp cả postgresql://u:p@h/db lẫn jdbc:x://h/db
MIGRATION_DB_URL_PARSE = _env("GUARD_DB_URL_REGEX", r"//(?:[^/@\s]*@)?([^/?\s]+)/([^/?\s;]+)")
# giá trị URL khớp regex này = lấy từ script tạo DB tạm -> cho qua
SCRATCH_URL_HELPER_REGEX = _env("GUARD_SCRATCH_URL_HELPER_REGEX", r"qa_db\.py\s+url\b")
# gợi ý cách lấy URL DB tạm in trong thông báo chặn
SCRATCH_URL_HINT = _env("GUARD_SCRATCH_URL_HINT", "$(python .claude/scripts/qa_db.py url <task>)")
# tiền tố DB tạm được phép chạy migration (cùng danh sách với conftest/fixture test tích hợp của dự án)
SCRATCH_DB = tuple(p for p in _env("SCRATCH_DB_PREFIXES", "qa_,tmp_itest_").split(",") if p)
# file env ở gốc repo đọc khi lệnh không đặt URL inline
ENV_FILE = _env("GUARD_ENV_FILE", ".env")
_ENVF = re.escape(os.path.basename(ENV_FILE) or ".env")
# bộ nạp `.env` trong lệnh (ghi đè biến môi trường): `. ./.env`, `. .env`, `source ../.env`, `set -a; .`, PowerShell
# `Get-Content .env | … Set-Item env:…`. Lệnh có bộ nạp: phép gán đứng SAU bộ nạp thắng; không có thì `.env` thắng
# biến môi trường và phép gán đứng TRƯỚC bộ nạp. '' = không nhận bộ nạp nào (thứ tự cũ: lệnh → môi trường → .env)
ENV_LOADER_REGEX = _env("GUARD_ENV_LOADER_REGEX",
                        rf"""(?:^|[\s;&|(])(?:\.|source)\s+["']?(?:[^\s;&|"']*[/\\])?{_ENVF}(?![\w.-])"""
                        rf"""|\bset\s+-a\s*[;&\n]+\s*(?:\.|source)\s"""
                        rf"""|\bGet-Content\s+(?:-Path\s+)?["']?(?:[^\s;&|"']*[/\\])?{_ENVF}(?![\w.-])""")
# script wrapper của dự án tự nạp `.env` lúc bắt đầu (vd `\brun-backend\.(?:ps1|sh)\b`): tính như bộ nạp đặt tại chỗ
# gọi wrapper -> chỉ phép gán đứng SAU nó trong lệnh mới thắng `.env`. '' = không có (mặc định)
ENV_WRAPPER_REGEX = _env("GUARD_ENV_WRAPPER_REGEX", "")
# lệnh khởi động app mà migration tự chạy lúc start (Spring Boot + Flyway/Liquibase, Rails db:prepare…): DB đích ở
# máy ngoài STARTUP_ALLOWED_HOSTS và không phải DB tạm -> CẢNH BÁO (không chặn). '' = tắt (mặc định)
STARTUP_CMD_REGEX = _env("GUARD_STARTUP_CMD_REGEX", "")
# host coi là DB dev cục bộ khi khởi động app (so không kèm port)
STARTUP_ALLOWED_HOSTS = tuple(h.strip().lower() for h in _env("GUARD_STARTUP_ALLOWED_HOSTS",
                                                               "localhost,127.0.0.1,::1").split(",") if h.strip())
# lệnh chạy test (vd Java `mvnw test|verify`, `gradlew test|check`; Python `pytest`): DB đích không phải DB tạm
# (tên không bắt đầu bằng SCRATCH_DB_PREFIXES) -> CẢNH BÁO, kể cả host localhost — DB dev và DB test qa_* có thể cùng
# một máy Postgres, chỉ tên DB phân biệt được. '' = tắt (mặc định). DB đích không xác định được -> im
TEST_CMD_REGEX = _env("GUARD_TEST_CMD_REGEX", "")
# biến URL DB mà lệnh test dùng (danh sách phẩy, biến trước ưu tiên); mặc định = GUARD_MIGRATION_DB_URL_VAR
TEST_DB_URL_VARS = tuple(v.strip() for v in _env("GUARD_TEST_DB_URL_VAR", ",".join(MIGRATION_DB_URL_VARS)).split(",")
                         if v.strip())
# đoạn lệnh bắt đầu bằng các lệnh này chỉ đọc/in chuỗi -> không xét. `codegraph` giữ lại có chủ đích: lệnh tra cứu
# kiểu `codegraph explore "alembic upgrade"` chứa chữ migration trong chuỗi hỏi, không được chặn nhầm; repo không dùng
# CodeGraph thì mục này vô hại (không lệnh nào bắt đầu bằng nó)
READONLY_CMDS = r"(grep|rg|echo|printf|git|cat|sed|ls|codegraph)\b"
MIGRATION_BYPASS = ("migration-guard: allow", "alembic-guard: allow")  # dấu cũ vẫn nhận
# ---------------------------------------------------------------------------------------------------------------
_ASSIGN = re.compile(r"(?:^|[\s;&(])(?:export\s+)?([A-Za-z_]\w*)=(\"[^\"]*\"|'[^']*'|\$\([^)]*\)|[^\s;&|)]+)")
# PowerShell `$env:VAR = …` (giá trị `(python … url t1)` cũng nhận)
_PS_ASSIGN = re.compile(r"\$env:([A-Za-z_]\w*)\s*=\s*(\"[^\"]*\"|'[^']*'|\$?\([^)]*\)|[^\s;&|)]+)", re.I)


def _environ_url(names):
    """(url, nguồn) từ biến môi trường, theo thứ tự names."""
    for var in names:
        if os.environ.get(var):
            return os.environ[var], f"biến môi trường {var}"
    return None, None


def _dotenv_url(cwd, names, loaded=False):
    """(url, nguồn) từ dòng `VAR=` của file env ở cwd / thư mục cha / CLAUDE_PROJECT_DIR."""
    d = cwd
    roots = []
    for _ in range(6):
        roots.append(d)
        d = os.path.dirname(d)
    roots.append(os.environ.get("CLAUDE_PROJECT_DIR") or "")
    for r in roots:
        p = os.path.join(r, ENV_FILE) if r else ""
        if not (p and os.path.isfile(p)):
            continue
        found = {}
        for line in open(p, encoding="utf-8", errors="ignore"):
            m = re.match(r"\s*(?:export\s+)?([A-Za-z_]\w*)\s*=(.*)$", line)
            if m and m.group(1) in names and m.group(1) not in found:
                found[m.group(1)] = m.group(2).strip().strip("\"'")
        for var in names:
            if found.get(var):
                return found[var], f"{p.replace(os.sep, '/')} ({var}{', lệnh tự nạp file này' if loaded else ''})"
    return None, None


def _loader_positions(cmd):
    """Vị trí các bộ nạp `.env` (GUARD_ENV_LOADER_REGEX / GUARD_ENV_WRAPPER_REGEX) trong lệnh, bỏ đoạn chỉ đọc."""
    pats = [p for p in (ENV_LOADER_REGEX, ENV_WRAPPER_REGEX) if p]
    if not pats:
        return []
    spans = _statement_spans(cmd)
    out = []
    for pat in pats:
        for m in re.finditer(pat, cmd):
            pos = m.start()
            seg = next((t for st, t in reversed(spans) if st <= pos), "")
            if not re.match(READONLY_CMDS, _command_part(seg.strip())):
                out.append(pos)
    return out


def _target_url(cmd, stmt, cwd, end=None, names=None):
    """(url, nguồn) DB đích của đoạn lệnh `stmt` (kết thúc ở vị trí `end` trong cmd; chỉ xét phép gán đứng trước đó).

    Đối số URL trong đoạn lệnh thắng. Sau đó, không có bộ nạp `.env` đứng trước `end`: phép gán VAR=… trong lệnh →
    biến môi trường → `.env`. Có bộ nạp (lấy bộ nạp cuối cùng): phép gán SAU bộ nạp → `.env` → phép gán TRƯỚC bộ nạp →
    biến môi trường (bộ nạp ghi đè cả hai)."""
    names = names or MIGRATION_DB_URL_VARS
    if DB_URL_ARG_REGEX:
        m = re.search(DB_URL_ARG_REGEX, stmt)
        if m:
            return m.group(1).strip("\"'"), "đối số URL trong lệnh"
    end = len(cmd) if end is None else end
    assigns = sorted((m.start(), m.group(1), m.group(2).strip("\"'"))
                     for rx in (_ASSIGN, _PS_ASSIGN) for m in rx.finditer(cmd))
    env_all = {k: v for _, k, v in assigns}  # để tra VAR=$URL

    def pick(items, label):
        d = {k: v for _, k, v in items}  # phép gán sau cùng thắng
        for var in names:
            val = d.get(var)
            if val is None:
                continue
            for _ in range(3):  # VAR=$URL → tra tiếp phép gán URL=… trong cùng lệnh
                ref = re.fullmatch(r"\$\{?(\w+)\}?", val or "")
                if not ref or ref.group(1) not in env_all:
                    break
                val = env_all[ref.group(1)]
            return val, f"{var} {label}"
        return None, None

    before_end = [a for a in assigns if a[0] < end]
    loaders = [p for p in _loader_positions(cmd) if p < end]
    if not loaders:
        tiers = (lambda: pick(before_end, "đặt trong lệnh"), lambda: _environ_url(names),
                 lambda: _dotenv_url(cwd, names))
    else:
        last = max(loaders)
        tiers = (lambda: pick([a for a in before_end if a[0] > last], "đặt trong lệnh sau bước nạp .env"),
                 lambda: _dotenv_url(cwd, names, loaded=True),
                 lambda: pick([a for a in before_end if a[0] < last], "đặt trong lệnh trước bước nạp .env"),
                 lambda: _environ_url(names))
    for tier in tiers:
        val, src = tier()
        if val:
            return val, src
    return None, None


_PS = False  # lệnh của tool PowerShell (backtick là escape, không phải thế lệnh) — đặt trong main()


def _toplevel(s, ps=False):
    """Đánh dấu ký tự nằm NGOÀI nháy '…' "…", $(…), backtick `…` (bash) và ký tự escape (bash `\\`, PowerShell
    backtick). Trả (mask, cân bằng?). Cùng thuật toán với handoff._toplevel (script chạy `-I`, không import lẫn nhau)."""
    esc = "`" if ps else "\\"
    stack, mask, i, n = [], [False] * len(s), 0, len(s)
    while i < n:
        c, top = s[i], (stack[-1] if stack else None)
        if top == "'":
            if c == "'":
                stack.pop()
        elif c == esc and i + 1 < n:
            i += 2
            continue
        elif top == '"':
            if c == '"':
                stack.pop()
            elif c == "$" and s[i + 1:i + 2] == "(":
                stack.append("(")
                i += 1
            elif c == "`" and not ps:
                stack.append("`")
        elif top == "`" and c == "`":
            stack.pop()
        elif c in "'\"" or (c == "`" and not ps):
            stack.append(c)
        elif c == "$" and s[i + 1:i + 2] == "(":
            stack.append("(")
            i += 1
        elif c == "(" and stack:
            stack.append("(")
        elif c == ")" and top == "(":
            stack.pop()
        elif not stack:
            mask[i] = True
        i += 1
    return mask, not stack


def _statement_spans(cmd):
    """[(vị trí bắt đầu, đoạn)] — tách câu lệnh tại && || ; xuống dòng đứng NGOÀI nháy/$(…)/backtick (không tách tại
    `|`: pipe thuộc đoạn). Ngoặc subshell `(`/`)` ở cấp ngoài thay bằng khoảng trắng (giữ nguyên vị trí) để
    `(cd x && ./mvnw test)` cho ra đoạn `./mvnw test` sạch. Không cân nháy/ngoặc -> tách thô bằng regex như cũ."""
    mask, ok = _toplevel(cmd, _PS)
    if not ok:
        spans, pos = [], 0
        for m in re.finditer(r"&&|\|\||;|\n", cmd):
            spans.append((pos, cmd[pos:m.start()]))
            pos = m.end()
        spans.append((pos, cmd[pos:]))
        return spans
    text = "".join(" " if mask[i] and c in "()" else c for i, c in enumerate(cmd))
    spans, start, i = [], 0, 0
    while i < len(text):
        step = 2 if mask[i] and text[i:i + 2] in ("&&", "||") else (1 if mask[i] and text[i] in ";\n" else 0)
        if step:
            spans.append((start, text[start:i]))
            start = i = i + step
        else:
            i += 1
    spans.append((start, text[start:]))
    return spans


def _statements(cmd):
    return [seg for _, seg in _statement_spans(cmd)]


def _command_part(seg):
    """Đoạn lệnh sau khi bỏ tiền tố gán `VAR=…` (giá trị có nháy/khoảng trắng/$(…)/backtick), `env`, `export`, `&`."""
    s = seg.strip()
    while s:
        m = re.match(r"(?:[&{!]\s*|env\s+|export\s+)", s)
        if m:
            s = s[m.end():]
            continue
        m = re.match(r"[A-Za-z_]\w*=", s)
        if not m:
            break
        mask, ok = _toplevel(s, _PS)
        j = (next((k for k in range(m.end(), len(s)) if mask[k] and s[k].isspace()), len(s)) if ok
             else re.match(r"\S*", s).end())
        s = s[j:].lstrip()
    return s


def _find_stmt(cmd, regex, skip_dryrun):
    """Đoạn lệnh đầu tiên khớp regex (bỏ đoạn chỉ đọc) -> (đoạn, match, vị trí cuối đoạn) hoặc (None, None, None)."""
    for start, stmt in _statement_spans(cmd):
        s = stmt.strip()
        if re.match(READONLY_CMDS, _command_part(s)):
            continue
        m = re.search(regex, s)
        if m and not (skip_dryrun and MIGRATION_DRYRUN_REGEX and re.search(MIGRATION_DRYRUN_REGEX, s)):
            return s, m, start + len(stmt)
    return None, None, None


def _parse_url(val):
    """(host không kèm port, host[:port], tên DB) từ URL; không tách được -> '?'."""
    m = re.search(MIGRATION_DB_URL_PARSE, val or "")
    if not m:
        return "?", "?", "?"
    hostport = m.group(1)
    hm = re.match(r"^\[([^\]]+)\]|^([^:]+)", hostport)
    host = (hm.group(1) or hm.group(2)) if hm else hostport
    return host.lower(), hostport, m.group(2)


def check_migration(cmd, cwd):
    """Chặn lệnh migration ghi DB khi DB đích không phải DB tạm (qa_* …) — HARD-STOPS "An toàn môi trường".

    Sự cố gốc: prod crash-loop vì SQL tay không stamp; subagent stamp nhầm DB dev vì dùng DATABASE_URL của máy.
    """
    if not MIGRATION_CMD_REGEX or any(b in cmd for b in MIGRATION_BYPASS):
        return None
    stmt, m, end = _find_stmt(cmd, MIGRATION_CMD_REGEX, True)
    if not stmt:
        return None
    hit = (next((g for g in m.groups() if g), None) or m.group(0)).strip()
    var = MIGRATION_DB_URL_VAR
    val, src = _target_url(cmd, stmt, cwd, end)
    if val and SCRATCH_URL_HELPER_REGEX and re.search(SCRATCH_URL_HELPER_REGEX, val):
        return None
    first_var = MIGRATION_DB_URL_VARS[0] if MIGRATION_DB_URL_VARS else var
    tail = (f" Chạy trên DB tạm: `{first_var}={SCRATCH_URL_HINT} <lệnh migration> …` cùng một lệnh. Cần chạy trên DB "
            f"này thật: hỏi user trước (kiểm bảng version của migration + cột/bảng thật), user đồng ý rồi thêm "
            f"`# {MIGRATION_BYPASS[0]}` cuối lệnh.")
    if not val:
        return (f"[migration-guard] Chặn `{hit}`: không xác định được DB đích (không có đối số URL, {var} trong lệnh, "
                f"môi trường hay {ENV_FILE}).{tail}")
    if "$" in val:
        return (f"[migration-guard] Chặn `{hit}`: URL DB là biến chưa rõ giá trị (`{val[:40]}`) — ghi URL tường minh "
                f"trong lệnh.{tail}")
    _, hostport, db = _parse_url(val)
    if db.startswith(SCRATCH_DB):
        return None
    return (f"[migration-guard] Chặn `{hit}`: DB đích là `{db}` trên {hostport} (theo {src}) — không phải DB tạm "
            f"{'/'.join(p + '*' for p in SCRATCH_DB)}.{tail}")


def check_startup(cmd, cwd):
    """CẢNH BÁO (không chặn) khi lệnh khởi động app — app tự chạy migration lúc start — trỏ DB ở máy khác.

    Chỉ biết DB đích qua đối số / phép gán trong lệnh / biến môi trường / .env; cấu hình nằm trong
    application.yml, profile, secret manager… thì không thấy -> im lặng (giới hạn, xem scripts/README.md).
    """
    if not STARTUP_CMD_REGEX or any(b in cmd for b in MIGRATION_BYPASS):
        return None
    stmt, m, end = _find_stmt(cmd, STARTUP_CMD_REGEX, False)
    if not stmt:
        return None
    val, src = _target_url(cmd, stmt, cwd, end)
    if not val or "$" in val or (SCRATCH_URL_HELPER_REGEX and re.search(SCRATCH_URL_HELPER_REGEX, val)):
        return None
    host, hostport, db = _parse_url(val)
    if host == "?" or db.startswith(SCRATCH_DB) or host in STARTUP_ALLOWED_HOSTS:
        return None
    return (f"[migration-guard] CẢNH BÁO: `{m.group(0).strip()[:60]}` khởi động app — migration tự chạy lúc start sẽ "
            f"ghi lên DB `{db}` trên {hostport} (theo {src}), không phải DB cục bộ/DB tạm. Nếu đây là DB dùng chung "
            f"hoặc prod: dừng app, trỏ về DB cục bộ hoặc {SCRATCH_URL_HINT}, và hỏi user trước.")


def check_test_db(cmd, cwd):
    """CẢNH BÁO (không chặn) khi lệnh chạy test (GUARD_TEST_CMD_REGEX) trỏ DB không phải DB tạm — so TÊN DB, không so
    host: DB dev và qa_* có thể cùng một Postgres (Docker local), host localhost không chứng minh được gì."""
    if not TEST_CMD_REGEX or any(b in cmd for b in MIGRATION_BYPASS):
        return None
    stmt, m, end = _find_stmt(cmd, TEST_CMD_REGEX, False)
    if not stmt:
        return None
    val, src = _target_url(cmd, stmt, cwd, end, TEST_DB_URL_VARS)
    if not val or "$" in val or (SCRATCH_URL_HELPER_REGEX and re.search(SCRATCH_URL_HELPER_REGEX, val)):
        return None
    _, hostport, db = _parse_url(val)
    if db == "?" or db.startswith(SCRATCH_DB):
        return None
    var = TEST_DB_URL_VARS[0] if TEST_DB_URL_VARS else MIGRATION_DB_URL_VAR
    return (f"[db-guard] CẢNH BÁO: `{m.group(0).strip()[:60]}` chạy test trên DB `{db}` ({hostport}, theo {src}) — "
            f"không phải DB tạm {'/'.join(p + '*' for p in SCRATCH_DB)} (kiểm cả host lẫn TÊN DB: DB dev và DB test có "
            f"thể cùng một máy). Test tích hợp: dùng lệnh hoàn chỉnh INTEGRATION_TEST_COMMAND "
            f"(PROJECT/stack-and-commands.md) — nạp .env TRƯỚC, rồi mới đặt {var}={SCRATCH_URL_HINT}. Test không đụng "
            f"DB thì bỏ qua cảnh báo này.")


def check_bash(data):
    cmd = (data.get("tool_input") or {}).get("command") or ""
    cwd = data.get("cwd") or os.getcwd()
    msg = check_migration(_strip_heredocs(cmd), cwd)
    if msg:
        return msg
    if BYPASS in cmd:
        return None
    cmd = _strip_heredocs(cmd)

    sed_ranges = []
    for m in re.finditer(r"\bsed\b[^|;&\n]*", cmd):
        seg = m.group(0)
        if not re.search(r"(^|\s)-\w*n", seg):
            continue
        if re.match(r"\|(?!\|)", cmd[m.end():]):  # `sed -n ... | grep/head` — đã lọc, không đổ thẳng vào context
            continue
        if re.search(r"(^|[^0-9&])>", seg):  # `sed -n ... > file` — trích ra file, không vào context
            continue
        for a, b in re.findall(r"(\d+)\s*,\s*(\d+|\$)\s*p", seg):
            size = 10**9 if b == "$" else int(b) - int(a) + 1
            sed_ranges.append((f"{a},{b}p", size))
    big = [r for r in sed_ranges if r[1] > SED_MAX]
    if big:
        label, size = big[0]
        shown = "tới cuối file" if size >= 10**9 else f"{size} dòng"
        return f"[context-guard] Chặn: `sed -n {label}` đọc {shown} (> {SED_MAX}). {HINT}"
    total = sum(s for _, s in sed_ranges)
    if total > SED_TOTAL_MAX:
        return f"[context-guard] Chặn: các khúc `sed -n` cộng lại {total} dòng (> {SED_TOTAL_MAX}). {HINT}"

    for stmt in _statements(cmd):
        stmt = stmt.strip()
        if not stmt:
            continue
        try:
            first = shlex.split(stmt.split("|")[0])
        except ValueError:
            continue
        if not first:
            continue
        if first[0] == "cd" and len(first) > 1:
            cwd = _to_native(first[1], cwd)
            continue
        piped = re.search(r"(?<!\|)\|(?!\|)", stmt) is not None
        redirected = re.search(r"(^|[^0-9&])>", stmt) is not None  # `> file`, bỏ qua `2>/dev/null`
        if first[0] != "cat" or piped or redirected:
            continue
        files = [a for a in first[1:] if not a.startswith("-")]
        sizes = [(f, _count_lines(_to_native(f, cwd))) for f in files]
        lines = sum(n for _, n in sizes)
        if lines > CAT_MAX:
            names = ", ".join(f"{f} ({n} dòng)" for f, n in sizes if n)
            return f"[context-guard] Chặn: `cat` đổ {lines} dòng vào context ({names}; > {CAT_MAX}). {HINT}"
    return None


def check_write(data):
    resp = data.get("tool_response")
    if not isinstance(resp, dict) or resp.get("type") != "update":
        return None
    inp = data.get("tool_input") or {}
    lines = (inp.get("content") or "").count("\n") + 1
    if lines <= WRITE_WARN:
        return None
    path = inp.get("file_path") or resp.get("filePath") or "?"
    return (
        f"[context-guard] Vừa ghi đè NGUYÊN file có sẵn {path} ({lines} dòng) bằng Write — toàn bộ nội dung "
        "đó nằm lại trong context suốt session. Lần sau sửa file có sẵn bằng Edit (kể cả mockup HTML, "
        "BA spec, script scratchpad) — CODING-GUIDELINES.md §5/§6."
    )


def main():
    global _PS
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    try:
        data = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
        _PS = data.get("tool_name") == "PowerShell"
        if mode == "bash":
            msg = check_bash(data)
            if msg:
                sys.stderr.buffer.write(msg.encode("utf-8"))
                sys.exit(2)
            cmd = _strip_heredocs((data.get("tool_input") or {}).get("command") or "")
            cwd = data.get("cwd") or os.getcwd()
            warn = check_test_db(cmd, cwd) or check_startup(cmd, cwd)
            if warn:
                out = {"systemMessage": warn,
                       "hookSpecificOutput": {"hookEventName": "PreToolUse", "additionalContext": warn}}
                sys.stdout.buffer.write(json.dumps(out, ensure_ascii=False).encode("utf-8"))
        elif mode == "write":
            msg = check_write(data)
            if msg:
                out = {"hookSpecificOutput": {"hookEventName": "PostToolUse", "additionalContext": msg}}
                sys.stdout.buffer.write(json.dumps(out, ensure_ascii=False).encode("utf-8"))
    except SystemExit:
        raise
    except Exception:
        pass
    sys.exit(0)


if __name__ == "__main__":
    main()

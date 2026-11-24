#!/usr/bin/env python
"""handoff.py — quản lý HANDOFF theo từng task (mỗi task một file, chạy song song).

  status                 in các task đang chạy (hook UserPromptSubmit gọi lệnh này)
  new <task> [opts]      tạo docs/HANDOFF.<task>.local.md từ mẫu
  close <task>           ghi entry vào docs/HANDOFF.history.md rồi xoá file của task
  timing [task]          thời gian từng phase (new/set-phase/raise/resolve tự ghi mốc giờ; close ghi HANDOFF.timing.jsonl)
  stale                  liệt kê file đã idle (chưa dọn) — không xoá gì
  raise <task> --text …  phiên triển khai ghi câu hỏi/lệch spec (OPEN) vào HANDOFF
  resolve <task> Q<n> …  điều phối ghi quyết định (RESOLVED)
  set-phase <task> <p>   đổi Phase ở cả block lẫn yaml (nhận tên cũ, ghi tên mới)
  scope-suggest <task>   gợi ý Scope từ nhánh/commit của task (--apply để ghi)
  claim <task>           session con ghi Workdir (cwd) để Stop hook giữ nó chạy tới hết pipeline
  mark / stopcheck       PostToolUse + Stop: nhắc chạy test (mặc định pytest; xem CONFIG) trước khi dừng
  guard                  PreToolUse Edit|Write: CHẶN sửa code thuộc Scope của task chưa duyệt thiết kế

File: docs/HANDOFF.<task>.local.md (không git-tracked, .gitignore: docs/HANDOFF.*.local.md).
Lịch sử: docs/HANDOFF.history.md (git-tracked, chỉ append).
Chi tiết quy trình: .claude/ORCHESTRATION.md §4. Sửa file này xong: chạy `python .claude/scripts/test_hooks.py`.
"""
import argparse
import collections
import glob
import os
import re
import sys
from datetime import date, datetime

for _s in (sys.stdin, sys.stdout, sys.stderr):  # -I bỏ PYTHONIOENCODING: hook gửi UTF-8
    try:
        _s.reconfigure(encoding="utf-8")
    except Exception:
        pass

ROOT = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()

# ---------------------------------------------------------------------------------------------------------------
# CONFIG — chỉnh theo stack dự án (đè bằng biến môi trường; mặc định = backend Python/pytest + frontend React)
# ---------------------------------------------------------------------------------------------------------------
def _env(name, default):
    v = os.environ.get(name)
    return default if v is None else v


def _csv(name, default):
    return tuple(x.strip() for x in _env(name, default).split(",") if x.strip())


# nhánh tích hợp (scope-suggest, select_tests, lời nhắc); không đặt / rỗng = tự dò: `develop` nếu có (local hoặc
# origin), không thì `main`, rồi `master` (dò lười — chỉ khi cần, không tốn git mỗi lần hook chạy). Dùng base_branch().
_BASE_BRANCH_ENV = _env("HANDOFF_BASE_BRANCH", "")
# đoạn đường dẫn = code sản phẩm (guard Scope, QA không được sửa, scope-suggest)
CODE_PATHS = _csv("HANDOFF_CODE_PATHS", "backend/app/,frontend/src/,apps/")
# đoạn đường dẫn mà sửa xong phải chạy test trước khi dừng (Stop hook nhắc)
BACKEND_SEGS = _csv("HANDOFF_BACKEND_SEGS", "backend/app/,backend/tests/")
# đường dẫn khớp regex này = file test (QA được sửa; không tính là code sản phẩm). Mặc định nhận: Python `tests/`,
# Maven/Gradle `src/test/`, JS/TS `*.test.ts(x)/.js(x)`, Go `*_test.go`, `*.check.*`
TEST_PATH_REGEX = _env("HANDOFF_TEST_PATH_REGEX", r"/tests/|\.check\.|/src/test/|\.test\.[jt]sx?$|_test\.go$")
# lệnh test: (1) regex tìm trong cả lệnh; (2) regex khớp ĐẦU từng đoạn lệnh (sau khi bỏ VAR=x phía trước).
#   Không tính `pip show pytest`, `grep pytest`… Ví dụ JS: HANDOFF_TEST_CMD_REGEX=''
#   HANDOFF_TEST_RUNNER_REGEX='(?:npm|pnpm|yarn)\s+(?:run\s+)?test\b|npx\s+(?:vitest|jest)\b|(?:\S*[/\\])?(?:vitest|jest)(?:\s|$)'
# Ví dụ JVM (Maven/Gradle): HANDOFF_TEST_CMD_REGEX=''
#   HANDOFF_TEST_RUNNER_REGEX='(?:&\s+)?(?:\S*[/\\])?(?:mvnw?(?:\.cmd)?|gradlew?(?:\.bat)?)\s+(?:\S+\s+)*?(?:test|verify|check)(?:\s|$)'
#   HANDOFF_TEST_LABEL='JUnit' HANDOFF_TEST_PREFILTER='mvn,gradle' HANDOFF_BACKEND_SEGS='backend/src/main/,backend/src/test/'
# Ví dụ Go: HANDOFF_TEST_CMD_REGEX='' HANDOFF_TEST_RUNNER_REGEX='go\s+test\b' HANDOFF_TEST_LABEL='go test'
#   HANDOFF_TEST_PREFILTER='go test'
TEST_CMD_REGEX = _env("HANDOFF_TEST_CMD_REGEX", r"-m\s+pytest\b")
TEST_RUNNER_REGEX = _env("HANDOFF_TEST_RUNNER_REGEX", r"(?:\S*[/\\])?pytest(?:\.exe)?(?:\s|$)")
# tên bộ test in trong lời nhắc + lệnh gợi ý chọn test ({base} = base_branch())
TEST_LABEL = _env("HANDOFF_TEST_LABEL", "pytest")
# mặc định chỉ in khi .claude/scripts/select_tests.py tồn tại ('' = không in)
_SELECT_TESTS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "select_tests.py")
TEST_SELECT_HINT = _env("HANDOFF_TEST_SELECT_HINT",
                        "select_tests.py --base {base}" if os.path.exists(_SELECT_TESTS) else "")
# ví dụ glob Scope in trong hướng dẫn `new --scope`
SCOPE_EXAMPLE = _env("HANDOFF_SCOPE_EXAMPLE", "backend/app/services/foo*.py,frontend/src/pages/Foo/")
# thư mục migration ghi trong Artifacts Registry của mẫu HANDOFF
MIGRATION_DIR = _env("HANDOFF_MIGRATION_DIR", "backend/alembic/versions/")
# script dọn DB test tạm khi `close` (gọi `<script> drop <task>`); '' hoặc file không có = bỏ qua
QA_DB_SCRIPT = _env("HANDOFF_QA_DB_SCRIPT", os.path.join(os.path.dirname(os.path.abspath(__file__)), "qa_db.py"))
# ---------------------------------------------------------------------------------------------------------------


_base_cache = []


def base_branch():
    """HANDOFF_BASE_BRANCH nếu đặt; không thì develop → main → master (nhánh local hoặc origin/), mặc định main."""
    if _BASE_BRANCH_ENV:
        return _BASE_BRANCH_ENV
    if not _base_cache:
        import subprocess
        names = ("develop", "main", "master")
        refs = [f"refs/{k}/{n}" for n in names for k in ("heads", "remotes/origin")]
        try:
            out = subprocess.run(["git", "for-each-ref", "--format=%(refname)", *refs], cwd=ROOT,
                                 capture_output=True, text=True, timeout=5).stdout.split()
        except Exception:
            out = []
        found = {r.rsplit("/", 1)[-1] for r in out}
        _base_cache.append(next((n for n in names if n in found), "main"))
    return _base_cache[0]


def __getattr__(name):  # tương thích: handoff.BASE_BRANCH vẫn đọc được (tính lười)
    if name == "BASE_BRANCH":
        return base_branch()
    raise AttributeError(name)


def _main_checkout(root):
    """Worktree không có docs/HANDOFF.*.local.md (không git-tracked) -> dùng checkout chính qua git common dir."""
    import subprocess
    try:
        common = subprocess.run(["git", "rev-parse", "--path-format=absolute", "--git-common-dir"], cwd=root,
                                capture_output=True, text=True, timeout=3).stdout.strip()
    except Exception:
        return root
    if common.endswith("/.git") or common.endswith(os.sep + ".git"):
        return os.path.dirname(common)
    return root


MAIN = _main_checkout(ROOT)
DOCS = os.path.join(MAIN, "docs")
HISTORY = os.path.join(DOCS, "HANDOFF.history.md")
MAX_LINES = 8

TEMPLATE = """# HANDOFF — {task}

## Current Feature

- **Task:** {title}
- **Feature:** {task}
- **Phase:** {phase}
- **Flow:** {flow}
- **Model:** {model}
- **Session:** {session}
- **Orchestrator:** {orch}
- **Workdir:** -
- **Scope:** {scope}

```yaml
phase: {phase}
flow: {flow}
feature_name: {task}
domain: null
capability: null
feature: null
knowledge_impact: none          # none | partial | full
knowledge_distill_status: pending
needs_db_detail: false
needs_api_detail: false
```

## Mốc giờ (handoff.py tự ghi khi new / set-phase / raise / resolve — đừng sửa tay)

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ⬜ | docs/tasks/{task}-ba-spec.md |
| Tech Design | ⬜ | docs/design/{task}-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ⬜ | docs/reviews/design-review-{task}.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/{task}-api-spec.md (khi needs_api_detail) |
| DB Migration | ⬜ | {migration_dir}… (khi needs_db_detail) |
| UX Spec | ⬜ | docs/design/{task}-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/{task}-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)
"""


def task_files():
    files = sorted(glob.glob(os.path.join(DOCS, "HANDOFF.*.local.md")))
    main = os.path.join(DOCS, "HANDOFF.local.md")
    if os.path.exists(main):
        files.insert(0, main)
    return files


def task_name(path):
    base = os.path.basename(path)
    if base == "HANDOFF.local.md":
        return "(main)"
    return base[len("HANDOFF."):-len(".local.md")]


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def current_block(text):
    m = re.search(r"^## Current[^\n]*\n(.*?)(?=^## |\Z)", text, re.S | re.M)
    return m.group(1) if m else ""


def field(block, key):
    m = re.search(rf"^-?\s*\*\*{key}:?\*\*:?\s*(.+)$", block, re.M)
    return m.group(1).strip() if m else ""


def is_idle(phase):
    return not phase or phase.lower().startswith(("idle", "done", "closed"))


ROLES = ("lead", "dev", "qa")


def _record_role():
    """UserPromptSubmit: brief của session có dòng `[[handoff-role <task> <lead|dev|qa>]]` -> nhớ session_id -> vai trò."""
    import json
    if sys.stdin is None or sys.stdin.isatty():
        return
    try:
        d = json.loads(sys.stdin.read() or "{}")
    except Exception:
        return
    m = re.search(r"\[\[handoff-role\s+([\w.-]+)\s+(lead|dev|qa)\]\]", d.get("prompt") or "")
    sid = d.get("session_id")
    if m and sid:
        os.makedirs(MARKS, exist_ok=True)
        with open(_mark_path(sid, "role"), "w", encoding="utf-8") as f:
            f.write(f"{m.group(1)} {m.group(2)}")


def _role_of(sid):
    try:
        with open(_mark_path(sid, "role"), encoding="utf-8") as f:
            task, role = f.read().split()
        return task, role
    except (OSError, ValueError):
        return None


def cmd_status(_):
    try:
        _record_role()
    except Exception:
        pass
    active, stale, old_names = [], [], 0
    for p in task_files():
        try:
            text_all = read(p)
            b = current_block(text_all)
        except OSError:
            continue
        phase = field(b, "Phase")
        if _is_orchestrator(p):
            continue  # file của phiên điều hành: không có Phase, đóng tay bằng `close --no-history` khi xong đợt
        if is_idle(phase):
            if task_name(p) != "(main)":
                stale.append(task_name(p))
            continue
        flow = field(b, "Flow") or "N/A"
        ph = _display_phase(phase)
        old_names += ph != phase
        sc = ""
        if not _scope_globs(b):
            # Sau khi duyệt thiết kế mà vẫn thiếu Scope: hook guard không bảo vệ được file của task
            sc = " | THIẾU SCOPE → scope-suggest --apply" if canon(phase) not in PRE_DEV else " | thiếu Scope"
        oq = _open_count(text_all)
        sc += f" | {oq} câu hỏi mở" if oq else ""
        active.append(f"{task_name(p)} | {ph[:60]} | {flow[:20]}{sc}")
    if not active:
        print("[HANDOFF] Không có task đang chạy")
    else:
        print(f"[HANDOFF] {len(active)} task đang chạy (file: docs/HANDOFF.<task>.local.md):")
        for line in active[:MAX_LINES]:
            print(f"  - {line}")
        if len(active) > MAX_LINES:
            print(f"  ... và {len(active) - MAX_LINES} task nữa")
    if old_names:
        print(f"  ({old_names} task ghi tên phase cũ — `python .claude/scripts/handoff.py normalize` để sửa file)")
    if stale:
        names = ", ".join(stale[:5]) + (f" +{len(stale) - 5}" if len(stale) > 5 else "")
        print(f"  ({len(stale)} file idle chưa đóng: {names} — chủ task chạy `handoff.py close <task>`)")


def _display_phase(phase):
    """Hiện tên phase chuẩn thay cho tên cũ, giữ phần ghi chú phía sau."""
    m = re.match(r"\s*([a-z][a-z-]*)(.*)$", phase or "", re.S | re.I)
    if not m:
        return phase
    tok = m.group(1).lower()
    return ALIAS[tok] + m.group(2) if tok in ALIAS else phase


# Phase theo vai (WORKFLOW §4; ALIAS). Tên cũ theo flow vẫn được chấp nhận qua ALIAS.
ALIAS = {
    "ba-start": "ba", "modify-ba": "ba", "bug-start": "ba",
    "design-start": "design", "bug-design": "design", "design-revision-required": "design", "upgrade-start": "design",
    "tech-lead-review": "tl-review", "modify-tech-lead": "tl-review", "bug-tech-lead": "tl-review",
    "upgrade-tech-lead": "tl-review", "refactor-start": "tl-review",
    "contract-detail-ready": "contract",
    "dev-ready": "dev", "code-revision-required": "dev", "hotfix-start": "dev", "perf-start": "dev",
    "code-review-ready": "code-review", "hotfix-built": "code-review",
    "qa-ready": "qa-plan", "qa-automation": "qa-run", "knowledge-distill": "distill",
}
PRE_DEV = ("ba", "design", "tl-review", "contract")  # chưa duyệt thiết kế -> chưa được sửa code sản phẩm
DEV_PHASES = ("dev", "qa-run")
FLOW_START = {"A": "dev", "B": "dev", "C-NHỎ": "dev", "C-NHO": "dev", "C": "ba", "D": "tl-review", "E": "dev", "F": "design", "G": "ba", "FULL": "ba"}


def canon(phase_text):
    m = re.match(r"[a-z][a-z-]*", (phase_text or "").strip().lower())
    tok = m.group(0) if m else ""
    return ALIAS.get(tok, tok)



def _scope_globs(block):
    raw = field(block, "Scope")
    return [g.strip().replace("\\", "/") for g in raw.split(",") if g.strip() and not g.strip().startswith("(")]


def _matches(rel, globs):
    import fnmatch
    for g in globs:
        if g.endswith("/") and rel.startswith(g):
            return True
        if fnmatch.fnmatch(rel, g) or rel == g:
            return True
    return False


def cmd_guard(_):
    """PreToolUse Edit|Write. Task khai báo `Scope` (glob) trong HANDOFF thì:
    - file thuộc scope CHỈ của task chưa qua duyệt thiết kế  -> CHẶN (exit 2)
    - file thuộc scope của cả task chưa duyệt lẫn task đã dev-ready -> nhắc (mơ hồ, không chặn)
    - task không khai báo Scope -> chỉ nhắc chung.
    Lỗi bất kỳ -> cho qua."""
    import json
    try:
        d = json.load(sys.stdin)
        path = (d.get("tool_input") or {}).get("file_path", "").replace("\\", "/")
    except Exception:
        return
    path = re.sub(r"^/([a-zA-Z])/", lambda m: m.group(1).upper() + ":/", path)  # Git Bash /d/x -> D:/x
    root = ROOT.replace("\\", "/").rstrip("/") + "/"
    rel = path[len(root):] if path.lower().startswith(root.lower()) else path
    rel = re.sub(r"^\.claude/worktrees/[^/]+/", "", rel)  # sửa trong worktree: so Scope theo đường dẫn trong repo
    if "docs/_ref/" in rel or rel.startswith("docs/_ref/"):
        sys.stderr.write("[BLOCKED] docs/_ref/ là auto-generated, chỉ đọc (HARD-STOPS). Ghi output vào docs/ khác.\n")
        sys.exit(2)
    role = _role_of(d.get("session_id")) if d.get("session_id") else None
    is_product = any(seg in rel for seg in CODE_PATHS) and not (TEST_PATH_REGEX and re.search(TEST_PATH_REGEX, rel))
    if role and role[1] == "qa" and is_product:
        sys.stderr.write(f"[HANDOFF] CHẶN: session QA của `{role[0]}` không sửa code sản phẩm ({rel}). Lỗi code -> báo "
                         "phiên task (REVIEW-REQUEST / SPEC-DEVIATION) để session dev sửa. QA chỉ ghi tests/ và docs/tests/.\n")
        sys.exit(2)
    # Tool call của subagent mang agent_id và dùng chung session_id với phiên cha: dev/QA subagent của phiên task (3 tầng)
    # không phải "phiên task tự sửa code".
    if role and role[1] == "lead" and is_product and not d.get("agent_id"):
        print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "additionalContext":
              f"[HANDOFF] Phiên task `{role[0]}` đang tự sửa code sản phẩm ({rel}). Mô hình Phiên task giao code cho "
              "session dev (Sonnet); chỉ tự sửa khi là chỉnh rất nhỏ và ghi rõ trong báo cáo."}}, ensure_ascii=False))
        return
    if not any(seg in rel for seg in CODE_PATHS):
        return
    pre_scoped, dev_scoped, pre_unscoped = [], [], []
    for p in task_files():
        try:
            b = current_block(read(p))
        except OSError:
            continue
        ph = field(b, "Phase").lower()
        if is_idle(ph):
            continue
        name, globs = task_name(p), _scope_globs(b)
        pre = canon(ph) in PRE_DEV
        if globs and _matches(rel, globs):
            (pre_scoped if pre else dev_scoped).append(name)
        elif pre and not globs:
            pre_unscoped.append(f"{name} ({ph.split()[0]})")
    if pre_scoped and not dev_scoped:
        sys.stderr.write(
            f"[HANDOFF] CHẶN: {rel} thuộc Scope của task chưa được duyệt thiết kế: {', '.join(pre_scoped)}.\n"
            "Không sửa code ở phase này (HARD-STOPS). Nếu thiết kế đã duyệt: đổi Phase trong "
            "`python .claude/scripts/handoff.py set-phase <task> dev`. Nếu Scope khai sai: sửa dòng Scope trong file đó.\n")
        sys.exit(2)
    notes = []
    if pre_scoped and dev_scoped:
        notes.append(f"file này nằm trong Scope của cả task chưa duyệt ({', '.join(pre_scoped)}) lẫn task đang dev ({', '.join(dev_scoped)})")
    if pre_unscoped:
        notes.append("task chưa duyệt thiết kế và chưa khai báo Scope: " + ", ".join(pre_unscoped[:5]))
    if notes:
        msg = "[HANDOFF] " + "; ".join(notes) + ". Nếu bạn đang sửa code cho task chưa duyệt thì DỪNG."
        print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "additionalContext": msg}}))


MARKS = os.path.join(ROOT, ".claude", ".marks")


def _hook_input():
    import json
    try:
        return json.load(sys.stdin)
    except Exception:
        return {}


def _mark_path(sid, kind):
    return os.path.join(MARKS, f"{sid}.{kind}")


def _mark_time(sid, kind):
    try:
        with open(_mark_path(sid, kind)) as f:
            return float(f.read().strip())
    except (OSError, ValueError):
        return 0.0


def _toplevel(s, ps=False):
    """Đánh dấu ký tự nằm NGOÀI nháy '…' "…", $(…), backtick `…` (bash) và ký tự escape (bash `\\`, PowerShell
    backtick). Trả (mask, cân bằng?). Ngoặc `(` đứng ngoài (subshell/nhóm) không tạo độ sâu — là ký tự ngoài."""
    esc = "`" if ps else "\\"
    stack, mask, i, n = [], [False] * len(s), 0, len(s)
    while i < n:
        c, top = s[i], (stack[-1] if stack else None)
        if top == "'":  # trong nháy đơn: không escape, không thế lệnh
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


def _segments(cmd, ps=False):
    """Tách câu lệnh tại && || ; | xuống dòng và ( ) đứng ngoài nháy/$(…)/backtick. Không cân nháy/ngoặc -> tách thô."""
    mask, ok = _toplevel(cmd, ps)
    if not ok:
        return [s.strip() for s in re.split(r"&&|\|\||[;|\n()]", cmd) if s.strip()]
    segs, start, i = [], 0, 0
    while i < len(cmd):
        step = 2 if mask[i] and cmd[i:i + 2] in ("&&", "||") else (1 if mask[i] and cmd[i] in ";|\n()" else 0)
        if step:
            segs.append(cmd[start:i])
            start = i = i + step
        else:
            i += 1
    segs.append(cmd[start:])
    return [s.strip() for s in segs if s.strip()]


def _command_part(seg, ps=False):
    """Bỏ phần đầu không phải lệnh: tiền tố gán `VAR=…` (giá trị có nháy chứa khoảng trắng, $(…), backtick),
    `env`, toán tử gọi PowerShell `&`, `{`/`!` — trả phần còn lại nguyên văn."""
    s = seg.strip()
    while s:
        m = re.match(r"(?:[&{!]\s*|env\s+)", s)
        if m:
            s = s[m.end():]
            continue
        m = re.match(r"[A-Za-z_]\w*=", s)
        if not m:
            break
        mask, ok = _toplevel(s, ps)
        j = (next((k for k in range(m.end(), len(s)) if mask[k] and s[k].isspace()), len(s)) if ok
             else re.match(r"\S*", s).end())
        s = s[j:].lstrip()
    return s


def _runs_tests(cmd, ps=False):
    """Lệnh có thật sự chạy test không (mặc định `python -m pytest`, hoặc `pytest` đứng đầu một đoạn lệnh) — không
    tính `pip show pytest`, `grep pytest`… Regex chỉnh ở CONFIG (HANDOFF_TEST_CMD_REGEX / HANDOFF_TEST_RUNNER_REGEX).
    Runner phải đứng đầu đoạn lệnh sau khi bỏ `VAR=…` (kể cả `DB_URL=$(python … url t) ./mvnw test`, `FOO="a b"`),
    `env`, `&` (PowerShell). `ps=True`: cú pháp PowerShell (backtick là escape, không phải thế lệnh)."""
    if TEST_CMD_REGEX and re.search(TEST_CMD_REGEX, cmd):
        return True
    if not TEST_RUNNER_REGEX:
        return False
    for seg in _segments(cmd, ps):
        rest = _command_part(seg, ps)
        if rest and re.match(TEST_RUNNER_REGEX, rest):
            return True
    return False


_runs_pytest = _runs_tests  # tên cũ


def cmd_mark(_):
    """PostToolUse Edit|Write|Bash: ghi mốc theo session — lần sửa backend gần nhất, lần chạy pytest gần nhất."""
    import time
    d = _hook_input()
    sid = d.get("session_id")
    if not sid:
        return
    ti = d.get("tool_input") or {}
    kind = None
    if d.get("tool_name") in ("Edit", "Write"):
        path = ti.get("file_path", "").replace(chr(92), "/")
        if any(seg in path for seg in BACKEND_SEGS):
            kind = "edit"
    elif d.get("tool_name") in ("Bash", "PowerShell") and not ti.get("run_in_background") \
            and _runs_tests(ti.get("command") or "", d.get("tool_name") == "PowerShell"):
        kind = "test"
    if kind:
        os.makedirs(MARKS, exist_ok=True)
        cutoff = time.time() - 7 * 86400  # dọn mốc của session cũ hơn 7 ngày
        for fn in os.listdir(MARKS):
            try:
                if os.path.getmtime(os.path.join(MARKS, fn)) < cutoff:
                    os.remove(os.path.join(MARKS, fn))
            except OSError:
                pass
        with open(_mark_path(sid, kind), "w") as f:
            f.write(str(time.time()))


def _norm(path):
    return os.path.normcase(os.path.abspath(path or "")).replace(chr(92), "/").rstrip("/")


WAITING = ("SPEC-DEVIATION", "QUESTION", "BLOCKED")


def _test_note(sid):
    """Session đã sửa backend mà chưa chạy pytest sau lần sửa cuối, khi có task đang ở phase dev."""
    edit = _mark_time(sid, "edit")
    if edit <= _mark_time(sid, "test") or edit <= _mark_time(sid, "blocked"):
        return None
    dev = []
    role = _role_of(sid)
    if role:
        # session có vai: chỉ xét task của chính nó (tránh nhắc nhầm task của session khác)
        path = os.path.join(DOCS, f"HANDOFF.{role[0]}.local.md")
        if os.path.exists(path) and canon(field(current_block(read(path)), "Phase")) in DEV_PHASES:
            dev.append(role[0])
        if not dev:
            return None
    # session không có vai (Solo, hotfix…): nhắc theo chính việc nó sửa, không gán vào task của session khác
    os.makedirs(MARKS, exist_ok=True)
    with open(_mark_path(sid, "blocked"), "w") as f:
        f.write(str(edit))
    where = f" (task: {', '.join(dev[:4])})" if dev else ""
    hint = f" (`{TEST_SELECT_HINT.format(base=base_branch())}`)" if TEST_SELECT_HINT else ""
    return (f"[HANDOFF] Session này đã sửa code ({', '.join(BACKEND_SEGS)}) nhưng chưa chạy {TEST_LABEL} sau lần sửa "
            f"cuối{where}. Chạy test liên quan{hint} rồi báo kết quả; không chạy được thì nói rõ lý do rồi kết thúc.")


STOP_OK = {
    "lead": ("DONE", "WAITING-CHILD", "SPEC-DEVIATION", "QUESTION", "BLOCKED"),
    "dev": ("DONE", "SPEC-DEVIATION", "QUESTION", "BLOCKED"),
    "qa": ("DONE", "REVIEW-REQUEST", "SPEC-DEVIATION", "QUESTION", "BLOCKED"),
}


def _child_target(d):
    """(task, role) của session đang dừng: theo mốc role (brief có [[handoff-role …]]), không thì Workdir khớp cwd."""
    sid = d.get("session_id")
    r = _role_of(sid) if sid else None
    if r:
        return r
    cwd = _norm(d.get("cwd") or os.getcwd())
    hits = []
    for p in task_files():
        try:
            b = current_block(read(p))
        except OSError:
            continue
        wd, orch = field(b, "Workdir"), field(b, "Orchestrator")
        if wd and wd != "-" and orch and orch != "-" and _norm(wd) == cwd:
            hits.append(task_name(p))
    return (hits[0], "lead") if len(hits) == 1 else None


def _child_note(d):
    """Session con định dừng mà không ở trạng thái dừng hợp lệ của vai mình -> nhắc làm tiếp."""
    target = _child_target(d)
    if not target:
        return None
    task, role = target
    path = os.path.join(DOCS, f"HANDOFF.{task}.local.md")
    if not os.path.exists(path):
        return None
    text = read(path)
    ph = canon(field(current_block(text), "Phase"))
    if ph == "idle" or _open_count(text):
        return None
    first = ((d.get("last_assistant_message") or "").strip().splitlines() or [""])[0]
    status = first.split("|")[0].strip().upper()
    ok = STOP_OK.get(role, STOP_OK["lead"])
    if status.startswith(ok):
        if role == "dev" and status.startswith("DONE") and ph == "dev":
            return f"[HANDOFF] `{task}`: báo DONE nhưng Phase vẫn `dev` — chạy `handoff.py set-phase {task} code-review` rồi báo lại."
        if role == "lead" and status.startswith("DONE"):
            return f"[HANDOFF] `{task}`: báo DONE nhưng Phase là `{ph}`, chưa `idle` — làm nốt pipeline (tới distill) rồi `set-phase idle`."
        return None
    wanted = {"lead": "DONE (phase idle) / WAITING-CHILD (đang chờ session con của mình) / SPEC-DEVIATION / QUESTION / BLOCKED",
              "dev": "DONE (sau `set-phase code-review`) / SPEC-DEVIATION / QUESTION / BLOCKED",
              "qa": "REVIEW-REQUEST (gửi kịch bản hoặc báo cáo cho phiên task duyệt) / DONE / SPEC-DEVIATION / QUESTION / BLOCKED"}
    return (f"[HANDOFF] Task `{task}` (vai `{role}`, phase `{ph}`) chưa tới điểm dừng hợp lệ. Đừng kết thúc lượt ở đây: "
            "làm bước kế tiếp; việc nền của chính mình (test, subagent) thì chờ trong lượt, không kết thúc với 'đang chạy nền'. "
            f"Chỉ dừng khi dòng đầu là: {wanted.get(role, wanted['lead'])}.")


def cmd_stopcheck(_):
    """Stop hook: nhắc (additionalContext) khi (1) sửa backend chưa chạy pytest, (2) session con dừng giữa pipeline.
    stop_hook_active + mốc blocked chống vòng lặp; docs Claude Code giới hạn thêm 8 lần nối tiếp."""
    import json
    d = _hook_input()
    sid = d.get("session_id")
    if d.get("stop_hook_active") or not sid:
        return
    notes = [n for n in (_test_note(sid), _child_note(d)) if n]
    if notes:
        print(json.dumps({"hookSpecificOutput": {"hookEventName": "Stop", "additionalContext": " ".join(notes)}},
                         ensure_ascii=False))


def cmd_claim(a):
    """Session con chạy lệnh này đầu tiên: ghi thư mục làm việc (cwd) vào HANDOFF để Stop hook nhận ra nó."""
    path = _task_path(a.task)
    text = read(path)
    wd = _norm(os.getcwd())
    if re.search(r"^-?\s*\*\*Workdir:?\*\*", text, re.M):
        text = re.sub(r"^(-?\s*\*\*Workdir:?\*\*:?\s*).*$", lambda m: m.group(1) + wd, text, count=1, flags=re.M)
    else:
        text = re.sub(r"^(-?\s*\*\*Phase:?\*\*.*)$", lambda m: m.group(1) + "\n- **Workdir:** " + wd, text, count=1,
                      flags=re.M)
    _save(path, text)
    print(f"{a.task}: Workdir = {wd}")


QSEC = "## Câu hỏi mở / Lệch spec"


def _task_path(task):
    path = os.path.join(DOCS, f"HANDOFF.{task}.local.md")
    if not os.path.exists(path):
        sys.exit(f"Không thấy {path}")
    return path


def _open_count(text):
    return len(re.findall(r"^### Q\d+ .*— OPEN\s*$", text, re.M))


def _save(path, text):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


# ---- Mốc giờ: đo thời gian thật từng phase và thời gian chờ user (cho `timing`) ----
TIMES = "## Mốc giờ"
TIMES_RE = r"^## Mốc giờ[^\n]*\n(.*?)(?=^## |\Z)"
TIMING_LOG = os.path.join(DOCS, "HANDOFF.timing.jsonl")  # git-tracked, chỉ append khi close


def _now():
    return datetime.now().astimezone()


def _stamp(text, event):
    """Thêm `- <giờ> | <kind>:<arg>` cuối mục Mốc giờ (tạo mục nếu file cũ chưa có)."""
    line = f"- {_now().isoformat(timespec='seconds')} | {event}\n"
    m = re.search(TIMES_RE, text, re.S | re.M)
    if m:
        return text[:m.end(1)].rstrip("\n") + "\n" + line + "\n" + text[m.end(1):].lstrip("\n")
    head = f"{TIMES} (handoff.py tự ghi — đừng sửa tay)\n\n{line}\n"
    nxt = re.search(r"^## (Artifacts|Báo cáo|Câu hỏi)", text, re.M)
    if nxt:
        return text[:nxt.start()] + head + text[nxt.start():]
    return text.rstrip() + "\n\n" + head


def _events(text):
    m = re.search(TIMES_RE, text, re.S | re.M)
    out = []
    for ln in (m.group(1).splitlines() if m else []):
        mm = re.match(r"-\s*(\S+)\s*\|\s*(start|phase|raise|resolve):(\S+)", ln)
        if mm:
            try:
                out.append((datetime.fromisoformat(mm.group(1)), mm.group(2), mm.group(3)))
            except ValueError:
                pass
    return out


def _timing(text, end=None):
    """{phase: {wall, wait}} (giây). wait = thời gian câu hỏi mở (raise→resolve) rơi vào phase đó."""
    ev = _events(text)
    end = end or _now()
    phases = []
    for t, k, v in ev:
        if k in ("start", "phase") and (not phases or phases[-1][1] != v):
            phases.append((t, v))
    rows = {}
    for i, (t, name) in enumerate(phases):
        if name == "idle":
            continue
        t2 = phases[i + 1][0] if i + 1 < len(phases) else end
        rows.setdefault(name, {"wall": 0.0, "wait": 0.0})["wall"] += (t2 - t).total_seconds()

    def add_wait(t0, t1):
        cur = [p for p in phases if p[0] <= t0]
        if cur and cur[-1][1] in rows:
            rows[cur[-1][1]]["wait"] += (t1 - t0).total_seconds()

    open_q = {}
    for t, k, v in ev:
        if k == "raise":
            open_q[v] = t
        elif k == "resolve" and v in open_q:
            add_wait(open_q.pop(v), t)
    for t0 in open_q.values():
        add_wait(t0, end)
    return rows


def _fmt(sec):
    sec = int(sec)
    return f"{sec // 3600}g{sec % 3600 // 60:02d}" if sec >= 3600 else f"{sec // 60}p"


def _timing_table(rows):
    lines = ["| Phase | Tổng | Chờ user | Làm việc |", "|---|---|---|---|"]
    for name, r in rows.items():
        lines.append(f"| {name} | {_fmt(r['wall'])} | {_fmt(r['wait'])} | {_fmt(max(r['wall'] - r['wait'], 0))} |")
    tot = sum(r["wall"] for r in rows.values())
    wait = sum(r["wait"] for r in rows.values())
    lines.append(f"| **Cộng** | {_fmt(tot)} | {_fmt(wait)} | {_fmt(max(tot - wait, 0))} |")
    return "\n".join(lines)


def cmd_timing(a):
    """timing [task]: có task đang chạy → thời gian từng phase tới giờ; không có → tổng hợp từ HANDOFF.timing.jsonl."""
    import json
    import statistics as st
    if a.task:
        rows = _timing(read(_task_path(a.task)))
        print(_timing_table(rows) if rows else f"{a.task}: chưa có mốc giờ (task mở trước khi có tính năng này).")
        return
    if not os.path.exists(TIMING_LOG):
        sys.exit("Chưa có dữ liệu: HANDOFF.timing.jsonl chỉ được ghi khi `close` task có mốc giờ.")
    recs = [json.loads(x) for x in read(TIMING_LOG).splitlines() if x.strip()]
    if a.flow:
        recs = [r for r in recs if r["flow"].upper().startswith(a.flow.upper())]
    by = collections.defaultdict(list)
    for r in recs:
        for ph, v in r["phases"].items():
            by[ph].append(v)
    print(f"{len(recs)} task" + (f" (Flow {a.flow})" if a.flow else "") + " — trung vị / lớn nhất, 'chờ' = thời gian câu hỏi mở")
    print("| Phase | Số task | Tổng (trung vị) | Chờ user (trung vị) | Làm việc (trung vị) | Tổng lớn nhất |")
    print("|---|---|---|---|---|---|")
    for ph in CANON:
        v = by.get(ph)
        if not v:
            continue
        w = [x["wall"] for x in v]
        q = [x["wait"] for x in v]
        wk = [max(x["wall"] - x["wait"], 0) for x in v]
        print(f"| {ph} | {len(v)} | {_fmt(st.median(w))} | {_fmt(st.median(q))} | {_fmt(st.median(wk))} | {_fmt(max(w))} |")
    tot = [sum(p["wall"] for p in r["phases"].values()) for r in recs]
    if tot:
        print(f"\nTổng mỗi task: trung vị {_fmt(st.median(tot))}, lớn nhất {_fmt(max(tot))}.")


def cmd_raise(a):
    """Phiên triển khai ghi một câu hỏi/lệch spec vào HANDOFF (trạng thái OPEN) rồi báo điều phối."""
    path = _task_path(a.task)
    text = read(path)
    if QSEC not in text:
        text = text.rstrip() + "\n\n" + QSEC + " (ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)\n"
    n = max([int(x) for x in re.findall(r"^### Q(\d+) ", text, re.M)] or [0]) + 1
    block = f"\n### Q{n} [{a.kind}] {date.today().isoformat()} — OPEN\n\n{a.text.strip()}\n"
    _save(path, _stamp(text.rstrip() + "\n" + block, f"raise:Q{n}"))
    print(f"Đã ghi Q{n} vào {os.path.relpath(path, ROOT)}")
    first = a.text.strip().split("|")[0].strip()[:120]
    print(f"Dòng đầu báo cáo / SendMessage: {a.kind.upper()} Q{n} | {a.task} | {first}")
    print(f"(chi tiết trong HANDOFF mục Q{n}; chờ RE Q{n} từ điều phối)")


def cmd_resolve(a):
    """Điều phối (hoặc user) ghi câu trả lời cho Q<n>, chuyển OPEN -> RESOLVED."""
    path = _task_path(a.task)
    text = read(path)
    qid = a.qid.upper().lstrip("Q")
    m = re.search(rf"^### Q{qid} (.*?)— OPEN\s*$", text, re.M)
    if not m:
        sys.exit(f"Không thấy Q{qid} đang OPEN trong {path}")
    nxt = re.search(r"^(### Q\d+ |## )", text[m.end():], re.M)
    end = m.end() + nxt.start() if nxt else len(text)
    head = f"### Q{qid} {m.group(1)}— RESOLVED ({date.today().isoformat()}, {a.by})"
    body = text[m.end():end].rstrip() + f"\n\n**Trả lời ({a.by}):** {a.text.strip()}\n"
    _save(path, _stamp(text[:m.start()] + head + body + ("\n" if nxt else "") + text[end:], f"resolve:Q{qid}"))
    print(f"Q{qid} -> RESOLVED. Gửi con (SendMessage): RE Q{qid}: <quyết định> | căn cứ: <nguồn> | việc tiếp theo")



CANON = ("ba", "design", "tl-review", "contract", "dev", "code-review", "qa-plan", "qa-run", "distill", "idle")


def cmd_set_phase(a):
    """Đổi Phase ở CẢ hai chỗ (block `## Current Feature` cho hook và khối yaml cho skill) — tránh lệch hai format."""
    path = _task_path(a.task)
    phase = canon(a.phase)
    if phase not in CANON:
        sys.exit(f"Phase không hợp lệ: {a.phase}. Dùng một trong: {', '.join(CANON)} (WORKFLOW §4; ALIAS)")
    label = phase + (f" ({a.note})" if a.note else "")
    text = read(path)
    text, n1 = re.subn(r"^(-?\s*\*\*Phase:?\*\*:?\s*).*$", lambda m: m.group(1) + label, text, count=1, flags=re.M)
    text, n2 = re.subn(r"^(phase:\s*).*$", lambda m: m.group(1) + phase, text, count=1, flags=re.M)
    if not n1:
        sys.exit(f"Không thấy dòng '- **Phase:**' trong block Current Feature của {path}")
    _save(path, _stamp(text, f"phase:{phase}"))
    print(f"{a.task}: Phase -> {label}" + ("" if n2 else " (không có khối yaml `phase:` — chỉ sửa block)"))


def cmd_normalize(a):
    """Đổi tên phase cũ (dev-ready, code-review-ready, knowledge-distill…) sang tên theo vai ở mọi file HANDOFF đang chạy."""
    n = 0
    for p in task_files():
        if a.task and task_name(p) != a.task:
            continue
        text = read(p)
        phase = field(current_block(text), "Phase")
        new = _display_phase(phase)
        if new == phase:
            continue
        text = re.sub(r"^(-?\s*\*\*Phase:?\*\*:?\s*).*$", lambda m: m.group(1) + new, text, count=1, flags=re.M)
        text = re.sub(r"^(phase:\s*).*$", lambda m: m.group(1) + canon(new), text, count=1, flags=re.M)
        _save(p, text)
        n += 1
        print(f"{task_name(p)}: {phase[:40]} -> {new[:40]}")
    print(f"-- đã chuẩn hoá {n} file")


def _changed_files(task, text):
    """File task đã sửa: nhánh có tên chứa task (worktree) so với base_branch(), hoặc các commit hash ghi trong HANDOFF."""
    import subprocess

    def git(*args):
        return subprocess.run(["git", *args], cwd=MAIN, capture_output=True, text=True).stdout

    files = set()
    for br in git("branch", "--format=%(refname:short)").split():
        if task in br:
            files |= set(git("diff", "--name-only", f"{base_branch()}...{br}").split())
    for h in set(re.findall(r"\b[0-9a-f]{9,12}\b", text)):
        if git("cat-file", "-t", h).strip() == "commit":
            files |= set(git("show", "--name-only", "--format=", h).split())
    return sorted(f for f in files if f.startswith(CODE_PATHS))


def _to_globs(files):
    dirs = {}
    for f in files:
        d = f.rsplit("/", 1)[0] + "/"
        dirs.setdefault(d, []).append(f)
    out = []
    for d, fs in sorted(dirs.items()):
        out.extend([d] if len(fs) >= 3 else fs)
    return out


def cmd_scope_suggest(a):
    path = _task_path(a.task)
    text = read(path)
    globs = _to_globs(_changed_files(a.task, text))
    if not globs:
        print(f"{a.task}: không suy được Scope (không có nhánh/commit) — khai tay.")
        return
    line = ", ".join(globs)
    print(f"{a.task}: Scope gợi ý = {line}")
    if a.apply:
        if _scope_globs(current_block(text)):
            print("  Đã có Scope — không ghi đè.")
            return
        if re.search(r"^-?\s*\*\*Scope:?\*\*", text, re.M):
            text = re.sub(r"^(-?\s*\*\*Scope:?\*\*:?\s*).*$", lambda m: m.group(1) + line, text, count=1, flags=re.M)
        else:
            text = re.sub(r"^(-?\s*\*\*Phase:?\*\*.*)$", lambda m: m.group(1) + "\n- **Scope:** " + line, text, count=1, flags=re.M)
        _save(path, text)
        print("  Đã ghi Scope vào HANDOFF.")


def cmd_new(a):
    if not a.scope and not a.no_scope:
        sys.exit(f"Thiếu --scope (glob file task được sửa, vd {SCOPE_EXAMPLE}). "
                 "Hook guard dùng Scope để chặn sửa code khi task chưa duyệt thiết kế. Chưa biết scope? Dùng --no-scope và bổ sung sau.")
    path = os.path.join(DOCS, f"HANDOFF.{a.task}.local.md")
    if os.path.exists(path):
        sys.exit(f"Đã tồn tại: {path} — trùng tên task? (ls docs/HANDOFF.*.local.md)")
    phase = a.phase or FLOW_START.get(a.flow.split()[0].upper(), "ba")
    text = TEMPLATE.format(task=a.task, title=a.title or a.task, phase=phase,
                           flow=a.flow, model=a.model, session=a.session or "-", orch=a.orchestrator or "-",
                           scope=a.scope or "(chưa khai báo)", migration_dir=MIGRATION_DIR)
    _save(path, _stamp(text, f"start:{canon(phase)}"))
    print(path)


def cmd_close(a):
    path = os.path.join(DOCS, f"HANDOFF.{a.task}.local.md")
    if not os.path.exists(path):
        sys.exit(f"Không thấy {path}")
    text = read(path)
    block = current_block(text)
    flow = field(block, "Flow") or "?"
    title = field(block, "Task") or a.task
    hist = read(HISTORY) if os.path.exists(HISTORY) else ""
    if a.no_history:
        print("Bỏ qua history (--no-history).")
    elif f"(`{a.task}`)" in hist and not a.force_append:
        print(f"History đã có entry cho `{a.task}` — chỉ xoá file.")
    else:
        notes = re.sub(r"^## Current[^\n]*\n.*?(?=^## |\Z)", "", text, flags=re.S | re.M)
        notes = re.sub(TIMES_RE, "", notes, flags=re.S | re.M)
        notes = re.sub(r"^# HANDOFF[^\n]*\n", "", notes).strip()
        rows = _timing(text)
        entry = (f"\n---\n\n### {flow} — {title} (`{a.task}`)\n\n"
                 f"**Phase**: `idle` ✅ ĐÓNG ({date.today().isoformat()}).\n\n"
                 + (a.summary.strip() + "\n\n" if a.summary else "")
                 + (f"**Thời gian**:\n\n{_timing_table(rows)}\n\n" if rows else "")
                 + (notes + "\n" if notes else ""))
        with open(HISTORY, "a", encoding="utf-8", newline="\n") as f:
            f.write(entry)
        if rows:
            import json
            rec = {"task": a.task, "flow": flow, "closed": _now().isoformat(timespec="seconds"),
                   "phases": {k: {"wall": round(v["wall"]), "wait": round(v["wait"])} for k, v in rows.items()}}
            with open(TIMING_LOG, "a", encoding="utf-8", newline="\n") as f:
                f.write(json.dumps(rec, ensure_ascii=False) + "\n")
        print(f"Đã append entry `{a.task}` vào {os.path.relpath(HISTORY, ROOT)}")
    os.remove(path)
    print(f"Đã xoá {os.path.relpath(path, ROOT)}")
    # lưới an toàn: DB test tạm của task (bình thường đã dọn khi phiên task chấp nhận báo cáo QA — PROJECT/qa-process.md)
    # Không có script (stack không dùng DB tạm) -> bỏ qua êm.
    qa = QA_DB_SCRIPT
    if not a.keep_db and qa and os.path.isfile(qa):
        import subprocess
        try:
            r = subprocess.run([sys.executable, "-I", qa, "drop", a.task], capture_output=True, text=True,
                               encoding="utf-8", timeout=60)
            print((r.stdout or r.stderr).strip().splitlines()[-1] if (r.stdout or r.stderr).strip() else "")
        except Exception as e:
            print(f"(không dọn được DB test: {e})")


def cmd_history_dupes(a):
    """Entry history trùng (`merge=union` khi rebase/cherry-pick có thể chép một entry hai lần). --fix: giữ entry đầu."""
    text = read(HISTORY)
    parts = re.split(r"(?m)^(?=---\n\n### )", text)
    seen, by_task, keep, dup = set(), {}, [], []
    for part in parts:
        m = re.match(r"---\n\n### .*?\(`([^`]+)`\)", part)
        if m:
            body = re.sub(r"\s+", " ", part).strip()
            by_task.setdefault(m.group(1), []).append(body)
            if (m.group(1), body) in seen:
                dup.append(m.group(1))
                continue
            seen.add((m.group(1), body))
        keep.append(part)
    print("\n".join(f"trùng nguyên văn (--fix xoá được): {t}" for t in dup) or "Không có entry trùng nguyên văn.")
    # đếm theo dòng tiêu đề: bắt cả entry bị union-merge dán vào đuôi entry khác (không có '---' phía trước)
    heads = collections.defaultdict(list)
    for i, line in enumerate(text.splitlines(), 1):
        m = re.match(r"### .*\(`([^`]+)`\)\s*$", line)
        if m:
            heads[m.group(1)].append(i)
    for t, lines in heads.items():
        if len(lines) > 1 and t not in dup:
            print(f"trùng tiêu đề (gộp tay): {t} — dòng {', '.join(map(str, lines))}")
    if dup and a.fix:
        _save(HISTORY, "".join(keep))
        print(f"Đã bỏ {len(dup)} entry trùng (giữ bản đầu).")


def _is_orchestrator(path):
    return task_name(path).endswith("-orchestrator")


def cmd_stale(_):
    n = 0
    for p in task_files():
        if task_name(p) == "(main)" or _is_orchestrator(p):
            continue
        b = current_block(read(p))
        if is_idle(field(b, "Phase")):
            n += 1
            print(task_name(p))
    print(f"-- {n} file idle chưa dọn", file=sys.stderr)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("status").set_defaults(fn=cmd_status)
    sub.add_parser("stale").set_defaults(fn=cmd_stale)
    sub.add_parser("guard").set_defaults(fn=cmd_guard)
    sub.add_parser("mark").set_defaults(fn=cmd_mark)
    cl = sub.add_parser("claim")
    cl.add_argument("task")
    cl.set_defaults(fn=cmd_claim)
    sp = sub.add_parser("set-phase")
    sp.add_argument("task")
    sp.add_argument("phase")
    sp.add_argument("--note", help="chú thích sau tên phase, vd 'BE xong, còn FE'")
    sp.set_defaults(fn=cmd_set_phase)
    sg = sub.add_parser("scope-suggest")
    sg.add_argument("task")
    sg.add_argument("--apply", action="store_true", help="ghi vào HANDOFF nếu task chưa có Scope")
    sg.set_defaults(fn=cmd_scope_suggest)
    r = sub.add_parser("raise")
    r.add_argument("task")
    r.add_argument("--kind", choices=["spec-deviation", "blocked", "question"], default="spec-deviation")
    r.add_argument("--text", required=True)
    r.set_defaults(fn=cmd_raise)
    v = sub.add_parser("resolve")
    v.add_argument("task")
    v.add_argument("qid")
    v.add_argument("--text", required=True)
    v.add_argument("--by", default="điều phối")
    v.set_defaults(fn=cmd_resolve)
    sub.add_parser("stopcheck").set_defaults(fn=cmd_stopcheck)
    n = sub.add_parser("new")
    n.add_argument("task")
    n.add_argument("--title")
    n.add_argument("--flow", default="C")
    n.add_argument("--phase", help="mặc định theo flow (WORKFLOW §3.2)")
    n.add_argument("--model", default="sonnet")
    n.add_argument("--session")
    n.add_argument("--orchestrator", help="id session điều phối (để session con gửi câu hỏi về)")
    n.add_argument("--no-scope", action="store_true")
    n.add_argument("--scope", help=f"glob file task được sửa, cách nhau dấu phẩy (vd {SCOPE_EXAMPLE})")
    n.set_defaults(fn=cmd_new)
    c = sub.add_parser("close")
    c.add_argument("task")
    c.add_argument("--summary", help="tóm tắt kết quả (1–3 câu) ghi vào history")
    c.add_argument("--force-append", action="store_true")
    c.add_argument("--keep-db", action="store_true", help="không xoá DB test qa_<task>")
    c.add_argument("--no-history", action="store_true",
                   help="chỉ xoá file, không ghi history (file của phiên điều hành, task huỷ)")
    hd = sub.add_parser("history-dupes")
    hd.add_argument("--fix", action="store_true", help="xoá bản trùng (giữ bản đầu) — chỉ khi không phiên nào đang sửa history")
    hd.set_defaults(fn=cmd_history_dupes)
    tm = sub.add_parser("timing")
    tm.add_argument("task", nargs="?", help="task đang chạy; bỏ trống = tổng hợp các task đã đóng")
    tm.add_argument("--flow", help="lọc theo flow (C, G, Full…)")
    tm.set_defaults(fn=cmd_timing)
    nm = sub.add_parser("normalize")
    nm.add_argument("task", nargs="?", help="mặc định: mọi task")
    nm.set_defaults(fn=cmd_normalize)
    c.set_defaults(fn=cmd_close)
    a = ap.parse_args()
    a.fn(a)


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception as e:  # hook không được chặn turn vì lỗi script
        if len(sys.argv) > 1 and sys.argv[1] in ("guard", "mark", "stopcheck"):
            sys.exit(0)
        if len(sys.argv) > 1 and sys.argv[1] == "status":
            print(f"[HANDOFF] lỗi đọc handoff: {e}")
        else:
            raise

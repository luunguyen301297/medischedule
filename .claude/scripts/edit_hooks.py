#!/usr/bin/env python
"""edit_hooks.py — một tiến trình cho mọi hook quanh Edit/Write (đọc JSON hook từ stdin).

  pre   (PreToolUse Edit|Write)  : handoff.py guard (chặn docs/_ref, chặn sửa code thuộc Scope task chưa duyệt)
                                   + cảnh báo Edit file >= LARGE dòng
  post  (PostToolUse Edit|Write) : handoff.py mark (mốc sửa code cho Stop hook)
                                   + kiểm format file vừa sửa (mặc định `black --check` cho backend/app/*.py)
                                   + nhắc cập nhật tài liệu kiến trúc khi sửa thư mục API

Gom vào một script để mỗi lần Edit/Write chỉ tốn một lần khởi động Python cho mỗi pha.
Lỗi bất kỳ ngoài việc chặn chủ đích -> cho qua (exit 0). Công cụ format không cài -> bỏ qua êm.
"""
import contextlib
import fnmatch
import io
import json
import os
import re
import shlex
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import handoff  # noqa: E402

# ---------------------------------------------------------------------------------------------------------------
# CONFIG — chỉnh theo stack dự án (đè bằng biến môi trường; mặc định = backend Python + black)
# Ví dụ frontend TS + prettier: EDIT_FORMAT_ROOT=frontend EDIT_FORMAT_SUBDIR=src/ EDIT_FORMAT_EXTS=.ts,.tsx
#   EDIT_FORMAT_CHECK_CMD='npx --no-install prettier --check {file}' EDIT_FORMAT_FIX_CMD='npx prettier --write {file}'
#   EDIT_FORMAT_NAME=prettier EDIT_FORMAT_FAIL_CODES=1
# ---------------------------------------------------------------------------------------------------------------
_env = handoff._env
LARGE = int(_env("EDIT_LARGE_FILE_LINES", "1000"))
# thư mục chạy công cụ format (cwd) và thư mục con trong đó cần kiểm; chỉ file có đuôi trong EDIT_FORMAT_EXTS
FORMAT_ROOT = _env("EDIT_FORMAT_ROOT", "backend").strip("/")
FORMAT_SUBDIR = _env("EDIT_FORMAT_SUBDIR", "app/")
FORMAT_EXTS = handoff._csv("EDIT_FORMAT_EXTS", ".py")
# lệnh kiểm format ({python} = Python đang chạy hook, {file} = đường dẫn tương đối từ FORMAT_ROOT); '' = tắt
FORMAT_CHECK_CMD = _env("EDIT_FORMAT_CHECK_CMD", "{python} -m black --check -q {file}")
FORMAT_FIX_CMD = _env("EDIT_FORMAT_FIX_CMD", "python -m black {file}")
FORMAT_NAME = _env("EDIT_FORMAT_NAME", "black")
FORMAT_FAIL_CODES = {int(x) for x in handoff._csv("EDIT_FORMAT_FAIL_CODES", "1")}  # mã thoát = "chưa đúng format"
# stderr chứa một trong các chuỗi này = công cụ chưa cài -> bỏ qua êm
TOOL_MISSING = ("No module named", "not found", "not recognized", "command not found")
# sửa file API -> nhắc cập nhật tài liệu ('' = tắt). Danh sách cách nhau dấu phẩy; mỗi mục là
#   đoạn đường dẫn (khớp chuỗi con, vd `backend/app/api/v1/`), glob có * ? [ ] (khớp cả đường dẫn, vd
#   `*Controller.java`, `*/web/*`, `*/routes/*.ts`), hoặc regex có tiền tố `re:` (vd `re:/api/.*\.go$`)
API_SEG = handoff._csv("EDIT_API_SEG", "backend/app/api/v1/")
API_DOC = _env("EDIT_API_DOC", ".claude/PROJECT/architecture.md")
# ---------------------------------------------------------------------------------------------------------------


def _run(fn, raw):
    """Chạy một lệnh của handoff.py với stdin giả, gom additionalContext nó in ra."""
    sys.stdin = io.StringIO(raw)
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        fn(None)  # SystemExit(2) của guard được để lọt ra ngoài — đó là chặn chủ đích
    out = buf.getvalue().strip()
    if not out:
        return []
    try:
        return [json.loads(out)["hookSpecificOutput"]["additionalContext"]]
    except Exception:
        return [out]


def _emit(event, notes):
    if notes:
        print(json.dumps({"hookSpecificOutput": {"hookEventName": event, "additionalContext": "\n".join(notes)}},
                         ensure_ascii=False))


def pre(raw, data):
    notes = _run(handoff.cmd_guard, raw)
    path = (data.get("tool_input") or {}).get("file_path", "")
    if data.get("tool_name") == "Edit" and path and os.path.isfile(path):
        try:
            with open(path, "rb") as f:
                n = sum(1 for _ in f)
        except OSError:
            n = 0
        if n >= LARGE:
            notes.append(f"[WARNING] {path} có {n} dòng (>= {LARGE}). Kiểm old_string khớp chính xác để Edit không hỏng.")
    _emit("PreToolUse", notes)


def post(raw, data):
    notes = _run(handoff.cmd_mark, raw)
    path = (data.get("tool_input") or {}).get("file_path", "").replace(chr(92), "/")
    note = _format_note(path)
    if note:
        notes.append(note)
    shown = _api_match(path)
    if shown:
        notes.append(f"[REMINDER] Sửa endpoint {shown} — kiểm {API_DOC} có cần cập nhật không.")
    _emit("PostToolUse", notes)


def _api_match(path):
    """Đường dẫn hiển thị nếu path thuộc một mục EDIT_API_SEG, không thì None."""
    for seg in API_SEG:
        if seg.startswith("re:"):
            hit = re.search(seg[3:], path) is not None
        elif any(ch in seg for ch in "*?["):
            hit = fnmatch.fnmatchcase(path, seg) or fnmatch.fnmatchcase(path, "*/" + seg.lstrip("/"))
        else:
            hit = seg in path
        if not hit:
            continue
        top = seg.split("/")[0] + "/" if "/" in seg and not seg.startswith(("re:", "*", "/")) else ""
        i = path.find(top) if top else -1
        if i >= 0:
            return path[i:]
        root = (os.environ.get("CLAUDE_PROJECT_DIR") or "").replace(chr(92), "/").rstrip("/")
        return path[len(root) + 1:] if root and path.lower().startswith(root.lower() + "/") else path
    return None


def _format_note(path):
    """Chạy FORMAT_CHECK_CMD cho file vừa sửa; trả lời nhắc nếu chưa đúng format. Công cụ không có -> None."""
    if not FORMAT_CHECK_CMD or not path.endswith(FORMAT_EXTS):
        return None
    seg = f"{FORMAT_ROOT}/{FORMAT_SUBDIR}" if FORMAT_ROOT else FORMAT_SUBDIR
    i = path.find(seg)
    if i < 0:
        return None
    cwd = path[:i] + FORMAT_ROOT if FORMAT_ROOT else (path[:i].rstrip("/") or ".")
    rel = path[i + len(FORMAT_ROOT) + 1:] if FORMAT_ROOT else path[i:]
    try:
        argv = [t.replace("{python}", sys.executable).replace("{file}", rel) for t in shlex.split(FORMAT_CHECK_CMD)]
        r = subprocess.run(argv, cwd=cwd, capture_output=True, text=True, timeout=30)
    except Exception:  # FileNotFoundError (công cụ không cài), timeout…
        return None
    if r.returncode not in FORMAT_FAIL_CODES or any(s in (r.stderr or "") for s in TOOL_MISSING):
        return None
    fix = FORMAT_FIX_CMD.replace("{file}", rel)
    return f"[{FORMAT_NAME}] {rel} chưa đúng format — chạy `cd {FORMAT_ROOT or '.'} && {fix}`."


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    raw = sys.stdin.read()
    try:
        data = json.loads(raw)
    except Exception:
        return
    {"pre": pre, "post": post}.get(mode, lambda *_: None)(raw, data)


if __name__ == "__main__":
    for s in (sys.stdin, sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8")
        except Exception:
            pass
    try:
        main()
    except SystemExit:
        raise
    except Exception:
        sys.exit(0)

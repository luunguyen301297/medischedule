#!/usr/bin/env python
"""test_hooks.py — tự kiểm các hook/script trong .claude/scripts (chạy sau mỗi lần sửa chúng).

  python -I -B .claude/scripts/test_hooks.py
      Chế độ mặc định: bỏ mọi biến cấu hình (GUARD_/HANDOFF_/EDIT_/QA_DB_/…) khỏi môi trường để kiểm hành vi MẶC ĐỊNH
      của script + các biến cấu hình đặt riêng trong từng ca. Dựng project tạm (không đụng docs/ thật).

  python -I -B .claude/scripts/test_hooks.py --project [--settings <settings.json>] [--cases <hook_cases.json>] [--root <dir>]
  (hoặc TEST_HOOKS_PROJECT=1)
      Chế độ dự án: KHÔNG xoá biến cấu hình; nạp khối `env` của .claude/settings.json (mặc định của dự án chứa script
      này) đè lên môi trường, rồi chạy bảng ca do dự án khai ở .claude/scripts/hook_cases.json (mẫu:
      hook_cases.example.json). Dùng để chứng minh cấu hình env của dự án bắt đúng lệnh thật của stack.
      Không chạy lệnh nào trong bảng ca — chỉ đưa lệnh/đường dẫn cho hook đánh giá.

In PASS/FAIL từng ca, exit 1 nếu có FAIL.
"""
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time

HERE = os.path.dirname(os.path.abspath(__file__))
HANDOFF = os.path.join(HERE, "handoff.py")
EDIT = os.path.join(HERE, "edit_hooks.py")
results = []


# biến cấu hình stack của các hook — bỏ khỏi môi trường test để case mặc định luôn chạy với giá trị mặc định
CONFIG_PREFIXES = ("GUARD_", "HANDOFF_", "EDIT_", "SCRATCH_DB_PREFIXES", "QA_DB_", "POSTGRES_", "SELECT_TESTS_",
                   "DB_URL", "TEST_DB_URL", "SPRING_DATASOURCE_", "FLYWAY_")
PROJECT_ENV = None  # chế độ --project: dict env của settings.json (không xoá biến cấu hình)


def run(script, args, payload=None, root=None, cwd=None, env=None):
    if PROJECT_ENV is not None:
        base = dict(os.environ, **PROJECT_ENV)
    else:
        base = {k: v for k, v in os.environ.items() if not k.startswith(CONFIG_PREFIXES)}
    env = dict(base, CLAUDE_PROJECT_DIR=root or "", PYTHONIOENCODING="utf-8", **(env or {}))
    r = subprocess.run([sys.executable, "-I", "-B", script, *args], input=json.dumps(payload) if payload is not None else "",
                       capture_output=True, text=True, encoding="utf-8", env=env, cwd=cwd)
    return r.returncode, r.stdout, r.stderr


def check(name, cond):
    results.append((name, bool(cond)))


def skip(name, reason):
    """Ca phụ thuộc file dự án được phép xoá (adapter stack, codegraph_nudge.py…) — không có file thì SKIP, không FAIL."""
    results.append((f"{name} — {reason}", None))


def have(fn):
    return os.path.exists(os.path.join(HERE, fn))


def handoff_file(root, task, phase, scope=None):
    lines = ["# x", "", "## Current Feature", "", f"- **Phase:** {phase}", "- **Flow:** C"]
    if scope:
        lines.append(f"- **Scope:** {scope}")
    with open(os.path.join(root, "docs", f"HANDOFF.{task}.local.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n\n```yaml\nphase: " + phase + "\n```\n")


FLYWAY_RE = (r"\bflyway(?:\b[:\s][^|;&]*?\b(migrate|clean|baseline|repair|undo)\b"
             r"|(Migrate|Clean|Baseline|Repair|Undo)\b)")
SPRING_START_RE = (r"\bmvnw?(?:\.cmd)?\b[^|;&]*\bspring-boot:run\b|\bgradlew?(?:\.bat)?\b[^|;&]*\bbootRun\b"
                   r"|\bjava\b[^|;&]*\s-jar\s")
QA_DB = os.path.join(HERE, "qa_db.py")


def _py(code, env, cwd=None):
    """Chạy đoạn Python import script trong HERE (đọc biến module sau khi nạp env)."""
    e = {k: v for k, v in os.environ.items() if not k.startswith(CONFIG_PREFIXES)} if PROJECT_ENV is None \
        else dict(os.environ, **PROJECT_ENV)
    e.update(PYTHONIOENCODING="utf-8", **env)
    r = subprocess.run([sys.executable, "-I", "-B", "-c", f"import sys; sys.path.insert(0, {HERE!r}); "
                        "[s.reconfigure(encoding='utf-8') for s in (sys.stdout, sys.stderr)]\n" + code],
                       capture_output=True, text=True, encoding="utf-8", env=e, cwd=cwd)
    return r.returncode, r.stdout.strip(), r.stderr.strip()


def new_feature_cases(root, r, guard_env):
    """Ca cho: đối số URL / nhiều biến URL / startup-guard (context_guard), qa_db không SQLAlchemy + JDBC, EDIT_API_SEG
    danh sách/glob/regex, check-docs-drift grep -r, TEST_PATH_REGEX mặc định, base branch tự dò, gợi ý select_tests."""
    fw = {"GUARD_MIGRATION_CMD_REGEX": FLYWAY_RE, "GUARD_MIGRATION_DRYRUN_REGEX": "",
          "GUARD_MIGRATION_DB_URL_VAR": "DB_URL,SPRING_DATASOURCE_URL"}
    # 1a. URL trong đối số
    code, err = guard_env("./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://db.example:5432/app_dev", **fw)
    check("guard arg: -Dflyway.url trỏ DB dev -> chặn, nêu nguồn đối số", code == 2 and "app_dev" in err and "đối số" in err)
    check("guard arg: flyway -url=…/qa_x -> cho qua",
          guard_env("flyway -url=jdbc:postgresql://h:5432/qa_x migrate", **fw)[0] == 0)
    check("guard arg: --url <dev> (cách bằng dấu cách) -> chặn",
          guard_env("flyway migrate --url jdbc:postgresql://h:5432/app_dev", **fw)[0] == 2)
    check("guard arg: -Dflyway.url=$(qa_db.py url) -> cho qua",
          guard_env("./mvnw flyway:migrate -Dflyway.url=$(python .claude/scripts/qa_db.py url t1)", **fw)[0] == 0)
    check("guard arg: đối số URL đè biến trong .env",
          guard_env("DB_URL=jdbc:postgresql://h/app_dev flyway -url=jdbc:postgresql://h/qa_y migrate", **fw)[0] == 0)
    check("guard flyway: `./gradlew flywayMigrate` -> chặn", guard_env("./gradlew flywayMigrate", **fw)[0] == 2)
    check("guard flyway: `./mvnw flyway:info` -> cho qua", guard_env("./mvnw flyway:info", **fw)[0] == 0)
    check("guard arg: `git remote set-url` không phải đối số URL",
          guard_env("git remote set-url origin x && echo ok", **fw)[0] == 0)
    # 1d. nhiều biến URL
    check("guard vars: SPRING_DATASOURCE_URL (biến thứ 2) trỏ qa_* -> cho qua",
          guard_env("SPRING_DATASOURCE_URL=jdbc:postgresql://h:5432/qa_s ./mvnw flyway:migrate", **fw)[0] == 0)
    with open(os.path.join(root, ".env"), "a", encoding="utf-8") as f:
        f.write("SPRING_DATASOURCE_URL=jdbc:postgresql://dev-db.example:5432/spring_dev\n")
    code, err = guard_env("./mvnw flyway:migrate", **fw)
    check("guard vars: .env có SPRING_DATASOURCE_URL -> chặn, nêu DB + tên biến", code == 2 and "spring_dev" in err
          and "SPRING_DATASOURCE_URL" in err)
    code, err = guard_env("./mvnw flyway:migrate", **dict(fw, DB_URL="jdbc:postgresql://h:5432/qa_env"))
    check("guard vars: biến môi trường DB_URL (ưu tiên trước) trỏ qa_* -> cho qua", code == 0)
    # 1b. startup-guard
    st = dict(fw, GUARD_STARTUP_CMD_REGEX=SPRING_START_RE)

    def start(cmd, **extra):
        c, out, err = run(os.path.join(HERE, "context_guard.py"), ["bash"], {"tool_input": {"command": cmd}, "cwd": root},
                          root=root, cwd=root, env=dict(st, **extra))
        return c, out
    code, out = start("cd backend && ./mvnw spring-boot:run")
    check("startup: spring-boot:run, .env trỏ DB máy khác -> CẢNH BÁO, không chặn",
          code == 0 and "CẢNH BÁO" in out and "spring_dev" in out and "additionalContext" in out)
    code, out = start("DB_URL=jdbc:postgresql://localhost:5432/app ./mvnw spring-boot:run")
    check("startup: DB localhost -> im", code == 0 and out.strip() == "")
    code, out = start("java -jar app.jar --spring.datasource.url=jdbc:postgresql://prod.example:5432/app")
    check("startup: java -jar --spring.datasource.url máy khác -> CẢNH BÁO", code == 0 and "prod.example" in out)
    code, out = start("./mvnw spring-boot:run", GUARD_STARTUP_CMD_REGEX="")
    check("startup: mặc định tắt (GUARD_STARTUP_CMD_REGEX='') -> im", code == 0 and out.strip() == "")
    code, out = start("./mvnw spring-boot:run", GUARD_STARTUP_ALLOWED_HOSTS="localhost,dev-db.example")
    check("startup: host trong GUARD_STARTUP_ALLOWED_HOSTS -> im", out.strip() == "")

    # 2. qa_db.py: không cần SQLAlchemy, JDBC, POSTGRES_*, host cho phép mọi port (adapter PostgreSQL — có thể bị xoá)
    if have("qa_db.py"):
        qa_db_cases(root)
    else:
        skip("qa_db: 8 ca adapter PostgreSQL", "không có qa_db.py")
    edit_seg_cases(root, r)


def qa_db_cases(root):
    code, _, err = run(QA_DB, ["url", "t1"], root=root, cwd=root,
                       env={"QA_DB_ADMIN_URL": "postgresql://u:p@prod.example:5432/postgres"})
    check("qa_db: host lạ -> từ chối, không kết nối", code != 0 and "Từ chối" in err and "prod.example" in err)
    code, _, err = run(QA_DB, ["url", "t1"], root=root, cwd=root,
                       env={"QA_DB_ADMIN_URL": "postgresql://u:p@prod.example:5432/postgres",
                            "QA_DB_ALLOWED_HOSTS": "dev.example"})
    check("qa_db: QA_DB_ALLOWED_HOSTS khác host -> vẫn từ chối host lạ", code != 0 and "Từ chối" in err)
    rc, out, err = _py("import qa_db as q; c = q._admin(); print(c.host, c.port, c.user, c.database, q.render_url(c, 'qa_t'))",
                       {"POSTGRES_USER": "app", "POSTGRES_PASSWORD": "s3 cret", "POSTGRES_PORT": "5435",
                        "POSTGRES_DB": "appdb"}, cwd=root)
    check("qa_db: ghép từ POSTGRES_* + localhost mọi port + URL mặc định asyncpg",
          rc == 0 and out == "localhost 5435 app appdb postgresql+asyncpg://app:s3%20cret@localhost:5435/qa_t")
    rc, out, err = _py("import qa_db as q; c = q._admin(); print(q.render_url(c, 'qa_t'))",
                       {"QA_DB_ADMIN_URL": "jdbc:postgresql://127.0.0.1:5442/postgres?user=a&password=b",
                        "QA_DB_URL_DRIVER": "jdbc:postgresql"}, cwd=root)
    check("qa_db: JDBC mặc định chỉ in URL, user ghi chú stderr",
          rc == 0 and out == "jdbc:postgresql://127.0.0.1:5442/qa_t" and "user=a" in err and "b" not in err.split("user=a")[1][:3])
    rc, out, _ = _py("import qa_db as q; c = q._admin(); print(q.render_url(c, 'qa_t'))",
                     {"QA_DB_ADMIN_URL": "jdbc:postgresql://localhost/postgres?user=a&password=b",
                      "QA_DB_URL_DRIVER": "jdbc:postgresql", "QA_DB_JDBC_CREDENTIALS": "query"}, cwd=root)
    check("qa_db: JDBC query -> ?user=&password=", rc == 0 and out == "jdbc:postgresql://localhost:5432/qa_t?user=a&password=b")
    rc, out, _ = _py("import qa_db as q; print(q._admin().source)",
                     {"MY_ADMIN": "postgresql://u@localhost/x", "QA_DB_ADMIN_URL_VAR": "MY_ADMIN"}, cwd=root)
    check("qa_db: tên biến admin cấu hình được (QA_DB_ADMIN_URL_VAR)", rc == 0 and out == "MY_ADMIN")
    rc, _, err = _py("import qa_db as q; q._admin()", {}, cwd=root)
    check("qa_db: .env chỉ có DATABASE_URL trỏ máy dev lạ -> từ chối", rc != 0 and "dev-db.example" in err)
    rc, out, _ = _py("import qa_db as q, sys; sys.modules['psycopg2'] = None; sys.modules['psycopg'] = None; "
                     "q.PSQL = 'no-such-psql-xyz'; c = q.Conn('localhost', 5432, 'u', '', 'p', 't')\n"
                     "try:\n    q.sql(c, 'SELECT 1')\nexcept SystemExit as e:\n    print(e)", {}, cwd=root)
    check("qa_db: không psycopg và không psql -> báo rõ, không traceback", "psql" in out and "psycopg" in out)


def edit_seg_cases(root, r):
    # 3. EDIT_API_SEG: danh sách / glob / regex, không in ký tự rác
    ctrl = r + "/backend/src/main/java/a/web/FooController.java"
    for seg, label in (("Controller.java", "chuỗi không có /"), ("*Controller.java", "glob"),
                       ("x/,*/web/*", "danh sách + glob thư mục"), (r"re:Controller\.java$", "regex")):
        _, out, _ = run(EDIT, ["post"], {"tool_name": "Edit", "tool_input": {"file_path": ctrl}}, root,
                        env={"EDIT_API_SEG": seg, "EDIT_FORMAT_CHECK_CMD": ""})
        check(f"edit API_SEG {label}: nhắc kèm đường dẫn tương đối gốc repo",
              "[REMINDER] Sửa endpoint backend/src/main/java/a/web/FooController.java" in out)
    _, out, _ = run(EDIT, ["post"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/api/v1/x.py"}}, root,
                    env={"EDIT_FORMAT_CHECK_CMD": ""})
    check("edit API_SEG mặc định (đoạn đường dẫn) vẫn chạy", "Sửa endpoint backend/app/api/v1/x.py" in out)
    _, out, _ = run(EDIT, ["post"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/web/src/a.ts"}}, root,
                    env={"EDIT_API_SEG": "*Controller.java", "EDIT_FORMAT_CHECK_CMD": ""})
    check("edit API_SEG glob: file khác -> không nhắc", "REMINDER" not in out)

    # 4. check-docs-drift.sh: grep -r thư mục, mặc định không gắn FastAPI
    bash = shutil.which("bash")
    if bash:
        d = os.path.join(root, "drift")
        os.makedirs(os.path.join(d, "src", "a"))
        os.makedirs(os.path.join(d, "node_modules", "x"))
        open(os.path.join(d, "src", "a", "C.java"), "w").write('@GetMapping("/api/orders")\n')
        open(os.path.join(d, "node_modules", "x", "i.js"), "w").write('app.get("/nm")\n')
        sh = os.path.join(HERE, "check-docs-drift.sh").replace(os.sep, "/")
        o1 = subprocess.run([bash, sh], cwd=d, capture_output=True, text=True, encoding="utf-8").stdout
        o2 = subprocess.run([bash, sh], cwd=d, capture_output=True, text=True, encoding="utf-8",
                            env=dict(os.environ, ROUTER_REGISTRY_FILE="src")).stdout
        check("drift: mặc định chưa cấu hình route -> bỏ qua", "bỏ qua (chưa đặt ROUTER_REGISTRY_FILE" in o1)
        check("drift: ROUTER_REGISTRY_FILE thư mục -> grep -r ra route", '@GetMapping("/api/orders"' in o2 and "/nm" not in o2)

    # 5. handoff: base branch tự dò; gợi ý select_tests chỉ khi script có
    g = os.path.join(root, "gitrepo")
    os.makedirs(g)
    git = lambda *a: subprocess.run(["git", *a], cwd=g, capture_output=True, text=True)
    git("init", "-q", "-b", "main")
    git("-c", "user.email=t@t", "-c", "user.name=t", "commit", "-q", "--allow-empty", "-m", "x")
    _, out, _ = _py("import handoff; print(handoff.base_branch())", {"CLAUDE_PROJECT_DIR": g})
    check("base branch: chỉ có main -> main", out == "main")
    git("branch", "develop")
    _, out, _ = _py("import handoff; print(handoff.base_branch(), handoff.BASE_BRANCH)", {"CLAUDE_PROJECT_DIR": g})
    check("base branch: có develop -> develop (cả thuộc tính cũ BASE_BRANCH)", out == "develop develop")
    _, out, _ = _py("import handoff; print(handoff.base_branch())", {"CLAUDE_PROJECT_DIR": g, "HANDOFF_BASE_BRANCH": "trunk"})
    check("base branch: HANDOFF_BASE_BRANCH đặt -> dùng nguyên", out == "trunk")
    alt = os.path.join(root, "scripts-noselect")
    os.makedirs(alt)
    for fn in ("handoff.py", "edit_hooks.py"):
        shutil.copy(os.path.join(HERE, fn), alt)
    sid = {"session_id": "SEL1"}
    run(os.path.join(alt, "edit_hooks.py"), ["post"], {**sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/s.py"}},
        root, env={"EDIT_FORMAT_CHECK_CMD": ""})
    _, out, _ = run(os.path.join(alt, "handoff.py"), ["stopcheck"], sid, root)
    check("stop: không có select_tests.py -> lời nhắc không gợi ý nó", "pytest" in out and "select_tests" not in out)
    if have("select_tests.py"):
        sid = {"session_id": "SEL2"}
        run(EDIT, ["post"], {**sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/s.py"}}, root,
            env={"EDIT_FORMAT_CHECK_CMD": ""})
        _, out, _ = run(HANDOFF, ["stopcheck"], sid, root, env={"HANDOFF_BASE_BRANCH": "trunk"})
        check("stop: có select_tests.py -> gợi ý kèm --base", "select_tests.py --base trunk" in out)
    else:
        skip("stop: có select_tests.py -> gợi ý kèm --base", "không có select_tests.py (adapter Python)")

    # 9. prefilter trong settings.json: danh sách runner cách nhau dấu phẩy
    if bash:
        settings = json.load(open(os.path.join(HERE, "..", "settings.json"), encoding="utf-8"))
        cmd = next(h["command"] for m in settings["hooks"]["PostToolUse"] if m["matcher"] == "Bash|PowerShell"
                   for h in m["hooks"])
        cmd = cmd.replace("$CLAUDE_PROJECT_DIR/.claude/scripts/handoff.py", HANDOFF.replace(os.sep, "/"))
        jvm = {"HANDOFF_TEST_CMD_REGEX": "", "HANDOFF_TEST_LABEL": "JUnit",
               "HANDOFF_TEST_RUNNER_REGEX": r"(?:\S*[/\\])?mvnw?(?:\.cmd)?\s+(?:\S+\s+)*?(?:test|verify)(?:\s|$)"}

        def prefilter(sid, command, **extra):
            e = {k: v for k, v in os.environ.items() if not k.startswith(CONFIG_PREFIXES)}
            e.update(CLAUDE_PROJECT_DIR=root, PYTHONIOENCODING="utf-8", **jvm, **extra)
            payload = {"session_id": sid, "tool_name": "Bash", "tool_input": {"command": command}}
            return subprocess.run([bash, "-c", cmd], input=json.dumps(payload), capture_output=True, text=True,
                                  encoding="utf-8", env=e).returncode
        for sid, pf, want in (("PF1", "pytest", "JUnit"), ("PF2", "gradle,mvn", ""), ("PF3", "", "")):
            run(EDIT, ["post"], {"session_id": sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/p.py"}},
                root, env=dict(jvm, EDIT_FORMAT_CHECK_CMD=""))
            time.sleep(0.05)
            rc = prefilter(sid, "cd backend && ./mvnw -q test -Dtest=FooTest", HANDOFF_TEST_PREFILTER=pf)
            _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": sid}, root, env=jvm)
            label = {"pytest": "không khớp -> không mark, vẫn nhắc", "gradle,mvn": "danh sách khớp `mvn` -> mark",
                     "": "rỗng -> luôn gọi mark"}[pf]
            check(f"prefilter HANDOFF_TEST_PREFILTER='{pf}': {label}", rc == 0 and (want in out if want else out.strip() == ""))


JVM_RUNNER_RE = r"(?:&\s+)?(?:\S*[/\\])?(?:mvnw?(?:\.cmd)?|gradlew?(?:\.bat)?)\s+(?:\S+\s+)*?(?:test|verify|check)(?:\s|$)"
JS_RUNNER_RE = r"(?:npm|pnpm|yarn)\s+(?:--prefix\s+\S+\s+)?(?:run\s+)?test\b|npx\s+(?:vitest|jest)\b"
SPRING_START_TEST_RE = (r"\bmvnw?(?:\.cmd)?\b[^|;&]*(?:\bspring-boot:run\b|\s(?:test|verify)(?=\s|$))"
                        r"|\bgradlew?(?:\.bat)?\b[^|;&]*(?:\bbootRun\b|\s(?:test|check)(?=\s|$))|\bjava\b[^|;&]*\s-jar\s")
JAVA_TEST_DB_RE = (r"\bmvnw?(?:\.cmd)?\b[^|;&]*\s(?:test|verify)(?=[\s)'\"]|$)"
                   r"|\bgradlew?(?:\.bat)?\b[^|;&]*\s(?:test|check)(?=[\s)'\"]|$)")


def cmd_parse_cases(root, r):
    """Tách đoạn lệnh có nháy / $(…) / backtick / PowerShell: handoff._runs_tests, mark qua PowerShell, context_guard."""
    jvm = {"HANDOFF_TEST_CMD_REGEX": "", "HANDOFF_TEST_RUNNER_REGEX": JVM_RUNNER_RE}
    js = {"HANDOFF_TEST_CMD_REGEX": "", "HANDOFF_TEST_RUNNER_REGEX": JS_RUNNER_RE}
    for cmd, env, ps, want in (
            ("TEST_DB_URL=$(python .claude/scripts/qa_db.py url t1) ./mvnw -q test", jvm, False, True),
            ("DB_URL=$(python .claude/scripts/qa_db.py url t1) ./mvnw -q test", jvm, False, True),
            ("cd backend && DB_URL=$(python .claude/scripts/qa_db.py url t1) ./mvnw -q test -Dtest=FooTest", jvm, False, True),
            ('DB_URL="$(python q.py url t1; echo)" ./mvnw verify', jvm, False, True),
            ('FOO="a b" pytest -q', {}, False, True),
            ("A=1 B=$(echo x y) npm test", js, False, True),
            ("A=`echo a b` pytest -q", {}, False, True),
            ("env CI=1 pytest", {}, False, True),
            ("(cd backend && pytest -q)", {}, False, True),
            ("& .\\mvnw.cmd test", jvm, True, True),
            ("cd backend; & .\\mvnw.cmd -q test 2>&1 | Select-Object -Last 30", jvm, True, True),
            ("echo 'x; pytest -q'", {}, False, False),
            ('git commit -m "fix; pytest -q pass"', {}, False, False),
            ("DB_URL=$(python .claude/scripts/qa_db.py url t1) ./mvnw -q test-compile", jvm, False, False)):
        rc, out, err = _py(f"import handoff; print(handoff._runs_tests({cmd!r}, {ps!r}))", env)
        check(f"runs_tests{' (PowerShell)' if ps else ''}: `{cmd}` -> {'test' if want else 'not-test'}"
              + ("" if out == str(want) else f"   | được `{out}` {err[-200:]}"), out == str(want))
    # mark qua tool PowerShell: `& .\mvnw.cmd test` được tính
    sid = {"session_id": "PSJ"}
    run(EDIT, ["post"], {**sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/ps.py"}}, root,
        env=dict(jvm, EDIT_FORMAT_CHECK_CMD=""))
    time.sleep(0.05)
    run(HANDOFF, ["mark"], {**sid, "tool_name": "PowerShell", "tool_input": {"command": "& .\\mvnw.cmd -q test"}}, root, env=jvm)
    _, out, _ = run(HANDOFF, ["stopcheck"], sid, root, env=jvm)
    check("mark PowerShell: `& .\\mvnw.cmd -q test` tính là đã test", out.strip() == "")

    guard = os.path.join(HERE, "context_guard.py")

    def g(cmd, tool="Bash", **env):
        code, out, err = run(guard, ["bash"], {"tool_name": tool, "tool_input": {"command": cmd}, "cwd": root},
                             root=root, cwd=root, env=env)
        return code, out
    check("guard: `;` trong nháy không tách đoạn -> echo chuỗi alembic không bị chặn",
          g('echo "done; alembic upgrade head"')[0] == 0)
    check("guard: `FOO=1 grep 'alembic upgrade'` (chỉ đọc sau tiền tố gán) -> cho qua",
          g("FOO=1 grep -rn 'alembic upgrade' docs")[0] == 0)
    check("guard: alembic sau $(…) có `;` bên trong vẫn bị chặn",
          g("X=$(echo a; echo b) alembic upgrade head")[0] == 2)
    st = {"GUARD_MIGRATION_CMD_REGEX": FLYWAY_RE, "GUARD_MIGRATION_DRYRUN_REGEX": "",
          "GUARD_MIGRATION_DB_URL_VAR": "FLYWAY_URL,DB_URL,SPRING_DATASOURCE_URL",  # giai đoạn 1: chưa có profile test
          "GUARD_STARTUP_CMD_REGEX": SPRING_START_TEST_RE}
    code, out = g("cd backend && ./mvnw -q test", **st)
    check("startup (mvnw test): .env trỏ DB máy khác -> CẢNH BÁO", code == 0 and "CẢNH BÁO" in out)
    code, out = g("TEST_DB_URL=$(python .claude/scripts/qa_db.py url t1) ./mvnw -q test", **st)
    check("startup (giai đoạn 1, chưa có profile test): TEST_DB_URL=$(qa_db.py url) -> app vẫn đọc DB_URL của .env -> CẢNH BÁO",
          code == 0 and "CẢNH BÁO" in out)
    st2 = dict(st, GUARD_MIGRATION_DB_URL_VAR="TEST_DB_URL,FLYWAY_URL,DB_URL,SPRING_DATASOURCE_URL")
    code, out = g("TEST_DB_URL=$(python .claude/scripts/qa_db.py url t1) ./mvnw -q test", **st2)
    check("startup (giai đoạn 2, TEST_DB_URL đứng đầu danh sách biến): TEST_DB_URL=$(qa_db.py url) -> im",
          code == 0 and out.strip() == "")
    code, out = g("DB_URL=$(python .claude/scripts/qa_db.py url t1) ./mvnw -q test", **st)
    check("startup (mvnw test): DB_URL=$(qa_db.py url) -> im", code == 0 and out.strip() == "")
    code, out = g("& .\\mvnw.cmd verify", "PowerShell", **st)
    check("startup (PowerShell): `& .\\mvnw.cmd verify`, .env trỏ DB máy khác -> CẢNH BÁO", code == 0 and "CẢNH BÁO" in out)
    code, out = g("$env:DB_URL = 'jdbc:postgresql://localhost:5432/app'; & .\\mvnw.cmd verify", "PowerShell", **st)
    check("startup (PowerShell): `$env:DB_URL = '…localhost…'` trong lệnh -> im", code == 0 and out.strip() == "")
    code, out = g("./mvnw -q test-compile", **st)
    check("startup: `mvnw test-compile` không phải chạy test -> im", out.strip() == "")

    # bộ nạp `.env` trong lệnh (GUARD_ENV_LOADER_REGEX / GUARD_ENV_WRAPPER_REGEX): .env đè môi trường + phép gán đứng
    # trước bộ nạp; chỉ phép gán đứng SAU bộ nạp thắng .env. .env của root trỏ dev-db.example (spring_dev).
    local = "jdbc:postgresql://localhost:5432/app"
    code, out = g("(set -a; . ./.env; set +a; cd backend && ./mvnw spring-boot:run)", DB_URL=local, **st)
    check("env-loader P1: môi trường DB_URL=localhost, lệnh tự nạp .env trỏ máy khác -> CẢNH BÁO theo .env",
          code == 0 and "CẢNH BÁO" in out and "spring_dev" in out)
    code, out = g("export DB_URL=$(python .claude/scripts/qa_db.py url t1); (set -a; . ./.env; set +a; ./mvnw test)", **st)
    check("env-loader P2: export DB_URL=$(qa_db.py url) TRƯỚC bước nạp .env -> .env đè lại -> CẢNH BÁO",
          code == 0 and "CẢNH BÁO" in out and "spring_dev" in out)
    wrap = dict(st, GUARD_STARTUP_CMD_REGEX=SPRING_START_TEST_RE + r"|\brun-backend\.(?:ps1|sh)\b",
                GUARD_ENV_WRAPPER_REGEX=r"\brun-backend\.(?:ps1|sh)\b")
    code, out = g("powershell -NoProfile -File scripts/run-backend.ps1", DB_URL=local, **wrap)
    check("env-loader P3: wrapper run-backend.ps1 (tự nạp .env), môi trường DB_URL=localhost -> CẢNH BÁO theo .env",
          code == 0 and "CẢNH BÁO" in out and "spring_dev" in out)
    code, out = g("powershell -NoProfile -File scripts/run-backend.ps1", DB_URL=local,
                  **dict(wrap, GUARD_ENV_WRAPPER_REGEX=""))
    check("env-loader: không khai wrapper (GUARD_ENV_WRAPPER_REGEX='') -> theo môi trường như cũ -> im",
          code == 0 and out.strip() == "")
    tdb = dict(st, GUARD_TEST_CMD_REGEX=JAVA_TEST_DB_RE)
    ok_cmd = ('(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url t1)"; '
              'cd backend && ./mvnw -q test)')
    code, out = g(ok_cmd, **tdb)
    check("env-loader: nạp .env TRƯỚC rồi export DB_URL=$(qa_db.py url) -> im", code == 0 and out.strip() == "")
    code, out = g('export DB_URL="$(python .claude/scripts/qa_db.py url t1)"; echo ". ./.env"; ./mvnw -q test', **tdb)
    check("env-loader: `. ./.env` chỉ nằm trong echo -> không tính là bộ nạp -> im", code == 0 and out.strip() == "")
    ps_load = ("Get-Content .env | Where-Object { $_ -match '^\\s*([A-Za-z_]\\w*)\\s*=(.*)$' } | ForEach-Object "
               "{ Set-Item \"env:$($Matches[1])\" $Matches[2].Trim().Trim('\"') }")
    code, out = g(ps_load + "; $env:DB_URL = (python .claude/scripts/qa_db.py url t1); & .\\backend\\mvnw.cmd -q test",
                  "PowerShell", **tdb)
    check("env-loader (PowerShell): Get-Content .env rồi $env:DB_URL = (qa_db.py url) -> im", code == 0 and out.strip() == "")
    code, out = g("$env:DB_URL = (python .claude/scripts/qa_db.py url t1); " + ps_load + "; & .\\backend\\mvnw.cmd -q test",
                  "PowerShell", **tdb)
    check("env-loader (PowerShell): $env:DB_URL đặt TRƯỚC Get-Content .env -> CẢNH BÁO", code == 0 and "CẢNH BÁO" in out)
    code, _ = g("export DB_URL=$(python .claude/scripts/qa_db.py url t1); (set -a; . ./.env; set +a; ./mvnw flyway:migrate)",
                **st)
    check("env-loader: migration sau bước nạp .env (URL qa đặt trước) -> chặn", code == 2)
    code, out = g("(set -a; . ./.env; set +a; ./mvnw test)", GUARD_ENV_LOADER_REGEX="", DB_URL=local, **st)
    check("env-loader: GUARD_ENV_LOADER_REGEX='' -> thứ tự cũ (môi trường trước .env) -> im", out.strip() == "")

    # test-db-guard (GUARD_TEST_CMD_REGEX): so TÊN DB, kể cả localhost (DB dev và qa_* cùng một Postgres)
    code, out = g("DB_URL=jdbc:postgresql://localhost:5435/app_dev ./mvnw -q test", **tdb)
    check("test-db: mvnw test trên localhost/app_dev -> CẢNH BÁO nêu tên DB", code == 0 and "[db-guard]" in out
          and "app_dev" in out)
    code, out = g("DB_URL=jdbc:postgresql://localhost:5435/app_dev ./mvnw -q test", **st)
    check("test-db: tắt (GUARD_TEST_CMD_REGEX rỗng) -> startup chỉ so host -> im", out.strip() == "")
    code, out = g("DB_URL=jdbc:postgresql://localhost:5435/qa_t1 ./mvnw -q test", **tdb)
    check("test-db: DB qa_* trên localhost -> im", code == 0 and out.strip() == "")
    code, out = g("(set -a; . ./.env; set +a; ./gradlew check)", DB_URL=local, **tdb)
    check("test-db: gradlew check sau bước nạp .env -> CẢNH BÁO theo .env", "[db-guard]" in out and "spring_dev" in out)
    py = {"GUARD_TEST_CMD_REGEX": r"\bpytest\b", "GUARD_TEST_DB_URL_VAR": "TEST_POSTGRES_URL"}
    code, out = g("cd backend && TEST_POSTGRES_URL=postgresql://u:p@localhost:5432/app_dev python -m pytest -q", **py)
    check("test-db (pytest): TEST_POSTGRES_URL trỏ localhost/app_dev -> CẢNH BÁO", "[db-guard]" in out and "app_dev" in out)
    code, out = g("(export TEST_POSTGRES_URL=\"$(python .claude/scripts/qa_db.py url t1)\"; cd backend && "
                  "python -m pytest -q tests/integration)", **py)
    check("test-db (pytest): URL từ qa_db.py -> im", code == 0 and out.strip() == "")
    code, out = g("cd backend && python -m pytest -q tests/test_x.py", **py)
    check("test-db (pytest): không xác định được DB (unit) -> im", code == 0 and out.strip() == "")
    for cmd in (ok_cmd, "(cd backend && ./mvnw -q test)"):
        rc, out, err = _py(f"import handoff; print(handoff._runs_tests({cmd!r}, False))", jvm)
        check(f"runs_tests: `{cmd}` (subshell) -> test" + ("" if out == "True" else f"   | được `{out}` {err[-200:]}"),
              out == "True")

    # settings.json: mọi script hook gọi phải tồn tại, trừ lệnh có chốt `[ ! -f … ] ||` (file tuỳ chọn, xoá được)
    settings_path = os.path.join(HERE, "..", "settings.json")
    if os.path.exists(settings_path):
        settings = json.load(open(settings_path, encoding="utf-8"))
        cmds = [h.get("command", "") for ev in (settings.get("hooks") or {}).values() for m in ev for h in m.get("hooks", [])]
        missing = []
        for c in cmds:
            for fn in re.findall(r"\.claude/scripts/([\w.-]+\.(?:py|sh))", c):
                if not have(fn) and "[ ! -f" not in c:
                    missing.append(fn)
        check("settings.json: script hook gọi đều tồn tại (hoặc có chốt `[ ! -f … ] ||`)"
              + (f" — thiếu: {', '.join(missing)}" if missing else ""), not missing)
        bash = shutil.which("bash")
        nudge = next((c for c in cmds if "codegraph_nudge.py" in c), None)
        if bash and nudge:
            empty = os.path.join(root, "no-scripts")
            os.makedirs(os.path.join(empty, ".claude", "scripts"), exist_ok=True)
            rc = subprocess.run([bash, "-c", nudge], input="{}", capture_output=True, text=True, encoding="utf-8",
                                env=dict(os.environ, CLAUDE_PROJECT_DIR=empty.replace(os.sep, "/"))).returncode
            check("settings.json: hook codegraph_nudge khi đã xoá script -> exit 0 (không chặn Grep/Glob/Agent)", rc == 0)
    if have("codegraph_nudge.py"):
        nudge_py = os.path.join(HERE, "codegraph_nudge.py")
        code, out, _ = run(nudge_py, [], {"session_id": "N1", "tool_name": "Grep"}, root)
        check("codegraph_nudge: repo chưa có .codegraph/ -> im", code == 0 and out.strip() == "")
        os.makedirs(os.path.join(root, ".codegraph"), exist_ok=True)
        code, out, _ = run(nudge_py, [], {"session_id": "N1", "tool_name": "Grep"}, root)
        code2, out2, _ = run(nudge_py, [], {"session_id": "N1", "tool_name": "Grep"}, root)
        check("codegraph_nudge: có .codegraph/ -> nhắc một lần mỗi session", code == 0 and "CODEGRAPH" in out
              and out2.strip() == "")
        shutil.rmtree(os.path.join(root, ".codegraph"), ignore_errors=True)
    else:
        skip("codegraph_nudge: 2 ca", "không có codegraph_nudge.py")


def run_project(argv):
    """--project: nạp env của settings.json dự án rồi chạy bảng ca hook_cases.json."""
    global PROJECT_ENV

    def opt(name, default):
        return argv[argv.index(name) + 1] if name in argv and argv.index(name) + 1 < len(argv) else default
    proj = opt("--root", os.environ.get("CLAUDE_PROJECT_DIR") or os.path.dirname(os.path.dirname(HERE)))
    settings = opt("--settings", os.path.join(proj, ".claude", "settings.json"))
    cases_path = opt("--cases", os.path.join(HERE, "hook_cases.json"))
    if not os.path.exists(cases_path):
        sys.exit(f"Không thấy {cases_path} — chép hook_cases.example.json thành hook_cases.json rồi sửa theo dự án.")
    if not os.path.isdir(proj):
        sys.exit(f"--root {proj}: không phải thư mục.")
    if not os.path.isfile(settings):
        sys.exit(f"Không thấy {settings} — truyền `--settings <file>` (file JSON có khối `env`; không bắt buộc nằm trong "
                 f"--root).")
    PROJECT_ENV = {k: str(v) for k, v in (json.load(open(settings, encoding="utf-8")).get("env") or {}).items()}
    cases = json.load(open(cases_path, encoding="utf-8"))
    cases = cases.get("cases", []) if isinstance(cases, dict) else cases
    print(f"# dự án: {proj}\n# settings: {settings} ({len(PROJECT_ENV)} biến env)\n# ca: {cases_path} ({len(cases)} ca)")
    guard = os.path.join(HERE, "context_guard.py")
    tmp = tempfile.mkdtemp(prefix="hookproj-")
    os.makedirs(os.path.join(tmp, "docs"))
    try:
        for i, c in enumerate(cases, 1):
            want = c.get("expect", "")
            extra = c.get("env") or {}
            if "bash" in c:
                tool = "PowerShell" if c.get("shell", "").lower() == "powershell" else "Bash"
                cwd = proj
                if "dotenv" in c:  # ca tự mang nội dung `.env`: chạy trong thư mục tạm chỉ có file đó
                    cwd = os.path.join(tmp, f"dotenv{i}")
                    os.makedirs(cwd)
                    body = c["dotenv"] if isinstance(c["dotenv"], str) else "\n".join(c["dotenv"])
                    with open(os.path.join(cwd, ".env"), "w", encoding="utf-8", newline="\n") as f:
                        f.write(body.rstrip("\n") + "\n")
                code, out, err = run(guard, ["bash"], {"tool_name": tool, "tool_input": {"command": c["bash"]}, "cwd": cwd},
                                     root=cwd, cwd=cwd, env=extra)
                got = "block" if code == 2 else ("warn" if out.strip() else "allow")
                subject, text = c["bash"], out + err
            elif "edit" in c:
                path = os.path.join(tmp, c["edit"]).replace(os.sep, "/")
                phase = "pre" if want in ("block", "allow") else "post"
                code, out, err = run(EDIT, [phase], {"session_id": f"P{i}", "tool_name": "Edit",
                                                     "tool_input": {"file_path": path}}, root=tmp, cwd=tmp, env=extra)
                got = ("block" if code == 2 else "allow") if phase == "pre" else ("remind" if "[REMINDER]" in out else "silent")
                subject, text = c["edit"], out + err
            elif "test_cmd" in c:
                ps = c.get("shell", "").lower() == "powershell"
                rc, out, err = _py(f"import handoff; print(handoff._runs_tests({c['test_cmd']!r}, {ps!r}))", extra, cwd=proj)
                got = {"True": "test", "False": "not-test"}.get(out, f"lỗi: {err[-200:]}")
                subject, text = c["test_cmd"], out
            elif "path" in c:
                rc, out, err = _py("import handoff, re; p = " + repr(c["path"]) + "; "
                                   "t = bool(handoff.TEST_PATH_REGEX and re.search(handoff.TEST_PATH_REGEX, p)); "
                                   "print('test' if t else ('product' if any(s in p for s in handoff.CODE_PATHS) else 'other'))",
                                   extra, cwd=proj)
                got, subject, text = out or f"lỗi: {err[-200:]}", c["path"], out
            else:
                got, subject, text = "?", json.dumps(c, ensure_ascii=False), ""
            ok = got == want and (not c.get("contains") or c["contains"] in text)
            name = c.get("name") or f"{want}: {subject}"
            check(name + ("" if ok else f"   | được `{got}`: {text.strip()[:200]}"), ok)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def main():
    if "--project" in sys.argv[1:] or os.environ.get("TEST_HOOKS_PROJECT") == "1":
        run_project(sys.argv[1:])
        return report()
    root = tempfile.mkdtemp(prefix="hooktest-")
    try:
        os.makedirs(os.path.join(root, "docs"))
        r = root.replace(os.sep, "/")
        handoff_file(root, "a-design", "design", "backend/app/services/aaa*.py")
        handoff_file(root, "b-dev", "dev-ready", "backend/app/services/bbb*.py")

        # guard / edit_hooks pre
        code, _, err = run(EDIT, ["pre"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/docs/_ref/x.md"}}, root)
        check("pre: chặn docs/_ref", code == 2 and "docs/_ref" in err)
        code, _, err = run(EDIT, ["pre"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/services/aaa_1.py"}}, root)
        check("pre: chặn file thuộc Scope task chưa duyệt", code == 2 and "a-design" in err)
        code, _, _ = run(EDIT, ["pre"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/services/bbb_1.py"}}, root)
        check("pre: cho qua file của task đang dev (tên phase cũ)", code == 0)
        code, out, _ = run(EDIT, ["pre"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/docs/x.md"}}, root)
        check("pre: cho qua docs thường", code == 0 and out.strip() == "")

        # mark + stopcheck
        sid = {"session_id": "T"}
        run(EDIT, ["post"], {**sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/x.py"}}, root)
        time.sleep(0.05)
        code, out, _ = run(HANDOFF, ["stopcheck"], sid, root)
        check("stop: nhắc khi sửa backend chưa pytest", code == 0 and "pytest" in out)
        code, out, _ = run(HANDOFF, ["stopcheck"], sid, root)
        check("stop: chỉ nhắc một lần mỗi lần sửa", out.strip() == "")
        code, out, _ = run(HANDOFF, ["stopcheck"], {**sid, "stop_hook_active": True}, root)
        check("stop: tôn trọng stop_hook_active", out.strip() == "")
        time.sleep(0.05)
        run(HANDOFF, ["mark"], {**sid, "tool_name": "Bash", "tool_input": {"command": "python -m pytest -q x"}}, root)
        run(EDIT, ["post"], {**sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/y.py"}}, root)
        code, out, _ = run(HANDOFF, ["stopcheck"], sid, root)
        check("stop: nhắc lại sau lần sửa mới", "pytest" in out)
        time.sleep(0.05)
        run(HANDOFF, ["mark"], {**sid, "tool_name": "Bash", "tool_input": {"command": "pytest x"}}, root)
        code, out, _ = run(HANDOFF, ["stopcheck"], sid, root)
        check("stop: im khi đã chạy pytest", out.strip() == "")
        time.sleep(0.05)
        run(EDIT, ["post"], {**sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/z.py"}}, root)
        run(HANDOFF, ["mark"], {**sid, "tool_name": "Bash", "tool_input": {"command": "pip show pytest; grep -rn pytest ."}}, root)
        _, out, _ = run(HANDOFF, ["stopcheck"], sid, root)
        check("mark: `pip show pytest`/`grep pytest` không tính là đã test", "pytest" in out)
        time.sleep(0.05)
        run(HANDOFF, ["mark"], {**sid, "tool_name": "PowerShell", "tool_input": {"command": "python -m pytest -q x"}}, root)
        _, out, _ = run(HANDOFF, ["stopcheck"], sid, root)
        check("mark: pytest chạy qua PowerShell được tính", out.strip() == "")

        # guard: sửa trong worktree vẫn so đúng Scope; file điều hành không bị coi là idle
        code, _, err = run(EDIT, ["pre"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/.claude/worktrees/wt1/backend/app/services/aaa_2.py"}}, root)
        check("pre: chặn file thuộc Scope khi sửa trong worktree", code == 2 and "a-design" in err)
        open(os.path.join(root, "docs", "HANDOFF.dot1-orchestrator.local.md"), "w", encoding="utf-8").write("# điều hành\n")
        _, out, _ = run(HANDOFF, ["status"], root=root)
        check("status: bỏ qua file điều hành", "dot1-orchestrator" not in out)
        os.remove(os.path.join(root, "docs", "HANDOFF.dot1-orchestrator.local.md"))
        code, _, _ = run(HANDOFF, ["new", "k-small", "--flow", "C-nhỏ", "--scope", "frontend/src/k/"], root=root)
        txt = open(os.path.join(root, "docs", "HANDOFF.k-small.local.md"), encoding="utf-8").read()
        check("new: C-nhỏ bắt đầu ở dev", code == 0 and "**Phase:** dev" in txt)
        os.remove(os.path.join(root, "docs", "HANDOFF.k-small.local.md"))

        # new / set-phase / raise / resolve / status / close
        code, _, _ = run(HANDOFF, ["new", "c-new", "--flow", "F", "--scope", "frontend/src/x/"], root=root)
        txt = open(os.path.join(root, "docs", "HANDOFF.c-new.local.md"), encoding="utf-8").read()
        check("new: phase mặc định theo flow (F -> design)", code == 0 and "**Phase:** design" in txt)
        code, _, err = run(HANDOFF, ["new", "d-new"], root=root)
        check("new: bắt khai --scope", code != 0 and "scope" in err.lower())
        run(HANDOFF, ["set-phase", "c-new", "dev-ready", "--note", "BE xong"], root=root)
        txt = open(os.path.join(root, "docs", "HANDOFF.c-new.local.md"), encoding="utf-8").read()
        check("set-phase: sửa cả block lẫn yaml, đổi tên cũ sang mới", "**Phase:** dev (BE xong)" in txt and "\nphase: dev\n" in txt)
        run(HANDOFF, ["raise", "c-new", "--text", "lệch A"], root=root)
        run(HANDOFF, ["raise", "c-new", "--kind", "question", "--text", "hỏi B"], root=root)
        _, out, _ = run(HANDOFF, ["status"], root=root)
        check("status: đếm 2 câu hỏi mở", "2 câu hỏi mở" in out)
        run(HANDOFF, ["resolve", "c-new", "Q1", "--text", "chọn A"], root=root)
        _, out, _ = run(HANDOFF, ["status"], root=root)
        check("resolve: còn 1 câu hỏi mở", "1 câu hỏi mở" in out)
        open(os.path.join(root, "docs", "HANDOFF.history.md"), "w", encoding="utf-8").write("# H\n")
        code, _, _ = run(HANDOFF, ["close", "c-new", "--summary", "xong", "--keep-db"], root=root)
        hist = open(os.path.join(root, "docs", "HANDOFF.history.md"), encoding="utf-8").read()
        check("close: append history + xoá file", code == 0 and "`c-new`" in hist
              and not os.path.exists(os.path.join(root, "docs", "HANDOFF.c-new.local.md")))

        # status: tên phase chuẩn, thiếu Scope sau duyệt, file idle; normalize; close --no-history
        handoff_file(root, "h-noscope", "dev-ready (BE xong)")
        handoff_file(root, "h-ba", "ba")
        handoff_file(root, "h-orch", "idle")
        _, out, _ = run(HANDOFF, ["status"], root=root)
        check("status: hiện tên phase chuẩn + giữ ghi chú", "h-noscope | dev (BE xong)" in out and "tên phase cũ" in out)
        check("status: THIẾU SCOPE chỉ khi đã qua thiết kế",
              "h-noscope | dev (BE xong) | C | THIẾU SCOPE" in out and "h-ba | ba | C | thiếu Scope" in out)
        check("status: liệt kê file idle kể cả khi có task chạy", "file idle chưa đóng: h-orch" in out)
        run(HANDOFF, ["normalize"], root=root)
        txt = open(os.path.join(root, "docs", "HANDOFF.h-noscope.local.md"), encoding="utf-8").read()
        check("normalize: sửa block lẫn yaml", "**Phase:** dev (BE xong)" in txt and "\nphase: dev\n" in txt)
        code, _, _ = run(HANDOFF, ["close", "h-orch", "--no-history", "--keep-db"], root=root)
        hist = open(os.path.join(root, "docs", "HANDOFF.history.md"), encoding="utf-8").read()
        check("close --no-history: xoá file, không ghi history", code == 0 and "h-orch" not in hist
              and not os.path.exists(os.path.join(root, "docs", "HANDOFF.h-orch.local.md")))
        for t in ("h-noscope", "h-ba"):
            os.remove(os.path.join(root, "docs", f"HANDOFF.{t}.local.md"))

        # session con: claim + Stop hook giữ chạy tới hết pipeline
        wd = os.path.join(root, "wt-child")
        os.makedirs(wd)
        run(HANDOFF, ["new", "e-child", "--flow", "C", "--scope", "frontend/src/e/", "--orchestrator", "local_x"], root=root)
        run(HANDOFF, ["set-phase", "e-child", "qa-run"], root=root)
        code, out, _ = run(HANDOFF, ["claim", "e-child"], root=root, cwd=wd)
        check("claim: ghi Workdir", code == 0 and "Workdir" in out)
        base = {"session_id": "C1", "cwd": wd}
        _, out, _ = run(HANDOFF, ["stopcheck"], {**base, "last_assistant_message": "PROGRESS | e-child | đang chạy nền"}, root)
        check("child: nhắc khi dừng giữa pipeline", "chưa tới điểm dừng hợp lệ" in out)
        _, out, _ = run(HANDOFF, ["stopcheck"], {**base, "last_assistant_message": "BLOCKED | e-child | DB lỗi"}, root)
        check("child: cho dừng khi BLOCKED", out.strip() == "")
        _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": "C1", "cwd": root, "last_assistant_message": "x"}, root)
        check("child: không đụng session khác thư mục", out.strip() == "")
        run(HANDOFF, ["raise", "e-child", "--text", "hỏi"], root=root)
        _, out, _ = run(HANDOFF, ["stopcheck"], {**base, "last_assistant_message": "đã hỏi"}, root)
        check("child: cho dừng khi có câu hỏi OPEN", out.strip() == "")
        run(HANDOFF, ["resolve", "e-child", "Q1", "--text", "ok"], root=root)
        run(HANDOFF, ["set-phase", "e-child", "idle", "--note", "chờ điều phối đóng"], root=root)
        _, out, _ = run(HANDOFF, ["stopcheck"], {**base, "last_assistant_message": "DONE | e-child | xong"}, root)
        check("child: cho dừng khi phase idle", out.strip() == "")

        # vai trò theo session (brief có [[handoff-role <task> <role>]], ghi lúc UserPromptSubmit)
        run(HANDOFF, ["new", "f-role", "--flow", "C", "--scope", "frontend/src/f/", "--orchestrator", "local_y"], root=root)
        run(HANDOFF, ["set-phase", "f-role", "dev"], root=root)
        run(HANDOFF, ["status"], {"session_id": "D1", "prompt": "Brief... [[handoff-role f-role dev]]"}, root)
        run(HANDOFF, ["status"], {"session_id": "Q1", "prompt": "[[handoff-role f-role qa]]"}, root)
        run(HANDOFF, ["status"], {"session_id": "L1", "prompt": "[[handoff-role f-role lead]]"}, root)
        _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": "D1", "last_assistant_message": "PROGRESS | f-role | đang chạy nền"}, root)
        check("role dev: nhắc khi PROGRESS", "vai `dev`" in out)
        _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": "D1", "last_assistant_message": "DONE | f-role | xong"}, root)
        check("role dev: DONE mà phase còn dev -> nhắc set-phase", "set-phase f-role code-review" in out)
        run(HANDOFF, ["set-phase", "f-role", "code-review"], root=root)
        _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": "D1", "last_assistant_message": "DONE | f-role | xong"}, root)
        check("role dev: DONE sau set-phase -> cho dừng", out.strip() == "")
        _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": "Q1", "last_assistant_message": "REVIEW-REQUEST | f-role | kịch bản"}, root)
        check("role qa: REVIEW-REQUEST -> cho dừng", out.strip() == "")
        _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": "L1", "last_assistant_message": "WAITING-CHILD | f-role | chờ dev"}, root)
        check("role lead: WAITING-CHILD -> cho dừng", out.strip() == "")
        _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": "L1", "last_assistant_message": "DONE | f-role | xong"}, root)
        check("role lead: DONE khi chưa idle -> nhắc", "chưa `idle`" in out)

        # vai qa không được sửa code sản phẩm; được ghi tests/
        run(HANDOFF, ["status"], {"session_id": "QG", "prompt": "[[handoff-role f-role qa]]"}, root)
        code, _, err = run(EDIT, ["pre"], {"session_id": "QG", "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/services/zzz.py"}}, root)
        check("role qa: chặn sửa code sản phẩm", code == 2 and "session QA" in err)
        code, _, _ = run(EDIT, ["pre"], {"session_id": "QG", "tool_name": "Write", "tool_input": {"file_path": r + "/backend/tests/integration/test_zzz_db.py"}}, root)
        check("role qa: cho ghi tests/", code == 0)
        run(HANDOFF, ["status"], {"session_id": "LG", "prompt": "[[handoff-role f-role lead]]"}, root)
        _, out, _ = run(EDIT, ["pre"], {"session_id": "LG", "tool_name": "Edit", "tool_input": {"file_path": r + "/frontend/src/f/a.tsx"}}, root)
        check("role lead: nhắc khi phiên task tự sửa code", "tự sửa code" in out)
        _, out, _ = run(EDIT, ["pre"], {"session_id": "LG", "agent_id": "sub1", "tool_name": "Edit", "tool_input": {"file_path": r + "/frontend/src/f/a.tsx"}}, root)
        check("role lead: subagent dev của phiên task không bị nhắc", "tự sửa code" not in out)

        # nhắc pytest chỉ cho task của chính session (có vai), không nhắc task người khác
        run(HANDOFF, ["new", "g-own", "--flow", "C", "--scope", "frontend/src/g/"], root=root)
        run(HANDOFF, ["set-phase", "g-own", "qa-plan"], root=root)
        run(HANDOFF, ["status"], {"session_id": "OW", "prompt": "[[handoff-role g-own lead]]"}, root)
        run(EDIT, ["post"], {"session_id": "OW", "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/q.py"}}, root)
        _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": "OW", "last_assistant_message": "WAITING-CHILD | g-own | x"}, root)
        check("test note: không nhắc task của session khác đang dev", "pytest" not in out)

        # mốc giờ: new/set-phase/raise/resolve ghi giờ; timing đọc được; close ghi HANDOFF.timing.jsonl
        run(HANDOFF, ["new", "t-time", "--flow", "C", "--scope", "frontend/src/t/"], root=root)
        run(HANDOFF, ["set-phase", "t-time", "dev"], root=root)
        run(HANDOFF, ["raise", "t-time", "--kind", "question", "--text", "q"], root=root)
        run(HANDOFF, ["resolve", "t-time", "Q1", "--text", "ok"], root=root)
        _, out, _ = run(HANDOFF, ["timing", "t-time"], root=root)
        check("timing: có phase ba và dev", "| ba |" in out and "| dev |" in out)
        run(HANDOFF, ["close", "t-time", "--keep-db"], root=root)
        log = os.path.join(root, "docs", "HANDOFF.timing.jsonl")
        check("timing: close ghi jsonl", os.path.exists(log) and '"t-time"' in open(log, encoding="utf-8").read())

        # --- CONFIG qua biến môi trường: handoff.py / edit_hooks.py cho stack JS ---
        js = {"HANDOFF_BACKEND_SEGS": "frontend/src/", "HANDOFF_TEST_LABEL": "vitest", "HANDOFF_TEST_CMD_REGEX": "",
              "HANDOFF_TEST_RUNNER_REGEX": r"(?:npm|pnpm|yarn)\s+(?:run\s+)?test\b|npx\s+(?:vitest|jest)\b",
              "HANDOFF_TEST_SELECT_HINT": "", "EDIT_FORMAT_CHECK_CMD": ""}
        js_sid = {"session_id": "JS1"}
        run(EDIT, ["post"], {**js_sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/frontend/src/a.ts"}}, root, env=js)
        _, out, _ = run(HANDOFF, ["stopcheck"], js_sid, root, env=js)
        check("env JS: sửa frontend/src -> nhắc chạy vitest", "vitest" in out and "pytest" not in out)
        time.sleep(0.05)
        run(EDIT, ["post"], {**js_sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/frontend/src/b.ts"}}, root, env=js)
        run(HANDOFF, ["mark"], {**js_sid, "tool_name": "Bash", "tool_input": {"command": "python -m pytest -q"}}, root, env=js)
        _, out, _ = run(HANDOFF, ["stopcheck"], js_sid, root, env=js)
        check("env JS: `python -m pytest` không tính là đã test", "vitest" in out)
        time.sleep(0.05)
        run(EDIT, ["post"], {**js_sid, "tool_name": "Edit", "tool_input": {"file_path": r + "/frontend/src/c.ts"}}, root, env=js)
        run(HANDOFF, ["mark"], {**js_sid, "tool_name": "Bash", "tool_input": {"command": "cd frontend && CI=1 npm run test -- a"}}, root, env=js)
        _, out, _ = run(HANDOFF, ["stopcheck"], js_sid, root, env=js)
        check("env JS: `npm run test` được tính là đã test", out.strip() == "")
        _, out, _ = run(EDIT, ["post"], {"session_id": "JS2", "tool_name": "Edit", "tool_input": {"file_path": r + "/backend/app/x.py"}},
                        root, env=js)
        _, out, _ = run(HANDOFF, ["stopcheck"], {"session_id": "JS2"}, root, env=js)
        check("env JS: sửa ngoài HANDOFF_BACKEND_SEGS -> không nhắc", out.strip() == "")

        run(HANDOFF, ["status"], {"session_id": "QJ", "prompt": "[[handoff-role f-role qa]]"}, root)
        spec = {"tool_name": "Write", "tool_input": {"file_path": r + "/frontend/src/f/a.test.ts"}, "session_id": "QJ"}
        check("role qa: mặc định QA được ghi *.test.ts trong src", run(EDIT, ["pre"], spec, root)[0] == 0)
        check("env HANDOFF_TEST_PATH_REGEX='/tests/': *.test.ts thành code sản phẩm -> chặn",
              run(EDIT, ["pre"], spec, root, env={"HANDOFF_TEST_PATH_REGEX": "/tests/"})[0] == 2)
        for f, label in (("/backend/src/test/java/a/FooTest.java", "Maven src/test"), ("/backend/app/x_test.go", "Go *_test.go"),
                         ("/frontend/src/f/b.test.jsx", "*.test.jsx")):
            sp = {"tool_name": "Write", "tool_input": {"file_path": r + f}, "session_id": "QJ"}
            check(f"role qa: mặc định cho ghi file test {label}",
                  run(EDIT, ["pre"], sp, root, env={"HANDOFF_CODE_PATHS": "backend/,frontend/src/"})[0] == 0)
        sp = {"tool_name": "Write", "tool_input": {"file_path": r + "/backend/src/main/java/a/Foo.java"}, "session_id": "QJ"}
        check("role qa: src/main vẫn là code sản phẩm -> chặn",
              run(EDIT, ["pre"], sp, root, env={"HANDOFF_CODE_PATHS": "backend/,frontend/src/"})[0] == 2)

        os.makedirs(os.path.join(root, "fe", "src"), exist_ok=True)
        fmt = {"EDIT_FORMAT_ROOT": "fe", "EDIT_FORMAT_SUBDIR": "src/", "EDIT_FORMAT_EXTS": ".ts,.tsx",
               "EDIT_FORMAT_NAME": "fakefmt", "EDIT_FORMAT_FIX_CMD": "fakefmt --write {file}",
               "EDIT_FORMAT_CHECK_CMD": '{python} -c "import sys; sys.exit(1)" {file}'}
        _, out, _ = run(EDIT, ["post"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/fe/src/a.ts"}}, root, env=fmt)
        check("env EDIT_FORMAT_*: formatter tuỳ chỉnh báo chưa format", "[fakefmt] src/a.ts" in out and "fakefmt --write src/a.ts" in out)
        _, out, _ = run(EDIT, ["post"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/fe/src/a.ts"}}, root,
                        env=dict(fmt, EDIT_FORMAT_CHECK_CMD="no-such-formatter-xyz --check {file}"))
        check("edit post: công cụ format không cài -> im lặng", out.strip() == "")
        _, out, _ = run(EDIT, ["post"], {"tool_name": "Edit", "tool_input": {"file_path": r + "/fe/src/a.py"}}, root, env=fmt)
        check("edit post: đuôi ngoài EDIT_FORMAT_EXTS -> không kiểm", out.strip() == "")

        run(HANDOFF, ["new", "m-dir", "--flow", "C", "--scope", "src/m/"], root=root,
            env={"HANDOFF_MIGRATION_DIR": "prisma/migrations/"})
        txt = open(os.path.join(root, "docs", "HANDOFF.m-dir.local.md"), encoding="utf-8").read()
        check("env HANDOFF_MIGRATION_DIR: mẫu HANDOFF ghi thư mục migration mới",
              "prisma/migrations/" in txt and "alembic" not in txt)
        code, out, err = run(HANDOFF, ["close", "m-dir", "--no-history"], root=root,
                             env={"HANDOFF_QA_DB_SCRIPT": os.path.join(root, "no_such_qa_db.py")})
        check("close: không có script DB tạm -> bỏ qua êm", code == 0 and "không dọn được" not in out + err)

        # --- alembic-guard (context_guard.py bash): chỉ cho alembic ghi lên DB tạm qa_* ---
        guard = os.path.join(HERE, "context_guard.py")
        with open(os.path.join(root, ".env"), "w", encoding="utf-8") as f:
            f.write("DATABASE_URL=postgresql://u:p@dev-db.example:5432/app_dev\n")
        saved = os.environ.pop("DATABASE_URL", None)

        def alembic(cmd):
            code, _, err = run(guard, ["bash"], {"tool_input": {"command": cmd}, "cwd": root}, root=root, cwd=root)
            return code, err

        code, err = alembic("cd backend && alembic upgrade head")
        check("alembic: không URL inline -> chặn, nêu DB trong .env", code == 2 and "app_dev" in err and "u:p" not in err)
        check("alembic: stamp lên DB dev inline -> chặn",
              alembic("DATABASE_URL=postgresql://u:p@dev-db.example:5432/app_dev python -m alembic stamp head")[0] == 2)
        check("alembic: URL qa_* inline -> cho qua",
              alembic("DATABASE_URL=postgresql://u:p@test-db.example:5432/qa_x alembic upgrade head")[0] == 0)
        check("alembic: URL từ qa_db.py -> cho qua",
              alembic("export DATABASE_URL=$(python .claude/scripts/qa_db.py url t1) && alembic downgrade -1")[0] == 0)
        check("alembic: biến trỏ qa_* qua phép gán -> cho qua",
              alembic("U=postgresql://u:p@h:5432/qa_y; DATABASE_URL=$U alembic stamp head")[0] == 0)
        check("alembic: biến không rõ -> chặn", alembic("DATABASE_URL=$SOMETHING alembic upgrade head")[0] == 2)
        check("alembic: có dấu cho phép -> cho qua", alembic("alembic upgrade head  # alembic-guard: allow")[0] == 0)
        check("alembic: lệnh chỉ đọc (heads/current/history) -> cho qua",
              alembic("alembic heads && alembic current && alembic history | tail -3")[0] == 0)
        check("alembic: grep chuỗi 'alembic upgrade' -> cho qua", alembic("grep -rn 'alembic upgrade' docs")[0] == 0)
        check("migration: dấu cho phép mới `migration-guard: allow` -> cho qua",
              alembic("alembic upgrade head  # migration-guard: allow")[0] == 0)
        check("migration: `alembic upgrade --sql` (chỉ in SQL) -> cho qua", alembic("alembic upgrade head --sql")[0] == 0)

        # --- CONFIG qua biến môi trường: đổi stack migration ---
        def guard_env(cmd, **env):
            code, _, err = run(guard, ["bash"], {"tool_input": {"command": cmd}, "cwd": root}, root=root, cwd=root, env=env)
            return code, err

        prisma = {"GUARD_MIGRATION_CMD_REGEX": r"\bprisma\s+(migrate\s+(?:deploy|dev|reset)|db\s+push)\b",
                  "GUARD_MIGRATION_DRYRUN_REGEX": ""}
        code, err = guard_env("npx prisma migrate deploy", **prisma)
        check("env prisma: `prisma migrate deploy` lên DB trong .env -> chặn", code == 2 and "app_dev" in err
              and "migrate deploy" in err)
        check("env prisma: DATABASE_URL qa_* inline -> cho qua",
              guard_env("DATABASE_URL=postgresql://u:p@h:5432/qa_z npx prisma migrate deploy", **prisma)[0] == 0)
        check("env prisma: `prisma migrate status` (chỉ đọc) -> cho qua",
              guard_env("npx prisma migrate status", **prisma)[0] == 0)
        check("env prisma: alembic không còn bị bắt", guard_env("alembic upgrade head", **prisma)[0] == 0)
        flyway = {"GUARD_MIGRATION_CMD_REGEX": r"\bflyway\b[^|]*?\b(migrate|clean|baseline|repair|undo)\b",
                  "GUARD_MIGRATION_DB_URL_VAR": "FLYWAY_URL"}
        code, err = guard_env("FLYWAY_URL=jdbc:postgresql://db.example:5432/app_dev flyway migrate", **flyway)
        check("env flyway: FLYWAY_URL (jdbc) trỏ DB dev -> chặn, nêu DB", code == 2 and "app_dev" in err and "FLYWAY_URL" in err)
        check("env flyway: FLYWAY_URL trỏ qa_* -> cho qua",
              guard_env("FLYWAY_URL=jdbc:postgresql://db.example:5432/qa_f flyway migrate", **flyway)[0] == 0)
        check("env flyway: `flyway info` -> cho qua", guard_env("flyway info", **flyway)[0] == 0)
        code, err = guard_env("flyway migrate", **flyway)
        check("env flyway: không có FLYWAY_URL ở đâu -> chặn (không xác định DB)", code == 2 and "không xác định" in err)
        check("env SCRATCH_DB_PREFIXES: tiền tố tạm riêng -> cho qua",
              guard_env("DATABASE_URL=postgresql://u:p@h:5432/ci_x alembic upgrade head", SCRATCH_DB_PREFIXES="ci_")[0] == 0)
        check("env: GUARD_MIGRATION_CMD_REGEX='' tắt migration-guard",
              guard_env("alembic upgrade head", GUARD_MIGRATION_CMD_REGEX="")[0] == 0)

        # --- giới hạn đọc (sed/cat) ---
        with open(os.path.join(root, "big.txt"), "w", encoding="utf-8") as f:
            f.write("x\n" * 500)
        code, err = guard_env("cat big.txt")
        check("read-limit: `cat` file 500 dòng -> chặn, gợi ý mặc định không nhắc công cụ riêng",
              code == 2 and "500" in err and "grep -n" in err and "codegraph" not in err.lower())
        check("read-limit: `cat big.txt | head` -> cho qua", guard_env("cat big.txt | head -5")[0] == 0)
        check("read-limit: dấu `# context-guard: allow` -> cho qua", guard_env("cat big.txt # context-guard: allow")[0] == 0)
        check("read-limit: GUARD_CAT_MAX_LINES=1000 -> cho qua", guard_env("cat big.txt", GUARD_CAT_MAX_LINES="1000")[0] == 0)
        check("read-limit: `sed -n 1,250p` -> chặn", guard_env("sed -n 1,250p big.txt")[0] == 2)
        check("read-limit: GUARD_SED_MAX_LINES=300 -> `sed -n 1,250p` cho qua",
              guard_env("sed -n 1,250p big.txt", GUARD_SED_MAX_LINES="300")[0] == 0)
        code, err = guard_env("cat big.txt", GUARD_SEARCH_HINT="Tra bằng `idx find`")
        check("read-limit: GUARD_SEARCH_HINT đổi câu gợi ý", code == 2 and "idx find" in err)
        new_feature_cases(root, r, guard_env)
        cmd_parse_cases(root, r)
        if saved is not None:
            os.environ["DATABASE_URL"] = saved
    finally:
        shutil.rmtree(root, ignore_errors=True)
    report()


def report():
    bad = [n for n, ok in results if ok is False]
    skipped = sum(1 for _, ok in results if ok is None)
    for n, ok in results:
        print({True: "PASS ", False: "FAIL ", None: "SKIP "}[ok] + n)
    ran = len(results) - skipped
    print(f"{ran - len(bad)}/{ran} PASS" + (f", {skipped} SKIP (file adapter/tuỳ chọn không có)" if skipped else ""))
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8")
        except Exception:
            pass
    main()

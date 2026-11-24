# Stack pack — Service FastAPI + Pydantic + pytest (ai-service, không DB)

Bản gốc của pack dành cho backend Python: FastAPI, Pydantic (v2, Pydantic Settings), SQLAlchemy 2 async (asyncpg),
Alembic, PostgreSQL, pytest (+ pytest-xdist). Các file chung (`PROJECT/*.md`, `WORKFLOW.md`, `HARD-STOPS.md`,
`LESSONS.md`…) chỉ ghi quy tắc trung lập với stack kèm placeholder; file này giữ phần **cụ thể cho stack** — lệnh, bẫy,
ví dụ code. Mỗi mục ghi file chung tương ứng để đối chiếu.

**Áp dụng ở MediSchedule cho service phụ `ai-service`** (Python FastAPI + PyTorch, chưa khởi tạo, không dùng DB —
đọc ảnh từ MinIO qua presigned URL do backend Java cấp). Backend chính là Java (pack
`.claude/stack-packs/backend-spring-boot-flyway.md`); pack này **đã bỏ** phần DB (SQLAlchemy, Alembic, migration, DB
test `qa_*`) và thay thư mục backend bằng `ai-service`. Lệnh test của service chỉ gồm unit / gọi endpoint bằng client
giả lập API ngoài; hook migration không cấu hình cho service này. Giá trị dưới là giá trị **dự kiến** — khi khởi tạo
service thì kiểm lại và ghi vào bảng "Service khác" của `PROJECT/stack-and-commands.md`.

Cột "Giá trị" là giá trị thuần; cột "Ghi chú" không chép vào giá trị. Mọi giá trị `*_COMMAND` chạy được từ gốc repo
(đã gồm `cd ai-service`).

| Khoá | Giá trị | Ghi chú |
|---|---|---|
| `BACKEND_LANG` | Python 3.11+ | |
| `BACKEND_FRAMEWORK` | FastAPI | |
| `SCHEMA_LIB` | Pydantic v2 | |
| `TEST_FRAMEWORK` | pytest + pytest-xdist | |
| `TEST_COMMAND` | `(cd ai-service && python -m pytest -q --tb=short <file…>)` | unit; đường dẫn file tính từ `ai-service` |
| `TEST_SINGLE_COMMAND` | `(cd ai-service && python -m pytest -q <file>::<test>)` | |
| `UNIT_TEST_DIR` | `ai-service/tests/test_<module>.py` | |
| `BE_LINT_COMMAND` | `(cd ai-service && python -m black . && python -m isort .)` | thêm `python -m flake8` nếu dự án dùng |
| `BE_TYPECHECK_COMMAND` | `(cd ai-service && python -m mypy app/)` | |
| `SERVICE_LAYER_GLOB` | `ai-service/app/services/*.py` | |
| `API_LAYER_GLOB` | `ai-service/app/api/**/*.py` | |
| `TEST_SELECT_COMMAND` | chọn thủ công theo package/module bị sửa | không có script chọn test (adapter `select_tests.py` đã xoá khỏi repo) |

---

## 1. Cấu trúc app & settings (← `PROJECT/architecture.md`, `PROJECT/conventions.md`)

- **Settings**: một entry point duy nhất `get_settings()` bọc `@lru_cache()`; không instantiate `Settings()` trực tiếp.
- **Router**: đăng ký ở registry router (vd `app/api/v1/router.py`), prefix `/api/v1`; router public (không auth) để
  thư mục riêng. Route handler giữ **mỏng**, business logic trong service class.
- **Auth dependency**: `Depends(get_current_user)`, `Depends(require_permission(...))`. Nếu dự án chọn kiểm trạng thái
  tài khoản mỗi request (`PROJECT/auth.md`), làm trong dependency này.
- **Requirements split**: tách file dependency cho image runtime (`requirements.txt`) và dev/test
  (`requirements-dev.txt`: pytest, xdist, black, mypy…). Đừng thêm thư viện test vào image runtime.
- **Chạy dev**: `python -m uvicorn app.main:app --reload`. Nếu dự án tắt auto-reload (debugpy, không `--reload`) thì sau
  mỗi lần sửa backend phải nhắc user restart (LESSONS "Môi trường & dữ liệu").

## 2. Error handling (← `PROJECT/conventions.md` § Error Handling)

Service raise `HTTPException` trực tiếp, không có custom exception class; route handler để exception lan lên. Mã lỗi
nghiệp vụ (nếu có) đặt trong `detail`.

```python
# raise ở service layer
raise HTTPException(status_code=404, detail="Order not found")
raise HTTPException(status_code=403, detail="Access denied")
```

Bẫy: service raise `ValueError` mà route không bắt ⇒ 500 thay vì 400. Hoặc service raise `HTTPException`, hoặc route bắt
`ValueError` → `HTTPException(400, detail=str(e))`.

## 3. Python style & công cụ (← `PROJECT/conventions.md`, `PROJECT/environment.md`)

- Full type hints everywhere; type check `python -m mypy app/`.
- Format: `black` **rồi** `isort` với `profile = black` — isort trần sẽ phá format của black.
- Máy dev mẫu có `python -m black`, `python -m flake8`; **không có** `ruff`. Kiểm lại khi đổi máy.

## 4. Pydantic — bẫy (← `LESSONS.md` § Frontend)

- **Response model bỏ im lặng field không khai báo.** Model ORM có cột `is_active` nhưng schema response không khai báo
  ⇒ JSON không có field đó, dù type FE có. Kiểm schema backend, không tin type FE.
- Schema đặt ở thư mục schemas riêng (vd `app/schemas/`), khai báo **trước** model trong thứ tự làm feature.

## 5. pytest (← `PROJECT/testing.md`, `PROJECT/qa-process.md`, `CODING-GUIDELINES.md` §6)

**Unit** — không fixture DB, không mock, không HTTP client:

```python
# Correct style — no pytest fixtures, no mock, no DB
def test_something() -> None:
    result = my_pure_function(args)
    assert result == expected


def test_raises_on_invalid() -> None:
    with pytest.raises(HTTPException):
        validate_something(bad_input)
```

**Gọi endpoint** (service không có DB)
- Endpoint: gọi qua `httpx.AsyncClient(transport=ASGITransport(app=app))` hoặc `TestClient`; API ngoài / mô hình nặng
  thay bằng client giả lập (fixture), không gọi dịch vụ thật trong test.
- Vòng sửa: `--lf` trên đúng lệnh đã chạy. Output gọn: `-q --tb=short`.
- Test QA viết thẳng vào file test dev của task; không tạo file `test_<x>_qa_*.py` riêng.
- **Khởi động:** mỗi lệnh pytest có test import router / `app.main` tốn vài giây tới vài chục giây import FastAPI app
  trước test đầu tiên ⇒ gom file vào một lệnh; test thuần service/model khởi động nhanh hơn nhiều.

## 6. Skill tham khảo & công cụ

- `.claude/skills/_shared/fastapi-expert` (routes, Pydantic V2, JWT, WebSocket),
  `.claude/skills/_shared/python-pro` (Python 3.11+, async, pytest).
- Lint ví dụ: `black` + `flake8`.

## Rule Pack bổ sung (team-code-reviewer)

Bổ sung cho `.claude/skills/team-code-reviewer/references/rule-pack.md` khi backend dùng stack này. Reviewer kết luận
từng dòng áp dụng cho file trong diff.

| Vùng | Rule | Mức khi vi phạm |
|---|---|---|
| routes | Validation input bằng schema Pydantic v2 ở boundary (request body/query), không validate tay rải trong service | Major |

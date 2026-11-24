## Định dạng Báo cáo Đầu ra (Test Report Template)

```markdown
# TEST REPORT: [Tên Tính Năng]

- **Verdict:** [PASSED / FAILED]
- **Reviewer:** QA Engineer

## 1. Test Summary

- **Total:** [N] test cases | **Passed:** [N] | **Failed:** [N]

## 2. Test Cases

| ID    | Scenario       | Expected   | Actual   | Status |
|-------|----------------|------------|----------|--------|
| TC-01 | [BDD scenario] | [expected] | [actual] | ✅ / ❌  |

## 2b. Ánh xạ AC → tầng test (BẮT BUỘC — thiếu AC nào là FAILED)

| AC    | Tầng test   | File spec                                  | TC liên quan |
|-------|-------------|--------------------------------------------|--------------|
| AC-01 | Thủ công    | — (steps ở TC-01)                          | TC-01        |
| AC-02 | Unit        | <file/lớp test unit trong `backend/src/test/java/vn/medischedule/<module>/*Test.java`>        | TC-02        |
| AC-03 | Integration | <file/lớp test trong `backend/src/test/java/vn/medischedule/<module>/*IntegrationTest.java`>      | TC-03        |

> Không dùng E2E. AC có logic backend → unit/integration; chỉ AC thuần UI mới được để Thủ công.

## 2c. Output thật của test runner

```
[dán nguyên output của `(set -a; . ./.env; set +a; cd backend && ./mvnw -q test -Dtest=<Lớp>Test)` — không tóm tắt, không chép tay]
```

## 2d. Chạy thật (PROJECT/qa-process.md — khi diff đổi FE hoặc endpoint)

DB backend: [host:port/db đã kiểm, không phải prod] · Tài khoản: [tên tài khoản test]

| AC | Luồng đã đi | Kết quả | Ảnh / ghi chú |
|----|-------------|---------|---------------|
| AC-x | Đăng nhập → … → … | Đạt / Lỗi | … |

Chưa kiểm được: [phần nào, vì sao — để user tự kiểm]

## 3. Regression Check

- [Liệt kê các tính năng liên quan đã kiểm tra, không bị ảnh hưởng]

## 4. Open Issues (nếu FAILED)

- [ ] [Mô tả bug cụ thể — file, function, behavior sai]
```

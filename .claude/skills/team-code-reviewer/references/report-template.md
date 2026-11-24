## Định dạng Báo cáo Đầu ra (Code Review Template)

> Chỉ tạo file này khi verdict là **REQUEST CHANGES**. LGTM không tạo report.

```markdown
# CODE REVIEW REPORT: [Tên Tính Năng]

- **Verdict:** REQUEST CHANGES
- **Reviewer:** Code Reviewer
- **Diff:** `<base>..<head>` — N file, +X/−Y

## 1. Điểm tốt (What Works)

- [Điểm viết tốt, đúng pattern — kèm `file:line`]

### Nghi ngờ đã loại trừ

| Nghi ngờ         | Cách xác minh                                | Kết luận                       |
|------------------|----------------------------------------------|--------------------------------|
| [giả thuyết lỗi] | [đếm / đọc file / chạy lệnh — số liệu thật]  | Không phải vấn đề vì [số liệu] |

## 2. Rủi ro & Điểm cần cải thiện (Real Risks & Improvements)

### 🚨 Critical — `path/to/file:123`: [claim 1 dòng]

- **Hiện trạng:** [code thật đang làm gì — trích snippet]
- **Rủi ro thực tế:** [triệu chứng user thấy]
- **Impact:** [định lượng: N query / endpoint X trả 500 / 9 round-trip DB]
- **Đề xuất:** [patch cụ thể]

### ⚠️ Major — `path/to/file:45`: [claim 1 dòng]

(4 field như trên)

> **Nits — không block.** LGTM không phụ thuộc mấy dòng này.
> - `path/to/file:88`: [naming / dead code / format]

## 3. Kết luận

- [Tóm tắt 1-2 câu]
```

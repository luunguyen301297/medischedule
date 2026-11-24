# Self-Review Pass (GATE 2, ngay sau draft)

Đọc lại spec như reviewer độc lập, không tin bản draft của chính mình. Không tạo file review riêng — sửa thẳng vào spec.

| Kiểm | Nội dung | Nguồn |
|---|---|---|
| Traceability | Mọi requirement quan trọng → User Story → AC → truy ngược được về requirement | `ba-methodology.md` §9 |
| Business rule | Mọi rule đã xác định khi khai thác đều có trong AC | §5 |
| Edge case | Dữ liệu thiếu/không hợp lệ, trạng thái không mong đợi, sai thứ tự… — chỉ case có ý nghĩa nghiệp vụ | §7 |
| Cross-capability (Flow C, G) | `docs/knowledge/{domain}/dependency-graph.yaml` → `depended_by` của capability đang sửa → spec mục 4 (input cho QA hồi quy) | — |
| Blast radius code-level | Khi đã biết symbol/endpoint cụ thể: tìm caller/dependent thật của symbol (CLAUDE.md §Tìm code) → vào mục 4, sub-mục "Blast radius code-level". Chưa biết symbol → ghi "chưa xác định code path cụ thể, cần SA/Dev xác nhận thêm" | — |
| Điểm mơ hồ | `grep -c "\[CẦN LÀM RÕ" <spec>` phải = 0; mỗi giả định ở mục 5 ghi rõ AC liên quan và hậu quả nếu sai | — |
| Completeness | Checklist §13: không assumption chưa xác nhận, không requirement mâu thuẫn, không lấn sang technical design | §13 |

Xử lý kết quả:

- Gap tự sửa được (thiếu AC, edge case, traceability) → Edit spec ngay, không hỏi user.
- Gap là điểm mơ hồ nghiệp vụ cần stakeholder quyết → quay lại GATE 1 (`AskUserQuestion`).
- Ghi tóm tắt ngắn đã sửa/đã kiểm gì để đưa vào thông báo GATE 3.

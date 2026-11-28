# TECH REVIEW REPORT: Menu Lịch hẹn — danh sách lịch hẹn và đặt lịch hộ khách

- **Verdict:** APPROVED
- **Reviewer:** Tech Lead
- **Revision Round:** 1

## 1. Đánh giá chi tiết

- **Khớp BA spec ↔ design:** Đạt — AC-01…23 đều có phần đáp ứng (menu/guard §5, danh sách §3.3, lưới giờ §3.1, đặt hộ
  §3.2, hủy §3.4, slotMinutes §3.6); GĐ-01…10 được theo đúng. Không có phần design ngoài scope.
- **Kiến trúc & Pattern:** Đạt — ghi qua service module sở hữu; native query chỉ-đọc nhiều bảng là ngoại lệ có chủ đích.
- **Dữ liệu & API Contract:** Đạt — index đủ cho lọc ngày, SĐT, slot; contract đủ chi tiết, không cần phase `contract`.
- **Bảo mật & Phân quyền:** Đạt — `/api/reception/**` + `hasAnyRole`, `createdByUserId` lấy từ principal.

## 2. Danh sách hành động (Action Items)

1. Index `shift (doctor_id, work_date, work_shift_id)` làm **unique không partial** (NULL vốn không đụng nhau) để
   `ON CONFLICT (doctor_id, work_date, work_shift_id) DO NOTHING` khớp mà không phải lặp predicate.
2. Native query danh sách: ghi comment ngay trên query rằng đây là read-model chỉ đọc, ngoại lệ quy tắc "không gọi chéo
   repository"; không dùng các bảng đó để ghi.
3. Lỗi unique khi `saveAndFlush` làm transaction bị đánh dấu rollback: map sang `ApiException` 409 rồi ném ra luôn,
   không làm thêm truy vấn nào sau đó trong cùng transaction.
4. Web: AC-17 validate phía client (SĐT, họ tên) trước khi gửi; `SLOT_TAKEN` phải refetch lưới giờ và bỏ chọn giờ.
5. Test bắt buộc: unit cho `AppointmentService` (Clock cố định) + integration trên DB `qa_*` cho migration V19, đặt trùng
   slot → 409, quyền DOCTOR 403 / RECEPTIONIST 200.

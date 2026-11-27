# TECH REVIEW REPORT: Chọn bác sĩ cho chuyên khoa

- **Verdict:** APPROVED
- **Reviewer:** Tech Lead
- **Revision Round:** 1 (Flow C, không có SA design — plan kỹ thuật ghi ngay dưới)

## 1. Đánh giá chi tiết

- **Khớp BA spec ↔ design:** Đạt — plan dưới phủ AC-01…AC-17; GĐ-01…GĐ-05 theo đúng.
- **Kiến trúc & Pattern:** Đạt — không migration (bảng `staff` đã có `user_id UNIQUE`, `specialty_id`); clinic gọi
  `auth` qua service, không đụng `UserRepository`.
- **Dữ liệu & API Contract:** Đạt — thêm 1 endpoint đọc, mở rộng request/response cũ; vài chục bản ghi nên gom trong
  bộ nhớ.
- **Bảo mật & Phân quyền:** Đạt — endpoint mới nằm trong `AdminSpecialtyController` (đã `hasRole('ADMIN')`).

## 2. Danh sách hành động (Action Items)

1. **auth:** thêm vào `AuthService` (hoặc service admin user sẵn có) một hàm public trả bác sĩ đang hoạt động
   `List<DoctorAccount(Long userId, String fullName)>` = users role DOCTOR, `enabled = true`. Clinic chỉ gọi hàm này.
2. **clinic:** `StaffRepository` (`findByUserId`, `findBySpecialtyIsNotNull`/`findBySpecialtyId`).
3. **API:**
   - `GET /api/admin/doctors` → `[{userId, fullName, specialtyId, specialtyName}]` — chỉ bác sĩ đang hoạt động
     (GĐ-02); `specialtyId` null nếu chưa có khoa.
   - `SpecialtyRequest` thêm `List<Long> doctorUserIds` (null = coi như rỗng; FE luôn gửi).
   - `SpecialtyResponse` thêm `doctorUserIds`, `doctorCount`, `singleDoctorName` (khi đúng 1), `singleRoomName` (khi
     đúng 1 phòng, tính cả phòng ngừng — khớp `roomCount`). Chỉ đếm bác sĩ đang hoạt động (GĐ-02); họ tên lấy từ tài
     khoản (GĐ-04), không từ `staff.full_name`.
4. **Gán bác sĩ** (`assignDoctors(specialty, ids)`, chạy SAU kiểm tên trong create/rename, cùng transaction → AC-06):
   - Mọi id phải có trong tập bác sĩ đang hoạt động, sai → `ApiException` 400 `DOCTOR_INVALID`
     "Bác sĩ không hợp lệ, hãy tải lại danh sách" (AC-07), ném trước khi ghi gì.
   - Staff đang thuộc khoa này mà không có trong danh sách → `specialty = null` (AC-05).
   - Id trong danh sách: có staff thì đổi `specialty` (chuyển khoa, AC-04), chưa có thì tạo staff
     (`userId`, `fullName` từ tài khoản) (GĐ-01). Không chặn khoa đang ngừng (AC-08).
   - Khử trùng id (`distinct`) trước khi xử lý.
5. **Danh sách chuyên khoa:** thay `countBySpecialty` bằng một lần lấy phòng có chuyên khoa rồi gom trong bộ nhớ (cần tên
   khi đúng 1 phòng); bác sĩ: 1 query staff + 1 lần gọi auth, gom theo `specialtyId`. Không query trong vòng lặp.
6. **Xóa chuyên khoa còn bác sĩ** (AC-14): FK `staff.specialty_id` đã chặn qua nhánh `inUse` hiện có — giữ, chỉ kiểm
   thông báo vẫn đúng. Lưu ý: bác sĩ bị khóa vẫn giữ `specialty_id` nên vẫn chặn xóa — chấp nhận (ghi `ponytail:`).
7. **FE** (`web/src/features/admin/specialty/`): ô `Select mode="multiple"` + `showSearch` theo tên, nhãn
   "Tên · Khoa" khi bác sĩ thuộc khoa khác, `notFoundContent="Chưa có tài khoản bác sĩ"`; dòng phụ theo AC-10…AC-13,
   AC-16 (`ellipsis` + `title`). Sau lưu chuyên khoa invalidate cả `['admin','doctors']`. AC-17 đã có sẵn nhờ
   `refresh()` invalidate `['admin','specialties']`.
8. **Test:** unit/integration cho `assignDoctors` (tạo mới, chuyển khoa, bỏ chọn, id không hợp lệ không ghi gì).

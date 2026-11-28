# TECH REVIEW REPORT: Ảnh hồ sơ nhân viên / bệnh nhân, hồ sơ bác sĩ trên trang chủ

- **Verdict:** APPROVED
- **Reviewer:** Tech Lead
- **Revision Round:** 1 (Flow C, không có SA: hướng giải pháp chốt trong file này)

## 1. Đánh giá chi tiết

- **Khớp BA spec ↔ design:** Đạt. AC-01..05 → dialog công khai từ `GET /api/public/doctors` (thêm `licenseNo`,
  `acceptsDesignation`); AC-06..11 → `avatarUrl` trong hồ sơ nhân viên (cột `staff.avatar_url` đã có từ V12);
  AC-12..15 → cột mới `patient.avatar_url`; AC-16 → giữ nguyên `@PreAuthorize` admin hiện có; AC-17 → DTO công khai
  không thêm trường nào khác.
- **Kiến trúc & Pattern:** Đạt. Không thêm endpoint; mở rộng record DTO hiện có. Upload dùng `filesApi.uploadImage`
  (`/api/admin/files`) như `ImageField`.
- **Dữ liệu & API Contract:** Đạt. Migration `V15__patient_avatar.sql`: `ALTER TABLE patient ADD COLUMN IF NOT EXISTS
  avatar_url VARCHAR(500)`. Thêm field cuối record (`StaffProfile`, `StaffProfileRequest`, `PatientProfile`,
  `PatientRequest`, `PublicDoctor`) — client cũ bỏ qua field mới.
- **Bảo mật & Phân quyền:** Đạt có điều kiện (Action 1).

## 2. Danh sách hành động (Action Items)

1. `avatarUrl` trong request: `@Size(max = 500)` + `@Pattern` chỉ nhận `https://…` hoặc `/api/public/files/…`
   (chặn `javascript:`/`data:`), rỗng → null.
2. `saveStaffProfile` ghi `avatarUrl` cho mọi vai trò nhân viên (không chỉ bác sĩ).
3. FE dùng một component ảnh chung cho hai dialog admin (xem/tải/gỡ, đổi chỉ khi Lưu; Hủy giữ ảnh cũ — AC-09).
4. Dialog bác sĩ công khai: `TopDoctors` đổi `figure` thành nút bấm được (bàn phím được), dialog không có dòng Tình trạng.
5. Test: cập nhật test hiện có dùng các record trên; thêm kiểm `@Pattern` từ chối `javascript:`.

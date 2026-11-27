# Test cases: Chọn bác sĩ cho chuyên khoa (specialty-doctor)

Spec: `docs/tasks/specialty-doctor-ba-spec.md` (AC-01…AC-17). TL: `docs/reviews/design-review-specialty-doctor.md`.

## 2b. Ánh xạ AC → tầng test → file

IT = `backend/src/test/java/vn/medischedule/clinic/ClinicCatalogIntegrationTest.java`;
UT = `.../clinic/service/ClinicCatalogServiceTest.java` (test của dev, không viết lại).

| AC | Tầng | Test |
|---|---|---|
| AC-01 ô "Bác sĩ" chọn nhiều, tìm theo tên, điền sẵn khi sửa | Thủ công/E2E | Mở Thêm/Sửa chuyên khoa: có ô Bác sĩ, gõ tên lọc được; Sửa khoa có bác sĩ thì ô điền sẵn. (Phần dữ liệu điền sẵn: IT `doctorMovesBetweenSpecialtiesAndCanBeUnassigned` kiểm `doctorUserIds` trong response) |
| AC-02 danh sách chọn: chỉ bác sĩ đang hoạt động, kèm khoa hiện tại | Integration | IT `doctorOptionsListOnlyActiveDoctorsWithCurrentSpecialty`; nhãn "Tên · Khoa" là UI (thủ công) |
| AC-03 gán bác sĩ chưa có khoa (tạo staff mới) | Integration | IT `createWithDoctorsCreatesStaffAndShowsSingleOrCount`; UT `assignCreatesStaffMovesFromOtherSpecialtyAndUnassignsRemoved` |
| AC-04 chuyển khoa | Integration | IT `doctorMovesBetweenSpecialtiesAndCanBeUnassigned`; UT như AC-03 |
| AC-05 bỏ chọn → không còn khoa (cả `doctorUserIds` null/rỗng) | Integration | IT `doctorMovesBetweenSpecialtiesAndCanBeUnassigned` |
| AC-06 lỗi tên (trùng/rỗng) không đổi bác sĩ | Integration | IT `nameErrorsKeepDoctorsUntouched` |
| AC-07 id không hợp lệ (bị khóa, không phải bác sĩ, không tồn tại) → 400 `DOCTOR_INVALID`, không ghi gì | Integration | IT `invalidDoctorIdsAreRejectedAndNothingChanges`; UT `inactiveOrUnknownDoctorRejectedBeforeAnyChange` |
| AC-08 khoa ngừng hoạt động vẫn gán được | Integration | IT `inactiveSpecialtyStillAcceptsDoctors` |
| AC-09 không có bác sĩ: thông báo trống + vẫn lưu | Integration + Thủ công | IT `createWithDoctorsCreatesStaffAndShowsSingleOrCount` (tạo không có `doctorUserIds` vẫn 201); thông báo "Chưa có tài khoản bác sĩ" là UI (thủ công) |
| AC-10 1 bác sĩ → `singleDoctorName` | Integration | IT `createWithDoctorsCreatesStaffAndShowsSingleOrCount`, `doctorMoves…`; hiển thị dòng phụ: thủ công |
| AC-11 ≥2 bác sĩ → `doctorUserIds` có N, `singleDoctorName` null | Integration | IT `createWithDoctorsCreatesStaffAndShowsSingleOrCount`; chữ "N bác sĩ": thủ công |
| AC-12 chưa có bác sĩ → rỗng | Integration | IT `createWithDoctorsCreatesStaffAndShowsSingleOrCount` |
| AC-13 tên dài cắt "…", tooltip, không lệch hàng | Thủ công/E2E | Gán bác sĩ tên rất dài, xem danh sách: một dòng, có "…", rê chuột thấy đủ tên, công tắc/nút thẳng hàng |
| AC-14 xóa khoa còn bác sĩ bị chặn | Integration | IT `specialtyWithAssignedDoctorCannotBeDeletedUntilUnassigned`, `lockedDoctorIsNotCounted…` (bác sĩ khóa vẫn chặn); `specialtyInUseCannotBeDeleted` (có sẵn) |
| AC-15 không phải ADMIN bị từ chối | Integration | IT `doctorEndpointsAndDoctorSavesRejectNonAdmin` (401 không token, 403 bacsi/letan/duocsi/benhnhan, dữ liệu không đổi) |
| AC-16 `singleRoomName` khi đúng 1 phòng (kể cả ngừng); 0 hoặc ≥2 thì null | Integration | IT `singleRoomNameCountsInactiveRoomsAndFollowsRoomChanges`; chữ "Phòng P104 · Tên" ở UI: thủ công |
| AC-17 dòng phụ cập nhật sau thao tác phòng | Integration + Thủ công | IT cùng test trên (API trả đúng sau thêm/đổi tên/xóa phòng); không cần tải lại trang là UI (thủ công, `refresh()` invalidate) |
| GĐ-02 bác sĩ khóa không hiện/không tính, lưu lại không gỡ khoa của họ | Integration | IT `lockedDoctorIsNotCountedKeepsSpecialtyAndStillBlocksDelete`; UT `lockedDoctorKeepsSpecialtyAndIsNotCounted` |

Tổng: 17 AC → Integration 14 / Unit 0 mới (dev đã có 3 UT) / Thủ công 3 (AC-01, AC-13, hiển thị dòng phụ ở AC-09/10/11/16/17).

## Kiểm thủ công (UI)

1. AC-01: `/admin/specialties` → Thêm: có ô Bác sĩ (nhiều lựa chọn, gõ lọc theo tên). Sửa khoa có bác sĩ: điền sẵn.
2. AC-02/09: mở ô Bác sĩ: bác sĩ thuộc khoa khác hiện "Tên · Khoa"; bác sĩ khóa không hiện; không có bác sĩ nào thì
   hiện "Chưa có tài khoản bác sĩ".
3. AC-10/11/12/16: dòng phụ "Phòng P104 · Nguyễn Văn A" (1 phòng, 1 bác sĩ); "2 phòng · 3 bác sĩ"; khoa chưa bác sĩ
   chỉ "N phòng"; chưa phòng "0 phòng".
4. AC-13: tên bác sĩ dài → một dòng, "…", tooltip đủ tên, công tắc + nút thẳng hàng.
5. AC-17: thêm/đổi tên/xóa phòng trong hộp phòng → dòng phụ khoa đổi ngay.

## Kết quả chạy

Lệnh: `(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url specialty_doctor)"; cd backend && ./mvnw -q test -Dtest='ClinicCatalogIntegrationTest,ClinicCatalogServiceTest')`
(DB `qa_specialty_doctor`)

```
ClinicCatalogIntegrationTest: Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
ClinicCatalogServiceTest:     Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
```

(Log ERROR SQLState 23503/23505 trong output là các ca kiểm chặn FK / trùng tên có chủ đích.)

Test mới thêm (10, trong IT): `doctorOptionsListOnlyActiveDoctorsWithCurrentSpecialty`,
`createWithDoctorsCreatesStaffAndShowsSingleOrCount`, `doctorMovesBetweenSpecialtiesAndCanBeUnassigned`,
`invalidDoctorIdsAreRejectedAndNothingChanges`, `nameErrorsKeepDoctorsUntouched`,
`lockedDoctorIsNotCountedKeepsSpecialtyAndStillBlocksDelete`, `specialtyWithAssignedDoctorCannotBeDeletedUntilUnassigned`,
`inactiveSpecialtyStillAcceptsDoctors`, `singleRoomNameCountsInactiveRoomsAndFollowsRoomChanges`,
`doctorEndpointsAndDoctorSavesRejectNonAdmin`.

Lỗi sản phẩm: không phát hiện.

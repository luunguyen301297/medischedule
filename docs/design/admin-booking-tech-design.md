# TECHNICAL DESIGN: Menu Lịch hẹn — danh sách lịch hẹn và đặt lịch hộ khách

- **BA spec:** `docs/tasks/admin-booking-ba-spec.md` (đã duyệt)
- **Target Component/Layer:** Fullstack — backend module `booking` (mới phần appointment), chạm `schedule`, `clinic`,
  `patient`, `system`; web `features/admin`, `features/home`.

## 1. Yêu cầu cốt lõi → đánh đổi → quyết định

| Yêu cầu | Đánh đổi | Quyết định |
|---|---|---|
| Lễ tân gọi được API (AC-02/04) | `/api/admin/**` bị SecurityConfig khóa ở tầng URL cho ADMIN | Prefix mới `/api/reception/**`, `@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")` trên controller; không sửa SecurityConfig |
| Ca khám `shift` sinh từ phân ca (GĐ-05, AC-09) | Sinh sẵn mọi ca khi phân ca → phải sửa luồng phân ca, tạo dư; sinh lười lúc đặt → chỉ chạm luồng đặt | **Sinh lười** (get-or-create) trong transaction đặt lịch. Xem lưới giờ không ghi gì: tính từ ca có sẵn hoặc "ca dự kiến" (chưa lưu) |
| Tìm ca khám tương ứng một phân ca | Khớp theo `start_time` vỡ khi admin sửa giờ ca làm việc | Thêm `shift.work_shift_id`, unique `(doctor_id, work_date, work_shift_id)` |
| Danh sách lọc/tìm theo tên BN, bác sĩ, phòng (AC-05/06) | Gom dữ liệu qua service từng module → không tìm/phân trang theo tên BN được | **Một native query chỉ-đọc** trong `AppointmentRepository` join `patient`, `staff`, `specialty`, `room`, `users` trả projection. Mọi thao tác ghi vẫn đi qua service module sở hữu (ngoại lệ read-model có chủ đích, ghi chú trong code) |
| Chống đặt trùng giờ (AC-13) | Khóa ứng dụng vs ràng buộc DB | Dựa vào unique index có sẵn `uq_appointment_doctor_slot` + khóa dòng ca (`SELECT … FOR UPDATE`); lỗi unique → 409 `SLOT_TAKEN` |
| Giới hạn lịch chờ theo SĐT (AC-15) | Đếm rồi ghi bị đua | Advisory lock theo SĐT như `DoctorRequestRepository.lockPhone` rồi đếm `status = BOOKED` theo `booker_phone` |

## 2. Database Schema Changes — `V19__appointment_staff_booking.sql`

(`git fetch` trước khi chốt số version, theo AGENTS.md.)

1. `specialty.slot_minutes SMALLINT NOT NULL DEFAULT 15 CHECK (slot_minutes BETWEEN 5 AND 120 AND slot_minutes % 5 = 0)`
   — AC-22/23. Dòng cũ nhận 15.
2. `shift.work_shift_id BIGINT REFERENCES work_shift (id)` (nullable vì cột thêm sau; mọi dòng do code này tạo đều có) +
   `CREATE UNIQUE INDEX uq_shift_doctor_work_shift ON shift (doctor_id, work_date, work_shift_id) `.
3. `appointment.channel`: thay CHECK thành `('APP','GUEST','AI','WALK_IN','STAFF')` (drop constraint tự đặt tên
   `appointment_channel_check` bằng `ALTER TABLE … DROP CONSTRAINT IF EXISTS`, add lại có tên rõ) — GĐ-04.
4. `appointment.created_by_user_id BIGINT REFERENCES users (id)` — nhân viên đặt hộ; NULL = khách tự đặt.
5. `CREATE INDEX idx_appointment_start ON appointment (start_at)` — lọc danh sách theo ngày.

Đã có, dùng lại: `idx_patient_phone`, `uq_appointment_doctor_slot`, `idx_appointment_booker`, setting
`booking.max_pending_per_phone`. Không cần dữ liệu mẫu mới (phòng đã có từ V8; lịch hẹn mẫu không cần cho test vì tạo
được qua UI). Entity: `Specialty.slotMinutes`, `Shift.workShiftId`, `Appointment.createdByUserId`, enum
`Appointment.Channel.STAFF`.

## 3. Processing Flow

### 3.1 Lưới giờ — `GET /api/reception/slots?date&workShiftId&doctorUserId`

`AppointmentService.slots` (booking, readOnly):

1. Kiểm ngày trong [hôm nay, hôm nay+6] theo `Asia/Ho_Chi_Minh` (`Clock` tiêm được như `DoctorRequestService`).
2. `WorkScheduleService.isAssigned(date, workShiftId, doctorUserId)` + `activeShift(workShiftId)` → lỗi
   `NOT_ON_SHIFT` / `SHIFT_INVALID`.
3. `ClinicCatalogService.bookingDoctor(userId)` → record `BookingDoctorInfo(staffId, fullName, specialtyId,
   specialtyName, slotMinutes, roomId, roomName)`; bác sĩ khóa/không có staff → `DOCTOR_UNAVAILABLE`; chưa khoa →
   `DOCTOR_NO_SPECIALTY`; khoa không có phòng active (lấy id nhỏ nhất) → `NO_ROOM` "Khoa X chưa có phòng khám đang hoạt
   động".
4. `WorkScheduleService.findClinicShift(staffId, date, workShiftId)`: có ca → dùng `slotMinutes`, `roomId`, giờ của ca
   đó (AC-18); chưa có → ca dự kiến từ work_shift + slotMinutes khoa + phòng ở bước 3.
5. Slot = `start + k*slot` với `slot_start + slot ≤ end`; slot đã qua (hôm nay) bỏ khỏi danh sách; slot có appointment
   `status <> CANCELLED` (query `AppointmentRepository.findTakenStartAt(doctorStaffId, from, to)`) → `available=false`.

### 3.2 Đặt hộ — `POST /api/reception/appointments`

`AppointmentService.createByStaff(meUserId, req)` trong một transaction:

1. Validate DTO (SĐT `^0\d{9}$`; hoặc `patientId`, hoặc `newPatient.fullName`).
2. Lặp bước 1–3 của 3.1.
3. `WorkScheduleService.getOrCreateClinicShift(staffId, date, workShift, roomId, slotMinutes)`: native
   `INSERT … ON CONFLICT (doctor_id, work_date, work_shift_id) DO NOTHING` với status
   `PUBLISHED`, `max_patients = floor(phút ca / slot)`, rồi `SELECT … FOR UPDATE` → `ClinicShiftInfo`.
4. `startTime` phải nằm trên lưới của ca (chia hết, trong khung, chưa qua) → `SLOT_INVALID`.
5. Số lịch không hủy của ca `< max_patients` → `SHIFT_FULL` (bảo hiểm; thường trùng điều kiện lưới).
6. `lockPhone(phone)` → đếm `BOOKED` theo `booker_phone` ≥ `SystemSettingService.intValue("booking.max_pending_per_phone", 2)` → 409 `TOO_MANY_PENDING`.
7. Bệnh nhân: `patientId` → `PatientService.requireActive(id)` và SĐT hồ sơ phải bằng SĐT nhập (chống chọn nhầm) →
   `PATIENT_PHONE_MISMATCH`; ngược lại `PatientService.createWalkUp(fullName, phone, dob, gender)` (owner NULL,
   relation SELF, source RECEPTION, mã `BN%06d` như `create`).
8. Sinh `booking_code` 8 ký tự từ bảng `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (`SecureRandom`), kiểm
   `existsByBookingCode` lặp tối đa 5 lần (không thử-bắt lỗi unique vì Postgres hủy transaction).
9. Lưu `Appointment` (channel STAFF, status BOOKED, designated=false, fee 0, `createdByUserId=me`), `saveAndFlush`; vi
   phạm `uq_appointment_doctor_slot` → 409 `SLOT_TAKEN` "Giờ này vừa có người đặt, vui lòng chọn giờ khác".
10. Trả `AppointmentRow` (cùng shape với dòng danh sách).

### 3.3 Danh sách — `GET /api/reception/appointments`

Tham số: `from`, `to` (LocalDate, mặc định hôm nay; tối đa 31 ngày → `RANGE_TOO_LONG`), `doctorUserId?`, `status?`,
`q?` (ILIKE họ tên BN, khớp `booker_phone`/`patient.phone` bắt đầu bằng, hoặc `booking_code` bằng — viết hoa), `page`,
`size` (≤ 100, mặc định 20). Native query + count query, `ORDER BY start_at, id`. Projection → `AppointmentRow(id,
bookingCode, startAt, endAt, status, channel, patientId, patientCode, patientName, phone, doctorUserId, doctorName,
specialtyName, roomName, createdByName, cancelReason, createdAt)`. `endAt = start_at + shift.slot_minutes`.

### 3.4 Hủy — `POST /api/reception/appointments/{id}/cancel` body `{reason}`

Tìm `FOR UPDATE`; không có → 404; `status ≠ BOOKED` → 409 `NOT_CANCELLABLE`; set CANCELLED + `cancelReason` (trim, 1–255).
Slot tự trống vì unique index loại CANCELLED.

### 3.5 Tìm bệnh nhân — `GET /api/reception/patients?phone=`

`PatientService.findByPhone(phone)`: chỉ khi khớp `^0\d{9}$`, `merged_into_id IS NULL`, tối đa 20, trả `PatientLite(id,
patientCode, fullName, dateOfBirth, gender)`.

### 3.6 Chuyên khoa

`SpecialtyRequest.slotMinutes` (`Integer`, `@Min(5) @Max(120)`, kiểm chia hết 5 ở service → `SLOT_MINUTES_INVALID`;
null khi tạo = 15, null khi sửa = giữ nguyên để không vỡ client cũ). `SpecialtyResponse.slotMinutes`.

## 4. API Contract Overview

| Method | Path | Mô tả | Lỗi chính |
|---|---|---|---|
| GET | `/api/reception/appointments` | Danh sách phân trang (Spring `Page`) | 400 `RANGE_TOO_LONG` |
| POST | `/api/reception/appointments` | Đặt hộ; body `{date, workShiftId, doctorUserId, startTime "HH:mm", phone, patientId?, newPatient?{fullName, dateOfBirth?, gender?}}` → 201 `AppointmentRow` | 400 validate/`DATE_OUT_OF_RANGE`/`NOT_ON_SHIFT`/`NO_ROOM`/`SLOT_INVALID`/`PATIENT_PHONE_MISMATCH`; 409 `SLOT_TAKEN`/`TOO_MANY_PENDING`/`SHIFT_FULL` |
| POST | `/api/reception/appointments/{id}/cancel` | Hủy, body `{reason}` | 404, 409 `NOT_CANCELLABLE` |
| GET | `/api/reception/slots` | `{doctorName, specialtyName, roomName, slotMinutes, slots:[{time, available}]}` | như bước 3.1 |
| GET | `/api/reception/patients?phone=` | `PatientLite[]` | — |
| GET | `/api/public/booking/shifts`, `/api/public/booking/doctors` | Dùng lại: buổi đang bật, bác sĩ trực | — |
| PUT/POST | `/api/admin/specialties…` | Thêm field `slotMinutes` | 400 `SLOT_MINUTES_INVALID` |

Bộ lọc bác sĩ ở danh sách dùng lại `GET /api/public/doctors`. `needs_api_detail: false` (bảng trên đủ để code).

## 5. Web

- `features/admin/booking/`: `api.ts` (type + gọi API trên), `BookingPage.tsx` (bộ lọc: RangePicker mặc định hôm nay,
  Select bác sĩ, Select trạng thái, Input.Search; Table phân trang server; nút Hủy với Modal lý do; nút "Đặt lịch hộ"),
  `BookingCreateDialog.tsx` (Form: ngày (Segmented/DatePicker 7 ngày) → buổi → bác sĩ trực → lưới giờ (nút chọn, giờ
  khóa disabled) → SĐT → danh sách hồ sơ (Radio, kèm "Người khác — tạo hồ sơ mới") → họ tên/ngày sinh/giới tính khi tạo
  mới). Lỗi `SLOT_TAKEN` → toast + refetch slots. Thành công → toast mã lịch, invalidate `['reception','appointments']`.
- React Query key: `['reception', 'appointments', filters]`, `['reception', 'slots', date, shiftId, doctorUserId]`,
  `['reception', 'patients', phone]`.
- `AdminLayout`: thêm mục `{ key: '/admin/bookings', icon: <ScheduleOutlined />, label: 'Lịch hẹn' }` đầu `MENU`; cho
  vào khi `role ∈ {ADMIN, RECEPTIONIST}`; RECEPTIONIST chỉ thấy mục này và mọi path khác `/admin/bookings` →
  `<Navigate to="/admin/bookings">`. `AdminRoutes`: route `bookings`, index → `bookings`.
- `HomePage`: RECEPTIONIST → `/admin/bookings` (cạnh nhánh ADMIN hiện có).
- Trang Chuyên khoa: `InputNumber` "Thời gian khám mỗi lượt (phút)" step 5, min 5, max 120, mặc định 15; cột hiển thị.
- Nhãn: trạng thái BOOKED "Đã đặt", CHECKED_IN "Đã check-in", IN_PROGRESS "Đang khám", DONE "Đã khám", CANCELLED "Đã
  hủy", NO_SHOW "Không đến"; kênh APP "App", GUEST "Web", AI "Trợ lý AI", WALK_IN "Tại quầy", STAFF "Nhân viên đặt".
  Màu Tag theo theme (không mã màu cứng).

## 6. Permission Model

| Hành động | ADMIN | RECEPTIONIST | Khác |
|---|---|---|---|
| `/api/reception/**` | ✓ | ✓ | 403 |
| Sửa slotMinutes chuyên khoa (`/api/admin/**`) | ✓ | 403 (URL) | 403 |
| Web `/admin/bookings` | ✓ | ✓ (chỉ trang này) | về `/` |

Không có ownership: lịch hẹn thuộc phòng khám, nhân viên được xem toàn bộ. `createdByUserId` lấy từ
`@AuthenticationPrincipal`, không nhận từ client.

## 7. Risks & Edge Cases

- **Giờ/múi giờ:** `start_at` = `LocalDate + LocalTime` tại `Asia/Ho_Chi_Minh` → `OffsetDateTime`; lọc ngày dùng
  `[from 00:00, to+1 00:00)` cùng múi. Test với `Clock` cố định.
- **Đua tạo ca khám:** `ON CONFLICT DO NOTHING` + `FOR UPDATE` cho hai request cùng lúc dùng chung một ca.
- **Đua đặt cùng slot:** unique index → 409; đã `saveAndFlush` nên lỗi nổ trong service, map được.
- **Admin đổi giờ work_shift sau khi ca khám đã có:** ca khám giữ giờ/slot cũ (lưới tính từ ca đã lưu) — khớp AC-18.
- **Gỡ phân ca khi đã có lịch (GĐ-10):** lịch vẫn hiện; lưới giờ báo `NOT_ON_SHIFT`, không đặt thêm.
- **Bác sĩ chưa có hồ sơ staff:** `doctorOptions` hiện có thể trả bác sĩ chưa có staff? → `bookingDoctor` trả
  `DOCTOR_UNAVAILABLE` rõ ràng thay vì lỗi FK.
- **Native query nhiều bảng:** viết `COUNT` riêng; tham số `q` escape `%`/`_`; chỉ đọc.
- **Hồ sơ đã gộp (`merged_into_id`)**: loại khỏi tìm theo SĐT và từ chối khi đặt.

## 8. Kiểm thử đề xuất

- Unit `AppointmentServiceTest` (Clock cố định, mock service module khác): lưới giờ (slot qua giờ, đã đặt, ca có sẵn
  slot khác khoa), đặt thành công (BN có sẵn / tạo mới), `SLOT_INVALID`, `TOO_MANY_PENDING`, `PATIENT_PHONE_MISMATCH`,
  `NOT_CANCELLABLE`, ngày ngoài khoảng.
- Integration (DB `qa_*`): migration V19 áp được; tạo ca lười + unique slot (đặt 2 lần cùng giờ → 409); native query
  danh sách lọc/tìm; 403 cho DOCTOR, 200 cho RECEPTIONIST.
- Web: typecheck + kiểm tay trên trình duyệt (admin và lễ tân).

## 9. Thay đổi 28/11 — đặt không chỉ định bác sĩ (AC-24…28)

- **`V20__appointment_unassigned.sql`** (V19 đã chạy trên DB dev nên không sửa): `appointment.doctor_id`, `shift_id`,
  `start_at` bỏ NOT NULL; thêm `visit_date DATE NOT NULL` (backfill từ `start_at` theo giờ VN), `work_shift_id` (FK
  work_shift, backfill từ shift), `specialty_id` (FK specialty, chuyên khoa khách chọn); CHECK ba cột bác sĩ/ca/giờ
  cùng NULL hoặc cùng có; index `visit_date`. FK ghép `(shift_id, doctor_id)` MATCH SIMPLE bỏ qua khi NULL; unique slot
  không ảnh hưởng (NULL khác nhau).
- **API:** `StaffBookingRequest.doctorUserId`, `startTime` không bắt buộc, thêm `specialtyId`; có bác sĩ thì bắt buộc
  `startTime` (`SLOT_REQUIRED`), `designated = true`. Không bác sĩ: kiểm ngày, buổi đang bật, buổi hôm nay chưa kết thúc
  (`SHIFT_ENDED`), chuyên khoa đang hoạt động (`SPECIALTY_INVALID`).
- **Danh sách:** lọc/sắp theo `visit_date`, giờ bắt đầu buổi, `start_at` (NULL cuối); join shift/staff/room thành LEFT
  JOIN; chuyên khoa = `COALESCE(khoa của phòng, appointment.specialty_id)`. `AppointmentRow` thêm `visitDate`,
  `workShiftName`, `workShiftStart/End`, `designated`; `startAt/endAt/doctor*` có thể null.
- **Web:** dialog thêm "Chuyên khoa" (không bắt buộc) lọc bác sĩ; "Bác sĩ (khách chỉ định)" allowClear; lưới giờ chỉ hiện
  khi có bác sĩ. Danh sách hiện buổi + "Chờ xếp bác sĩ" cho lịch chưa xếp.

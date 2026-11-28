# TECHNICAL DESIGN: Đặt bác sĩ từ hồ sơ bác sĩ (yêu cầu chờ xếp ca)

- **Spec:** `docs/tasks/doctor-request-ba-spec.md` (Approved)
- **Target Component/Layer:** Fullstack (backend module `booking`, frontend feature `landing`)

## Database Schema Changes

Migration `V16__doctor_request.sql` (bảng mới, không đụng `appointment`: appointment bắt buộc có `shift` cụ thể, yêu cầu
này chưa có ca).

```
doctor_request
  id              BIGINT IDENTITY PK
  doctor_user_id  BIGINT NOT NULL REFERENCES users(id)      -- users.id như PublicDoctor.userId (bác sĩ có thể chưa có staff)
  requested_date  DATE   NOT NULL
  work_shift_id   BIGINT NOT NULL REFERENCES work_shift(id) -- buổi (Sáng/Chiều/Tối)
  full_name       VARCHAR(120) NOT NULL
  phone           VARCHAR(15)  NOT NULL
  booker_user_id  BIGINT REFERENCES users(id)               -- NULL = khách
  status          VARCHAR(12) NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING','SCHEDULED','REJECTED','CANCELLED'))
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()

UNIQUE INDEX uq_doctor_request_pending ON (phone, doctor_user_id, requested_date, work_shift_id) WHERE status = 'PENDING'  -- AC-11, chống đua
INDEX idx_doctor_request_phone_status ON (phone, status)                                                                  -- đếm AC-12
INDEX idx_doctor_request_doctor_date  ON (doctor_user_id, requested_date)                                                -- màn xếp ca đợt sau
```

## Processing Flow

1. FE: `DoctorProfileDialog` → nút "Đặt bác sĩ này" → `DoctorRequestDialog` (component mới trong `landing`): tái dùng
   `DateStrip` + `SessionTiles` (tách ra khỏi `BookingWidget` thành file dùng chung trong `landing/components`), ca lấy
   từ query `['public','booking-shifts']` sẵn có. Đã đăng nhập bệnh nhân (`tokenStore` có token + `me.role === 'PATIENT'`)
   → ẩn ô họ tên/SĐT; khách → hiện 2 ô. Bấm Gửi → `modal.confirm` (AC-05) → `onOk` trả promise gọi API (AC-08) → màn
   thành công (AC-06).
   - `LandingLayout` đã biết người đăng nhập qua prop `account`; truyền `booker` xuống `TopDoctors` → `DoctorProfileDialog`
     → `DoctorRequestDialog` (không để landing import `auth`).
2. BE `DoctorRequestService.create(cmd)` trong 1 transaction:
   1. Kiểm bác sĩ: `ClinicCatalogService.publicDoctors()` chứa `doctorUserId` (tài khoản bật + hồ sơ chưa nghỉ) — sai →
      400 `DOCTOR_UNAVAILABLE` (AC-09).
   2. Kiểm ngày: `today(Asia/Ho_Chi_Minh) <= date <= today+6` — sai → 400 `DATE_OUT_OF_RANGE`; ca:
      `WorkScheduleService.activeShifts()` chứa `workShiftId` — sai → 400 `SHIFT_INVALID`; ngày = hôm nay và
      `endTime <= now` → 400 `SHIFT_ENDED` (AC-10).
   3. `pg_advisory_xact_lock(hashtext(phone))` (native query trong repository) để đếm + ghi không đua.
   4. Đã có PENDING cùng (phone, bác sĩ, ngày, ca) → 409 `REQUEST_EXISTS` (AC-11); đếm PENDING theo phone ≥ 3 → 409
      `TOO_MANY_PENDING` (AC-12). Unique index là lớp chặn cuối (DataIntegrityViolation → `REQUEST_EXISTS`).
   5. Lưu, trả `DoctorRequestResponse`.
3. Gọi chéo module qua service (`clinic.ClinicCatalogService`, `schedule.WorkScheduleService`, `auth.AuthService`), không
   dùng repository module khác.

## API Contract Overview

| Method | Path | Quyền | Body | Trả |
|---|---|---|---|---|
| POST | `/api/public/doctor-requests` | công khai (khách) | `{doctorUserId, date, workShiftId, fullName, phone}` | 201 `DoctorRequestResponse` |
| POST | `/api/doctor-requests` | `hasRole('PATIENT')` | `{doctorUserId, date, workShiftId}` — họ tên/SĐT lấy từ `AuthService.get(me.id)` | 201 `DoctorRequestResponse` |

- `DoctorRequestResponse {id, doctorUserId, doctorName, date, workShiftName, startTime, endTime, fullName, phone, status, createdAt}`.
- Validation (Bean Validation): `doctorUserId`, `date`, `workShiftId` `@NotNull`; khách: `fullName` `@NotBlank @Size(max=120)`,
  `phone` `@Pattern(^0\d{9}$)` (AC-13). Lỗi nghiệp vụ dạng `{code, message}` tiếng Việt qua `ApiException`.
- Tài khoản bệnh nhân không có SĐT → 400 `PHONE_REQUIRED` ("Tài khoản chưa có số điện thoại").

## Permission Model

- Khách: chỉ endpoint public, bắt buộc họ tên + SĐT.
- Bệnh nhân đã đăng nhập: endpoint `/api/doctor-requests` (`@PreAuthorize("hasRole('PATIENT')")`), không nhận họ tên/SĐT
  từ client (AC-14). Nhân viên gọi → 403.
- FE: người đã đăng nhập luôn đi endpoint có xác thực.
- Không có endpoint đọc / sửa yêu cầu ở đợt này (GĐ-03); dữ liệu chỉ admin xem qua DB.

## Risks & Edge Cases

- Đua hai request cùng phone: advisory lock theo phone + unique index partial.
- Múi giờ: ngày "hôm nay" theo `Asia/Ho_Chi_Minh` (`WorkScheduleService.CLINIC_ZONE`), không theo máy chủ.
- Ca bị tắt / bác sĩ nghỉ sau khi người dùng mở dialog: kiểm lại ở server (AC-09, AC-10).
- Spam từ khách: giới hạn theo SĐT (GĐ-01); chưa có rate-limit theo IP (ghi nhận, làm sau nếu cần).
- Tách `DateStrip`/`SessionTiles` khỏi `BookingWidget`: chỉ di chuyển, không đổi hành vi form trang chủ (hồi quy QA).

## Tests

- Unit `DoctorRequestServiceTest` (mock service khác + repository): AC-09, AC-10 (ngoài khoảng, ca tắt, ca đã hết hôm
  nay), AC-11, AC-12, luồng bệnh nhân lấy tên/SĐT từ tài khoản, `PHONE_REQUIRED`.
- Integration `DoctorRequestIntegrationTest` trên `qa_*`: migration V16, unique index chặn trùng, 403 với nhân viên ở
  endpoint có xác thực, 201 khách.

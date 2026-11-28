package vn.medischedule.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.auth.dto.AuthDtos.UserResponse;
import vn.medischedule.auth.service.AuthService;
import vn.medischedule.booking.domain.DoctorRequest;
import vn.medischedule.booking.dto.DoctorRequestDtos.DoctorRequestResponse;
import vn.medischedule.booking.dto.DoctorRequestDtos.GuestDoctorRequest;
import vn.medischedule.booking.dto.DoctorRequestDtos.PatientDoctorRequest;
import vn.medischedule.booking.repository.DoctorRequestRepository;
import vn.medischedule.clinic.dto.ClinicDtos.PublicDoctor;
import vn.medischedule.clinic.service.ClinicCatalogService;
import vn.medischedule.common.exception.ApiException;
import vn.medischedule.schedule.dto.ScheduleDtos.WorkShiftResponse;
import vn.medischedule.schedule.service.WorkScheduleService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Yêu cầu đặt đích danh bác sĩ (ngày + buổi), chờ nhân sự xếp ca. Chống spam theo SĐT:
 * không trùng (bác sĩ, ngày, buổi) khi còn chờ, tối đa {@link #MAX_PENDING} yêu cầu chờ.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DoctorRequestService {

    static final int MAX_PENDING = 3;
    /** Chọn được hôm nay → hôm nay + 6 (giống dải ngày của form đặt lịch). */
    static final int DAYS_AHEAD = 6;
    static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorRequestRepository requests;
    private final ClinicCatalogService catalog;
    private final WorkScheduleService schedule;
    private final AuthService auth;

    /** Đổi được trong test. */
    Clock clock = Clock.system(CLINIC_ZONE);

    public DoctorRequestResponse createForGuest(GuestDoctorRequest req) {
        return create(req.doctorUserId(), req.date(), req.workShiftId(), req.fullName().trim(), req.phone().trim(), null);
    }

    /** Họ tên / SĐT lấy từ tài khoản, không nhận từ client. */
    public DoctorRequestResponse createForPatient(Long userId, PatientDoctorRequest req) {
        UserResponse me = auth.get(userId);
        if (me.phone() == null || me.phone().isBlank()) {
            throw ApiException.badRequest("PHONE_REQUIRED", "Tài khoản chưa có số điện thoại, vui lòng cập nhật trước khi đặt");
        }
        return create(req.doctorUserId(), req.date(), req.workShiftId(), me.fullName(), me.phone(), userId);
    }

    private DoctorRequestResponse create(Long doctorUserId, LocalDate date, Long shiftId, String fullName, String phone,
                                         Long bookerUserId) {
        PublicDoctor doctor = catalog.publicDoctors().stream()
                .filter(d -> d.userId().equals(doctorUserId)).findFirst()
                .orElseThrow(() -> ApiException.badRequest("DOCTOR_UNAVAILABLE", "Bác sĩ hiện không nhận đặt lịch"));
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || date.isAfter(today.plusDays(DAYS_AHEAD))) {
            throw ApiException.badRequest("DATE_OUT_OF_RANGE", "Chỉ đặt được trong 7 ngày tới");
        }
        WorkShiftResponse shift = schedule.activeShifts().stream()
                .filter(s -> s.id().equals(shiftId)).findFirst()
                .orElseThrow(() -> ApiException.badRequest("SHIFT_INVALID", "Buổi khám không còn mở"));
        if (date.equals(today) && !shift.endTime().isAfter(LocalTime.now(clock))) {
            throw ApiException.badRequest("SHIFT_ENDED", "Buổi khám này của hôm nay đã kết thúc");
        }

        requests.lockPhone(phone);
        if (requests.existsByPhoneAndDoctorUserIdAndRequestedDateAndWorkShiftIdAndStatus(
                phone, doctorUserId, date, shiftId, DoctorRequest.Status.PENDING)) {
            throw duplicate();
        }
        if (requests.countByPhoneAndStatus(phone, DoctorRequest.Status.PENDING) >= MAX_PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "TOO_MANY_PENDING",
                    "Bạn đang có %d yêu cầu chờ xếp ca, vui lòng chờ phòng khám liên hệ".formatted(MAX_PENDING));
        }

        DoctorRequest r = new DoctorRequest();
        r.setDoctorUserId(doctorUserId);
        r.setRequestedDate(date);
        r.setWorkShiftId(shiftId);
        r.setFullName(fullName);
        r.setPhone(phone);
        r.setBookerUserId(bookerUserId);
        try {
            r = requests.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_doctor_request_pending")) {
                throw duplicate();
            }
            throw e;
        }
        return new DoctorRequestResponse(r.getId(), doctorUserId, doctor.fullName(), date, shift.name(), shift.startTime(),
                shift.endTime(), fullName, phone, r.getStatus().name(), r.getCreatedAt());
    }

    private static ApiException duplicate() {
        return new ApiException(HttpStatus.CONFLICT, "REQUEST_EXISTS", "Bạn đã gửi yêu cầu này, phòng khám sẽ liên hệ");
    }
}

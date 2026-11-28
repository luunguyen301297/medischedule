package vn.medischedule.patient.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.common.exception.ApiException;
import vn.medischedule.patient.domain.Patient;
import vn.medischedule.patient.dto.PatientDtos.PatientLite;
import vn.medischedule.patient.dto.PatientDtos.PatientProfile;
import vn.medischedule.patient.dto.PatientDtos.PatientRequest;
import vn.medischedule.patient.repository.PatientRepository;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientService {

    private static final Set<Short> BHYT_RATES = Set.of((short) 80, (short) 95, (short) 100);

    private final PatientRepository patients;

    /** Hồ sơ của tài khoản: hồ sơ bản thân (SELF) trước, rồi người thân theo thứ tự tạo. */
    @Transactional(readOnly = true)
    public List<PatientProfile> byOwner(Long userId) {
        return patients.findByOwnerUserIdAndMergedIntoIsNullOrderByIdAsc(userId).stream()
                .sorted(Comparator.comparing(p -> p.getRelation() != Patient.Relation.SELF))
                .map(PatientProfile::of)
                .toList();
    }

    /** Admin thêm hồ sơ cho tài khoản; mã BN sinh theo id (BN000123). */
    public PatientProfile create(PatientRequest req) {
        if (req.ownerUserId() == null) {
            throw ApiException.badRequest("OWNER_REQUIRED", "Thiếu tài khoản sở hữu hồ sơ");
        }
        Patient p = new Patient();
        p.setOwnerUserId(req.ownerUserId());
        p.setSource(Patient.Source.RECEPTION);
        // ponytail: mã tạm rồi đổi theo id sau khi insert, khỏi tự đếm max (đua nhau khi tạo đồng thời)
        p.setPatientCode("TMP" + UUID.randomUUID().toString().substring(0, 16));
        apply(p, req);
        save(p);
        p.setPatientCode("BN%06d".formatted(p.getId()));
        return PatientProfile.of(save(p));
    }

    /** Hồ sơ còn hiệu lực theo SĐT khớp chính xác (GĐ-09); SĐT sai định dạng thì trả rỗng. */
    @Transactional(readOnly = true)
    public List<PatientLite> findByPhone(String phone) {
        String p = phone == null ? "" : phone.trim();
        if (!p.matches("^0\\d{9}$")) {
            return List.of();
        }
        return patients.findTop20ByPhoneAndMergedIntoIsNullOrderByIdAsc(p).stream().map(PatientLite::of).toList();
    }

    /** Hồ sơ còn hiệu lực để gắn lịch hẹn; đã gộp hoặc không có → PATIENT_NOT_FOUND. */
    @Transactional(readOnly = true)
    public PatientLite requireActive(Long id) {
        return patients.findById(id)
                .filter(x -> x.getMergedInto() == null)
                .map(PatientLite::of)
                .orElseThrow(() -> ApiException.notFound("PATIENT_NOT_FOUND", "Hồ sơ bệnh nhân không tồn tại"));
    }

    /**
     * Hồ sơ cùng SĐT và cùng họ tên (không phân biệt hoa thường, khoảng trắng thừa) thì dùng lại; không có thì tạo mới.
     * Tránh hồ sơ trùng khi lễ tân gõ lại tên khách quen mà không bấm chọn hồ sơ có sẵn.
     */
    public PatientLite findOrCreateAtReception(String fullName, String phone, LocalDate dateOfBirth, String gender) {
        String name = normalizeName(fullName);
        return patients.findTop20ByPhoneAndMergedIntoIsNullOrderByIdAsc(phone.trim()).stream()
                .filter(p -> normalizeName(p.getFullName()).equals(name))
                .findFirst()
                .map(PatientLite::of)
                .orElseGet(() -> createAtReception(fullName, phone, dateOfBirth, gender));
    }

    static String normalizeName(String s) {
        return s == null ? "" : Normalizer.normalize(s, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ")
                .toLowerCase(Locale.forLanguageTag("vi"));
    }

    /** Lễ tân tạo hồ sơ cho khách chưa có tài khoản: chủ sở hữu NULL, nguồn RECEPTION (AC-12). */
    public PatientLite createAtReception(String fullName, String phone, LocalDate dateOfBirth, String gender) {
        Patient p = new Patient();
        p.setSource(Patient.Source.RECEPTION);
        p.setRelation(Patient.Relation.SELF);
        p.setPatientCode("TMP" + UUID.randomUUID().toString().substring(0, 16));
        p.setFullName(fullName.trim());
        p.setPhone(phone.trim());
        p.setDateOfBirth(dateOfBirth);
        p.setGender(blankToNull(gender));
        save(p);
        p.setPatientCode("BN%06d".formatted(p.getId()));
        return PatientLite.of(save(p));
    }

    public PatientProfile update(Long id, PatientRequest req) {
        Patient p = patients.findById(id)
                .filter(x -> x.getMergedInto() == null)
                .orElseThrow(() -> ApiException.notFound("PATIENT_NOT_FOUND", "Hồ sơ không tồn tại"));
        apply(p, req);
        return PatientProfile.of(save(p));
    }

    private static void apply(Patient p, PatientRequest r) {
        if (r.bhytRate() != null && !BHYT_RATES.contains(r.bhytRate())) {
            throw ApiException.badRequest("BHYT_RATE_INVALID", "Mức hưởng BHYT: 80, 95 hoặc 100");
        }
        p.setRelation(r.relation());
        p.setFullName(r.fullName().trim());
        p.setDateOfBirth(r.dateOfBirth());
        p.setGender(blankToNull(r.gender()));
        p.setNationalId(blankToNull(r.nationalId()));
        p.setPhone(r.phone().trim());
        p.setAddress(blankToNull(r.address()));
        p.setBhytNumber(blankToNull(r.bhytNumber()));
        p.setBhytValidTo(r.bhytValidTo());
        p.setBhytRate(r.bhytRate());
        p.setAllergyNote(blankToNull(r.allergyNote()));
        p.setAvatarUrl(blankToNull(r.avatarUrl()));
    }

    /** Index unique (CCCD, một hồ sơ SELF / tài khoản) chặn ở DB, đổi thành lỗi nghiệp vụ. */
    private Patient save(Patient p) {
        try {
            return patients.saveAndFlush(p);
        } catch (DataIntegrityViolationException e) {
            String msg = String.valueOf(e.getMostSpecificCause().getMessage());
            if (msg.contains("national_id")) {
                throw new ApiException(HttpStatus.CONFLICT, "NATIONAL_ID_EXISTS", "CCCD đã thuộc hồ sơ khác");
            }
            if (msg.contains("uq_patient_owner_self")) {
                throw new ApiException(HttpStatus.CONFLICT, "SELF_EXISTS", "Tài khoản đã có hồ sơ bản thân");
            }
            throw e;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

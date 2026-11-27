package vn.medischedule.patient.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.medischedule.common.exception.ApiException;
import vn.medischedule.patient.domain.Patient;
import vn.medischedule.patient.dto.PatientDtos.PatientProfile;
import vn.medischedule.patient.dto.PatientDtos.PatientRequest;
import vn.medischedule.patient.repository.PatientRepository;

import java.util.Comparator;
import java.util.List;
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

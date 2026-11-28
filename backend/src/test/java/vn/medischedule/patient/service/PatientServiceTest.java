package vn.medischedule.patient.service;

import org.junit.jupiter.api.Test;
import vn.medischedule.patient.domain.Patient;
import vn.medischedule.patient.repository.PatientRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit test (không Spring, không DB): lễ tân đặt hộ dùng lại hồ sơ trùng SĐT + họ tên. */
class PatientServiceTest {

    static final String PHONE = "0912000111";

    PatientRepository repo = mock(PatientRepository.class);
    PatientService service = new PatientService(repo);

    @Test
    void sameNameIgnoringCaseAndSpacesReusesProfile() {
        when(repo.findTop20ByPhoneAndMergedIntoIsNullOrderByIdAsc(PHONE))
                .thenReturn(List.of(patient(5L, "Trần Thị B"), patient(7L, "Nguyễn Văn A")));

        var res = service.findOrCreateAtReception("  nguyễn   văn a ", PHONE, null, null);

        assertThat(res.id()).isEqualTo(7L);
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void differentNameCreatesNewProfile() {
        when(repo.findTop20ByPhoneAndMergedIntoIsNullOrderByIdAsc(PHONE)).thenReturn(List.of(patient(5L, "Trần Thị B")));
        when(repo.saveAndFlush(any())).thenAnswer(i -> {
            Patient p = i.getArgument(0);
            if (p.getId() == null) p.setId(9L);
            return p;
        });

        var res = service.findOrCreateAtReception("Trần Thị C", PHONE, null, null);

        assertThat(res.id()).isEqualTo(9L);
        assertThat(res.patientCode()).isEqualTo("BN000009");
        assertThat(res.fullName()).isEqualTo("Trần Thị C");
    }

    private static Patient patient(Long id, String name) {
        Patient p = new Patient();
        p.setId(id);
        p.setFullName(name);
        p.setPhone(PHONE);
        p.setPatientCode("BN%06d".formatted(id));
        return p;
    }
}

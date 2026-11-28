package vn.medischedule.clinic.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.medischedule.clinic.dto.ClinicDtos.StaffProfileRequest;
import vn.medischedule.patient.domain.Patient;
import vn.medischedule.patient.dto.PatientDtos.PatientRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test (không Spring, không DB): ảnh hồ sơ chỉ nhận rỗng, https:// hoặc ảnh đã tải lên; chặn javascript:, data:. */
class AvatarUrlValidationTest {

    final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"", "/api/public/files/site/a.png", "https://images.example.com/a.jpg"})
    void acceptsAllowedUrls(String url) {
        assertThat(validator.validate(staff(url))).isEmpty();
        assertThat(validator.validate(patient(url))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "data:image/png;base64,AAAA", "http://x/a b.png", "/etc/passwd"})
    void rejectsOtherUrls(String url) {
        assertThat(validator.validate(staff(url))).extracting(v -> v.getPropertyPath().toString()).containsExactly("avatarUrl");
        assertThat(validator.validate(patient(url))).extracting(v -> v.getPropertyPath().toString()).containsExactly("avatarUrl");
    }

    @Test
    void nullIsAllowed() {
        assertThat(validator.validate(staff(null))).isEmpty();
    }

    private static StaffProfileRequest staff(String url) {
        return new StaffProfileRequest(null, null, null, false, true, url);
    }

    private static PatientRequest patient(String url) {
        return new PatientRequest(null, Patient.Relation.SELF, "Nguyễn A", null, null, null, "0900000000", null, null,
                null, null, null, url);
    }
}

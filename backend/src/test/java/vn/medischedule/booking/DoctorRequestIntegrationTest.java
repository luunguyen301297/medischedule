package vn.medischedule.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Tích hợp yêu cầu đặt bác sĩ qua HTTP trên DB qa_*. Không đặt @Transactional trên lớp. */
@SpringBootTest
@AutoConfigureMockMvc
class DoctorRequestIntegrationTest {

    private static final String DEMO_PASSWORD = "demo@123";
    private static final String PHONE = "0977000333";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    long doctorUserId;
    long shiftId;
    LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);

    @DynamicPropertySource
    static void qaDatabaseOnly(DynamicPropertyRegistry registry) {
        String url = System.getenv("DB_URL");
        if (url == null || System.getenv("SPRING_FLYWAY_URL") != null
                || !url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:\\d+)?/qa_\\w+(\\?.*)?")) {
            throw new IllegalStateException("Test tích hợp chỉ chạy trên DB qa_*. Dùng lệnh INTEGRATION_TEST_COMMAND "
                    + "trong .claude/PROJECT/stack-and-commands.md");
        }
        registry.add("spring.datasource.url", () -> url);
    }

    @BeforeEach
    void setUp() {
        cleanUp();
        // Bác sĩ demo "bacsi" (db/seed), ca đang bật đầu tiên
        doctorUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'bacsi'", Long.class);
        jdbc.update("UPDATE users SET enabled = TRUE WHERE id = ?", doctorUserId);
        jdbc.update("UPDATE staff SET active = TRUE WHERE user_id = ?", doctorUserId);
        shiftId = jdbc.queryForObject("SELECT id FROM work_shift WHERE active ORDER BY start_time LIMIT 1", Long.class);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM doctor_request WHERE phone IN (?, (SELECT phone FROM users WHERE username = 'benhnhan'))", PHONE);
    }

    @Test
    void guestCreatesPendingRequestAndDuplicateIsRejected() throws Exception {
        var res = send("/api/public/doctor-requests", null, guestBody(tomorrow));
        assertThat(res.getStatus()).isEqualTo(201);
        JsonNode body = json.readTree(res.getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.get("status").asText()).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM doctor_request WHERE phone = ? AND booker_user_id IS NULL",
                Integer.class, PHONE)).isEqualTo(1);

        var dup = send("/api/public/doctor-requests", null, guestBody(tomorrow));
        assertThat(dup.getStatus()).isEqualTo(409);
        assertThat(dup.getContentAsString(StandardCharsets.UTF_8)).contains("REQUEST_EXISTS");
    }

    @Test
    void fourthPendingRequestIsRejected() throws Exception {
        for (int i = 1; i <= 3; i++) {
            assertThat(send("/api/public/doctor-requests", null, guestBody(tomorrow.plusDays(i - 1))).getStatus()).isEqualTo(201);
        }
        var res = send("/api/public/doctor-requests", null, guestBody(tomorrow.plusDays(3)));
        assertThat(res.getStatus()).isEqualTo(409);
        assertThat(res.getContentAsString(StandardCharsets.UTF_8)).contains("TOO_MANY_PENDING");
    }

    @Test
    void invalidGuestPhoneIsFieldError() throws Exception {
        Map<String, Object> body = guestBody(tomorrow);
        body.put("phone", "12345");
        var res = send("/api/public/doctor-requests", null, body);
        assertThat(res.getStatus()).isEqualTo(400);
        assertThat(res.getContentAsString(StandardCharsets.UTF_8)).contains("phone");
    }

    @Test
    void patientUsesAccountInfoAndStaffIsForbidden() throws Exception {
        Map<String, Object> body = Map.of("doctorUserId", doctorUserId, "date", tomorrow.toString(), "workShiftId", shiftId);

        // Tài khoản chưa có SĐT thì không gửi được
        jdbc.update("UPDATE users SET phone = NULL WHERE username = 'benhnhan'");
        var noPhone = send("/api/doctor-requests", bearer("benhnhan"), body);
        assertThat(noPhone.getStatus()).isEqualTo(400);
        assertThat(noPhone.getContentAsString(StandardCharsets.UTF_8)).contains("PHONE_REQUIRED");

        jdbc.update("UPDATE users SET phone = '0966000444' WHERE username = 'benhnhan'");
        var res = send("/api/doctor-requests", bearer("benhnhan"), body);
        assertThat(res.getStatus()).isEqualTo(201);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT r.full_name, r.phone, r.booker_user_id FROM doctor_request r JOIN users u ON u.id = r.booker_user_id "
                        + "WHERE u.username = 'benhnhan'");
        assertThat(row.get("full_name")).isNotNull();
        assertThat(row.get("phone")).isEqualTo("0966000444");

        assertThat(send("/api/doctor-requests", bearer("letan"), body).getStatus()).isEqualTo(403);
        assertThat(send("/api/doctor-requests", null, body).getStatus()).isEqualTo(401);
    }

    private Map<String, Object> guestBody(LocalDate date) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("doctorUserId", doctorUserId);
        m.put("date", date.toString());
        m.put("workShiftId", shiftId);
        m.put("fullName", "Khách QA");
        m.put("phone", PHONE);
        return m;
    }

    private MockHttpServletResponse send(String path, String auth, Map<String, Object> body) throws Exception {
        var req = post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (auth != null) req.header("Authorization", auth);
        return mvc.perform(req).andReturn().getResponse();
    }

    private String bearer(String login) throws Exception {
        var res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", login, "password", DEMO_PASSWORD))))
                .andReturn().getResponse();
        assertThat(res.getStatus()).as("đăng nhập %s", login).isEqualTo(200);
        return "Bearer " + json.readTree(res.getContentAsString(StandardCharsets.UTF_8)).get("accessToken").asText();
    }
}

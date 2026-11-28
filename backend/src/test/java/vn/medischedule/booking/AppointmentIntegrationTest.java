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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Tích hợp menu Lịch hẹn (đặt hộ, lưới giờ, danh sách, hủy) qua HTTP trên DB qa_*. Không đặt @Transactional trên lớp. */
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentIntegrationTest {

    private static final String DEMO_PASSWORD = "demo@123";
    private static final String PHONE = "0977000555";
    /** Tên dễ nhận ra để dọn; id lớn hơn phòng cũ nên chỉ được chọn khi khoa chưa có phòng nào khác. */
    private static final String ROOM = "QA-APPT-01";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    long doctorUserId;
    long staffId;
    long workShiftId;
    String firstSlot;
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
        // Bác sĩ demo "bacsi" thuộc khoa đầu tiên có phòng đang hoạt động, trực ca đang bật đầu tiên ngày mai
        doctorUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'bacsi'", Long.class);
        jdbc.update("UPDATE users SET enabled = TRUE WHERE id = ?", doctorUserId);
        // Phòng không có trong migration (tạo qua giao diện): test tự tạo phòng riêng cho khoa đang hoạt động đầu tiên
        Long specialtyId = jdbc.queryForObject("SELECT id FROM specialty WHERE active ORDER BY id LIMIT 1", Long.class);
        jdbc.update("INSERT INTO room (name, specialty_id) VALUES (?, ?) ON CONFLICT DO NOTHING", ROOM, specialtyId);
        jdbc.update("UPDATE room SET active = TRUE, specialty_id = ? WHERE name = ?", specialtyId, ROOM);
        jdbc.update("INSERT INTO staff (user_id, full_name) SELECT id, full_name FROM users WHERE id = ? "
                + "ON CONFLICT (user_id) DO NOTHING", doctorUserId);
        jdbc.update("UPDATE staff SET active = TRUE, specialty_id = ? WHERE user_id = ?", specialtyId, doctorUserId);
        jdbc.update("UPDATE specialty SET slot_minutes = 15 WHERE id = ?", specialtyId);
        staffId = jdbc.queryForObject("SELECT id FROM staff WHERE user_id = ?", Long.class, doctorUserId);
        workShiftId = jdbc.queryForObject("SELECT id FROM work_shift WHERE active ORDER BY start_time LIMIT 1", Long.class);
        cleanUp();
        jdbc.update("INSERT INTO work_assignment (work_date, work_shift_id, user_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                tomorrow, workShiftId, doctorUserId);
        firstSlot = jdbc.queryForObject("SELECT to_char(start_time, 'HH24:MI') FROM work_shift WHERE id = ?",
                String.class, workShiftId);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM appointment WHERE booker_phone = ? OR doctor_id = ?", PHONE, staffId);
        jdbc.update("DELETE FROM patient WHERE phone = ?", PHONE);
        jdbc.update("DELETE FROM shift WHERE doctor_id = ? AND work_date >= ?", staffId, tomorrow.minusDays(1));
        jdbc.update("DELETE FROM work_assignment WHERE user_id = ? AND work_date = ?", doctorUserId, tomorrow);
    }

    @Test
    void receptionistBooksNewPatientThenSlotIsTakenAndListed() throws Exception {
        String letan = bearer("letan");

        var res = send(post("/api/reception/appointments"), letan, booking(firstSlot, null));
        assertThat(res.getStatus()).as(body(res)).isEqualTo(201);
        JsonNode row = json.readTree(body(res));
        assertThat(row.get("bookingCode").asText()).matches("^[A-HJ-NP-Z2-9]{8}$");
        assertThat(row.get("channel").asText()).isEqualTo("STAFF");
        assertThat(row.get("status").asText()).isEqualTo("BOOKED");
        assertThat(row.get("patientName").asText()).isEqualTo("Khách QA");
        assertThat(row.get("createdByName").asText()).isNotBlank();
        assertThat(row.get("roomName").asText()).isNotBlank();
        assertThat(row.get("startAt").asText()).contains("+07:00");

        // Ca khám được sinh lười, slot 15 phút
        assertThat(jdbc.queryForObject("SELECT slot_minutes FROM shift WHERE doctor_id = ? AND work_date = ? AND work_shift_id = ?",
                Integer.class, staffId, tomorrow, workShiftId)).isEqualTo(15);

        // Giờ đã đặt bị khóa trong lưới
        var grid = json.readTree(body(send(get("/api/reception/slots").param("date", tomorrow.toString())
                .param("workShiftId", String.valueOf(workShiftId)).param("doctorUserId", String.valueOf(doctorUserId)), letan, null)));
        assertThat(grid.get("slots").get(0).get("available").asBoolean()).isFalse();
        assertThat(grid.get("slots").get(1).get("available").asBoolean()).isTrue();

        // Đặt trùng giờ (hồ sơ có sẵn) → 409 SLOT_TAKEN
        long patientId = row.get("patientId").asLong();
        Map<String, Object> again = booking(firstSlot, patientId);
        var dup = send(post("/api/reception/appointments"), letan, again);
        assertThat(dup.getStatus()).isEqualTo(409);
        assertThat(body(dup)).contains("SLOT_TAKEN");

        // Tìm hồ sơ theo SĐT
        var patients = json.readTree(body(send(get("/api/reception/patients").param("phone", PHONE), letan, null)));
        assertThat(patients).hasSize(1);

        // Danh sách: lọc ngày, tìm theo mã lịch và theo tên
        var list = json.readTree(body(send(get("/api/reception/appointments").param("from", tomorrow.toString())
                .param("to", tomorrow.toString()).param("q", row.get("bookingCode").asText().toLowerCase()), letan, null)));
        assertThat(list.get("page").get("totalElements").asLong()).isEqualTo(1);
        var byName = json.readTree(body(send(get("/api/reception/appointments").param("from", tomorrow.toString())
                .param("q", "khách qa").param("status", "BOOKED"), letan, null)));
        assertThat(byName.get("content")).hasSize(1);

        // Hủy → slot trống lại, hủy lần hai bị từ chối
        long id = row.get("id").asLong();
        var cancel = send(post("/api/reception/appointments/" + id + "/cancel"), letan, Map.of("reason", "Khách bận"));
        assertThat(cancel.getStatus()).isEqualTo(200);
        assertThat(json.readTree(body(cancel)).get("status").asText()).isEqualTo("CANCELLED");
        assertThat(send(post("/api/reception/appointments/" + id + "/cancel"), letan, Map.of("reason", "x")).getStatus())
                .isEqualTo(409);
        assertThat(send(post("/api/reception/appointments"), letan, again).getStatus()).isEqualTo(201);
    }

    @Test
    void unassignedBookingIsListedByShiftWithoutDoctor() throws Exception {
        String letan = bearer("letan");
        Map<String, Object> body = booking(null, null);
        body.remove("doctorUserId");
        body.remove("startTime");
        var res = send(post("/api/reception/appointments"), letan, body);
        assertThat(res.getStatus()).as(body(res)).isEqualTo(201);
        JsonNode row = json.readTree(body(res));
        assertThat(row.get("doctorName").isNull()).isTrue();
        assertThat(row.get("startAt").isNull()).isTrue();
        assertThat(row.get("designated").asBoolean()).isFalse();
        assertThat(row.get("visitDate").asText()).isEqualTo(tomorrow.toString());
        assertThat(row.get("workShiftStart").asText()).isEqualTo(firstSlot);

        var list = json.readTree(body(send(get("/api/reception/appointments").param("from", tomorrow.toString()), letan, null)));
        assertThat(list.get("page").get("totalElements").asLong()).isEqualTo(1);

        // Có bác sĩ mà thiếu giờ → SLOT_REQUIRED
        Map<String, Object> noTime = booking(null, row.get("patientId").asLong());
        noTime.remove("startTime");
        assertThat(body(send(post("/api/reception/appointments"), letan, noTime))).contains("SLOT_REQUIRED");
    }

    @Test
    void thirdPendingForPhoneIsRejected() throws Exception {
        String admin = bearer("letan");
        var first = json.readTree(body(send(post("/api/reception/appointments"), admin, booking(firstSlot, null))));
        long patientId = first.get("patientId").asLong();
        String second = jdbc.queryForObject("SELECT to_char(start_time + interval '15 minutes', 'HH24:MI') FROM work_shift WHERE id = ?",
                String.class, workShiftId);
        String third = jdbc.queryForObject("SELECT to_char(start_time + interval '30 minutes', 'HH24:MI') FROM work_shift WHERE id = ?",
                String.class, workShiftId);
        assertThat(send(post("/api/reception/appointments"), admin, booking(second, patientId)).getStatus()).isEqualTo(201);
        var res = send(post("/api/reception/appointments"), admin, booking(third, patientId));
        assertThat(res.getStatus()).isEqualTo(409);
        assertThat(body(res)).contains("TOO_MANY_PENDING");
    }

    @Test
    void onlyAdminAndReceptionistCanUseReceptionApi() throws Exception {
        var list = get("/api/reception/appointments");
        assertThat(send(list, bearer("bacsi"), null).getStatus()).isEqualTo(403);
        assertThat(send(get("/api/reception/appointments"), bearer("benhnhan"), null).getStatus()).isEqualTo(403);
        assertThat(send(get("/api/reception/appointments"), null, null).getStatus()).isEqualTo(401);
        assertThat(send(get("/api/reception/appointments"), bearer("letan"), null).getStatus()).isEqualTo(200);
        // Lễ tân không vào được API quản trị khác
        assertThat(send(get("/api/admin/users"), bearer("letan"), null).getStatus()).isEqualTo(403);
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        String letan = bearer("letan");
        Map<String, Object> badPhone = booking(firstSlot, null);
        badPhone.put("phone", "12345");
        var res = send(post("/api/reception/appointments"), letan, badPhone);
        assertThat(res.getStatus()).isEqualTo(400);
        assertThat(body(res)).contains("phone");

        Map<String, Object> farDate = booking(firstSlot, null);
        farDate.put("date", tomorrow.plusDays(7).toString());
        assertThat(body(send(post("/api/reception/appointments"), letan, farDate))).contains("DATE_OUT_OF_RANGE");

        Map<String, Object> offGrid = booking(firstSlot.substring(0, 3) + "07", null);
        assertThat(body(send(post("/api/reception/appointments"), letan, offGrid))).contains("SLOT_INVALID");
    }

    private Map<String, Object> booking(String time, Long patientId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", tomorrow.toString());
        m.put("workShiftId", workShiftId);
        m.put("doctorUserId", doctorUserId);
        if (time != null) m.put("startTime", time);
        m.put("phone", PHONE);
        if (patientId != null) {
            m.put("patientId", patientId);
        } else {
            m.put("newPatient", Map.of("fullName", "Khách QA", "gender", "Nữ"));
        }
        return m;
    }

    private MockHttpServletResponse send(MockHttpServletRequestBuilder req, String auth, Map<String, Object> body) throws Exception {
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (auth != null) req.header("Authorization", auth);
        return mvc.perform(req).andReturn().getResponse();
    }

    private static String body(MockHttpServletResponse res) throws Exception {
        return res.getContentAsString(StandardCharsets.UTF_8);
    }

    private String bearer(String login) throws Exception {
        var res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", login, "password", DEMO_PASSWORD))))
                .andReturn().getResponse();
        assertThat(res.getStatus()).as("đăng nhập %s", login).isEqualTo(200);
        return "Bearer " + json.readTree(body(res)).get("accessToken").asText();
    }
}

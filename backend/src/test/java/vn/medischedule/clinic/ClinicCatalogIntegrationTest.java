package vn.medischedule.clinic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Tích hợp module clinic qua HTTP trên DB qa_*. Không đặt @Transactional trên lớp (qa-process, cổng mục 5).
 * Kịch bản: docs/tests/clinic-specialty-admin-test-cases.md.
 */
@SpringBootTest(properties = {
        "app.admin.username=qaadmin",
        "app.admin.email=qaadmin@test.local",
        "app.admin.password=" + ClinicCatalogIntegrationTest.ADMIN_PASSWORD
})
@AutoConfigureMockMvc
class ClinicCatalogIntegrationTest {

    static final String ADMIN_PASSWORD = "qa-admin@123";
    /** Mật khẩu tài khoản demo trong db/seed. */
    private static final String DEMO_PASSWORD = "demo@123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    /**
     * Chốt an toàn: context khởi động là Flyway chạy, nên dừng trước đó nếu DB đích không phải qa_* trên máy này.
     * Gán thẳng spring.datasource.url để biến SPRING_DATASOURCE_URL không đè được giá trị đã kiểm.
     */
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

    /** Đăng nhập qua HTTP, trả giá trị header Authorization. Tài khoản: qaadmin, bacsi, letan, duocsi, benhnhan. */
    String bearer(String login) throws Exception {
        String password = "qaadmin".equals(login) ? ADMIN_PASSWORD : DEMO_PASSWORD;
        var res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", login, "password", password))))
                .andReturn().getResponse();
        assertThat(res.getStatus()).as("đăng nhập %s", login).isEqualTo(200);
        JsonNode body = json.readTree(res.getContentAsString(StandardCharsets.UTF_8));
        return "Bearer " + body.get("accessToken").asText();
    }

    private static final List<String> SEED = List.of("Nội tổng quát", "Nhi", "Tai Mũi Họng", "Da liễu", "Sản phụ khoa",
            "Tim mạch", "Tiêu hóa", "Cơ xương khớp", "Mắt", "Răng Hàm Mặt");
    private static final String SPECIALTIES = "/api/admin/specialties";
    private static final String ROOMS = "/api/admin/rooms";

    private String admin;

    /** DB qa_* dùng lại giữa các lượt: đưa danh mục về đúng 10 chuyên khoa seed, đang hoạt động, không phòng. */
    @BeforeEach
    void resetCatalog() throws Exception {
        clean();
        admin = bearer("qaadmin");
    }

    @AfterEach
    void clean() {
        for (String table : List.of("shift", "service", "staff", "room")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("DELETE FROM specialty WHERE name NOT IN (" + "?,".repeat(SEED.size() - 1) + "?)", SEED.toArray());
        jdbc.update("UPDATE specialty SET active = TRUE");
    }

    // ---------- Quyền: TC-02, TC-03 ----------

    @Test
    void adminEndpointsRejectAnonymousAndOtherRoles() throws Exception {
        long sp = newSpecialty("QA Quyền");
        long room = newRoom("QA-Q1", sp);
        var calls = List.of(
                new Call(HttpMethod.GET, SPECIALTIES, null),
                new Call(HttpMethod.POST, SPECIALTIES, Map.of("name", "QA Lạ")),
                new Call(HttpMethod.PUT, SPECIALTIES + "/" + sp, Map.of("name", "QA Lạ")),
                new Call(HttpMethod.PATCH, SPECIALTIES + "/" + sp + "/active?value=false", null),
                new Call(HttpMethod.DELETE, SPECIALTIES + "/" + sp, null),
                new Call(HttpMethod.GET, SPECIALTIES + "/" + sp + "/rooms", null),
                new Call(HttpMethod.POST, ROOMS, roomBody("QA-Q2", sp)),
                new Call(HttpMethod.PUT, ROOMS + "/" + room, roomBody("QA-Q3", sp)),
                new Call(HttpMethod.PATCH, ROOMS + "/" + room + "/active?value=false", null),
                new Call(HttpMethod.DELETE, ROOMS + "/" + room, null));
        String before = snapshot();
        for (Call c : calls) {
            assertThat(send(null, c.method, c.url, c.body).status).as("không token %s %s", c.method, c.url).isEqualTo(401);
        }
        for (String user : List.of("bacsi", "letan", "duocsi", "benhnhan")) {
            String token = bearer(user);
            for (Call c : calls) {
                assertThat(send(token, c.method, c.url, c.body).status).as("%s %s %s", user, c.method, c.url).isEqualTo(403);
            }
        }
        assertThat(snapshot()).isEqualTo(before);
    }

    // ---------- Chuyên khoa: TC-04 → TC-13 ----------

    @Test
    void listShowsEverySpecialtyWithRoomCountSortedInVietnamese() throws Exception {
        long empty = newSpecialty("Đông y");
        long used = newSpecialty("QA Hai phòng");
        newRoom("QA-L1", used);
        long off = newRoom("QA-L2", used);
        ok(HttpMethod.PATCH, ROOMS + "/" + off + "/active?value=false", null);
        insertSharedRoom("QA-XN");
        ok(HttpMethod.PATCH, SPECIALTIES + "/" + empty + "/active?value=false", null);

        JsonNode list = ok(HttpMethod.GET, SPECIALTIES, null);
        assertThat(list).hasSize(SEED.size() + 2);
        assertThat(byId(list, empty).get("active").asBoolean()).isFalse();
        assertThat(byId(list, empty).get("roomCount").asLong()).isZero();
        assertThat(byId(list, used).get("roomCount").asLong()).isEqualTo(2);
        assertThat(byId(list, used).get("active").asBoolean()).isTrue();
        assertThat(names(list)).containsSubsequence("Da liễu", "Đông y", "Mắt")
                .containsSubsequence("Tiêu hóa", "Tim mạch");
    }

    @Test
    void createTrimsNameAndIsActive() throws Exception {
        Res res = send(admin, HttpMethod.POST, SPECIALTIES, Map.of("name", "  QA Thần kinh  "));
        assertThat(res.status).isEqualTo(201);
        assertThat(res.body.get("name").asText()).isEqualTo("QA Thần kinh");
        assertThat(res.body.get("active").asBoolean()).isTrue();
        assertThat(res.body.get("roomCount").asLong()).isZero();
        assertThat(names(ok(HttpMethod.GET, SPECIALTIES, null))).contains("QA Thần kinh");
    }

    @Test
    void specialtyNameIsValidated() throws Exception {
        long id = newSpecialty("QA Tên");
        String before = snapshot();
        for (String bad : List.of("", "   ", "a".repeat(101))) {
            assertInvalid(send(admin, HttpMethod.POST, SPECIALTIES, Map.of("name", bad)), "name");
            assertInvalid(send(admin, HttpMethod.PUT, SPECIALTIES + "/" + id, Map.of("name", bad)), "name");
        }
        assertThat(snapshot()).isEqualTo(before);
        assertThat(send(admin, HttpMethod.POST, SPECIALTIES, Map.of("name", "b".repeat(100))).status).isEqualTo(201);
        ok(HttpMethod.PUT, SPECIALTIES + "/" + id, Map.of("name", "c".repeat(100)));
    }

    @Test
    void specialtyNameIsUniqueIgnoringCaseAndSpaces() throws Exception {
        long id = newSpecialty("QA Khác");
        String before = snapshot();
        for (String dup : List.of("nhi", " Nhi ", "DA LIỄU", "RĂNG HÀM MẶT")) {
            assertError(send(admin, HttpMethod.POST, SPECIALTIES, Map.of("name", dup)),
                    409, "SPECIALTY_EXISTS", "Chuyên khoa đã tồn tại");
            assertError(send(admin, HttpMethod.PUT, SPECIALTIES + "/" + id, Map.of("name", dup)),
                    409, "SPECIALTY_EXISTS", "Chuyên khoa đã tồn tại");
        }
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void renameShowsEverywhereAndMayChangeOnlyCase() throws Exception {
        long id = newSpecialty("QA Cũ");
        newRoom("QA-R1", id);
        JsonNode renamed = ok(HttpMethod.PUT, SPECIALTIES + "/" + id, Map.of("name", " QA Mới "));
        assertThat(renamed.get("name").asText()).isEqualTo("QA Mới");
        assertThat(renamed.get("roomCount").asLong()).isEqualTo(1);
        assertThat(renamed.get("active").asBoolean()).isTrue();
        assertThat(names(ok(HttpMethod.GET, SPECIALTIES, null))).contains("QA Mới").doesNotContain("QA Cũ");
        assertThat(publicNames()).contains("QA Mới").doesNotContain("QA Cũ");
        assertThat(ok(HttpMethod.PUT, SPECIALTIES + "/" + id, Map.of("name", "qa mới")).get("name").asText())
                .isEqualTo("qa mới");
    }

    @Test
    void deactivationNeedsNoActiveRoomAndCanBeReverted() throws Exception {
        long id = newSpecialty("QA Ngừng");
        long room = newRoom("QA-N1", id);
        assertError(send(admin, HttpMethod.PATCH, SPECIALTIES + "/" + id + "/active?value=false", null),
                409, "SPECIALTY_HAS_ACTIVE_ROOMS", "Hãy ngừng hoặc chuyển hết phòng khám của chuyên khoa trước");
        assertThat(byId(ok(HttpMethod.GET, SPECIALTIES, null), id).get("active").asBoolean()).isTrue();

        ok(HttpMethod.PATCH, ROOMS + "/" + room + "/active?value=false", null);
        assertThat(ok(HttpMethod.PATCH, SPECIALTIES + "/" + id + "/active?value=false", null)
                .get("active").asBoolean()).isFalse();
        assertThat(ok(HttpMethod.PATCH, SPECIALTIES + "/" + id + "/active?value=true", null)
                .get("active").asBoolean()).isTrue();
        long noRoom = newSpecialty("QA Không phòng");
        assertThat(ok(HttpMethod.PATCH, SPECIALTIES + "/" + noRoom + "/active?value=false", null)
                .get("active").asBoolean()).isFalse();
    }

    @Test
    void unusedSpecialtyIsDeleted() throws Exception {
        long id = newSpecialty("QA Xóa");
        assertThat(send(admin, HttpMethod.DELETE, SPECIALTIES + "/" + id, null).status).isEqualTo(204);
        assertThat(names(ok(HttpMethod.GET, SPECIALTIES, null))).doesNotContain("QA Xóa");
        assertThat(count("specialty WHERE id = " + id)).isZero();
    }

    @Test
    void specialtyInUseCannotBeDeleted() throws Exception {
        long activeRoom = newSpecialty("QA Có phòng");
        newRoom("QA-U1", activeRoom);
        long inactiveRoom = newSpecialty("QA Phòng ngừng");
        long off = newRoom("QA-U2", inactiveRoom);
        ok(HttpMethod.PATCH, ROOMS + "/" + off + "/active?value=false", null);
        long hasDoctor = newSpecialty("QA Có bác sĩ");
        upsertDemoDoctor(hasDoctor);
        long hasService = newSpecialty("QA Có dịch vụ");
        insertService(hasService, "QA Khám");
        String before = snapshot();
        for (long id : List.of(activeRoom, inactiveRoom, hasDoctor, hasService)) {
            assertError(send(admin, HttpMethod.DELETE, SPECIALTIES + "/" + id, null),
                    409, "SPECIALTY_IN_USE", "Chuyên khoa đang được dùng, không thể xóa. Hãy ngừng hoạt động.");
        }
        assertThat(snapshot()).isEqualTo(before);
        assertThat(count("staff")).isEqualTo(1);
        assertThat(count("service")).isEqualTo(1);
    }

    @Test
    void unknownSpecialtyIsNotFound() throws Exception {
        String url = SPECIALTIES + "/999999";
        for (Call c : List.of(new Call(HttpMethod.PUT, url, Map.of("name", "QA X")),
                new Call(HttpMethod.PATCH, url + "/active?value=false", null),
                new Call(HttpMethod.DELETE, url, null),
                new Call(HttpMethod.GET, url + "/rooms", null))) {
            assertError(send(admin, c.method, c.url, c.body), 404, "SPECIALTY_NOT_FOUND", null);
        }
    }

    // ---------- Phòng khám: TC-14 → TC-24 ----------

    @Test
    void roomsAreListedPerSpecialtyAndCreatedActive() throws Exception {
        long a = newSpecialty("QA A");
        long b = newSpecialty("QA B");
        Res created = send(admin, HttpMethod.POST, ROOMS, roomBody("  QA102 ", a));
        assertThat(created.status).isEqualTo(201);
        assertThat(created.body.get("name").asText()).isEqualTo("QA102");
        assertThat(created.body.get("active").asBoolean()).isTrue();
        assertThat(created.body.get("specialtyId").asLong()).isEqualTo(a);
        long off = newRoom("QA101", a);
        newRoom("QA201", b);
        ok(HttpMethod.PATCH, ROOMS + "/" + off + "/active?value=false", null);

        JsonNode list = ok(HttpMethod.GET, SPECIALTIES + "/" + a + "/rooms", null);
        assertThat(names(list)).containsExactly("QA101", "QA102");
        assertThat(byId(list, off).get("active").asBoolean()).isFalse();
        assertThat(byId(list, off).get("specialtyId").asLong()).isEqualTo(a);
        assertThat(roomCount(a)).isEqualTo(2);
    }

    @Test
    void roomInputIsValidated() throws Exception {
        long a = newSpecialty("QA A");
        long room = newRoom("QA-V1", a);
        String before = snapshot();
        for (String bad : List.of("", "   ", "p".repeat(51))) {
            assertInvalid(send(admin, HttpMethod.POST, ROOMS, roomBody(bad, a)), "name");
            assertInvalid(send(admin, HttpMethod.PUT, ROOMS + "/" + room, roomBody(bad, a)), "name");
        }
        assertInvalid(send(admin, HttpMethod.POST, ROOMS, Map.of("name", "QA-V2")), "specialtyId");
        assertInvalid(send(admin, HttpMethod.PUT, ROOMS + "/" + room, Map.of("name", "QA-V2")), "specialtyId");
        assertThat(snapshot()).isEqualTo(before);
        newRoom("q".repeat(50), a);
        ok(HttpMethod.PUT, ROOMS + "/" + room, roomBody("r".repeat(50), a));
    }

    @Test
    void roomNameIsUniqueAcrossTheWholeClinic() throws Exception {
        long a = newSpecialty("QA A");
        long b = newSpecialty("QA B");
        long first = newRoom("QA-P101", a);
        long other = newRoom("QA-P202", b);
        insertSharedRoom("QA-XN1");
        String before = snapshot();
        for (String dup : List.of("qa-p101", " QA-P101 ", "qa-xn1")) {
            assertError(send(admin, HttpMethod.POST, ROOMS, roomBody(dup, b)), 409, "ROOM_EXISTS", "Tên phòng đã tồn tại");
            assertError(send(admin, HttpMethod.PUT, ROOMS + "/" + other, roomBody(dup, b)),
                    409, "ROOM_EXISTS", "Tên phòng đã tồn tại");
        }
        assertThat(snapshot()).isEqualTo(before);
        ok(HttpMethod.PUT, ROOMS + "/" + first, roomBody("QA-P101", a));
    }

    @Test
    void roomMovesToAnotherActiveSpecialty() throws Exception {
        long a = newSpecialty("QA A");
        long b = newSpecialty("QA B");
        long room = newRoom("QA-M1", a);
        assertThat(ok(HttpMethod.PUT, ROOMS + "/" + room, roomBody("QA-M1", b)).get("specialtyId").asLong()).isEqualTo(b);
        assertThat(ok(HttpMethod.GET, SPECIALTIES + "/" + a + "/rooms", null)).isEmpty();
        assertThat(names(ok(HttpMethod.GET, SPECIALTIES + "/" + b + "/rooms", null))).containsExactly("QA-M1");
        assertThat(roomCount(a)).isZero();
        assertThat(roomCount(b)).isEqualTo(1);
    }

    @Test
    void inactiveSpecialtyBlocksCreateMoveAndReactivate() throws Exception {
        long open = newSpecialty("QA Mở");
        long closed = newSpecialty("QA Đóng");
        long inside = newRoom("QA-C1", closed);
        ok(HttpMethod.PATCH, ROOMS + "/" + inside + "/active?value=false", null);
        ok(HttpMethod.PATCH, SPECIALTIES + "/" + closed + "/active?value=false", null);
        long activeRoom = newRoom("QA-O1", open);
        long inactiveRoom = newRoom("QA-O2", open);
        ok(HttpMethod.PATCH, ROOMS + "/" + inactiveRoom + "/active?value=false", null);
        String before = snapshot();
        for (Call c : List.of(new Call(HttpMethod.POST, ROOMS, roomBody("QA-C2", closed)),
                new Call(HttpMethod.PUT, ROOMS + "/" + activeRoom, roomBody("QA-O1", closed)),
                new Call(HttpMethod.PUT, ROOMS + "/" + inactiveRoom, roomBody("QA-O2", closed)),
                new Call(HttpMethod.PATCH, ROOMS + "/" + inside + "/active?value=true", null))) {
            assertError(send(admin, c.method, c.url, c.body), 409, "SPECIALTY_INACTIVE", "Hãy mở lại chuyên khoa trước");
        }
        assertThat(snapshot()).isEqualTo(before);

        // TC-20: đổi tên tại chỗ và chuyển ra ngoài vẫn được
        ok(HttpMethod.PUT, ROOMS + "/" + inside, roomBody("QA-C1b", closed));
        assertThat(ok(HttpMethod.PUT, ROOMS + "/" + inside, roomBody("QA-C1b", open))
                .get("specialtyId").asLong()).isEqualTo(open);
    }

    @Test
    void roomCanBeDeactivatedAndReactivated() throws Exception {
        long a = newSpecialty("QA A");
        long room = newRoom("QA-T1", a);
        String url = ROOMS + "/" + room + "/active?value=";
        assertThat(ok(HttpMethod.PATCH, url + false, null).get("active").asBoolean()).isFalse();
        assertThat(roomCount(a)).isEqualTo(1);
        assertThat(ok(HttpMethod.PATCH, url + true, null).get("active").asBoolean()).isTrue();
        assertThat(byId(ok(HttpMethod.GET, SPECIALTIES + "/" + a + "/rooms", null), room).get("active").asBoolean()).isTrue();
    }

    @Test
    void roomWithoutShiftIsDeleted() throws Exception {
        long a = newSpecialty("QA A");
        long active = newRoom("QA-D1", a);
        long inactive = newRoom("QA-D2", a);
        ok(HttpMethod.PATCH, ROOMS + "/" + inactive + "/active?value=false", null);
        for (long id : List.of(active, inactive)) {
            assertThat(send(admin, HttpMethod.DELETE, ROOMS + "/" + id, null).status).isEqualTo(204);
        }
        assertThat(ok(HttpMethod.GET, SPECIALTIES + "/" + a + "/rooms", null)).isEmpty();
        assertThat(roomCount(a)).isZero();
        assertThat(count("room")).isZero();
    }

    @Test
    void roomWithAnyShiftCannotBeDeleted() throws Exception {
        long a = newSpecialty("QA A");
        long cancelled = newRoom("QA-S1", a);
        long published = newRoom("QA-S2", a);
        long doctor = upsertDemoDoctor(null);
        insertShift(doctor, cancelled, "08:00", "CANCELLED");
        insertShift(doctor, published, "10:00", "PUBLISHED");
        String before = snapshot();
        for (long id : List.of(cancelled, published)) {
            assertError(send(admin, HttpMethod.DELETE, ROOMS + "/" + id, null),
                    409, "ROOM_IN_USE", "Phòng đã có ca trực, không thể xóa. Hãy ngừng hoạt động.");
        }
        assertThat(snapshot()).isEqualTo(before);
        assertThat(count("shift")).isEqualTo(2);
    }

    @Test
    void sharedRoomsAndUnknownIdsAreNotFound() throws Exception {
        long a = newSpecialty("QA A");
        long room = newRoom("QA-F1", a);
        long shared = insertSharedRoom("QA-XN2");
        String before = snapshot();
        for (long id : List.of(shared, 999999L)) {
            for (Call c : List.of(new Call(HttpMethod.PUT, ROOMS + "/" + id, roomBody("QA-F2", a)),
                    new Call(HttpMethod.PATCH, ROOMS + "/" + id + "/active?value=false", null),
                    new Call(HttpMethod.DELETE, ROOMS + "/" + id, null))) {
                assertError(send(admin, c.method, c.url, c.body), 404, "ROOM_NOT_FOUND", null);
            }
        }
        assertError(send(admin, HttpMethod.POST, ROOMS, roomBody("QA-F3", 999999L)), 404, "SPECIALTY_NOT_FOUND", null);
        assertError(send(admin, HttpMethod.PUT, ROOMS + "/" + room, roomBody("QA-F1", 999999L)),
                404, "SPECIALTY_NOT_FOUND", null);
        assertThat(snapshot()).isEqualTo(before);
    }

    // ---------- Migration, đồng thời: TC-25, TC-26 ----------

    @Test
    void migrationSeedsCatalogAndAddsCaseInsensitiveIndexes() {
        assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version = '8'", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForList("SELECT name FROM specialty WHERE active", String.class)).containsAll(SEED);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO specialty (name) VALUES ('NHI')"))
                .hasMessageContaining("uq_specialty_name_ci");
        insertSharedRoom("QA-IDX");
        assertThatThrownBy(() -> insertSharedRoom("qa-idx")).hasMessageContaining("uq_room_name_ci");
    }

    @Test
    void concurrentDuplicateNamesCreateOnlyOneRow() throws Exception {
        assertOneWinner(SPECIALTIES, Map.of("name", "QA Đua"), Map.of("name", "qa đua"), "SPECIALTY_EXISTS");
        assertThat(count("specialty WHERE lower(name) = 'qa đua'")).isEqualTo(1);
        long a = newSpecialty("QA A");
        assertOneWinner(ROOMS, roomBody("QA-RACE", a), roomBody("qa-race", a), "ROOM_EXISTS");
        assertThat(count("room WHERE lower(name) = 'qa-race'")).isEqualTo(1);
    }

    // ---------- Public: TC-27, TC-28 ----------

    @Test
    void publicListHasOnlyActiveSpecialtiesAndFollowsAdminChanges() throws Exception {
        long id = newSpecialty("Đông y");
        Res res = send(null, HttpMethod.GET, "/api/public/specialties", null);
        assertThat(res.status).isEqualTo(200);
        assertThat(names(res.body)).containsExactly("Cơ xương khớp", "Da liễu", "Đông y", "Mắt", "Nhi", "Nội tổng quát",
                "Răng Hàm Mặt", "Sản phụ khoa", "Tai Mũi Họng", "Tiêu hóa", "Tim mạch");
        for (JsonNode item : res.body) {
            assertThat(item.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "name");
        }

        ok(HttpMethod.PUT, SPECIALTIES + "/" + id, Map.of("name", "QA Y học cổ truyền"));
        assertThat(publicNames()).contains("QA Y học cổ truyền").doesNotContain("Đông y");
        ok(HttpMethod.PATCH, SPECIALTIES + "/" + id + "/active?value=false", null);
        assertThat(publicNames()).doesNotContain("QA Y học cổ truyền").hasSize(SEED.size());

        for (JsonNode s : ok(HttpMethod.GET, SPECIALTIES, null)) {
            ok(HttpMethod.PATCH, SPECIALTIES + "/" + s.get("id").asLong() + "/active?value=false", null);
        }
        assertThat(publicNames()).isEmpty();
    }

    // ---------- Helper HTTP ----------

    private record Call(HttpMethod method, String url, Object body) {}

    private record Res(int status, JsonNode body) {}

    private Res send(String auth, HttpMethod method, String url, Object body) throws Exception {
        var req = request(method, URI.create(url));
        if (auth != null) req.header("Authorization", auth);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var res = mvc.perform(req).andReturn().getResponse();
        String text = res.getContentAsString(StandardCharsets.UTF_8);
        return new Res(res.getStatus(), text.isEmpty() ? null : json.readTree(text));
    }

    /** Gọi bằng admin, bắt buộc 200. */
    private JsonNode ok(HttpMethod method, String url, Object body) throws Exception {
        Res res = send(admin, method, url, body);
        assertThat(res.status).as("%s %s → %s", method, url, res.body).isEqualTo(200);
        return res.body;
    }

    private long create(String url, Object body) throws Exception {
        Res res = send(admin, HttpMethod.POST, url, body);
        assertThat(res.status).as("POST %s → %s", url, res.body).isEqualTo(201);
        return res.body.get("id").asLong();
    }

    private long newSpecialty(String name) throws Exception {
        return create(SPECIALTIES, Map.of("name", name));
    }

    private long newRoom(String name, long specialtyId) throws Exception {
        return create(ROOMS, roomBody(name, specialtyId));
    }

    private static Map<String, Object> roomBody(String name, long specialtyId) {
        return Map.of("name", name, "specialtyId", specialtyId);
    }

    private static void assertError(Res res, int status, String code, String message) {
        assertThat(res.status).as("%s", res.body).isEqualTo(status);
        assertThat(res.body.get("code").asText()).isEqualTo(code);
        if (message != null) assertThat(res.body.get("message").asText()).isEqualTo(message);
    }

    private static void assertInvalid(Res res, String field) {
        assertThat(res.status).as("%s", res.body).isEqualTo(400);
        assertThat(res.body.path("fields").path(field).asText()).isNotBlank();
    }

    private void assertOneWinner(String url, Object first, Object second, String code) throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var futures = Stream.of(first, second).map(body -> pool.submit(() -> {
                start.await();
                return send(admin, HttpMethod.POST, url, body);
            })).toList();
            start.countDown();
            Res one = futures.get(0).get(30, TimeUnit.SECONDS);
            Res two = futures.get(1).get(30, TimeUnit.SECONDS);
            assertThat(List.of(one.status, two.status)).containsExactlyInAnyOrder(201, 409);
            assertError(one.status == 409 ? one : two, 409, code, null);
        } finally {
            pool.shutdownNow();
        }
    }

    private static JsonNode byId(JsonNode list, long id) {
        for (JsonNode n : list) {
            if (n.get("id").asLong() == id) return n;
        }
        throw new AssertionError("Không thấy id " + id + " trong " + list);
    }

    private static List<String> names(JsonNode list) {
        var out = new ArrayList<String>();
        list.forEach(n -> out.add(n.get("name").asText()));
        return out;
    }

    private List<String> publicNames() throws Exception {
        return names(send(null, HttpMethod.GET, "/api/public/specialties", null).body);
    }

    private long roomCount(long specialtyId) throws Exception {
        return byId(ok(HttpMethod.GET, SPECIALTIES, null), specialtyId).get("roomCount").asLong();
    }

    private long count(String fromWhere) {
        return jdbc.queryForObject("SELECT count(*) FROM " + fromWhere, Long.class);
    }

    /** Toàn bộ hai bảng danh mục, để kiểm "dữ liệu không đổi". */
    private String snapshot() {
        return jdbc.queryForList("SELECT id, name, active FROM specialty ORDER BY id").toString()
                + jdbc.queryForList("SELECT id, name, specialty_id, active FROM room ORDER BY id");
    }

    // ---- SQL cho dữ liệu chưa có API tạo (bác sĩ, dịch vụ, ca trực, phòng dùng chung) ----

    long insertSharedRoom(String name) {
        return jdbc.queryForObject("INSERT INTO room (name) VALUES (?) RETURNING id", Long.class, name);
    }

    long insertService(long specialtyId, String name) {
        return jdbc.queryForObject(
                "INSERT INTO service (name, type, specialty_id, price) VALUES (?, 'CONSULT', ?, 0) RETURNING id",
                Long.class, name, specialtyId);
    }

    /** Gắn tài khoản demo bacsi làm bác sĩ; specialtyId null = bác sĩ chỉ dùng cho ca trực. Gọi lại thì cập nhật. */
    long upsertDemoDoctor(Long specialtyId) {
        return jdbc.queryForObject("""
                INSERT INTO staff (user_id, full_name, specialty_id)
                SELECT id, full_name, ? FROM users WHERE username = 'bacsi'
                ON CONFLICT (user_id) DO UPDATE SET specialty_id = EXCLUDED.specialty_id
                RETURNING id
                """, Long.class, specialtyId);
    }

    long insertShift(long doctorId, long roomId, String startTime, String status) {
        return jdbc.queryForObject("""
                INSERT INTO shift (doctor_id, room_id, work_date, start_time, end_time, slot_minutes, max_patients, status)
                VALUES (?, ?, CURRENT_DATE, ?::time, ?::time + interval '1 hour', 15, 4, ?) RETURNING id
                """, Long.class, doctorId, roomId, startTime, startTime, status);
    }
}

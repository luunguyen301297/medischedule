# KỊCH BẢN TEST: Menu Chuyên khoa (quản lý chuyên khoa và phòng khám)

- **Task:** clinic-specialty-admin
- **BA spec:** [`docs/tasks/clinic-specialty-admin-ba-spec.md`](../tasks/clinic-specialty-admin-ba-spec.md) (24 AC)
- **Design:** [`docs/design/clinic-specialty-admin-tech-design.md`](../design/clinic-specialty-admin-tech-design.md)
- **Trạng thái:** đã chạy (2026-11-27). **Verdict: PASSED** ở HEAD `813ab3e` (lượt 2). Lượt 1 FAILED vì lỗi #1, đã sửa

## 1. Phạm vi và quy ước

- **Test:** module `clinic` (API admin, API public, migration V8), trang `/admin/specialties`, menu Cài đặt, form đặt
  lịch trang chủ, tab Đặt lịch của "Thiết lập trang chủ". Không chạy hồi quy toàn bộ.
- **Unit:** dùng `ClinicCatalogServiceTest` của dev (design §8), không viết lại. Sau khi nhận diff, QA đối chiếu và chỉ
  bổ sung nhánh dev chưa phủ vào chính file đó.
- **Integration:** `backend/src/test/java/vn/medischedule/clinic/ClinicCatalogIntegrationTest.java` (dev có file tích hợp
  thì viết thêm vào file đó). `@SpringBootTest` + `@AutoConfigureMockMvc`, gọi qua HTTP, token lấy từ
  `POST /api/auth/login`, không đặt `@Transactional` trên lớp. DB `qa_clinic-specialty-admin` (tên do `qa_db.py url`
  sinh).
- **Chốt an toàn:** đây là test tích hợp đầu tiên của repo. Lớp test tự dừng trước khi nạp context nếu `DB_URL` không
  trỏ DB `qa_*`, để Flyway không chạy lên DB dev.
- Mỗi lỗi nghiệp vụ kiểm đủ: HTTP status, `code`, thông báo (khi AC nêu thông báo) và **dữ liệu không đổi**.
- Dữ liệu chuyên khoa/phòng tạo bằng API ở các bước trước của luồng. Riêng `staff`, `service`, `shift` và phòng dùng
  chung chưa có API tạo nên chèn bằng SQL (`JdbcTemplate`). Tên dữ liệu test không đụng 10 chuyên khoa seed.

## 2. Test Cases

### 2.1 Quyền và menu

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-01 | (UI) Đăng nhập admin → mở nhóm Cài đặt → bấm "Chuyên khoa" | Mục nằm ngay dưới "Trang chủ"; mở `/admin/specialties`, mục menu được tô chọn, nhóm Cài đặt đang mở; F5 vẫn ở trang này |
| TC-02 | Không token gọi cả 10 endpoint `/api/admin/specialties…`, `/api/admin/rooms…` | 401; số dòng và nội dung `specialty`, `room` không đổi |
| TC-03 | Token DOCTOR, RECEPTIONIST, PHARMACIST, PATIENT gọi cả 10 endpoint (body hợp lệ, id có thật) | 403; dữ liệu không đổi |

### 2.2 Chuyên khoa

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-04 | `GET /api/admin/specialties` khi có chuyên khoa đang ngừng, chuyên khoa 0 phòng, chuyên khoa có 1 phòng hoạt động + 1 phòng ngừng | Đủ mọi chuyên khoa; đúng `name`, `active`; `roomCount` = 0 và 2 (tính cả phòng ngừng, không tính phòng dùng chung); sắp theo tên tiếng Việt |
| TC-05 | `POST` tên `"  QA Thần kinh  "` | 201; tên lưu đã trim, `active=true`, `roomCount=0`; có trong `GET` ngay sau đó |
| TC-06 | `POST` và `PUT` với tên `""`, `"   "`, 101 ký tự; biên đúng 100 ký tự | Ba giá trị đầu: 400, `fields.name` có thông báo, không tạo/không đổi. 100 ký tự: thành công |
| TC-07 | Đã có "Nhi": `POST` `"nhi"`, `" Nhi "`; `PUT` chuyên khoa khác thành `"NHI"`; tên có dấu viết hoa (`"DA LIỄU"` khi đã có "Da liễu") | 409 `SPECIALTY_EXISTS` "Chuyên khoa đã tồn tại"; không lưu |
| TC-08 | `PUT` đổi tên hợp lệ; `PUT` chỉ đổi hoa/thường tên của chính nó | 200; tên mới ở `GET` admin và `GET /api/public/specialties`; không bị coi là trùng chính nó; giữ `active`, `roomCount` |
| TC-09 | Chuyên khoa còn 1 phòng hoạt động → `PATCH active?value=false` | 409 `SPECIALTY_HAS_ACTIVE_ROOMS`, thông báo yêu cầu ngừng/chuyển hết phòng; vẫn `active=true` |
| TC-10 | Ngừng hết phòng (hoặc chuyển đi) → ngừng chuyên khoa → mở lại; thêm chuyên khoa 0 phòng ngừng được | 200; `active` đổi đúng chiều |
| TC-11 | Xóa chuyên khoa mới tạo, không gắn gì | 204; không còn trong `GET` và DB. (UI: có Popconfirm, hủy thì không xóa) |
| TC-12 | Xóa chuyên khoa có (a) phòng hoạt động, (b) chỉ phòng ngừng, (c) bác sĩ, (d) dịch vụ — mỗi trường hợp một chuyên khoa | 409 `SPECIALTY_IN_USE`, thông báo gợi ý ngừng hoạt động; chuyên khoa và bản ghi liên quan còn nguyên |
| TC-13 | `PUT`, `PATCH`, `DELETE`, `GET …/rooms` với id chuyên khoa không có | 404 `SPECIALTY_NOT_FOUND`, không 500 |

### 2.3 Phòng khám

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-14 | `GET /api/admin/specialties/{id}/rooms` với hai chuyên khoa, có phòng đang ngừng | Chỉ phòng của chuyên khoa đó, gồm cả đang ngừng, đúng `name`, `active`, `specialtyId`, sắp theo tên; không 500 do lazy-load |
| TC-15 | `POST /api/admin/rooms` tên `"  QA101 "` vào chuyên khoa đang hoạt động | 201; tên trim, `active=true`, đúng `specialtyId`; `roomCount` tăng 1 |
| TC-16 | `POST`/`PUT` phòng tên `""`, `"   "`, 51 ký tự; biên 50 ký tự; thiếu `specialtyId` | Sai → 400 (`fields.name` / `fields.specialtyId`), không lưu; 50 ký tự → thành công |
| TC-17 | Có "QA-P101" ở chuyên khoa A: tạo `"qa-p101"`, `" QA-P101 "` ở chuyên khoa B; đổi tên phòng khác thành nó; có phòng dùng chung "QA-XN1" → tạo/đổi thành `"qa-xn1"`; `PUT` giữ nguyên tên của chính phòng | Trùng → 409 `ROOM_EXISTS` "Tên phòng đã tồn tại", không lưu; giữ tên chính nó → 200 |
| TC-18 | `PUT` chuyển phòng từ A sang B đang hoạt động | 200, `specialtyId`=B; mất khỏi danh sách A, có ở B; `roomCount` hai bên cập nhật |
| TC-19 | Chuyên khoa C đang ngừng: (a) tạo phòng vào C; (b) chuyển phòng đang hoạt động vào C; (c) chuyển phòng **đang ngừng** vào C (TL action 4); (d) mở lại phòng thuộc C | Cả bốn: 409 `SPECIALTY_INACTIVE` "Hãy mở lại chuyên khoa trước"; phòng giữ nguyên chuyên khoa và trạng thái; không tạo dòng mới |
| TC-20 | Phòng thuộc chuyên khoa đang ngừng: đổi tên (không đổi chuyên khoa); chuyển sang chuyên khoa đang hoạt động | Cả hai thành công (design §3 chỉ chặn tạo / chuyển vào / mở lại) |
| TC-21 | Ngừng phòng rồi mở lại (chuyên khoa đang hoạt động) | 200; `active` false → true; `roomCount` không đổi |
| TC-22 | Xóa phòng chưa có ca trực (cả phòng đang ngừng) | 204; mất khỏi danh sách và DB; `roomCount` giảm |
| TC-23 | Xóa phòng có ca trực `CANCELLED` và phòng có ca `PUBLISHED` | 409 `ROOM_IN_USE`, gợi ý ngừng hoạt động; phòng và ca trực còn nguyên |
| TC-24 | Phòng dùng chung (`specialty_id` NULL): `PUT`, `PATCH active`, `DELETE`; id phòng không có; `POST`/`PUT` với `specialtyId` không có | Phòng dùng chung và id không có → 404 `ROOM_NOT_FOUND`, dòng DB không đổi; `specialtyId` không có → 404 `SPECIALTY_NOT_FOUND` |

### 2.4 Migration và đồng thời

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-25 | Flyway chạy trên DB `qa_*` rỗng, app khởi động (`ddl-auto: validate`) | V8 `success`; đủ 10 chuyên khoa của GĐ-01, đều `active` (kiểm theo tên, không theo tổng số dòng); `INSERT` SQL trực tiếp tên chỉ khác hoa/thường vào `specialty` và `room` bị index chặn |
| TC-26 | Race: 2 request `POST` cùng lúc, tên chỉ khác hoa/thường — một lượt cho chuyên khoa, một lượt cho phòng (mỗi loại chỉ chạy 1 lượt) | Mỗi lượt đúng một 201, một 409 `*_EXISTS`; không 500; DB chỉ có 1 dòng |

Ghi chú TC-26: hai request có thể chạy lần lượt (khi đó chỉ đi qua nhánh `exists…`). Nhánh bắt
`DataIntegrityViolationException` được kiểm chắc chắn bằng unit test (dev, hoặc QA bổ sung nếu thiếu).

### 2.5 Trang chủ

| ID | Kịch bản | Kết quả mong đợi |
|---|---|---|
| TC-27 | `GET /api/public/specialties` không token, có chuyên khoa đang ngừng và chuyên khoa "QA Đông y" | 200; chỉ chuyên khoa hoạt động; mỗi phần tử chỉ có `id`, `name`; thứ tự theo tiếng Việt ("Đông y" sau "Da liễu", trước "Mắt"; "Tiêu hóa" trước "Tim mạch") |
| TC-28 | Qua API admin: thêm → đổi tên → ngừng một chuyên khoa, sau mỗi bước gọi lại API public; ngừng hết → gọi public (dữ liệu khôi phục sau test) | Public phản ánh ngay từng bước; ngừng hết → `[]` |
| TC-29 | (UI) Mở trang chủ ở chế độ khách → mở ô chọn chuyên khoa → chọn 1 mục → điền đủ → Đặt lịch khám | Danh sách đúng bằng chuyên khoa hoạt động, A→Z, không có mục đang ngừng; tên đã chọn hiện trên nút và màn hình xác nhận; "Đặt thêm lịch khác" reset form |
| TC-30 | (UI) Admin → Thiết lập trang chủ → tab Đặt lịch đầu trang; bấm link; quay lại sửa dòng ghi chú rồi Lưu, mở trang chủ, trả lại giá trị cũ | Không còn ô gõ chuyên khoa; có dòng giải thích + link mở `/admin/specialties`; lưu thành công, các trường khác (tiêu đề, cam kết, số liệu, ghi chú) giữ nguyên và hiện đúng ở trang chủ |
| TC-31 | (UI, DB dev) Ghi lại chuyên khoa đang hoạt động → ngừng hết (ngừng phòng trước nếu có) → mở trang chủ ở chế độ khách → mở ô chọn, bấm Đặt lịch khám → mở lại đúng các mục đã ghi | Hiện "Chưa có chuyên khoa"; nút gửi bị khóa, không có màn hình "Đã gửi yêu cầu"; sau khôi phục danh sách như cũ |

### 2.6 Trang quản trị (UI)

| ID | Steps | Kết quả mong đợi |
|---|---|---|
| TC-32 | Vào `/admin/specialties`; tạo chuyên khoa "QA UI"; đổi tên; thử tên trống và tên trùng "nhi" | Danh sách có tên, trạng thái, số phòng; thêm/đổi tên hiện ngay không cần F5; lỗi trống ở ô tên, lỗi trùng hiện "Chuyên khoa đã tồn tại" |
| TC-33 | Chọn "QA UI" → thêm phòng → sửa tên → chuyển sang chuyên khoa khác (Select) → chuyển về | Cột phải đúng phòng của chuyên khoa đang chọn; Select chỉ có chuyên khoa hoạt động; phòng rời/hiện đúng danh sách; số phòng cập nhật ngay |
| TC-34 | Ngừng "QA UI" khi còn phòng hoạt động → ngừng phòng → ngừng chuyên khoa → thử mở lại phòng / thêm phòng | Lần đầu báo lỗi, Switch giữ trạng thái cũ; sau đó có Tag "Đang ngừng"; nút Thêm phòng và Switch mở lại phòng bị tắt |
| TC-35 | Mở lại chuyên khoa; xóa chuyên khoa còn phòng; xóa phòng (Popconfirm: hủy, rồi đồng ý); xóa chuyên khoa | Xóa khi còn phòng báo lỗi, dữ liệu giữ nguyên; hủy không xóa; đồng ý thì mất khỏi danh sách; cột phải không còn giữ phòng của chuyên khoa vừa xóa, không lỗi console |

## 2b. Ánh xạ AC → tầng test

Viết tắt: **IT** = `ClinicCatalogIntegrationTest` · **UT** = `ClinicCatalogServiceTest` (dev).

| AC | Tầng test | File spec | TC liên quan |
|---|---|---|---|
| AC-01 | Thủ công | — | TC-01 |
| AC-02 | Integration | IT | TC-02, TC-03 |
| AC-03 | Integration | IT | TC-04, TC-32 |
| AC-04 | Integration | IT | TC-05, TC-32 |
| AC-05 | Integration | IT | TC-06, TC-32 |
| AC-06 | Integration | IT + UT | TC-07, TC-25, TC-26 |
| AC-07 | Integration | IT | TC-08, TC-32 |
| AC-08 | Integration | IT + UT | TC-09, TC-34 |
| AC-09 | Integration | IT | TC-10, TC-34 |
| AC-10 | Integration | IT | TC-11, TC-35 |
| AC-11 | Integration | IT + UT | TC-12, TC-35 |
| AC-12 | Integration | IT | TC-13, TC-14, TC-33 |
| AC-13 | Integration | IT | TC-15, TC-33 |
| AC-14 | Integration | IT | TC-16 |
| AC-15 | Integration | IT + UT | TC-17, TC-24, TC-25, TC-26 |
| AC-16 | Integration | IT | TC-18, TC-33 |
| AC-17 | Integration | IT + UT | TC-19, TC-20, TC-34 |
| AC-18 | Integration | IT | TC-21 |
| AC-19 | Integration | IT | TC-22, TC-35 |
| AC-20 | Integration | IT + UT | TC-23 |
| AC-21 | Integration | IT | TC-25, TC-27, TC-29 |
| AC-22 | Integration | IT | TC-28, TC-29 |
| AC-23 | Thủ công | — | TC-30 |
| AC-24 | Thủ công | — (API trả `[]` kiểm ở TC-28) | TC-31 |

## 2e. Endpoint → integration test qua HTTP (cổng mục 5)

| Endpoint | TC |
|---|---|
| `GET /api/admin/specialties` | TC-04 |
| `POST /api/admin/specialties` | TC-05–07, TC-26 |
| `PUT /api/admin/specialties/{id}` | TC-06–08 |
| `PATCH /api/admin/specialties/{id}/active` | TC-09, TC-10 |
| `DELETE /api/admin/specialties/{id}` | TC-11, TC-12 |
| `GET /api/admin/specialties/{id}/rooms` | TC-14 |
| `POST /api/admin/rooms` | TC-15–17, TC-19 |
| `PUT /api/admin/rooms/{id}` | TC-17–20 |
| `PATCH /api/admin/rooms/{id}/active` | TC-19, TC-21 |
| `DELETE /api/admin/rooms/{id}` | TC-22, TC-23 |
| `GET /api/public/specialties` | TC-27, TC-28 |

Luồng: tạo qua `POST` → sửa / bật tắt / chuyển / xóa chính bản ghi đó bằng các request tiếp theo, đọc lại bằng `GET`.

## 2c. Output thật của test tự động

Lệnh (chạy từ gốc repo, HEAD `9d88e78`):

```bash
(set -a; . ./.env; set +a; export DB_URL="$(python .claude/scripts/qa_db.py url clinic-specialty-admin)"; cd backend && ./mvnw -q test -Dtest=ClinicCatalogServiceTest,ClinicCatalogIntegrationTest)
```

`DB=jdbc:postgresql://localhost:5435/qa_clinic_specialty_admin`. Lượt đầu Flyway áp 9 migration lên DB rỗng.

```
Test set: vn.medischedule.clinic.ClinicCatalogIntegrationTest
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 29.96 s -- in vn.medischedule.clinic.ClinicCatalogIntegrationTest
Test set: vn.medischedule.clinic.service.ClinicCatalogServiceTest
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.659 s -- in vn.medischedule.clinic.service.ClinicCatalogServiceTest
```

`npm --prefix web run typecheck` (HEAD `05a9a36`): `tsc --noEmit` không báo lỗi. Commit `05a9a36` chỉ đổi web nên không
chạy lại JUnit.

Unit test của dev đã phủ các nhánh ở design §3, kể cả nhánh `DataIntegrityViolationException`; QA không bổ sung.

## 2d. Chạy thật

App dev đang chạy sẵn (không do QA khởi động): web `:5175`, backend `:8085`. `.env` trỏ
`localhost:5435/medischedule`; DB này có V8 và có kết nối JDBC đang mở. Tài khoản: phiên admin đăng nhập sẵn trong
trình duyệt; khách dùng origin `http://[::1]:5175`. Khung trình duyệt ở trạng thái ẩn nên ảnh chụp có lúc trễ so với
DOM; kết quả dưới đây lấy theo DOM.

| TC (AC) | Kết quả | Ghi chú |
|---|---|---|
| TC-01 (01) | Đạt | Mục nằm dưới "Trang chủ"; tải lại trang vẫn ở `/admin/specialties`, mục được tô chọn |
| TC-29 (21, 22) | Đạt | Ngừng "Mắt" → khách thấy 9 mục A→Z, không có "Mắt"; chọn "Nhi", gửi, màn hình xác nhận đúng; đặt thêm thì reset |
| TC-30 (23) | Đạt | Không còn ô gõ; link mở đúng trang; lưu rồi trả lại dòng ghi chú. So JSON trước/sau: chỉ mất `booking.specialties` |
| TC-31 (24) | Đạt | "Chưa có chuyên khoa", ô chọn và nút gửi bị khóa. Ngừng/khôi phục bằng SQL (chỉ cột `specialty.active`), không qua UI |
| TC-32 (03–05, 07) | Đạt | Tên trống và `" nhi "` báo lỗi tại ô; thêm, đổi tên hiện không cần F5 |
| TC-33 (12, 13, 16) | Đạt, có lỗi #1 | Thêm phòng, chuyển sang "Nhi" rồi chuyển về, số phòng đúng. Chưa thử đổi tên phòng và Select khi có chuyên khoa ngừng qua UI |
| TC-34 (08, 09, 17) | Đạt, có lỗi #1 | Ngừng khi còn phòng: báo đúng, Switch giữ nguyên. Sau đó có "Đang ngừng", cảnh báo, nút Thêm phòng bị tắt. Switch mở lại phòng bị tắt, nhưng lúc đo trùng với lỗi #1 nên phần này dựa vào code (`SpecialtyPage.tsx:100`) |
| TC-36 (ngoài AC, `767a415`) | Đạt | Playwright chặn `/api/public/specialties` (HTTP 500 và hủy request): ô chọn hiện "Không tải được danh sách chuyên khoa, thử lại sau", ô chọn và nút gửi bị khóa. Bỏ chặn thì đủ 10 mục |
| TC-35 (10, 11, 19) | Đạt, có lỗi #1 | Xóa khi còn phòng bị chặn đúng thông báo; Hủy không xóa; xóa phòng, xóa chuyên khoa được; cột phải chuyển về mục đầu. Console chỉ có 409 dự kiến |

Dữ liệu dev sau khi kiểm: 10/10 chuyên khoa và 10/10 phòng hoạt động, không còn dữ liệu "QA". Trong lúc kiểm, DB dev
được thêm phòng P101–P110 từ ngoài phiên QA.

Chưa kiểm trên trình duyệt (cả hai lượt): đổi tên phòng, và Select khi sửa phòng lúc có chuyên khoa đang ngừng (rename
đã có IT; Select dựa vào code).

### Lượt 2 (HEAD `813ab3e`)

- **JUnit:** cùng lệnh ở mục 2c; Flyway chạy lại `R__seed dev data` trên DB `qa_clinic_specialty_admin`.
  `ClinicCatalogIntegrationTest`: Tests run: 22, Failures: 0, Errors: 0. `ClinicCatalogServiceTest`: Tests run: 15,
  Failures: 0, Errors: 0. Phòng mẫu không ảnh hưởng IT vì mỗi ca xóa hết phòng trước khi chạy. `typecheck` sạch.
- **Seed:** trên DB qa (0 phòng, trong transaction rồi `ROLLBACK`), ngừng "Mắt" rồi chạy khối 3 của `R__`: chèn 9
  phòng, không có P109.
- **Lỗi #1:** đã sửa. Với chuyên khoa "QA Fix", Switch chỉ khóa khoảng 50–90 ms; giá trị mới (hoặc dòng bị xóa) hiện
  ngay khi toast vừa xuất hiện.
- **Thanh cuộn (`05a9a36`):** chuyên khoa 0 phòng không có thanh cuộn ngang.
- **TC-33–35, đo lại phần liên quan lỗi #1** (HEAD `813ab3e` + commit tài liệu, dữ liệu "QA Fix2"): bật tắt bị chặn
  (409), ngừng phòng, ngừng chuyên khoa, xóa chuyên khoa bị chặn, xóa phòng, xóa chuyên khoa. Lần nào Switch cũng chỉ
  khóa 30–60 ms, mở ngay khi toast hiện. Chuyên khoa đang ngừng: Switch mở lại phòng và nút Thêm phòng bị tắt (đã xác
  nhận trên UI). Chuyên khoa 0 phòng (mới tạo và sau khi xóa phòng cuối) không có thanh cuộn ngang. Bước thêm và
  chuyển phòng (TC-33) đi đường lưu, `813ab3e` không đổi; lần này chỉ đi lại bước thêm.
- **TC-29, TC-31:** kết quả ở bảng trên chạy tại `9d88e78`, đã gồm `767a415`; từ đó `features/landing` không đổi nên
  không chạy lại. TC-31 chạy trước khi biết dev DB có phòng mẫu: chỉ ngừng cột `specialty.active` bằng SQL rồi bật
  lại, không đi bước ngừng phòng. Bước chặn ngừng chuyên khoa còn phòng đã kiểm ở TC-34 và TC-09.
- **Dữ liệu dev:** 10/10 chuyên khoa, 10/10 phòng P101–P110 hoạt động, không còn dữ liệu "QA".

## 3. Regression

| Chỗ bị ảnh hưởng | Cách kiểm |
|---|---|
| Form đặt lịch trang chủ (đổi nguồn danh sách) | TC-29, TC-31 |
| Thiết lập trang chủ (bỏ trường `booking.specialties`; JSON cũ trong `site_content` và seed `R__` còn trường này) | TC-30 |
| Menu và route khu admin | Chạy thật: mở lại Trang chủ, trang nhân sự sau khi thêm route |
| Entity `specialty`, `room` khớp schema | IT nạp context với `ddl-auto: validate` |
| Kiểu `BookingContent` / `LIMITS` bị bỏ trường | `npm run typecheck` |

Test chọn theo diff: `ClinicCatalogServiceTest`, `ClinicCatalogIntegrationTest` (diff backend chỉ chạm module
`clinic` và seed dev). Mở lại trang nhân sự và bệnh nhân sau khi thêm route: hiển thị bình thường.

## 4. Tự chấm cổng kịch bản

| Mục | Kết quả | Bằng chứng |
|---|---|---|
| 1. Mọi AC có đúng một dòng ánh xạ | Đạt | Mục 2b: AC-01 → AC-24, 24 dòng |
| 2. AC logic backend/race/migration/quyền không chỉ Thủ công | Đạt | 21 AC Integration (quyền TC-02/03, race TC-26, migration TC-25). 3 AC Thủ công (01, 23, 24) thuần UI |
| 3. AC Thủ công có steps + expected | Đạt | TC-01, TC-30, TC-31 |
| 4. Không còn AC mơ hồ | Đạt | Không có câu hỏi mở. TC-20 theo design §3 (spec không cấm đổi tên / chuyển ra) |
| 5. Endpoint mới có integration test qua HTTP theo luồng thật | Đạt (theo kế hoạch) | Mục 2e: đủ 11 endpoint. Chỉ `staff`/`service`/`shift`/phòng dùng chung chèn SQL vì chưa có API |

## 5. Open Issues

- [x] **#1 (đã sửa ở `813ab3e`, kiểm lại ở lượt 2):** `web/src/features/admin/specialty/SpecialtyPage.tsx:39-40`. `onSuccess` / `onError` của
  `action` trả về `message.success(...)` / `message.error(...)`. Giá trị này là thenable chỉ resolve khi toast đóng, và
  React Query chờ nó trước khi chạy `onSettled`. Hậu quả: sau mỗi lần bật tắt hoặc xóa, dữ liệu chưa làm mới và mọi
  Switch bị khóa khoảng 3 giây. Đo sau khi mở lại chuyên khoa: toast hiện ở 2641 ms, Switch vẫn `checked=false`,
  `disabled=true` tới 5716 ms. Khi khung trình duyệt ẩn thì khóa lâu hơn (quan sát trên 8 giây). Ảnh hưởng TC-33–35
  ("cập nhật ngay"). Hướng sửa: bọc thân hai callback trong `{ }`.

Ghi nhận thêm, không tính vào verdict:

- Hộp thoại "Sửa phòng khám" khi đang đóng đổi tiêu đề thành "Thêm chuyên khoa" trong chốc lát (`editing` về `null`
  trước khi hết hiệu ứng, `SpecialtyPage.tsx:77-79, 202`).
- Sau khi xóa một chuyên khoa, trang gọi thêm một lần `GET …/specialties/{id}/rooms` của chuyên khoa vừa xóa và nhận
  404 (thấy trong console), rồi chuyển sang mục đầu. Không còn dữ liệu sai trên màn hình.
- Seed dev: lỗi chèn phòng vào chuyên khoa đang ngừng đã sửa ở `813ab3e`. Còn lại: phòng mẫu đã xóa hoặc đổi tên được
  tạo lại mỗi khi `R__` chạy lại. Chỉ ảnh hưởng môi trường dev.

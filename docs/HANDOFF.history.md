
---

### C-nho — Tách màn tài khoản nhân viên / bệnh nhân (`split-admin-accounts`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-25).

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| dev | 17p | 0p | 17p |
| **Cộng** | 17p | 0p | 17p |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ⬜ | docs/tasks/split-admin-accounts-ba-spec.md |
| Tech Design | ⬜ | docs/design/split-admin-accounts-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ⬜ | docs/reviews/design-review-split-admin-accounts.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/split-admin-accounts-api-spec.md (khi needs_api_detail) |
| DB Migration | ⬜ | backend/src/main/resources/db/migration/… (khi needs_db_detail) |
| UX Spec | ⬜ | docs/design/split-admin-accounts-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/split-admin-accounts-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

---

### B — Migration CSDL theo thiết kế đã duyệt (29 bảng) (`db-core-schema`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-26).

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| dev | 1g00 | 0p | 1g00 |
| **Cộng** | 1g00 | 0p | 1g00 |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ⬜ | docs/tasks/db-core-schema-ba-spec.md |
| Tech Design | ⬜ | docs/design/db-core-schema-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ⬜ | docs/reviews/design-review-db-core-schema.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/db-core-schema-api-spec.md (khi needs_api_detail) |
| DB Migration | ✅ | backend/src/main/resources/db/migration/V5__users_google_login_and_reset.sql, V6__clinic_core_schema.sql |
| UX Spec | ⬜ | docs/design/db-core-schema-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/db-core-schema-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

- dev: viết V5 (users: password_hash nullable, google_sub, reset_*, fcm_token, CHECK) + V6 (26 bảng theo thiết kế bản 2); User.passwordHash bỏ nullable=false. `mvnw compile` OK. CHƯA chạy migration trên Postgres (Docker tắt). Chưa commit.
- test: Flyway áp V1–V6 + seed lên qa_db_core_schema OK, app khởi động (ddl validate OK). 29 bảng; smoke SQL: CHECK login users, unique slot, walk-in qua, FK ghép shift/doctor, CHECK ai_analysis đều đúng. Đã drop DB qa.

---

### B — Entity JPA cho 26 bảng V6 + bổ sung User (`jpa-entities`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-26).

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| dev | 2p | 0p | 2p |
| code-review | 1p | 0p | 1p |
| **Cộng** | 4p | 0p | 4p |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ⬜ | docs/tasks/jpa-entities-ba-spec.md |
| Tech Design | ⬜ | docs/design/jpa-entities-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ⬜ | docs/reviews/design-review-jpa-entities.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/jpa-entities-api-spec.md (khi needs_api_detail) |
| DB Migration | ⬜ | backend/src/main/resources/db/migration/… (khi needs_db_detail) |
| UX Spec | ⬜ | docs/design/jpa-entities-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/jpa-entities-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

- dev: 26 entity mới (clinic, patient, schedule, booking, emr, pharmacy, billing, aftercare, commission, notification, system, ai) + User thêm googleSub/resetTokenHash/resetExpiresAt/fcmToken. Cùng module @ManyToOne LAZY, khác module id Long. compile OK; app khởi động trên qa_jpa_entities (ddl validate OK); so cột DB↔entity 29/29 bảng khớp đủ. Đã drop DB qa. Chưa commit.

---

### B — Javadoc cho mọi bảng và cột trong entity (`entity-docs`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-26).

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| dev | 2p | 0p | 2p |
| **Cộng** | 2p | 0p | 2p |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ⬜ | docs/tasks/entity-docs-ba-spec.md |
| Tech Design | ⬜ | docs/design/entity-docs-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ⬜ | docs/reviews/design-review-entity-docs.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/entity-docs-api-spec.md (khi needs_api_detail) |
| DB Migration | ⬜ | backend/src/main/resources/db/migration/… (khi needs_db_detail) |
| UX Spec | ⬜ | docs/design/entity-docs-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/entity-docs-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

- dev: Javadoc 1 dòng cho mọi field của 29 entity (245 dòng), không đổi code. compile OK. Chưa commit.

---

### B — Menu admin 2 cấp: Cài đặt > Trang chủ (`admin-menu-settings`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-27).

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| dev | 23p | 0p | 23p |
| **Cộng** | 23p | 0p | 23p |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ⬜ | docs/tasks/admin-menu-settings-ba-spec.md |
| Tech Design | ⬜ | docs/design/admin-menu-settings-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ⬜ | docs/reviews/design-review-admin-menu-settings.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/admin-menu-settings-api-spec.md (khi needs_api_detail) |
| DB Migration | ⬜ | backend/src/main/resources/db/migration/… (khi needs_db_detail) |
| UX Spec | ⬜ | docs/design/admin-menu-settings-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/admin-menu-settings-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

---

### C-nho — Đăng nhập bằng số điện thoại (`login-phone`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-27).

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| dev | 12p | 0p | 12p |
| **Cộng** | 12p | 0p | 12p |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ⬜ | docs/tasks/login-phone-ba-spec.md |
| Tech Design | ⬜ | docs/design/login-phone-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ⬜ | docs/reviews/design-review-login-phone.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/login-phone-api-spec.md (khi needs_api_detail) |
| DB Migration | ⬜ | backend/src/main/resources/db/migration/… (khi needs_db_detail) |
| UX Spec | ⬜ | docs/design/login-phone-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/login-phone-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

---

### Full — Admin: menu Chuyên khoa quản lý chuyên khoa và phòng khám (`clinic-specialty-admin`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-27).

Menu Cài đặt > Chuyên khoa: CRUD chuyên khoa + phòng khám (admin), GET /api/public/specialties cho form đặt lịch trang chủ, V8 index lower(name) + seed 10 chuyên khoa, seed dev phòng P101–P110. QA: 37/37 JUnit, chạy thật đạt.

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| ba | 3p | 0p | 3p |
| design | 1p | 0p | 1p |
| tl-review | 0p | 0p | 0p |
| dev | 4p | 0p | 4p |
| code-review | 1p | 0p | 1p |
| qa-run | 14p | 0p | 14p |
| **Cộng** | 24p | 0p | 24p |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ✅ | docs/tasks/clinic-specialty-admin-ba-spec.md |
| Tech Design | ✅ | docs/design/clinic-specialty-admin-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ✅ | docs/reviews/design-review-clinic-specialty-admin.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/clinic-specialty-admin-api-spec.md (khi needs_api_detail) |
| DB Migration | ⬜ | backend/src/main/resources/db/migration/… (khi needs_db_detail) |
| UX Spec | ⬜ | docs/design/clinic-specialty-admin-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/clinic-specialty-admin-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)
- code-review SỬA DONE: commit 767a415 (lỗi tải chuyên khoa hiện thông báo riêng, vẫn khóa gửi); `npm run typecheck` sạch; chưa kiểm trên trình duyệt.
- dev DONE: commit e414aa7; ClinicCatalogServiceTest 15/15 PASS; `npm run typecheck` sạch. Chưa kiểm trên trình duyệt, V8 chưa chạy trên DB nào. `.claude/PROJECT/architecture.md` ngoài phạm vi nên không sửa.

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

- ba: spec duyệt; user chọn xóa-nếu-chưa-dùng, chặn ngừng CK còn phòng, bỏ phòng dùng chung, nối trang chủ luôn.
- design/tl-review: APPROVED, action items ở docs/reviews/design-review-clinic-specialty-admin.md. Nhánh feat/clinic-specialty-admin.

---

### C — Chọn bác sĩ cho chuyên khoa, hiện tên khi chỉ có 1 bác sĩ (`specialty-doctor`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-27).

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| ba | 1p | 0p | 1p |
| tl-review | 0p | 0p | 0p |
| dev | 4p | 0p | 4p |
| code-review | 0p | 0p | 0p |
| qa-run | 11p | 0p | 11p |
| **Cộng** | 18p | 0p | 18p |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ✅ | docs/tasks/specialty-doctor-ba-spec.md |
| Tech Design | ⬜ | docs/design/specialty-doctor-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ✅ | docs/reviews/design-review-specialty-doctor.md |
| API Spec | ⬜ | docs/api/specialty-doctor-api-spec.md (khi needs_api_detail) |
| DB Migration | ✅ | backend/src/main/resources/db/migration/V9__assign_and_mock_specialty_doctors.sql (dữ liệu mẫu) |
| UX Spec | ⬜ | docs/design/specialty-doctor-ux-spec.md (không bắt buộc) |
| Test cases | ✅ | docs/tests/specialty-doctor-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

- 2026-11-27 · ba/tl-review: spec duyệt (17 AC, GĐ-01…05); TL APPROVED, không migration, action items ở design-review.
- 2026-11-27 · dev: BE (`AuthService.activeDoctors`, `StaffRepository`, `GET /api/admin/doctors`, `doctorUserIds` /
  `singleRoomName` / `singleDoctorName`) + FE (ô Bác sĩ, dòng phụ). Typecheck sạch. Chưa commit.
- 2026-11-27 · code-review: LGTM (0 Critical/Major), đã áp nit `doctorOptions`.
- 2026-11-27 · qa-run: tự động xanh trên `qa_specialty_doctor` (IT 32, UT 18, AuthServiceTest 4); DB tạm đã drop.
  **Còn lại: user tự kiểm giao diện thủ công** theo mục "Kiểm thủ công (UI)" trong
  `docs/tests/specialty-doctor-test-cases.md` (AC-01, AC-13, dòng phụ AC-09/10/11/16/17) — cần restart backend :8085.

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

- 2026-11-27 · dev (thêm theo yêu cầu user): V9 dữ liệu mẫu — gán bác sĩ chưa có khoa vào khoa trống, tạo bác sĩ mẫu
  (`bs.mau<id>@medischedule.test`, mật khẩu ngẫu nhiên không đăng nhập được) cho khoa còn trống. Thử trên bản chép DB
  dev: 10/10 khoa có bác sĩ (3 thật + 7 mẫu), chạy lại không đổi; IT/UT xanh qua Flyway. DB dev 5435 nhận V9 khi
  restart backend.

- [ ] Kiểm UI thủ công (user làm): đạt → commit + đóng task; lỗi → ghi lại đây, quay về `dev`.

---

### Full — Ca làm việc + Lịch làm việc nhân viên (`work-shift`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-27).

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| ba | 2p | 0p | 2p |
| design | 1p | 0p | 1p |
| tl-review | 0p | 0p | 0p |
| dev | 7p | 0p | 7p |
| code-review | 0p | 0p | 0p |
| qa-plan | 1g23 | 0p | 1g23 |
| **Cộng** | 1g35 | 0p | 1g35 |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ⬜ | docs/tasks/work-shift-ba-spec.md |
| Tech Design | ⬜ | docs/design/work-shift-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ⬜ | docs/reviews/design-review-work-shift.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/work-shift-api-spec.md (khi needs_api_detail) |
| DB Migration | ⬜ | backend/src/main/resources/db/migration/… (khi needs_db_detail) |
| UX Spec | ⬜ | docs/design/work-shift-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/work-shift-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

---

### B — Danh sách bác sĩ theo chuyên khoa ở trang Chuyên khoa (`specialty-doctor-list`)

**Phase**: `idle` ✅ ĐÓNG (2026-11-27).

**Thời gian**:

| Phase | Tổng | Chờ user | Làm việc |
|---|---|---|---|
| dev | 2p | 0p | 2p |
| **Cộng** | 2p | 0p | 2p |

## Artifacts Registry (⬜ chưa có · ✅ đã có — tác giả artifact đổi khi ghi xong, điền đường dẫn)

| Artifact | Trạng thái | Đường dẫn |
|---|---|---|
| BA Spec | ⬜ | docs/tasks/specialty-doctor-list-ba-spec.md |
| Tech Design | ⬜ | docs/design/specialty-doctor-list-tech-design.md (Full / G nhiều hướng fix) |
| TL Review | ⬜ | docs/reviews/design-review-specialty-doctor-list.md (chỉ khi có action item; không thì ghi "inline") |
| API Spec | ⬜ | docs/api/specialty-doctor-list-api-spec.md (khi needs_api_detail) |
| DB Migration | ⬜ | backend/src/main/resources/db/migration/… (khi needs_db_detail) |
| UX Spec | ⬜ | docs/design/specialty-doctor-list-ux-spec.md (không bắt buộc) |
| Test cases | ⬜ | docs/tests/specialty-doctor-list-test-cases.md |

## Báo cáo (phiên con append cuối mỗi mốc: phase, việc xong, commit, test)

## Câu hỏi mở / Lệch spec (phiên con ghi vào đây VÀ báo điều phối — chưa giải quyết thì không đi tiếp)

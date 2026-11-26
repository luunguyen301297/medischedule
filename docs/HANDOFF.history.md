
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

# Architecture

> Đọc khi: thiết kế feature mới, cần hiểu routing/luồng request giữa layer.
> Bỏ qua nếu chỉ sửa 1 hàm nội bộ không ảnh hưởng luồng giữa các layer.

Nguồn sự thật về cấu trúc thư mục và quy ước code: `AGENTS.md` (mục "Cấu trúc", "Quy ước backend", "Quy ước web"). File
này chỉ ghi luồng request và những điểm agent hay làm sai.

## Overview

```
web/ (React 18 + React Router 6 + Ant Design 5 + TanStack Query)      mobile/ (Flutter, chưa khởi tạo)
              |
              | HTTP (axios `http`, baseURL `/api`; Vite proxy /api → :8085)
              v
   backend/ Spring Boot 3 (MediScheduleApplication, :8085) — modular monolith vn.medischedule.<module>
              |                         |                         |
              v                         v                         v
   PostgreSQL 16 (:5435)          Redis 7 (:6390)           MinIO (:9010, bucket medical-files)
              ^
              | REST (AI_SERVICE_URL, module vn.medischedule.ai)
   ai-service/ (Python FastAPI + PyTorch, chưa khởi tạo) — đọc ảnh MinIO qua presigned URL
```

Frontend gọi API qua `web/src/shared/api/http.ts` (`http`: gắn JWT, 401 → về `/login`; `errorMessage`, `errorCode`,
`fieldErrors` đọc body lỗi) → module API của từng khu `web/src/features/<khu>/api.ts`.

## Key Architecture Notes

**Backend settings**: `AppProperties` (`vn.medischedule.common.config`, `@ConfigurationProperties(prefix = "app")`,
record lồng) — một entry point duy nhất, framework bind từ `application.yml` / biến môi trường; không tự khởi tạo object
settings ở chỗ khác. Khoá mới: field record + `${ENV_VAR:default}` trong `application.yml` + dòng trong `.env.example`.
Mẫu theo stack: xem `.claude/stack-packs/backend-spring-boot-flyway.md`.

**API routes**: phân tán — route khai báo ngay trên controller (`@RestController` + `@RequestMapping` / `@GetMapping`…)
trong package `web` của từng module, Spring tự quét package `vn.medischedule`; không có file đăng ký router. Prefix
`/api`. Endpoint public: danh sách `PUBLIC` trong `vn.medischedule.auth.security.SecurityConfig` (`/api/auth/login`,
`/api/auth/register`, `/api/public/**`, health, Swagger); `/api/admin/**` yêu cầu role `ADMIN` ở tầng URL; còn lại cần
đăng nhập, quyền chi tiết bằng `@PreAuthorize("hasRole('...')")` trên controller.

**Module boundary**: theo module — mỗi module một package `vn.medischedule.<module>` với `domain` (entity) /
`repository` / `service` / `web` (controller) / `dto` (record); module mẫu `auth`. Không gọi chéo repository của module
khác — gọi qua service của module đó (`AGENTS.md` § Quy ước backend). Phần dùng chung ở `vn.medischedule.common`
(`config`, `exception`). Module đã có code: `auth`, `cms`, `storage`; các module khác (`booking`, `schedule`, `clinic`,
`patient`, `emr`, `pharmacy`, `billing`, `commission`, `aftercare`, `notification`, `ai`) mới có `package-info.java` mô
tả phạm vi.

**Database**: Spring Data JPA repository + `@Transactional` ở service quản lý transaction (`open-in-view: false`) của
Spring Data JPA (Hibernate) trên PostgreSQL 16. Models/entity ở `backend/src/main/java/vn/medischedule/**/domain`,
schemas/DTO ở `backend/src/main/java/vn/medischedule/**/dto`. Mọi thay đổi schema đi qua Flyway
(`backend/src/main/resources/db/migration`, kiểu SQL versioned, chạy lúc khởi động: có); Hibernate `ddl-auto: validate`.
Dữ liệu demo dev là repeatable `backend/src/main/resources/db/seed/R__seed_dev_data.sql` (nạp khi `FLYWAY_LOCATIONS`
gồm `classpath:db/seed`; môi trường thật chỉ `classpath:db/migration`).

**Object storage endpoint**: backend gọi MinIO qua `MINIO_ENDPOINT` (dev `http://localhost:9010`, vì backend chạy ngoài
Docker); bucket `medical-files` do job `minio-init` của docker-compose tạo. Upload qua `POST /api/admin/files`, đọc file
công khai qua `FileController` (module `storage`).

**Dependency runtime vs dev/test**: Maven scope — thư viện test (`spring-boot-starter-test`, `spring-security-test`) để
scope `test`; Lombok `optional` và bị loại khỏi jar bởi `spring-boot-maven-plugin`. Frontend: thư viện build/type ở
`devDependencies` của `web/package.json`.

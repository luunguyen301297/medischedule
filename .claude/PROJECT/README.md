# Project Context — MediSchedule

Tài liệu kỹ thuật dự án. Agents và engineers đọc file liên quan trước khi làm việc — không cần load toàn bộ.

## Topics

| Topic                  | File                                             | Khi nào đọc                                    |
|------------------------|--------------------------------------------------|------------------------------------------------|
| Stack, commands, ports | [stack-and-commands.md](./stack-and-commands.md) | Setup môi trường, chạy dev/Docker              |
| Stack packs            | [../stack-packs/README.md](../stack-packs/README.md) | Bẫy / lệnh riêng công cụ khi code, test    |
| Architecture           | [architecture.md](./architecture.md)             | Thiết kế feature, routing, luồng request       |
| Auth                   | [auth.md](./auth.md)                             | Bất kỳ task liên quan login, token, permission |
| Conventions            | [conventions.md](./conventions.md)               | Implement code, error handling, feature order  |
| Testing                | [testing.md](./testing.md)                       | Viết unit / integration test                   |
| QA process             | [qa-process.md](./qa-process.md)                 | Phase `qa-plan` / `qa-run`, duyệt kịch bản     |
| Frontend state         | [frontend-state.md](./frontend-state.md)         | State management phía frontend                 |
| Environment            | [environment.md](./environment.md)               | OS, shell, đường dẫn, encoding                 |
| Điều hành 3 tầng       | [orchestration-3tier.md](./orchestration-3tier.md) | Vai phiên điều hành                          |
| KDD                    | [kdd.md](./kdd.md)                               | Phase `distill`, knowledge base                |

## Domain overview

Hệ thống đặt lịch và quản lý phòng khám: bệnh nhân xem trang giới thiệu, đặt lịch, (sau này) xem kết quả qua web và
app Flutter; nhân viên (quản trị, bác sĩ, lễ tân, dược sĩ) làm việc trên khu quản trị web. Backend Spring Boot chia
module theo nghiệp vụ: `auth`, `cms`, `storage` (đã có code), `booking`, `schedule`, `clinic`, `patient`, `emr`,
`pharmacy`, `billing`, `commission`, `aftercare`, `notification`, `ai` (đang dựng dần). Đăng nhập bằng email hoặc tên
đăng nhập + mật khẩu, JWT stateless, phân quyền theo một role mỗi user — xem chi tiết tại [skills/_shared/domain-context.md](../skills/_shared/domain-context.md).

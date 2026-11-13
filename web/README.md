# web

Trang giới thiệu / đặt lịch / đăng nhập cho bệnh nhân và khu quản trị cho nhân viên.
React 18 + TypeScript + Vite + React Router + Ant Design 5 + TanStack Query.

```bash
npm install
npm run dev         # http://localhost:5175, proxy /api → http://localhost:8085
npm run typecheck   # kiểm kiểu TypeScript
npm run build
```

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `src/app/` | Entry, provider, router (lazy load từng khu), guard đăng nhập |
| `src/shared/` | API client, theme (bảng màu duy nhất), CSS nền, UI dùng chung. Không import từ `features/` |
| `src/features/<khu>/` | Mỗi khu tự chứa api + type + trang + css; feature khác chỉ import qua `index.ts` |

Quy ước chi tiết: xem mục "Quy ước web" trong `CLAUDE.md` ở thư mục gốc.

Dự kiến thêm: `@ant-design/pro-components` (bảng/form quản trị), `orval` (sinh client từ OpenAPI của backend), `@stomp/stompjs` (hàng đợi realtime).

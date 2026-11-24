# Frontend State Management

> Đọc khi: task đụng tới global store, server state (cache dữ liệu API) hoặc state được persist. Bỏ qua nếu chỉ sửa UI
> thuần không đổi state.

Thư viện state: TanStack React Query 5 (biến thể B — chỉ server state). API cụ thể (query key, invalidate, bẫy):
chi tiết theo stack: xem `.claude/stack-packs/frontend-react-antd-react-query.md`.

## Chỉ server state, không có global store

Không có store global. Dữ liệu từ API sống trong cache của TanStack React Query 5; state còn lại là state cục bộ của
component hoặc context nhỏ (auth, theme).

| Nhóm dữ liệu | Query key | Hàm gọi API | Invalidate sau mutation |
|---|---|---|---|
| Người đang đăng nhập | `['me']` | `authApi.me` (`features/auth/api.ts`) | — (đọc ở `AdminLayout`, `StaffPage`, `HomePage`) |
| Nội dung trang chủ | `['homepage']` | `contentApi.homepage` (`features/landing/api.ts`) | lưu ở `/admin/homepage` ⇒ `qc.setQueryData(['homepage'], saved)` |
| Danh sách nhân viên (admin) | `['admin-users', role, q, page, size]` | `usersApi` (`features/admin/staff/api.ts`) | `['admin-users']` sau đổi role / khoá–mở / tạo tài khoản |

Cấu hình client mặc định (`web/src/app/providers.tsx`): `retry: false`, `refetchOnWindowFocus: false`; `staleTime` đặt
theo từng query khi cần (vd `['homepage']` ở `LandingLayout`: 60 s).
Auth: không có context/store — token trong `localStorage` (`tokenStore`, key `ms_token`), user hiện tại lấy bằng query
`['me']` (xem [auth.md](./auth.md)).

**Rules**

- Mọi dữ liệu từ API đi qua query/mutation của TanStack React Query 5; không chép dữ liệu server sang `useState` rồi tự
  đồng bộ (trừ giá trị khởi tạo form).
- Query key chứa **mọi** tham số mà hàm gọi API dùng (filter, trang, id) — thiếu ⇒ cache trả dữ liệu sai.
- Sau mutation: invalidate (hoặc cập nhật cache) đúng nhóm key bị ảnh hưởng, ghi trong bảng trên.
- State dùng chung nhiều màn mà không phải dữ liệu server (bộ lọc giữ khi chuyển trang…): URL search params hoặc context
  nhỏ; cần nhiều hơn thế thì đề xuất thêm store ở design, không tự thêm.

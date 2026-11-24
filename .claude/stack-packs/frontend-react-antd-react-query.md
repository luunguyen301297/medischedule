# Stack pack — Frontend: React 18 + TypeScript + Ant Design 5 + TanStack React Query 5 + Vite

Áp dụng khi `FRONTEND_FRAMEWORK` = React (React Router), `FE_LANG` = TypeScript strict, `UI_LIBRARY` = Ant Design 5,
`STATE_LIBRARY` = TanStack React Query 5 (**không** store global kiểu Zustand/Redux), `FE_BUILD_TOOL` = Vite; HTTP qua
axios. Bổ sung cho `frontend-engineer`, `team-code-reviewer` (Rule Pack bổ sung) và
`ui-ux-pro-max/references/project-ux-spec.md`. `PROJECT/frontend-state.md` dùng **biến thể B** (chỉ server state).

Mục **[kiểm dự án]** là chỗ các dự án hay làm khác nhau — đọc code thật rồi ghi quy ước vào `PROJECT/*.md`.

## Lệnh

| Khoá | Giá trị | Ghi chú |
|---|---|---|
| `FE_TYPECHECK_COMMAND` | `npm --prefix web run typecheck` | không có script thì `(cd web && npx tsc --noEmit)` |
| `FE_LINT_COMMAND` | `npm --prefix web run lint` | ESLint; chưa có thì `chưa có` |
| `FE_CHECK_COMMAND` | `chưa có` | script kiểm bổ sung nếu dự án dựng (vd `*.check.ts` chạy bằng esbuild + node) — khi đó điền lệnh |
| Dev | `npm --prefix web run dev` | cổng trong `vite.config.ts`, proxy `/api` → backend |
| Build | `npm --prefix web run build` | thường `tsc --noEmit && vite build` |

## Skill tham khảo (`_shared/`, không bắt buộc)

| Khi nào dùng | Skill |
|---|---|
| React 18 components, hooks, Suspense, performance | `.claude/skills/_shared/react-expert/SKILL.md` |
| TypeScript nâng cao: generics, conditional types, strict | `.claude/skills/_shared/typescript-pro/SKILL.md` |

## Cấu trúc feature (← `PROJECT/conventions.md` § Frontend)

- **[kiểm dự án]** Cấu trúc theo feature phổ biến: `src/app/` (entry, providers, router, guard) · `src/shared/` (api
  client, theme, ui dùng chung — **không** import `features/`) · `src/features/<khu>/` (api + type + trang + css của
  khu; cổng công khai `index.ts`).
- Alias `@/` cần khai cả hai chỗ: `vite.config.ts` (`resolve.alias`) và `tsconfig` (`paths`). Trong cùng feature import
  tương đối; feature khác chỉ qua `@/features/<khu>` (index), không import sâu.
- Khu theo vai trò: feature export bộ route từ `index.ts`, gắn vào router bằng `lazy(() => import('@/features/<khu>'))`,
  bọc guard đăng nhập; kiểm vai trò trong layout của khu (vd `role !== 'ADMIN'` → `<Navigate>`). Guard FE chỉ để điều
  hướng — phân quyền thật ở backend.

## React Query (← `PROJECT/frontend-state.md` biến thể B)

- Hàm gọi API ở `features/<khu>/api.ts` trả dữ liệu đã bóc (`http.get<T>(...).then((r) => r.data)`); component dùng
  `useQuery({ queryKey, queryFn })` / `useMutation`. **[kiểm dự án]** Cấu hình `QueryClient` mặc định (`retry`,
  `refetchOnWindowFocus`, `staleTime`) — ghi vào `PROJECT/frontend-state.md`.
- Query key là mảng, phần đầu là tên nhóm, sau đó mọi tham số: `['users', filter]`; sau mutation
  `qc.invalidateQueries({ queryKey: ['users'] })` (khớp tiền tố). Danh sách phân trang: `placeholderData: keepPreviousData`.
- Lỗi mutation: `onError: (e) => message.error(<hàm lấy message lỗi>(e))` với `const { message } = App.useApp()` (cần
  bọc `<App>` của antd — `message` tĩnh không ăn theme/context). Lỗi theo trường: `form.setFields` từ danh sách field
  lỗi của backend.
- Bẫy: `queryFn: api.me` truyền thẳng hàm — hàm nhận tham số sẽ nhận nhầm `QueryFunctionContext`; hàm có tham số thì bọc
  `() => api.x(arg)` và đưa `arg` vào `queryKey`.
- Bẫy: query phụ thuộc giá trị chưa có (id từ route, user chưa nạp) ⇒ `enabled: !!id`, không gọi với `undefined`.
- Không chép `data` của query sang `useState` để sửa — form dùng `initialValues`/`setFieldsValue` khi data về.

## Giao diện (← `PROJECT/conventions.md`, `LESSONS.md` § Frontend)

- **[kiểm dự án]** Màu tập trung một chỗ (file theme: palette → biến CSS + `ConfigProvider` `theme` của antd); CSS dùng
  `var(--<prefix>-*)`, không mã màu cứng.
- CSS của khu đặt cạnh feature, import trong layout/trang của khu; prefix class theo khu để không đè nhau.
- Control tự viết đặt trong `Form.Item` phải nhận `id`/`value`/`onChange` và gắn `id` vào phần tử nhập thật (label
  click, validate scroll mới đúng).
- Locale antd (`ConfigProvider locale`) + locale dayjs phải khớp nhau.
- Quy ước giao diện (shadow, nền, màu chữ phụ…): theo `.claude/skills/ui-ux-pro-max/references/project-ux-spec.md` § "Quy ước UI mặc định"; với antd, các quy ước
  đó ánh xạ qua token/theme (`ConfigProvider theme`), không viết style cứng từng component. Nhấn mạnh tiêu đề bằng weight / `Typography`, không italic.

## Bẫy công cụ

- `tsc` KHÔNG bắt lỗi dùng `const` trước khai báo (TDZ) — bật ESLint `@typescript-eslint/no-use-before-define`, hoặc rà
  tay ở component lớn khi chưa có ESLint.
- Không `prettier --write` lên file có sẵn nếu repo chưa format bằng prettier; chỉ format file mới.
- `strictPort: true` trong `vite.config.ts` ⇒ cổng bận thì dev server thoát, không tự đổi cổng; danh sách CORS origin của
  backend cũng phải khớp cổng.
- Type-check worktree tạm: dùng tsc của checkout chính `npx --prefix <main>/web tsc -p <wt>/web --noEmit`
  thay vì junction `node_modules` (xoá worktree có junction sẽ xoá luôn node_modules thật).
- Interceptor 401 của axios thường xoá token + chuyển `/login` (trừ khi đang ở trang đăng nhập/đăng ký): test luồng hết
  phiên bằng token hết hạn, và đừng để interceptor gây vòng lặp chuyển trang.

## Mapping state giao diện → Ant Design (dùng trong UX Spec §3)

| State | Antd / pattern gợi ý |
|-------|----------------------|
| Default | Component bình thường |
| Hover | Antd default (không custom CSS trừ khi cần) |
| Focus | Focus ring Antd / keyboard accessible |
| Disabled | `disabled` prop |
| Loading | `<Spin>` (toàn trang: `<Spin fullscreen />` khi lazy route/layout) / `<Skeleton>` / `Button loading` / `Table loading={isFetching}` |
| Empty | `<Empty>` + CTA gợi ý |
| Error | `<Result status="error">` / `message.error(...)` / Form `validateStatus` |
| Success | `message.success` / `<Result status="success">` |

Layout chính: `<Flex>` / `<Grid>` / `<Space>`. Ví dụ component trong UX Spec §2: `Table` (danh sách, phân trang),
`Button type="primary"` (hành động chính).

## Rule Pack bổ sung (team-code-reviewer)

| Path chạm vào | Check bắt buộc |
|---|---|
| `web/src/**/*.tsx` | hooks deps (`useEffect`/`useMemo`/`useCallback`) — **chỉ flag khi deps thiếu gây stale value THẬT**, phải chỉ ra render path |
| `web/src/**/*.tsx` | `queryKey` thiếu tham số mà `queryFn` dùng ⇒ cache trả dữ liệu sai bộ lọc — **Major** |
| `web/src/**` | Mutation không invalidate/cập nhật nhóm key bị ảnh hưởng ⇒ màn hiển thị dữ liệu cũ — **Major** |
| `web/src/**` | Mã màu cứng / `box-shadow` / import sâu vào feature khác / `shared` import `features` — **Major** |
| `web/src/features/*/api.ts` | Type response lệch DTO backend (field FE có mà backend không trả) — **Major** |

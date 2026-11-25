# MediSchedule — tư vấn UX/UI và UX Spec

Không nằm trong pipeline mặc định: `frontend-engineer` tự thiết kế UI từ BA Spec + Tech Design; chỉ viết UX Spec khi
user nhờ hoặc màn phức tạp (nhiều state, nhiều component phối hợp).

Tên component cụ thể và mapping state → component của UI library nằm trong stack pack frontend (`.claude/stack-packs/frontend-react-antd-react-query.md`).

## Quy tắc ưu tiên trên mọi gợi ý của ui-ux-pro-max

- **Ant Design 5 + convention codebase > palette/style/font gợi ý của search.** CMS của dự án không đổi theme,
  không đổi font, không rotate style mỗi lần chạy. Dùng search chủ yếu cho UX guideline, accessibility, chart, responsive.
- Trước khi đề xuất: Glob/Grep `web` tìm màn tương tự, giữ đồng nhất cách đang dùng UI library.
- Không invent số liệu / copy giả khi BA Spec không có.
- Mockup HTML (khi cần): palette dịu, 3 thiết bị (desktop/tablet/phone), chốt mockup trước khi sang SA. Mockup clone
  CSS/layout thật của màn hiện có; mở rộng điều hướng sẵn có (tab, menu) thay vì tạo màn mới.
- Không viết mã triển khai trong vai tư vấn; đầu ra là UX Spec markdown.

## Quy ước UI mặc định

Đã viết lại theo quy ước của MediSchedule (dự án thắng mặc định của template).

**Nguồn duy nhất trong `.claude/`** của quy ước giao diện — `LESSONS.md`, `PROJECT/conventions.md`, stack pack frontend
chỉ trỏ tới mục này. **Nguồn sự thật** là `AGENTS.md` § "Quy ước web" và `web/src/shared/theme/theme.ts` (giữ nguyên);
mục này là bản tóm tắt agent đọc khi làm UI và phải khớp nguồn — quy ước đổi thì sửa nguồn trước, rồi cập nhật mục này.
Áp dụng cho mọi màn và mọi mockup:

- **Màu chỉ khai báo ở `web/src/shared/theme/theme.ts`** (`palette` → biến CSS `--ms-*` + theme Ant Design). CSS dùng
  `var(--ms-*)`, không viết mã màu cứng.
- **Một màu chủ đạo** (`primary`, cyan y tế) cho mọi nút và điểm nhấn; không thêm màu nhấn thứ hai.
- **Không `box-shadow`**: theme antd đã tắt shadow (`boxShadow: 'none'`, shadow của Button/Input) — không thêm lại ở CSS
  tự viết.
- Nền phẳng: nền trang / layout là `surface` (trắng); vùng chọn / nhãn dùng `primarySoft`; phân tách bằng viền
  `--ms-border`.
- Chữ: chữ chính `ink`; chữ mô tả dùng `inkSoft` qua token `colorTextDescription` (tương phản > 7:1 trên trắng) — không
  tự đặt màu xám khác hay giảm opacity.
- Font `Be Vietnam Pro`; giao diện tiếng Việt (`ConfigProvider` locale `vi_VN`).
- CSS riêng của khu đặt cạnh feature, import trong layout/trang của khu đó. Control tự viết trong `Form.Item` nhận
  `id` / `value` / `onChange` và gắn `id` vào phần tử nhập thật.

## Luồng

1. Đọc BA Spec / Tech Design liên quan (hoặc yêu cầu trực tiếp của user).
2. Quét pattern hiện có trong `web`.
3. Tra ui-ux-pro-max khi cần (guideline, a11y, chart, responsive) — lọc qua quy tắc ưu tiên ở trên.
4. Viết UX Spec theo mẫu dưới, ghi ra `docs/design/[feature-name]-ux-spec.md`.
5. Đang giữa pipeline: điền path và đổi ⬜ → ✅ dòng **UX Spec** trong Artifacts Registry của
   `docs/HANDOFF.<task>.local.md` (không bắt buộc đổi `phase`).

## Mẫu UX Spec

```markdown
# UX/UI SPECIFICATION: [Tên Tính Năng]

## 1. Luồng Người dùng (User Flow & Interaction)

- **Bước 1:** Người dùng truy cập...
- **Bước 2:** Hệ thống hiển thị...

## 2. Cấu trúc Màn hình & Lựa chọn Component (Ant Design 5)

### Trang: [Tên Màn Hình]

- **Layout chính:** [component layout của Ant Design 5]
- **Components sử dụng:**
    - `[Component bảng]`: Hiển thị danh sách, hỗ trợ phân trang.
    - `[Component nút chính]`: hành động chính.

## 3. Trạng thái Giao diện (Feedback States)

| State | Component / pattern gợi ý (Ant Design 5) |
|-------|----------------------|
| Default | Component bình thường |
| Hover | Mặc định của UI library (không custom CSS trừ khi cần) |
| Focus | Focus ring mặc định / keyboard accessible |
| Disabled | thuộc tính disabled |
| Loading | spinner / skeleton / nút ở trạng thái loading |
| Empty | trạng thái rỗng + CTA gợi ý |
| Error | trang/khối lỗi, thông báo lỗi, trạng thái lỗi của form field |
| Success | thông báo thành công / trang kết quả |

## 4. Anti-slop check

- [ ] Không invented metrics / fake social proof nếu BA không cung cấp
- [ ] Không clutter: pill clusters, stat strips, badge thừa trên CMS
- [ ] Không fake browser/phone chrome trong mock mô tả
- [ ] Contrast / hierarchy đủ đọc (bảng, form label, primary CTA rõ)
- [ ] Đúng mục "Quy ước UI mặc định" ở trên (màu từ theme, không shadow, một màu chủ đạo, chữ mô tả `inkSoft`)
- [ ] Không italic display headers; nhấn mạnh bằng weight / typography của Ant Design 5
- [ ] Responsive: không horizontal scroll; action chính usable trên mobile/tablet nếu trong scope

## 5. Self-critique

Philosophy / Hierarchy / Execution / Specificity / Restraint / Variety (1–5). Axis nào < 3 → chỉnh lại section liên quan trước khi handoff.
```

---
name: mobile-screen-loop
description: "Vòng lặp dựng 1 màn hình cho app mobile: đọc hiện trạng web/backend → thiết kế theo design system (Google Stitch hoặc công cụ khác) → dựng lại chính xác pixel → build → chạy emulator/simulator nghiệm thu. Dùng khi thêm/sửa màn hình app mobile hoặc khi user nói 'stitch màn X', 'dựng lại đúng thiết kế', 'nghiệm thu màn trên máy ảo'."
---

Đóng gói quy trình dựng app mobile (`Flutter`, thư mục `mobile/`) thành vòng lặp, **mỗi
vòng = 1 màn hình**. Chạy SAU pipeline BA→SA→TL (spec/design đã duyệt), ở phase `dev` cho mobile — không thay thế
pipeline.

Phần riêng stack (vị trí file, theme, route, lệnh build, emulator/simulator, bẫy compile): đọc stack pack mobile của dự
án `.claude/stack-packs/mobile-flutter.md` trước bước 4.

## Trước khi làm

- **KB:** nạp `docs/knowledge/{domain}/` theo `.claude/WORKFLOW.md` §5.2 cho capability của màn; không đọc toàn bộ KB.
- **Input:** BA Spec + Tech Design đã duyệt (ví dụ `docs/tasks/<app>-mobile-app-ba-spec.md`,
  `docs/design/<app>-mobile-app-tech-design.md`); project thiết kế của app (URL/ID ở README app, mục "Thiết kế");
  dùng Stitch thì MCP `stitch` đã add.

## Vòng lặp (6 bước, không gộp)

1. **Đọc hiện trạng web/backend** — nguồn sự thật, không bịa. Router/service backend phục vụ app
   (`backend/...` — module API của app mobile): method, schema request/response, rule nghiệp vụ (trạng thái,
   mã rule KB, điều kiện duyệt). Màn web/CMS tương ứng (`web/...`) nếu có: trường/nhãn/trạng thái người
   dùng đã quen. Gom thành **brief 1 màn**: mục đích, field thật, hành động, các state (rỗng/tải/lỗi/chờ duyệt/từ
   chối), rule chặn. Endpoint chưa sẵn sàng → ghi rõ, dựng UI-only với mock, KHÔNG nối API giả.
2. **Thiết kế theo design system** — `Read` [references/stitch-and-design.md](references/stitch-and-design.md)
   (Stitch: lấy `designMd`, sinh màn đúng project, tránh bản trùng khi timeout, tải ảnh/HTML; công cụ khác: lấy token
   + ảnh màn tương đương).
3. **Đọc thiết kế từng chi tiết** (cùng file trên): liệt kê từng khối, màu lấy từ HTML/token/pixel — không ước lượng
   mắt, mỗi màn có thể có palette riêng; lập bảng chi tiết → giá trị trước khi code.
4. **Dựng UI bằng `Flutter`** — `Read` [references/build-verify.md](references/build-verify.md) (quy tắc
   chung) + stack pack mobile (vị trí file, token vs màu cục bộ, route, cuộn lồng, theme sáng/tối, bàn phím, bẫy
   compile).
5. **Build + emulator/simulator** (cùng file trên + lệnh trong stack pack).
6. **Nghiệm thu** (cùng file trên): đặt cạnh ảnh thiết kế, so từng dòng bảng bước 3, chụp lại tới khi khớp; ghi
   README app mục "Đã verify" + HANDOFF; tắt emulator/simulator.

## Quy tắc

- Mỗi vòng chỉ 1 màn; xong bước n mới sang n+1. Nghiệm thu FAIL → quay lại bước 4 (lệch UI) hoặc bước 2 (thiết kế
  thiếu state / không khớp web/backend).
- Đổi behavior nghiệp vụ so với BA Spec → dừng, quay về `team-business-analyst`, không tự quyết.
- Không sửa logo/màu thương hiệu của dự án; vị trí asset logo theo stack pack / README app.
- Chỉ implement app/màn được yêu cầu. Nhiều app cho nhiều vai trò người dùng → mỗi app một thư mục riêng
  (`mobile` cho từng app) theo cùng vòng lặp, chung design system.
- Nghiệm thu chỉ tính khi đã chạy thật trên emulator/simulator; không chạy được thì nói rõ, không báo "xong".

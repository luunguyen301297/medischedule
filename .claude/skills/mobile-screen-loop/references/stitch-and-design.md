# Bước 2–3: thiết kế (Stitch hoặc công cụ khác) và đọc thiết kế từng chi tiết

Đọc file này ở bước 2 của `mobile-screen-loop`.

## Bước 2 — Thiết kế theo design system

Dùng công cụ thiết kế dự án đã chọn (ghi ở README app). Với công cụ khác Stitch: lấy token design system (màu,
typography, spacing, shape) + ảnh màn, rồi làm tiếp bước 3 như dưới. Với Google Stitch:

- `mcp__stitch__get_project` → lấy `designTheme.designMd` (màu/typography/spacing/shape chính xác) — đây là source
  of truth token, KHÔNG hard-code màu đoán mắt.
- `mcp__stitch__generate_screen_from_text` vào ĐÚNG project hiện có (giữ design system, logo gốc của dự án).
  Prompt = brief bước 1 + yêu cầu: mobile, ngôn ngữ tiếng Việt, bám design system của project, liệt kê đủ các
  state. Mỗi màn 1 lần gọi, đặt tên rõ; làm tuần tự để dễ quản lý.
- Tool hay timeout → gọi `list_screens` kiểm tra trước khi gọi lại (tránh sinh nhiều bản trùng cùng một màn). Dọn
  bản trùng thủ công trên Stitch UI.
- Lấy ảnh: `get_screen`/`list_screens` → `screenshot.downloadUrl` (host `lh3.googleusercontent.com`, thêm hậu tố
  `=w780` mới ra ảnh đủ kích thước). HTML nguồn: `htmlCode.downloadUrl` — loại `app_companion_generated_files`
  curl trần tải được → dùng làm nguồn màu CHÍNH XÁC (Tailwind class + tailwind.config); loại `stitch_files`
  redirect sang trang đăng nhập Google, khi đó lấy màu bằng pixel từ screenshot và đối chiếu với `designMd`.

## Bước 3 — Đọc thiết kế từng chi tiết (không "gần giống")

Lỗi hay gặp nhất là dựng "gần giống" — màu, bố trí, card lệch thiết kế. Quy trình bắt buộc:

1. Tải ảnh màn về scratchpad, xem bằng Read (ảnh), liệt kê từng khối theo thứ tự trên xuống: header, pill, card,
   input (filled hay outlined?), button (filled/outlined/disabled?), icon (tên + màu + nền), badge, divider.
2. Màu: KHÔNG giả định các màn dùng chung một bảng màu — mỗi màn có thể dùng palette riêng (kể cả viền/đổ bóng pill).
   Có HTML → resolve từng class ra hex (+alpha), mỗi file màn tự khai màu cục bộ (cách khai theo stack pack),
   header/footer riêng từng màn, không dùng header chung. Không có HTML → sample pixel và đối chiếu token design
   system. Token là chuẩn khi màn dùng token của theme. Chỗ không có token → sample pixel bằng PIL, KHÔNG ước lượng
   mắt: `Image.open(p).getpixel((x,y))` (cần `pillow`; nếu pip hệ thống bị chặn (PEP 668) → tạo venv riêng).
3. Lập bảng: chi tiết → giá trị (kích thước, bo góc, màu, font weight) trước khi viết code.

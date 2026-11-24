# Bước 4–6: dựng UI, build, nghiệm thu trên emulator/simulator

Đọc file này ở bước 4 của `mobile-screen-loop`. Đây là quy tắc chung; vị trí file, cú pháp, lệnh build/chạy và bẫy
riêng `Flutter` nằm trong stack pack mobile của dự án (`.claude/stack-packs/mobile-flutter.md`).

## Bước 4 — Dựng UI

- Mỗi màn một file theo cấu trúc feature của app; header/footer riêng trong từng file màn (màu mỗi màn có thể khác
  nhau nên không ép header chung).
- Màn dùng token theme (ví dụ Splash/Login/Home): lấy từ file theme của app (= token design system), không inline
  hex. Màn con có palette riêng: khai màu cục bộ trong file màn. Nền tô kín tới mép, tôn trọng safe area.
- Đăng ký route trong navigation của app và trỏ entry point (tile/menu ở màn Home) tới route; back quay về màn trước.
- Danh sách nhỏ cố định trong màn cuộn: render thẳng từng dòng, KHÔNG lồng list ảo hoá (lazy/virtualized) trong một
  vùng cuộn cùng chiều (nội dung phía dưới bị cụt hoặc không cuộn tới được).
- Theme sáng/tối theo máy: màu thích ứng phải được đọc lại khi đổi chế độ (không chốt giá trị một lần lúc khởi tạo);
  chữ trắng trên nền tô màu dùng trắng cố định. Nghiệm thu cả 2 chế độ.
- Màn có ô nhập: nếu gốc app đã có cơ chế ẩn bàn phím khi chạm ra ngoài thì KHÔNG tự xử lý lại; đừng bắt tap lên
  container bao ngoài (nuốt gesture). Dialog/bottom sheet là cửa sổ/lớp riêng thì tự gắn cơ chế đó cho nội dung.
- Dữ liệu chưa có API: mock tĩnh, ghi comment 1 dòng nói rõ "mock, chưa nối <method>".
- Chỉ dùng icon/component thật sự tồn tại trong thư viện của stack (kiểm tên trước khi dùng).

## Bước 5 — Build + chạy emulator/simulator

Toolchain cụ thể (SDK, phiên bản runtime, tên máy ảo) ghi ở README app; lệnh build/cài/chạy mẫu ở stack pack.

Backend thật (khi cần login/API): app debug gọi backend dev qua địa chỉ host nhìn từ máy ảo (khác nhau theo nền tảng —
xem stack pack) + `/api`. Tài khoản test lấy từ seed/fixture của dự án (không ghi mật khẩu vào skill). Chỉ
nới cleartext/HTTP cho build debug — release build KHÔNG được nới.

## Bước 6 — Nghiệm thu (so với thiết kế, không tự chấm)

1. Đi tới màn bằng thao tác thật (login → Home → entry point), chụp màn hình máy ảo vào scratchpad (lệnh theo stack
   pack).
2. Đặt cạnh ảnh thiết kế, đối chiếu bảng chi tiết bước 3 từng dòng; sample pixel màu chủ đạo của ảnh chụp so với
   token. Sai → sửa → build lại → chụp lại; lặp tới khi khớp hết.
3. Cuộn xuống cuối màn (phát hiện cuộn lồng), bấm back, xoay/đổi trạng thái nếu màn có state, đổi theme sáng/tối,
   chạm ô nhập rồi chạm ra ngoài (bàn phím hiện/ẩn đúng).
4. **Toạ độ tap:** luôn chụp ảnh mới ngay trước khi tính toạ độ (ảnh hiển thị × hệ số scale → toạ độ thật). Dùng
   lại toạ độ cũ sau khi cuộn/back là nguyên nhân tap nhầm.
5. Xem log mạng/crash của app khi màn có gọi mạng (lệnh theo stack pack).
6. Ghi kết quả vào README app mục "Đã verify" (bug thật tìm ra + đã sửa) và cập nhật `docs/HANDOFF.<task>.local.md`
   (artifact + phần còn thiếu). Xong nghiệm thu → tắt emulator/simulator.

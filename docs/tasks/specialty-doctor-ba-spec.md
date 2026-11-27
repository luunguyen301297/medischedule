# BA SPEC: Chọn bác sĩ cho chuyên khoa

- **Trạng thái:** Đã phê duyệt
- **Độ phức tạp:** Nhỏ
- **Domain:** clinic (danh mục chuyên khoa, nhân sự)

## 1. Tổng quan & Mục tiêu

Quản trị viên gán bác sĩ vào chuyên khoa ngay trên trang Chuyên khoa (`/admin/specialties`), để biết khoa nào có ai
khám và sau này xếp ca, đặt lịch theo đúng bác sĩ của khoa. Mỗi bác sĩ thuộc đúng một chuyên khoa. Danh sách chuyên
khoa hiện luôn tên bác sĩ khi khoa chỉ có một bác sĩ, để nhìn là biết ngay mà không phải mở từng khoa.

## 2. Danh sách User Stories

- **US-01:** Với vai trò là quản trị viên, tôi muốn chọn các bác sĩ thuộc một chuyên khoa khi thêm hoặc sửa chuyên
  khoa, để danh mục phản ánh đúng ai đang khám ở khoa đó.
- **US-02:** Với vai trò là quản trị viên, tôi muốn thấy khoa hiện tại của từng bác sĩ trong danh sách chọn, để không
  vô tình chuyển nhầm bác sĩ đang làm ở khoa khác.
- **US-03:** Với vai trò là quản trị viên, tôi muốn danh sách chuyên khoa hiện tên bác sĩ khi khoa chỉ có một người
  (và số bác sĩ khi có nhiều người), và hiện tên phòng khi khoa chỉ có một phòng, để nắm nhanh từng khoa.

## 3. Tiêu chí nghiệm thu (BDD)

### Chọn bác sĩ

- **AC-01 (Luồng chính):** Với điều kiện mở hộp thoại Thêm hoặc Sửa chuyên khoa, khi xem form, thì ngoài ô Tên còn có
  ô "Bác sĩ" chọn được nhiều người, có tìm theo tên; khi sửa, ô này điền sẵn các bác sĩ đang thuộc khoa.
- **AC-02 (Luồng chính):** Với điều kiện mở ô "Bác sĩ", khi xem các lựa chọn, thì thấy mọi tài khoản vai trò bác sĩ
  đang hoạt động; bác sĩ đang thuộc khoa khác có ghi kèm tên khoa đó (vd. "Nguyễn Văn A · Nhi"), bác sĩ chưa có khoa
  không ghi gì thêm.
- **AC-03 (Luồng chính):** Với điều kiện chọn một bác sĩ chưa thuộc khoa nào, khi lưu, thì bác sĩ thuộc chuyên khoa
  này.
- **AC-04 (Luồng chính):** Với điều kiện chọn bác sĩ A đang thuộc khoa Nhi cho khoa Da liễu, khi lưu, thì A chuyển sang
  Da liễu và không còn thuộc Nhi; số bác sĩ / tên hiển thị của cả hai khoa trong danh sách cập nhật theo.
- **AC-05 (Luồng chính):** Với điều kiện khoa đang có bác sĩ B, khi bỏ B khỏi ô "Bác sĩ" và lưu, thì B không còn thuộc
  khoa nào (không tự chuyển sang khoa khác).
- **AC-06 (Trường hợp biên):** Với điều kiện tên chuyên khoa không hợp lệ hoặc trùng (AC-05, AC-06 của spec
  `clinic-specialty-admin`), khi lưu, thì báo lỗi như cũ và không thay đổi bác sĩ của khoa nào.
- **AC-07 (Trường hợp biên):** Với điều kiện một tài khoản được chọn không còn là bác sĩ hoặc đã bị khóa vào lúc lưu
  (vd. admin khác vừa đổi), khi lưu, thì báo lỗi "Bác sĩ không hợp lệ, hãy tải lại danh sách" và không lưu gì.
- **AC-08 (Trường hợp biên):** Với điều kiện chuyên khoa đang ngừng hoạt động, khi sửa và chọn bác sĩ, thì vẫn lưu được
  (bác sĩ không gắn với việc khoa có nhận đặt lịch hay không).
- **AC-09 (Trường hợp biên):** Với điều kiện không có tài khoản bác sĩ nào, khi mở ô "Bác sĩ", thì thấy thông báo trống
  "Chưa có tài khoản bác sĩ" và vẫn lưu được chuyên khoa.

### Hiển thị trên danh sách chuyên khoa

- **AC-10 (Luồng chính):** Với điều kiện khoa có đúng 1 bác sĩ, khi xem danh sách chuyên khoa, thì dòng phụ hiện
  "2 phòng · Nguyễn Văn A" (họ tên đầy đủ của bác sĩ).
- **AC-11 (Luồng chính):** Với điều kiện khoa có từ 2 bác sĩ trở lên, khi xem danh sách, thì dòng phụ hiện
  "2 phòng · 3 bác sĩ".
- **AC-12 (Luồng chính):** Với điều kiện khoa chưa có bác sĩ, khi xem danh sách, thì dòng phụ chỉ hiện số phòng như
  hiện nay.
- **AC-13 (Trường hợp biên):** Với điều kiện tên bác sĩ dài, khi xem danh sách, thì dòng phụ cắt bằng dấu "…" trong
  một dòng, không đẩy công tắc và nút thao tác lệch hàng; di chuột vào thấy tên đầy đủ.

- **AC-16 (Luồng chính):** Với điều kiện khoa có đúng 1 phòng (tính cả phòng đang ngừng, như số phòng hiện nay), khi
  xem danh sách, thì phần số phòng hiện tên phòng thay cho "1 phòng", vd. "Phòng P104 · Nguyễn Văn A"; có từ 2 phòng
  vẫn hiện "N phòng", không có phòng hiện "0 phòng" như hiện nay.
- **AC-17 (Luồng chính):** Với điều kiện thêm, đổi tên, chuyển hoặc xóa phòng làm khoa chuyển giữa 1 phòng và số phòng
  khác, khi thao tác xong, thì dòng phụ của khoa cập nhật ngay không cần tải lại trang.

### Xóa chuyên khoa

- **AC-14 (Trường hợp biên):** Với điều kiện khoa còn ít nhất một bác sĩ, khi xóa khoa, thì vẫn bị chặn như AC-11 của
  spec `clinic-specialty-admin` (gợi ý ngừng hoạt động hoặc chuyển bác sĩ trước).

### Phân quyền

- **AC-15 (Trường hợp biên):** Với điều kiện không phải quản trị viên, khi gọi lấy danh sách bác sĩ hoặc lưu bác sĩ
  cho chuyên khoa, thì bị từ chối.

## 4. Ảnh hưởng liên Capability

Domain chưa có KB (`docs/knowledge/` chưa có `dependency-graph.yaml`).

- **Capability đang sửa:** danh mục chuyên khoa (module `clinic`).
- **Các capability có thể bị ảnh hưởng:** tài khoản nhân viên (module `auth`) — nguồn danh sách bác sĩ; xếp ca
  (`schedule`), đặt lịch (`booking`), hoa hồng (`commission`) — sau này đọc bác sĩ qua `staff`, hiện chưa có code đọc.
- **Blast radius code-level:** `ClinicCatalogService.createSpecialty` / `renameSpecialty` / `listSpecialties` /
  `deleteSpecialty`; trang `web/src/features/admin/specialty/SpecialtyPage.tsx`. Chưa có code nào đọc/ghi bảng
  `staff`.
- **Đề xuất phạm vi kiểm thử hồi quy cho QA:** thêm/sửa tên/xóa chuyên khoa, bật tắt chuyên khoa, danh sách phòng của
  khoa; trang Tài khoản nhân viên (khóa/mở, đổi vai trò bác sĩ) không bị ảnh hưởng.

## 5. Giả định

- **GĐ-01:** Bác sĩ = tài khoản vai trò DOCTOR đang hoạt động trong "Tài khoản nhân viên"; hồ sơ bác sĩ (họ tên lấy
  từ tài khoản) được tự tạo lần đầu bác sĩ được gán khoa — AC-02, AC-03. Nếu sai (cần nhập hồ sơ bác sĩ riêng: học
  vị, chứng chỉ…) thì phải thêm màn hồ sơ bác sĩ trước.
- **GĐ-02:** Bác sĩ đã bị khóa hoặc đổi sang vai trò khác nhưng vẫn còn gán khoa cũ thì không hiện trong ô chọn và
  không được tính/hiện ở danh sách chuyên khoa — AC-02, AC-10, AC-11. Nếu sai thì phải hiện thêm trạng thái "đã khóa".
- **GĐ-03:** Có từ 2 bác sĩ hiện "N bác sĩ"; không có bác sĩ thì không hiện gì thêm — AC-11, AC-12. Người dùng chỉ yêu
  cầu trường hợp 1 bác sĩ; nếu muốn ẩn số khi nhiều bác sĩ thì bỏ AC-11.
- **GĐ-04:** Họ tên hiển thị lấy theo tài khoản tại thời điểm xem (đổi tên tài khoản thì danh sách đổi theo) — AC-10.
- **GĐ-05:** Chọn bác sĩ ở cả hộp thoại Thêm và Sửa chuyên khoa, lưu cùng lúc với tên (một nút Lưu) — AC-01, AC-06.

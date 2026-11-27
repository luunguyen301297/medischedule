# BA SPEC: Ca làm việc và Lịch làm việc nhân viên

- **Trạng thái:** Đã phê duyệt
- **Độ phức tạp:** Trung bình
- **Domain:** schedule (lịch nhân sự)

## 1. Tổng quan & Mục tiêu

Phòng khám cần khai báo các ca làm việc chuẩn (ví dụ: Sáng 07:00–11:30, Chiều 13:00–17:00) và phân nhân viên vào ca
theo từng ngày, rồi xem lịch đi làm theo ngày/tuần/tháng để biết hôm nào ai làm ca nào. Áp dụng cho mọi nhân viên
(bác sĩ, lễ tân, dược sĩ; quản trị viên không đi ca). Tính năng này tách biệt với ca khám của bác sĩ đang dùng cho đặt lịch (bảng
`shift`): phân ca làm việc không tạo hay sửa ca khám, không ảnh hưởng slot đặt lịch.

## 2. Danh sách User Stories

- **US-01:** Với vai trò là quản trị viên, tôi muốn thêm, sửa, bật/tắt các ca làm việc trong menu Cài đặt › Ca làm
  việc, để có danh sách ca chuẩn dùng khi phân lịch.
- **US-02:** Với vai trò là quản trị viên, tôi muốn gán một hoặc nhiều nhân viên vào một ca của một ngày cụ thể, để lập
  lịch đi làm.
- **US-03:** Với vai trò là quản trị viên, tôi muốn gỡ nhân viên khỏi ca của một ngày, để chỉnh lịch khi có thay đổi.
- **US-04:** Với vai trò là quản trị viên, tôi muốn xem lịch làm việc theo ngày, tuần hoặc tháng, để biết mỗi ngày có
  những ai đi làm và làm ca nào.

## 3. Tiêu chí nghiệm thu (BDD)

### Ca làm việc (US-01)

- **AC-01 (Luồng chính):** Với điều kiện admin đang ở Cài đặt › Ca làm việc, khi nhập tên, giờ bắt đầu, giờ kết thúc,
  màu hiển thị rồi lưu, thì ca mới xuất hiện trong danh sách ở trạng thái đang dùng, sắp theo giờ bắt đầu.
- **AC-02 (Trường hợp biên):** Với điều kiện giờ kết thúc không sau giờ bắt đầu, khi lưu, thì hệ thống báo lỗi
  "Giờ kết thúc phải sau giờ bắt đầu" và không lưu.
- **AC-03 (Trường hợp biên):** Với điều kiện đã có ca cùng tên (không phân biệt hoa thường), khi lưu ca mới hoặc đổi
  tên, thì hệ thống báo trùng tên và không lưu.
- **AC-04 (Luồng chính):** Với điều kiện ca đã có, khi admin sửa tên/giờ/màu, thì thay đổi áp dụng cho mọi ngày đã
  phân ca đó (lịch hiển thị giờ mới).
- **AC-05 (Luồng chính):** Với điều kiện ca đang dùng, khi admin tắt ca, thì ca không còn trong danh sách chọn khi
  phân lịch mới, nhưng các ngày đã phân ca đó vẫn hiển thị trên lịch.
- **AC-06 (Trường hợp biên):** Với điều kiện ca chưa từng được phân cho ai, khi admin xoá, thì ca bị xoá; với điều
  kiện ca đã được phân, thì hệ thống không cho xoá và gợi ý tắt ca.
- **AC-07 (Phân quyền):** Với điều kiện người dùng không phải ADMIN, khi truy cập menu hoặc API ca làm việc, thì bị
  từ chối.

### Phân ca (US-02, US-03)

- **AC-08 (Luồng chính):** Với điều kiện admin chọn một ngày và một ca đang dùng, khi chọn một hoặc nhiều nhân viên
  đang hoạt động rồi lưu, thì các nhân viên đó xuất hiện trong ca của ngày đó.
- **AC-09 (Trường hợp biên):** Với điều kiện nhân viên đã có trong ca đó của ngày đó, khi gán lại, thì không tạo bản
  ghi trùng (bỏ qua người đã có).
- **AC-10 (Luồng chính):** Với điều kiện một nhân viên đã làm ca Sáng, khi admin gán thêm ca Chiều cùng ngày, thì được
  phép (một người có thể làm nhiều ca trong ngày).
- **AC-11 (Trường hợp biên):** Với điều kiện hai ca có giờ chồng nhau, khi gán cùng một nhân viên vào cả hai ca trong
  cùng ngày, thì hệ thống báo trùng giờ và không lưu.
- **AC-12 (Trường hợp biên):** Với điều kiện nhân viên đã bị khoá (không hoạt động), khi phân ca, thì nhân viên đó
  không có trong danh sách chọn; lịch cũ của người đó vẫn hiển thị.
- **AC-13 (Luồng chính):** Với điều kiện nhân viên đang có trong ca của một ngày, khi admin gỡ, thì người đó biến khỏi
  ca của ngày đó, các ngày khác không đổi.
- **AC-14 (Trường hợp biên):** Với điều kiện ngày đã qua, khi admin xem lịch, thì vẫn thấy dữ liệu nhưng không thêm
  hoặc gỡ được.

### Xem lịch (US-04)

- **AC-15 (Luồng chính — ngày):** Với điều kiện admin chọn chế độ Ngày, khi chọn một ngày, thì thấy từng ca (tên,
  giờ, màu) kèm danh sách nhân viên làm ca đó: họ tên, vai trò; ca không có ai hiển thị "Chưa phân".
- **AC-16 (Luồng chính — tuần):** Với điều kiện chế độ Tuần, khi chọn một tuần (thứ Hai → Chủ nhật), thì thấy bảng 7
  ngày, mỗi ô ngày liệt kê các ca và nhân viên trong ca.
- **AC-17 (Luồng chính — tháng):** Với điều kiện chế độ Tháng, khi chọn một tháng, thì thấy lịch tháng; mỗi ngày tóm
  tắt số người theo từng ca (màu ca); bấm vào ngày thì mở chế độ Ngày của ngày đó.
- **AC-18 (Luồng chính):** Với điều kiện đang xem bất kỳ chế độ nào, khi lọc theo vai trò (ví dụ chỉ Bác sĩ), thì
  chỉ hiển thị nhân viên vai trò đó.
- **AC-19 (Luồng chính):** Với điều kiện đang xem, khi bấm Trước/Sau/Hôm nay, thì lịch chuyển sang ngày/tuần/tháng
  tương ứng.

### Menu

- **AC-20:** Với điều kiện admin đăng nhập, khi mở menu, thì thấy "Lịch làm việc" là mục đầu tiên ở cấp ngoài cùng và "Ca làm việc"
  trong nhóm Cài đặt.

## 4. Ảnh hưởng liên Capability

Tính năng mới, domain chưa có KB — bỏ qua. Không chạm bảng `shift` / `appointment`.

## 5. Giả định

- **GĐ-01:** Một nhân viên được làm nhiều ca trong ngày nếu giờ các ca không chồng nhau — AC-10, AC-11. Nếu sai (mỗi
  người tối đa 1 ca/ngày) thì đổi AC-10 thành lỗi.
- **GĐ-02:** Ca không qua đêm (giờ kết thúc phải sau giờ bắt đầu trong cùng ngày) — AC-02. Nếu cần ca đêm thì phải
  thêm cờ qua đêm.
- **GĐ-03:** Không cho sửa lịch của ngày đã qua — AC-14. Nếu cần chấm công sửa ngược thì bỏ ràng buộc này.
- **GĐ-04:** Menu "Lịch làm việc" ở cấp ngoài cùng, đầu danh sách (user đổi sau duyệt), chỉ ADMIN dùng; nhân viên tự xem lịch của
  mình để phase sau — AC-20.
- **GĐ-05:** Không có số người tối thiểu mỗi ca, không sao chép tuần, không lặp theo tuần (theo trả lời GATE 1).

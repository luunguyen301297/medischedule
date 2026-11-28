# BA SPEC: Menu Lịch hẹn trong khu quản trị — danh sách lịch hẹn và đặt lịch hộ khách

- **Trạng thái:** Đã phê duyệt
- **Độ phức tạp:** Lớn
- **Domain:** booking (đặt lịch khám)

## 1. Tổng quan & Mục tiêu

Lễ tân và quản trị viên cần một chỗ duy nhất để xem khách nào đã hẹn khám, khám với bác sĩ nào, giờ nào, và đặt lịch
thay cho khách gọi điện hoặc tới quầy. Hiện hệ thống chưa tạo được lịch hẹn thật (bảng `appointment` trống, form trang
chủ chỉ gửi yêu cầu đặt bác sĩ chờ xếp ca). Task này dựng luồng lịch hẹn đầu tiên, phía nhân viên:

- Menu **Lịch hẹn** đứng đầu sidebar khu quản trị.
- Danh sách lịch hẹn có lọc, tìm kiếm, hủy lịch.
- Đặt lịch hộ khách theo giờ cụ thể của bác sĩ đang trực.
- Mỗi chuyên khoa có cài đặt **thời gian khám một lượt** (độ dài một slot), quyết định lưới giờ khi đặt.

Ca khám của bác sĩ không nhập tay: bác sĩ được phân vào ca làm việc (Sáng / Chiều / Tối) ngày nào thì ngày đó có ca
khám, khám tại phòng của chuyên khoa mình, chia slot theo thời gian khám của khoa.

Ngoài phạm vi: khách tự đặt lịch hẹn trên web/app, check-in, đổi lịch, no-show, voucher, phí chỉ định, gửi SMS/thông
báo, chuyển yêu cầu đặt bác sĩ (`doctor_request`) thành lịch hẹn.

## 2. Danh sách User Stories

- **US-01:** Với vai trò là quản trị viên hoặc lễ tân, tôi muốn thấy menu "Lịch hẹn" ở đầu sidebar, để vào ngay việc
  dùng nhiều nhất.
- **US-02:** Với vai trò là quản trị viên hoặc lễ tân, tôi muốn xem danh sách lịch hẹn theo ngày, lọc theo bác sĩ và
  trạng thái, tìm theo tên, SĐT hoặc mã lịch, để biết ai sẽ tới khám và chuẩn bị đón.
- **US-03:** Với vai trò là quản trị viên hoặc lễ tân, tôi muốn đặt lịch hộ khách bằng cách chọn ngày, buổi, bác sĩ đang
  trực và một giờ còn trống, để khách gọi điện hoặc tới quầy có lịch ngay.
- **US-04:** Với vai trò là quản trị viên hoặc lễ tân, tôi muốn nhập SĐT để tìm hồ sơ bệnh nhân đã có, chọn đúng người
  hoặc tạo hồ sơ mới, để không sinh hồ sơ trùng cho khách quen.
- **US-05:** Với vai trò là quản trị viên hoặc lễ tân, tôi muốn hủy một lịch hẹn kèm lý do, để giải phóng giờ đó cho
  khách khác.
- **US-06:** Với vai trò là quản trị viên, tôi muốn đặt thời gian khám một lượt cho từng chuyên khoa, để lưới giờ hẹn
  khớp thực tế từng khoa (vd. Da liễu 10 phút, Răng Hàm Mặt 30 phút).

## 3. Tiêu chí nghiệm thu (BDD)

### Menu và quyền

- **AC-01 (Luồng chính):** Với điều kiện quản trị viên đã đăng nhập, khi mở khu quản trị, thì mục "Lịch hẹn" đứng đầu
  sidebar, trên "Lịch làm việc"; bấm vào mở trang danh sách lịch hẹn.
- **AC-02 (Luồng chính):** Với điều kiện lễ tân đã đăng nhập, khi vào hệ thống, thì được đưa tới trang Lịch hẹn; sidebar
  chỉ có mục "Lịch hẹn", không thấy các mục quản trị khác.
- **AC-03 (Trường hợp biên):** Với điều kiện lễ tân gõ thẳng đường dẫn trang quản trị khác (vd. trang Nhân viên), khi trang
  tải, thì bị đưa về trang Lịch hẹn; API quản trị khác trả lỗi không có quyền.
- **AC-04 (Trường hợp biên):** Với điều kiện người dùng có vai trò bác sĩ, dược sĩ hoặc bệnh nhân, khi gọi API danh sách,
  đặt hộ hoặc hủy lịch hẹn, thì bị từ chối (không có quyền).

### Danh sách lịch hẹn

- **AC-05 (Luồng chính):** Với điều kiện có lịch hẹn trong ngày, khi mở trang Lịch hẹn, thì mặc định lọc ngày hôm nay,
  mỗi dòng có: mã lịch, giờ hẹn, họ tên + SĐT bệnh nhân, bác sĩ, chuyên khoa, phòng, kênh đặt (kèm tên nhân viên nếu
  đặt hộ), trạng thái, thời điểm đặt; lịch đã hủy hiện lý do hủy; sắp theo giờ hẹn tăng dần.
- **AC-06 (Luồng chính):** Với điều kiện đang ở trang danh sách, khi đổi ngày (hoặc khoảng ngày), chọn bác sĩ, chọn
  trạng thái, hoặc gõ tên / SĐT / mã lịch, thì danh sách lọc theo đúng các điều kiện đó; xóa ô tìm kiếm thì về danh sách
  theo các bộ lọc còn lại.
- **AC-07 (Trường hợp biên):** Với điều kiện không có lịch nào khớp bộ lọc, khi danh sách tải xong, thì hiện trạng thái
  trống "Không có lịch hẹn" kèm nút "Đặt lịch hộ".
- **AC-08 (Trường hợp biên):** Với điều kiện có nhiều hơn 20 lịch khớp, khi xem danh sách, thì phân trang 20 dòng một
  trang và hiện tổng số.

### Đặt lịch hộ

- **AC-09 (Luồng chính):** Với điều kiện bác sĩ A được phân ca Sáng (07:00–11:30) ngày mai, khoa của A có thời gian khám
  15 phút và có phòng P101, khi nhân viên chọn ngày mai → buổi Sáng → bác sĩ A, thì hiện lưới giờ 07:00, 07:15, …,
  11:15; giờ đã có lịch (chưa hủy) bị khóa.
- **AC-10 (Luồng chính):** Với điều kiện đã chọn giờ trống, khi nhân viên nhập SĐT khách, chọn hoặc tạo hồ sơ bệnh nhân
  và bấm "Đặt lịch", thì tạo lịch hẹn trạng thái "Đã đặt", kênh "Nhân viên đặt", có mã lịch duy nhất; hiện thông báo
  thành công kèm mã lịch; danh sách cập nhật ngay.
- **AC-11 (Luồng chính):** Với điều kiện SĐT đã có một hoặc nhiều hồ sơ bệnh nhân, khi nhập đủ SĐT, thì hiện các hồ sơ
  đó (họ tên, ngày sinh, mã bệnh nhân) để chọn, cùng lựa chọn "Người khác — tạo hồ sơ mới".
- **AC-12 (Luồng chính):** Với điều kiện SĐT chưa có hồ sơ hoặc chọn "tạo hồ sơ mới", khi nhập họ tên (bắt buộc), ngày
  sinh và giới tính (không bắt buộc) rồi đặt lịch, thì hệ thống tạo hồ sơ bệnh nhân mới (nguồn: lễ tân) với mã bệnh
  nhân mới và gắn lịch vào hồ sơ đó.
- **AC-13 (Trường hợp biên):** Với điều kiện hai nhân viên cùng chọn một giờ của cùng bác sĩ, khi cả hai bấm đặt, thì chỉ
  một lịch được tạo; người sau nhận thông báo "Giờ này vừa có người đặt, vui lòng chọn giờ khác" và lưới giờ tải lại.
- **AC-14 (Trường hợp biên):** Với điều kiện ngày khám ngoài khoảng hôm nay → 6 ngày tới, khi đặt, thì bị từ chối "Chỉ
  đặt được trong 7 ngày tới"; giờ đã qua của hôm nay không hiện là giờ trống.
- **AC-15 (Trường hợp biên):** Với điều kiện SĐT người đặt đã có đủ số lịch đang chờ khám (trạng thái "Đã đặt") theo cài
  đặt hệ thống `booking.max_pending_per_phone` (hiện là 2), khi đặt thêm, thì bị từ chối "Số điện thoại này đã có N lịch
  đang chờ khám".
- **AC-16 (Trường hợp biên):** Với điều kiện bác sĩ không còn được phân ca đó, bị khóa tài khoản, chưa thuộc chuyên khoa
  nào, hoặc khoa không có phòng đang hoạt động, khi xem lưới giờ hoặc đặt lịch, thì không đặt được và hiện lý do cụ thể
  (vd. "Khoa Mắt chưa có phòng khám đang hoạt động").
- **AC-17 (Trường hợp biên):** Với điều kiện SĐT không đúng định dạng (10 số, bắt đầu bằng 0) hoặc thiếu họ tên khi tạo hồ
  sơ mới, khi bấm đặt, thì báo lỗi ngay tại ô nhập, không gửi đi.
- **AC-18 (Trường hợp biên):** Với điều kiện bác sĩ đã có ca khám ngày đó (đã có lịch được đặt), khi quản trị viên đổi
  thời gian khám của khoa, thì ca khám đã có giữ nguyên độ dài slot cũ; chỉ ngày/ca chưa có lịch nào mới dùng giá trị
  mới.

### Hủy lịch

- **AC-19 (Luồng chính):** Với điều kiện lịch đang ở trạng thái "Đã đặt", khi nhân viên bấm "Hủy", nhập lý do và xác
  nhận, thì lịch chuyển "Đã hủy", lưu lý do, giờ đó trở lại trống trong lưới giờ.
- **AC-20 (Trường hợp biên):** Với điều kiện lịch không ở trạng thái "Đã đặt" (đã hủy, đã check-in, đang khám, xong),
  khi xem dòng đó, thì không có nút "Hủy"; gọi API hủy thì bị từ chối.
- **AC-21 (Trường hợp biên):** Với điều kiện để trống lý do hủy, khi xác nhận, thì báo "Nhập lý do hủy".

### Cài đặt thời gian khám theo khoa

- **AC-22 (Luồng chính):** Với điều kiện quản trị viên ở trang Chuyên khoa, khi thêm hoặc sửa một khoa, thì có ô "Thời gian
  khám mỗi lượt (phút)", mặc định 15; lưu xong, bảng chuyên khoa hiện giá trị này.
- **AC-23 (Trường hợp biên):** Với điều kiện nhập thời gian khám ngoài 5–120 phút hoặc không chia hết cho 5, khi lưu, thì
  báo lỗi tại ô nhập.

### Thay đổi 28/11 — bác sĩ và giờ khám không bắt buộc (yêu cầu bổ sung của user)

- **AC-24 (Luồng chính):** Với điều kiện khách không chỉ định bác sĩ, khi nhân viên chỉ chọn ngày + buổi (chuyên khoa
  không bắt buộc), nhập SĐT, chọn/tạo hồ sơ rồi đặt, thì tạo lịch "Đã đặt" chưa có bác sĩ, chưa có giờ; phòng khám
  xếp bác sĩ khi khách tới check-in (giống khách tự đặt không chỉ định khoa/bác sĩ trên trang chủ).
- **AC-25 (Luồng chính):** Với điều kiện khách chỉ định bác sĩ, khi nhân viên chọn bác sĩ trực buổi đó, thì bắt buộc chọn
  giờ trống như AC-09/AC-10 và lịch được đánh dấu chỉ định.
- **AC-26 (Luồng chính):** Với điều kiện lịch chưa có bác sĩ, khi xem danh sách, thì cột giờ hiện buổi (vd. "Chiều
  13:00–17:00") và cột bác sĩ hiện "Chờ xếp bác sĩ" kèm chuyên khoa nếu có; lọc theo ngày vẫn ra lịch này.
- **AC-27 (Trường hợp biên):** Với điều kiện ngày là hôm nay và buổi đã kết thúc, khi đặt không chỉ định, thì bị từ chối
  "Buổi khám này của hôm nay đã kết thúc".
- **AC-28 (Trường hợp biên):** Với điều kiện chọn chuyên khoa, khi mở danh sách bác sĩ, thì chỉ hiện bác sĩ trực buổi đó
  thuộc khoa đã chọn.

## 4. Ảnh hưởng liên Capability

Bỏ qua theo template (Full Pipeline, chưa có KB). Điểm chạm code cần QA hồi quy:

- Trang Chuyên khoa (thêm field thời gian khám): thêm/sửa/bật tắt khoa vẫn chạy như cũ.
- Khung khu quản trị (`AdminLayout`) và trang chủ sau đăng nhập (`HomePage`): quản trị viên vẫn vào đủ các trang; bệnh
  nhân, bác sĩ không vào được khu quản trị.
- Form đặt lịch trang chủ và yêu cầu đặt bác sĩ: không đổi.

## 5. Giả định

- **GĐ-01:** Nhãn menu là "Lịch hẹn" (tiếng Việt như các menu khác), đường dẫn `/admin/bookings` — US-01. Muốn giữ chữ
  "Booking" thì chỉ đổi nhãn.
- **GĐ-02:** Lễ tân dùng chung khung khu quản trị nhưng chỉ thấy menu Lịch hẹn; đăng nhập xong vào thẳng trang Lịch hẹn
  — AC-02, AC-03.
- **GĐ-03:** Giới hạn "như khách tự đặt" với lịch hẹn = cài đặt hệ thống `booking.max_pending_per_phone` (2 lịch "Đã
  đặt" mỗi SĐT) + khoảng ngày hôm nay → 6 ngày tới + mỗi giờ của bác sĩ chỉ một lịch. Không áp chặn no-show (cài đặt
  đó dành cho đặt online, và chưa có luồng ghi no-show) và không áp hạn "hủy trước 2 giờ" cho nhân viên — AC-14, AC-15,
  AC-19. Nếu muốn dùng mức 3 như yêu cầu đặt bác sĩ thì sửa cài đặt hệ thống.
- **GĐ-04:** Kênh đặt mới "Nhân viên đặt" để phân biệt với khách tự đặt; lưu thêm nhân viên nào đã đặt hộ, hiện trong
  danh sách — AC-05, AC-10.
- **GĐ-05:** Phòng khám = phòng đang hoạt động của chuyên khoa bác sĩ; khoa có nhiều phòng thì lấy phòng tạo sớm nhất.
  Một ca khám cố định phòng từ lúc có lịch đầu tiên — AC-09, AC-16.
- **GĐ-06:** (sửa 28/11) Có chọn bác sĩ = lịch chỉ định (`is_designated`), phí chỉ định tạm để 0 vì chưa có bảng giá; không áp voucher — AC-25.
- **GĐ-07:** Số khách tối đa của ca = thời lượng ca ÷ thời gian khám (làm tròn xuống); không cho đặt quá số slot — AC-09.
- **GĐ-08:** Mã lịch gồm 8 ký tự chữ in hoa và số, bỏ các ký tự dễ nhầm (0/O, 1/I) — AC-10.
- **GĐ-09:** Tìm hồ sơ bệnh nhân theo SĐT khớp chính xác 10 số; chỉ hiện hồ sơ chưa bị gộp — AC-11.
- **GĐ-10:** Đổi buổi làm việc (ca Sáng/Chiều/Tối) hoặc gỡ phân ca của bác sĩ đã có lịch hẹn không bị chặn ở task này; lịch
  đã đặt giữ nguyên, nhân viên tự hủy nếu cần — AC-16.
- **GĐ-11:** Lịch không chỉ định không giới hạn số lượng mỗi buổi (chỉ giới hạn lịch chờ theo SĐT), giống form trang chủ
  — AC-24. Nếu cần trần theo buổi thì thêm cài đặt hệ thống ở đợt sau.

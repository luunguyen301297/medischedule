# BA SPEC: Menu Chuyên khoa (quản lý chuyên khoa và phòng khám)

- **Trạng thái:** Đã phê duyệt (Status: Approved)
- **Độ phức tạp:** Trung bình
- **Domain:** clinic (danh mục phòng khám)

## 1. Tổng quan & Mục tiêu

Phòng khám đa khoa có nhiều chuyên khoa (Nội, Nhi, Da liễu…), mỗi chuyên khoa có một hoặc nhiều phòng khám (buồng khám
thật, ví dụ "Nội 1", "P101"). Hiện chưa có chỗ nào để quản trị viên khai báo hai danh mục này, còn ô chọn chuyên khoa
ở form đặt lịch trang chủ lấy từ danh sách gõ tay trong "Thiết lập trang chủ", không gắn với dữ liệu thật.

Task này thêm menu con **Chuyên khoa** trong nhóm **Cài đặt** của khu quản trị để quản lý chuyên khoa và các phòng
khám thuộc từng chuyên khoa, rồi cho form đặt lịch trang chủ lấy chuyên khoa từ danh mục này.

Ngoài phạm vi: phòng dùng chung không thuộc chuyên khoa nào (xét nghiệm, siêu âm, X-quang). Menu này không hiển thị và
không sửa các phòng đó.

## 2. Danh sách User Stories

- **US-01:** Với vai trò là quản trị viên, tôi muốn xem, thêm, sửa tên, ngừng/mở lại hoạt động và xóa chuyên khoa, để
  danh mục chuyên khoa khớp với những gì phòng khám đang thực sự khám.
- **US-02:** Với vai trò là quản trị viên, tôi muốn xem, thêm, sửa, chuyển sang chuyên khoa khác, ngừng/mở lại hoạt
  động và xóa phòng khám của từng chuyên khoa, để sau này xếp ca trực bác sĩ vào đúng phòng.
- **US-03:** Với vai trò là khách vào trang chủ, tôi muốn ô chọn chuyên khoa ở form đặt lịch chỉ hiện các chuyên khoa
  đang hoạt động, để không đặt nhầm vào chuyên khoa phòng khám không còn khám.

## 3. Tiêu chí nghiệm thu (BDD)

**Menu và quyền**

- **AC-01 (Luồng chính):** Với điều kiện đăng nhập bằng tài khoản quản trị viên, khi mở nhóm Cài đặt trong menu trái,
  thì thấy mục **Chuyên khoa** ngay dưới **Trang chủ**; bấm vào thì mở trang quản lý chuyên khoa.
- **AC-02 (Trường hợp biên):** Với điều kiện đăng nhập bằng vai trò khác quản trị viên (hoặc chưa đăng nhập), khi gọi
  thao tác xem/thêm/sửa/xóa chuyên khoa hoặc phòng khám, thì bị từ chối và dữ liệu không đổi.

**Chuyên khoa**

- **AC-03 (Luồng chính):** Với điều kiện vào trang Chuyên khoa, khi trang tải xong, thì thấy mọi chuyên khoa (cả đang
  ngừng), mỗi dòng có tên, trạng thái, số phòng khám; chuyên khoa đang ngừng được đánh dấu rõ.
- **AC-04 (Luồng chính):** Với điều kiện nhập tên chuyên khoa mới hợp lệ, khi lưu, thì chuyên khoa được tạo ở trạng
  thái đang hoạt động và hiện ngay trong danh sách.
- **AC-05 (Trường hợp biên):** Với điều kiện tên để trống, chỉ có khoảng trắng hoặc dài quá 100 ký tự, khi lưu, thì
  báo lỗi tại ô tên và không tạo.
- **AC-06 (Trường hợp biên):** Với điều kiện đã có chuyên khoa "Nhi", khi thêm hoặc đổi tên một chuyên khoa khác thành
  "nhi" hoặc " Nhi ", thì báo "Chuyên khoa đã tồn tại" và không lưu.
- **AC-07 (Luồng chính):** Với điều kiện chuyên khoa đang hoạt động, khi sửa tên hợp lệ và lưu, thì tên mới hiện ở
  danh sách và ở mọi nơi đang hiển thị chuyên khoa đó.
- **AC-08 (Trường hợp biên):** Với điều kiện chuyên khoa còn ít nhất một phòng đang hoạt động, khi ngừng hoạt động
  chuyên khoa, thì bị chặn với thông báo phải ngừng hoặc chuyển hết phòng trước; trạng thái không đổi.
- **AC-09 (Luồng chính):** Với điều kiện chuyên khoa không còn phòng đang hoạt động, khi ngừng hoạt động, thì chuyên
  khoa chuyển sang đang ngừng; mở lại hoạt động thì chuyển về đang hoạt động.
- **AC-10 (Luồng chính):** Với điều kiện chuyên khoa chưa được dùng ở đâu (không có phòng khám, bác sĩ hay dịch vụ nào
  gắn với nó), khi xóa và xác nhận, thì chuyên khoa bị xóa hẳn khỏi danh sách.
- **AC-11 (Trường hợp biên):** Với điều kiện chuyên khoa đã có phòng khám (kể cả phòng đang ngừng), bác sĩ hoặc dịch
  vụ gắn vào, khi xóa, thì báo không xóa được vì đang được dùng và gợi ý ngừng hoạt động; dữ liệu không đổi.

**Phòng khám**

- **AC-12 (Luồng chính):** Với điều kiện chọn một chuyên khoa trên trang, khi xem phòng khám, thì thấy danh sách phòng
  của chuyên khoa đó (cả đang ngừng) với tên và trạng thái.
- **AC-13 (Luồng chính):** Với điều kiện chuyên khoa đang hoạt động và tên phòng hợp lệ, khi thêm phòng, thì phòng được
  tạo ở trạng thái đang hoạt động, thuộc chuyên khoa đó.
- **AC-14 (Trường hợp biên):** Với điều kiện tên phòng để trống, chỉ có khoảng trắng hoặc dài quá 50 ký tự, khi lưu,
  thì báo lỗi tại ô tên và không lưu.
- **AC-15 (Trường hợp biên):** Với điều kiện đã có phòng "P101" ở bất kỳ chuyên khoa nào (kể cả phòng dùng chung), khi
  thêm hoặc đổi tên phòng khác thành "p101", thì báo "Tên phòng đã tồn tại" và không lưu.
- **AC-16 (Luồng chính):** Với điều kiện sửa một phòng, khi chọn chuyên khoa khác đang hoạt động và lưu, thì phòng
  chuyển sang chuyên khoa mới và biến mất khỏi danh sách phòng của chuyên khoa cũ.
- **AC-17 (Trường hợp biên):** Với điều kiện chuyên khoa đang ngừng, khi thêm phòng vào, chuyển phòng vào, hoặc mở lại
  hoạt động một phòng của chuyên khoa đó, thì bị chặn với thông báo phải mở lại chuyên khoa trước.
- **AC-18 (Luồng chính):** Với điều kiện phòng đang hoạt động, khi ngừng hoạt động, thì phòng chuyển sang đang ngừng;
  mở lại thì về đang hoạt động (khi chuyên khoa đang hoạt động).
- **AC-19 (Luồng chính):** Với điều kiện phòng chưa từng được xếp ca trực, khi xóa và xác nhận, thì phòng bị xóa hẳn.
- **AC-20 (Trường hợp biên):** Với điều kiện phòng đã có ít nhất một ca trực (bất kỳ trạng thái), khi xóa, thì báo
  không xóa được vì đang được dùng và gợi ý ngừng hoạt động; dữ liệu không đổi.

**Trang chủ**

- **AC-21 (Luồng chính):** Với điều kiện khách mở trang chủ, khi mở ô chọn chuyên khoa ở form đặt lịch, thì thấy đúng
  các chuyên khoa đang hoạt động, sắp theo tên A→Z; chuyên khoa đang ngừng không xuất hiện.
- **AC-22 (Luồng chính):** Với điều kiện quản trị viên vừa thêm, đổi tên hoặc ngừng một chuyên khoa, khi khách tải lại
  trang chủ, thì ô chọn phản ánh thay đổi đó.
- **AC-23 (Luồng chính):** Với điều kiện vào "Thiết lập trang chủ", khi mở tab Đặt lịch đầu trang, thì không còn ô gõ
  danh sách chuyên khoa; thay vào đó là một dòng cho biết danh sách lấy từ menu Chuyên khoa (có link tới đó).
- **AC-24 (Trường hợp biên):** Với điều kiện không có chuyên khoa nào đang hoạt động, khi khách mở form đặt lịch, thì
  ô chọn trống kèm thông báo chưa có chuyên khoa, và không gửi được form.

## 4. Ảnh hưởng liên Capability

Bỏ qua (Full Pipeline, chưa có KB). Ghi chú cho SA/QA: bảng `specialty` đang được `staff`, `service` trỏ tới, bảng
`room` được `shift` trỏ tới; chưa có code nghiệp vụ nào ghi vào các bảng này. Trang chủ (`features/landing`,
`HomepageSettingsPage`) đổi nguồn danh sách chuyên khoa → QA hồi quy form đặt lịch và trang Thiết lập trang chủ.

## 5. Giả định

- **GĐ-01:** Dữ liệu ban đầu của danh mục chuyên khoa là 10 chuyên khoa đang hiện ở trang chủ (Nội tổng quát, Nhi, Tai
  Mũi Họng, Da liễu, Sản phụ khoa, Tim mạch, Tiêu hóa, Cơ xương khớp, Mắt, Răng Hàm Mặt), để trang chủ không bị trống
  sau khi đổi nguồn — áp dụng cho AC-21. Nếu sai thì danh mục bắt đầu rỗng và quản trị viên tự nhập.
- **GĐ-02:** Trùng tên so sánh không phân biệt hoa thường và bỏ khoảng trắng đầu/cuối; tên lưu đã bỏ khoảng trắng đầu/
  cuối — AC-06, AC-15. Nếu sai thì "Nhi" và "nhi" được coi là hai chuyên khoa.
- **GĐ-03:** Chuyên khoa chỉ có một thông tin là tên (không mô tả, ảnh, thứ tự hiển thị); trang chủ sắp A→Z — AC-03,
  AC-21. Nếu cần sắp tay thì phải thêm trường thứ tự.
- **GĐ-04:** Tên phòng duy nhất trên toàn phòng khám, không chỉ trong một chuyên khoa — AC-15 (theo ràng buộc dữ liệu
  hiện có).
- **GĐ-05:** Trang dạng hai cột: trái là danh sách chuyên khoa, phải là phòng khám của chuyên khoa đang chọn; không phân
  trang (số chuyên khoa và phòng của một phòng khám ở mức vài chục) — AC-03, AC-12.
- **GĐ-06:** Đổi tên chuyên khoa không ảnh hưởng lượt đặt lịch đã gửi từ trang chủ, vì form đặt lịch hiện chưa lưu xuống
  hệ thống — AC-07.

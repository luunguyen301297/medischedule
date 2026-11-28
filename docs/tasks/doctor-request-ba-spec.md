# BA SPEC: Đặt bác sĩ từ hồ sơ bác sĩ (yêu cầu chờ xếp ca)

- **Trạng thái:** Đã phê duyệt (Status: Approved)
- **Độ phức tạp:** Trung bình
- **Domain:** booking

## 1. Tổng quan & Mục tiêu

Bệnh nhân xem hồ sơ một bác sĩ trên trang chủ và muốn khám đúng bác sĩ đó, kể cả khi bác sĩ chưa được phân ca vào ngày
/ buổi họ muốn. Hệ thống ghi nhận **yêu cầu đặt bác sĩ** (ngày + buổi mong muốn) để nhân sự sắp xếp ca cho bác sĩ sau.
Yêu cầu chưa phải lịch hẹn: không cấp giờ, không chiếm chỗ trong ca, chưa có mã lịch.

## 2. Danh sách User Stories

- **US-01:** Với vai trò là bệnh nhân đã đăng nhập, tôi muốn bấm "Đặt bác sĩ này" trong hồ sơ bác sĩ rồi chọn ngày và
  buổi, để gửi yêu cầu khám đúng bác sĩ mình chọn mà không phải nhập lại họ tên, số điện thoại.
- **US-02:** Với vai trò là khách chưa đăng nhập, tôi muốn gửi yêu cầu đặt bác sĩ bằng họ tên và số điện thoại, để không
  bắt buộc tạo tài khoản.
- **US-03:** Với vai trò là nhân sự phòng khám, tôi muốn các yêu cầu được lưu trong hệ thống ở trạng thái chờ xếp ca,
  để sau này sắp ca cho bác sĩ theo nhu cầu thật (màn xử lý làm ở đợt sau).

## 3. Tiêu chí nghiệm thu (BDD)

**Mở form đặt**

- **AC-01 (Luồng chính):** Với điều kiện dialog hồ sơ bác sĩ đang mở, khi bấm nút "Đặt bác sĩ này", thì mở dialog đặt
  gồm: tên + chuyên khoa bác sĩ (chỉ đọc), dải chọn 7 ngày tới (hôm nay → 6 ngày sau, mặc định ngày mai) và các ô buổi
  khám, cùng kiểu với form đặt lịch ở trang chủ.
- **AC-02 (Luồng chính):** Với điều kiện mở dialog đặt, khi xem các ô buổi, thì buổi lấy theo các ca làm việc đang bật
  (Sáng / Chiều / Tối và giờ), mặc định chọn buổi đầu tiên; buổi đã kết thúc của hôm nay bị khóa.
- **AC-03 (Luồng chính):** Với điều kiện đã đăng nhập bằng tài khoản bệnh nhân, khi mở dialog đặt, thì không có ô họ
  tên / số điện thoại; hệ thống dùng thông tin tài khoản.
- **AC-04 (Luồng chính):** Với điều kiện chưa đăng nhập, khi mở dialog đặt, thì có ô Họ và tên (bắt buộc, tối đa 120 ký
  tự) và Số điện thoại (bắt buộc, 10 số bắt đầu bằng 0).

**Gửi yêu cầu**

- **AC-05 (Luồng chính):** Với điều kiện đã chọn ngày + buổi (và nhập đủ họ tên / SĐT nếu là khách), khi bấm "Gửi yêu
  cầu", thì hiện hộp xác nhận liệt kê bác sĩ, chuyên khoa, ngày, buổi (giờ), người khám, số điện thoại; bấm "Xác nhận"
  mới gửi, bấm "Sửa lại" thì quay về form giữ nguyên lựa chọn.
- **AC-06 (Luồng chính):** Với điều kiện xác nhận gửi thành công, khi hệ thống lưu xong, thì dialog hiện thông báo "Đã
  gửi yêu cầu đặt bác sĩ" kèm tóm tắt (bác sĩ, buổi, ngày) và câu "Phòng khám sẽ sắp xếp ca và gọi số … để xác nhận";
  yêu cầu được lưu ở trạng thái **Chờ xếp ca**.
- **AC-07 (Luồng chính):** Với điều kiện yêu cầu đã lưu, khi nhân sự tra trong hệ thống, thì mỗi yêu cầu có: bác sĩ, ngày,
  buổi (ca làm việc), họ tên, số điện thoại, tài khoản người đặt (nếu đã đăng nhập), trạng thái, thời điểm gửi.
- **AC-08 (Trường hợp biên):** Với điều kiện đang gửi, khi bấm xác nhận nhiều lần, thì chỉ một yêu cầu được tạo (nút
  quay chờ, không bấm lặp được).

**Kiểm tra phía hệ thống**

- **AC-09 (Trường hợp biên):** Với điều kiện bác sĩ không còn hoạt động (tài khoản khóa hoặc đã nghỉ) hoặc không tồn tại,
  khi gửi, thì báo "Bác sĩ hiện không nhận đặt lịch" và không lưu.
- **AC-10 (Trường hợp biên):** Với điều kiện ngày ngoài khoảng hôm nay → 6 ngày sau, buổi không tồn tại / đã tắt, hoặc
  buổi của hôm nay đã kết thúc, khi gửi, thì báo lỗi tiếng Việt tương ứng và không lưu.
- **AC-11 (Trường hợp biên):** Với điều kiện cùng số điện thoại đã có yêu cầu Chờ xếp ca cho cùng bác sĩ, cùng ngày,
  cùng buổi, khi gửi lại, thì báo "Bạn đã gửi yêu cầu này, phòng khám sẽ liên hệ" và không tạo trùng.
- **AC-12 (Trường hợp biên — chống spam):** Với điều kiện một số điện thoại đã có 3 yêu cầu Chờ xếp ca, khi gửi thêm, thì
  báo "Bạn đang có 3 yêu cầu chờ xếp ca, vui lòng chờ phòng khám liên hệ" và không lưu.
- **AC-13 (Trường hợp biên):** Với điều kiện họ tên / SĐT sai định dạng (khách), khi gửi, thì báo lỗi tại ô tương ứng.
- **AC-14 (Phân quyền):** Với điều kiện người gọi đã đăng nhập bằng tài khoản không phải bệnh nhân (nhân viên), khi gửi
  theo luồng đã đăng nhập, thì bị từ chối; luồng khách không cần đăng nhập. Người đã đăng nhập không thể gửi thay họ tên
  / SĐT khác ở luồng đã đăng nhập (hệ thống lấy từ tài khoản).

## 4. Ảnh hưởng liên Capability

Domain chưa có KB; blast radius code-level:

- **Capability đang sửa:** booking (yêu cầu đặt bác sĩ — mới).
- **Blast radius code-level:**
  - `DoctorProfileDialog`, `TopDoctors` (landing): thêm nút và dialog đặt.
  - Đọc ca làm việc đang bật (cùng nguồn `GET /api/public/booking/shifts`) và danh sách bác sĩ đang hoạt động
    (`ClinicCatalogService.publicDoctors` / `AuthService.activeDoctors`).
  - Không đổi bảng `appointment` và luồng đặt lịch ở form trang chủ.
- **Đề xuất phạm vi kiểm thử hồi quy cho QA:** form đặt lịch trang chủ (cho mình / cho người khác), dialog hồ sơ bác sĩ,
  danh sách ca đang bật.

## 5. Giả định

- **GĐ-01:** Chống spam theo số điện thoại: tối đa 3 yêu cầu Chờ xếp ca cùng lúc, không trùng (bác sĩ, ngày, buổi) —
  AC-11, AC-12. Nếu sai thì đổi ngưỡng.
- **GĐ-02:** Khoảng ngày chọn giống form trang chủ (7 ngày từ hôm nay) — AC-01, AC-10.
- **GĐ-03:** Đợt này chưa có màn xử lý cho nhân sự, chưa đổi trạng thái, chưa nhắn tin xác nhận; trạng thái khác ngoài
  Chờ xếp ca (Đã xếp ca, Từ chối, Đã hủy) được dự trù trong dữ liệu để đợt sau dùng — US-03, AC-07.
- **GĐ-04:** Bác sĩ có "Nhận chỉ định đích danh = Không" vẫn nhận yêu cầu (yêu cầu chỉ là mong muốn, nhân sự quyết khi
  xếp ca); chưa tính phụ phí chỉ định — AC-01, AC-09.
- **GĐ-05:** Người đã đăng nhập luôn đặt cho chính mình ở luồng này (đặt cho người khác dùng form trang chủ) — AC-03.

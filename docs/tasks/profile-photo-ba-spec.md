# BA SPEC: Ảnh hồ sơ nhân viên / bệnh nhân và hồ sơ bác sĩ trên trang chủ

- **Trạng thái:** Đã phê duyệt (Status: Approved)
- **Độ phức tạp:** Trung bình
- **Domain:** clinic (nhân sự, bác sĩ) + patient (hồ sơ bệnh nhân)

## 1. Tổng quan & Mục tiêu

Bệnh nhân chọn bác sĩ cần biết bác sĩ là ai: bấm ảnh bác sĩ ở cụm "Các bác sĩ chuyên khoa hàng đầu" để xem hồ sơ
chuyên môn. Quản trị viên cần gắn ảnh cho từng nhân viên và từng hồ sơ bệnh nhân để nhận diện nhanh tại quầy và để
ảnh bác sĩ trên trang chủ được quản lý trong hệ thống thay vì chỉ có từ dữ liệu mẫu.

## 2. Danh sách User Stories

- **US-01:** Với vai trò là bệnh nhân, tôi muốn bấm vào ảnh một bác sĩ trên trang chủ để xem hồ sơ chuyên môn của bác
  sĩ đó, để chọn đúng bác sĩ mình tin trước khi đặt lịch.
- **US-02:** Với vai trò là quản trị viên, tôi muốn xem, tải lên, thay hoặc gỡ ảnh trong hồ sơ nhân viên, để ảnh bác sĩ
  hiển thị đúng trên trang chủ và nhân viên được nhận diện trong hệ thống.
- **US-03:** Với vai trò là quản trị viên, tôi muốn xem, tải lên, thay hoặc gỡ ảnh cho từng hồ sơ bệnh nhân (bản thân và
  từng người thân của một tài khoản), để lễ tân nhận diện đúng người khi tiếp đón.

## 3. Tiêu chí nghiệm thu (BDD)

**Hồ sơ bác sĩ trên trang chủ (US-01)**

- **AC-01 (Luồng chính):** Với điều kiện trang chủ đang hiện cụm "Các bác sĩ chuyên khoa hàng đầu", khi người dùng bấm
  vào ảnh hoặc tên một bác sĩ, thì mở dialog hồ sơ bác sĩ gồm: ảnh, họ tên, chuyên khoa, học hàm / học vị, chứng chỉ
  hành nghề, nhận chỉ định đích danh (Có / Không).
- **AC-02 (Luồng chính):** Với điều kiện dialog hồ sơ bác sĩ đang mở, khi xem nội dung, thì không có dòng "Tình trạng",
  không có email, số điện thoại, tên đăng nhập hay thông tin tài khoản nào khác của bác sĩ.
- **AC-03 (Trường hợp biên):** Với điều kiện bác sĩ chưa có ảnh hoặc chưa có một trường hồ sơ, khi mở dialog, thì ảnh
  thay bằng biểu tượng người và trường trống hiện "—".
- **AC-04 (Trường hợp biên):** Với điều kiện người dùng đang kéo / cuộn ngang dải bác sĩ, khi thả chuột sau thao tác
  kéo, thì không mở dialog (chỉ mở khi bấm).
- **AC-05 (Luồng chính):** Với điều kiện dialog đang mở, khi bấm nút đóng, bấm ra ngoài hoặc nhấn Esc, thì dialog đóng.

**Ảnh hồ sơ nhân viên (US-02)**

- **AC-06 (Luồng chính):** Với điều kiện quản trị viên mở hồ sơ một nhân viên (mọi vai trò), khi xem, thì đầu dialog
  hiện ảnh của nhân viên (chưa có ảnh thì hiện biểu tượng người).
- **AC-07 (Luồng chính):** Với điều kiện hồ sơ nhân viên đang ở chế độ Sửa, khi quản trị viên tải ảnh lên rồi Lưu, thì
  ảnh được lưu và hiện ngay trong dialog; nếu nhân viên là bác sĩ đang hoạt động thì ảnh mới hiện trên trang chủ.
- **AC-08 (Luồng chính):** Với điều kiện hồ sơ nhân viên có ảnh và đang ở chế độ Sửa, khi gỡ ảnh rồi Lưu, thì hồ sơ
  không còn ảnh.
- **AC-09 (Trường hợp biên):** Với điều kiện đang Sửa và đã tải ảnh mới, khi bấm Hủy, thì ảnh cũ giữ nguyên.
- **AC-10 (Trường hợp biên):** Với điều kiện tệp chọn không phải ảnh hoặc vượt giới hạn dung lượng tải ảnh hiện có của
  hệ thống, khi tải lên, thì báo lỗi bằng tiếng Việt và không đổi ảnh.
- **AC-11 (Trường hợp biên):** Với điều kiện nhân viên chưa có hồ sơ nhân viên, khi quản trị viên chỉ thêm ảnh rồi Lưu,
  thì hồ sơ nhân viên được tạo kèm ảnh.

**Ảnh hồ sơ bệnh nhân (US-03)**

- **AC-12 (Luồng chính):** Với điều kiện quản trị viên mở hồ sơ một tài khoản bệnh nhân, khi xem từng hồ sơ (bản thân,
  người thân), thì mỗi hồ sơ hiện ảnh riêng (chưa có ảnh thì hiện biểu tượng người).
- **AC-13 (Luồng chính):** Với điều kiện một hồ sơ bệnh nhân đang Sửa, khi tải ảnh lên / gỡ ảnh rồi Lưu, thì chỉ hồ sơ
  đó đổi ảnh, các hồ sơ khác của cùng tài khoản giữ nguyên.
- **AC-14 (Trường hợp biên):** Với điều kiện thêm mới hồ sơ người thân, khi tải ảnh kèm thông tin rồi Lưu, thì hồ sơ mới
  được tạo kèm ảnh.
- **AC-15 (Trường hợp biên):** Lỗi tệp và Hủy giống AC-09, AC-10.

**Phân quyền**

- **AC-16:** Với điều kiện người dùng không phải quản trị viên, khi gọi thao tác đổi ảnh nhân viên / bệnh nhân, thì bị
  từ chối như các thao tác sửa hồ sơ hiện có.
- **AC-17:** Với điều kiện người dùng chưa đăng nhập hoặc là bệnh nhân, khi xem hồ sơ bác sĩ công khai, thì chỉ nhận các
  trường ở AC-01 (không lộ thêm trường nào khác so với hiện nay ngoài chứng chỉ hành nghề và nhận chỉ định đích danh).

## 4. Ảnh hưởng liên Capability

Domain chưa có KB; blast radius code-level:

- **Capability đang sửa:** hồ sơ nhân viên (`clinic`), hồ sơ bệnh nhân (`patient`), danh sách bác sĩ công khai.
- **Blast radius code-level:**
  - `ClinicCatalogService.publicDoctors` / `PublicDoctorController` (`GET /api/public/doctors`): dùng cho cụm bác sĩ
    hàng đầu trên trang chủ và số liệu "Bác sĩ" ở thống kê trang chủ.
  - Hồ sơ nhân viên (`StaffProfileDialog`, API hồ sơ nhân viên admin) và trang Chuyên khoa (thêm / gỡ bác sĩ) dùng chung
    bảng `staff`.
  - Hồ sơ bệnh nhân (`PatientProfileDialog`, API hồ sơ bệnh nhân admin).
  - Migration V12 đã có cột `staff.avatar_url` và ảnh mẫu cho bác sĩ.
- **Đề xuất phạm vi kiểm thử hồi quy cho QA:** trang chủ (cụm bác sĩ hàng đầu, thống kê, đặt lịch chọn bác sĩ), sửa hồ
  sơ nhân viên không đổi ảnh, thêm / gỡ bác sĩ ở trang Chuyên khoa, sửa hồ sơ bệnh nhân không đổi ảnh, trang
  `/admin/homepage` chọn bác sĩ hàng đầu.

## 5. Giả định

- **GĐ-01:** Dialog hồ sơ bác sĩ mở được cho mọi người xem trang chủ (cả khách chưa đăng nhập), vì cụm bác sĩ dùng chung
  một trang — áp dụng AC-01..05, AC-17. Nếu sai thì chỉ bệnh nhân đã đăng nhập mới bấm được.
- **GĐ-02:** Chứng chỉ hành nghề được coi là thông tin công khai (theo quy định hành nghề khám chữa bệnh phải niêm yết)
  — áp dụng AC-01, AC-17. Nếu sai thì bỏ dòng này khỏi dialog.
- **GĐ-03:** Ảnh tải lên dùng cơ chế tải ảnh sẵn có của trang quản trị (cùng giới hạn loại tệp và dung lượng), không cắt
  / xoay ảnh trong hệ thống — áp dụng AC-07, AC-10, AC-13.
- **GĐ-04:** Bệnh nhân chưa tự đổi ảnh của mình trong đợt này; avatar góc phải trang chủ vẫn là chữ cái đầu tên — áp
  dụng US-03. Nếu sai thì cần thêm màn hồ sơ cá nhân cho bệnh nhân.

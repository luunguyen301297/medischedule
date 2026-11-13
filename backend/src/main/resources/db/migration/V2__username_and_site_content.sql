-- Đăng nhập bằng email hoặc tên đăng nhập (vd. tài khoản quản trị "admin")
ALTER TABLE users ADD COLUMN username VARCHAR(50) UNIQUE;

-- Nội dung trang chủ / trang đăng nhập do admin thiết lập. Mỗi khóa một khối JSON.
CREATE TABLE site_content (
    content_key  VARCHAR(50) PRIMARY KEY,
    content      JSONB        NOT NULL,
    updated_by   BIGINT       REFERENCES users (id),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Dữ liệu mẫu (mock) để trang có nội dung ngay; admin sửa ở /admin/homepage
INSERT INTO site_content (content_key, content) VALUES ('homepage', '{
  "brandName": "MediSchedule",
  "headline": "Đặt lịch khám nhanh, không phải xếp hàng chờ",
  "subheadline": "Một tài khoản để đặt lịch, theo dõi lượt khám và xem lại hồ sơ sức khỏe của cả gia đình.",
  "hotline": "1900 6868",
  "address": "125 Nguyễn Văn Linh, Phường Tân Phong, Quận 7, TP.HCM",
  "workingHours": "T2 – T7: 7:00 – 20:00 · CN: 7:00 – 12:00",
  "footer": "© 2026 Phòng khám Đa khoa MediSchedule",
  "features": [
    { "icon": "calendar", "title": "Chọn chuyên khoa, bác sĩ và giờ khám", "description": "Xem khung giờ còn trống và giữ chỗ ngay." },
    { "icon": "clock", "title": "Biết trước thời gian chờ", "description": "Nhận thông báo khi sắp đến lượt khám." },
    { "icon": "file", "title": "Kết quả và đơn thuốc ở một nơi", "description": "Tra cứu lại lịch sử khám bất cứ lúc nào." }
  ],
  "stats": [
    { "value": "15+", "label": "Chuyên khoa" },
    { "value": "40+", "label": "Bác sĩ chuyên môn" },
    { "value": "120.000+", "label": "Lượt khám mỗi năm" }
  ],
  "promotions": [
    { "badge": "Ưu đãi tháng 11", "title": "Gói khám sức khỏe tổng quát", "description": "Giảm 20% khi đặt lịch online trước 30/11. Gồm xét nghiệm máu, siêu âm ổ bụng và điện tim.", "active": true },
    { "badge": "Mới", "title": "Khám nhi ngoài giờ đến 21:00", "description": "Bác sĩ nhi trực tối các ngày trong tuần, phụ huynh không phải xin nghỉ làm.", "active": true },
    { "badge": "Miễn phí", "title": "Tư vấn dinh dưỡng cho người tiểu đường", "description": "Sáng thứ Bảy hằng tuần, đăng ký trước để được giữ chỗ.", "active": true }
  ]
}'::jsonb);

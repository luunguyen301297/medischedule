-- Nội dung trang chủ chia 2 phần:
--   intro    : giới thiệu chung, đặt song song với form đăng nhập / đăng ký
--   sections : các khối kèm ảnh nền bên dưới, trượt vào khi cuộn
-- Dữ liệu mẫu (mock); admin sửa ở /admin/homepage.
INSERT INTO site_content (content_key, content) VALUES ('homepage', '{}'::jsonb)
ON CONFLICT (content_key) DO NOTHING;

UPDATE site_content SET updated_at = now(), content = '{
  "intro": {
    "brandName": "MediSchedule",
    "headline": "Đặt lịch khám nhanh, không phải xếp hàng chờ",
    "subheadline": "Một tài khoản để đặt lịch, theo dõi lượt khám và xem lại hồ sơ sức khỏe của cả gia đình.",
    "features": [
      { "icon": "calendar", "title": "Chọn chuyên khoa, bác sĩ và giờ khám", "description": "Xem khung giờ còn trống và giữ chỗ ngay." },
      { "icon": "bell", "title": "Biết trước thời gian chờ", "description": "Nhận thông báo khi sắp đến lượt khám." },
      { "icon": "file", "title": "Kết quả và đơn thuốc ở một nơi", "description": "Tra cứu lại lịch sử khám bất cứ lúc nào." }
    ],
    "hotline": "1900 6868",
    "workingHours": "T2 – T7: 7:00 – 20:00 · CN: 7:00 – 12:00",
    "address": "125 Nguyễn Văn Linh, Phường Tân Phong, Quận 7, TP.HCM",
    "footer": "© 2026 MediSchedule"
  },
  "sections": [
    {
      "active": true, "align": "left",
      "eyebrow": "Đội ngũ bác sĩ",
      "title": "Hơn 40 bác sĩ chuyên khoa, khám tận tâm",
      "description": "Bác sĩ nhiều năm kinh nghiệm ở 15 chuyên khoa. Bạn có thể chọn đích danh bác sĩ quen khi đặt lịch.",
      "imageUrl": "https://images.unsplash.com/photo-1576091160550-2173dba999ef?w=1920&q=70&auto=format&fit=crop",
      "buttonText": "Đặt lịch với bác sĩ"
    },
    {
      "active": true, "align": "right",
      "eyebrow": "Ưu đãi tháng 11",
      "title": "Gói khám sức khỏe tổng quát giảm 20%",
      "description": "Gồm xét nghiệm máu, siêu âm ổ bụng và điện tim. Áp dụng khi đặt lịch online trước 30/11.",
      "imageUrl": "https://images.unsplash.com/photo-1579684385127-1ef15d508118?w=1920&q=70&auto=format&fit=crop",
      "buttonText": "Nhận ưu đãi"
    },
    {
      "active": true, "align": "left",
      "eyebrow": "Cơ sở vật chất",
      "title": "Không gian sạch sẽ, thiết bị hiện đại",
      "description": "Phòng khám rộng rãi, máy xét nghiệm và chẩn đoán hình ảnh trả kết quả ngay trong ngày.",
      "imageUrl": "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=1920&q=70&auto=format&fit=crop",
      "buttonText": ""
    },
    {
      "active": true, "align": "right",
      "eyebrow": "Mới",
      "title": "Khám nhi ngoài giờ đến 21:00",
      "description": "Bác sĩ nhi trực tối các ngày trong tuần, phụ huynh không phải xin nghỉ làm để đưa con đi khám.",
      "imageUrl": "https://images.unsplash.com/photo-1631217868264-e5b90bb7e133?w=1920&q=70&auto=format&fit=crop",
      "buttonText": "Đặt lịch khám nhi"
    }
  ]
}'::jsonb
WHERE content_key = 'homepage';

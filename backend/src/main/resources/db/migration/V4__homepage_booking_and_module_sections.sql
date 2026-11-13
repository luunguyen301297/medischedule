-- Trang chủ thêm loại nội dung thứ 3 và đổi bộ cụm ảnh nền mẫu:
--   booking  : khối đặt lịch khám + giới thiệu cơ bản ở đầu trang (mới)
--   sections : 8 cụm thông tin kèm ảnh nền, giới thiệu các chức năng của app bệnh nhân
-- Dữ liệu mẫu (mock); admin sửa ở /admin/homepage. Giữ nguyên phần intro.
UPDATE site_content
SET updated_at = now(),
    content = content
      || jsonb_build_object('booking', '{
  "badge": "Đặt lịch online · Xác nhận trong 15 phút",
  "title": "Đặt lịch khám trong 1 phút, đến là được khám",
  "subtitle": "Chọn chuyên khoa và buổi khám phù hợp. Không cần tài khoản, phòng khám gọi xác nhận qua số điện thoại của bạn.",
  "highlights": [
    "Không cần tạo tài khoản",
    "Xác nhận qua điện thoại trong 15 phút",
    "Hủy hoặc đổi lịch miễn phí trước 2 giờ"
  ],
  "specialties": [
    "Nội tổng quát",
    "Nhi",
    "Tai Mũi Họng",
    "Da liễu",
    "Sản phụ khoa",
    "Tim mạch",
    "Tiêu hóa",
    "Cơ xương khớp",
    "Mắt",
    "Răng Hàm Mặt"
  ],
  "stats": [
    {
      "value": "15+",
      "label": "Chuyên khoa"
    },
    {
      "value": "40+",
      "label": "Bác sĩ"
    },
    {
      "value": "4.8/5",
      "label": "Bệnh nhân đánh giá"
    }
  ],
  "note": "Trường hợp cấp cứu, vui lòng gọi 115 hoặc đến trực tiếp phòng khám."
}'::jsonb)
      || jsonb_build_object('sections', '[
  {
    "active": true,
    "align": "left",
    "eyebrow": "Đặt lịch khám",
    "title": "Chọn chuyên khoa, chọn đúng bác sĩ bạn tin",
    "description": "Xem khung giờ còn trống theo từng ngày, chỉ định đích danh bác sĩ quen và nhận mã lịch ngay sau khi đặt.",
    "imageUrl": "https://images.unsplash.com/photo-1532938911079-1b06ac7ceec7?w=1920&q=70&auto=format&fit=crop",
    "buttonText": "Đặt lịch ngay"
  },
  {
    "active": true,
    "align": "right",
    "eyebrow": "Không cần tài khoản",
    "title": "Đặt nhanh chỉ với số điện thoại",
    "description": "Chưa có tài khoản vẫn đặt được lịch: nhập số điện thoại, chọn giờ khám, tra cứu và hủy lịch bằng mã lịch được gửi qua tin nhắn.",
    "imageUrl": "https://images.unsplash.com/photo-1576091160550-2173dba999ef?w=1920&q=70&auto=format&fit=crop",
    "buttonText": "Đặt lịch nhanh"
  },
  {
    "active": true,
    "align": "left",
    "eyebrow": "Thời gian chờ",
    "title": "Biết còn bao lâu đến lượt, không phải ngồi chờ ở sảnh",
    "description": "Theo dõi số thứ tự đang gọi và thời gian chờ ước tính ngay trên điện thoại. App báo trước khi còn 3 người.",
    "imageUrl": "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=1920&q=70&auto=format&fit=crop",
    "buttonText": ""
  },
  {
    "active": true,
    "align": "right",
    "eyebrow": "Kết quả khám",
    "title": "Kết quả xét nghiệm và chẩn đoán hình ảnh trên app",
    "description": "Xem lại bệnh án, kết quả xét nghiệm, phim X-quang và siêu âm của từng lần khám, tải bản PDF khi cần.",
    "imageUrl": "https://images.unsplash.com/photo-1666214280557-f1b5022eb634?w=1920&q=70&auto=format&fit=crop",
    "buttonText": "Đăng nhập để xem"
  },
  {
    "active": true,
    "align": "left",
    "eyebrow": "Đơn thuốc",
    "title": "Đơn thuốc điện tử và nhắc giờ uống thuốc",
    "description": "Đơn thuốc có hướng dẫn liều dùng sáng, trưa, chiều, tối. App nhắc đúng giờ để không quên liều nào.",
    "imageUrl": "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=1920&q=70&auto=format&fit=crop",
    "buttonText": ""
  },
  {
    "active": true,
    "align": "right",
    "eyebrow": "Bệnh mạn tính",
    "title": "Bác sĩ đồng hành cùng bạn giữa các lần tái khám",
    "description": "Người bệnh tiểu đường, huyết áp tự nhập chỉ số hằng ngày. Chỉ số vượt ngưỡng, bác sĩ phụ trách được báo để liên hệ kịp thời.",
    "imageUrl": "https://images.unsplash.com/photo-1576765608535-5f04d1e3f289?w=1920&q=70&auto=format&fit=crop",
    "buttonText": "Tìm hiểu chương trình"
  },
  {
    "active": true,
    "align": "left",
    "eyebrow": "Hồ sơ gia đình",
    "title": "Một tài khoản chăm sóc cả nhà",
    "description": "Thêm hồ sơ cho con nhỏ và bố mẹ, đặt lịch hộ người thân và xem lại lịch sử khám của từng người.",
    "imageUrl": "https://images.unsplash.com/photo-1584515933487-779824d29309?w=1920&q=70&auto=format&fit=crop",
    "buttonText": "Tạo tài khoản"
  },
  {
    "active": true,
    "align": "right",
    "eyebrow": "Trợ lý AI",
    "title": "Chưa biết nên khám khoa nào? Hỏi trợ lý",
    "description": "Mô tả triệu chứng, trợ lý gợi ý chuyên khoa và bác sĩ phù hợp rồi điền sẵn phiếu đặt lịch. Gợi ý chỉ để tham khảo, không thay chẩn đoán của bác sĩ.",
    "imageUrl": "https://images.unsplash.com/photo-1631217868264-e5b90bb7e133?w=1920&q=70&auto=format&fit=crop",
    "buttonText": "Thử trợ lý"
  }
]'::jsonb)
WHERE content_key = 'homepage';

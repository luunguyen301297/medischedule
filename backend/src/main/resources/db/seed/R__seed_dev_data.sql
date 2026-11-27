-- ============================================================================
-- Dữ liệu dev dùng chung (Flyway repeatable migration). SINH TỰ ĐỘNG bởi scripts/export_seed.py
-- Sửa nội dung trang chủ ở /admin/homepage rồi chạy lại script để cập nhật file này.
--
-- Chạy sau các migration V*, và chạy lại mỗi khi file này thay đổi.
-- Không ghi đè dữ liệu người dùng đã sửa: trang chủ chỉ cập nhật khi chưa sửa ở /admin, tài khoản demo chỉ thêm khi chưa có.
-- Chỉ nạp khi FLYWAY_LOCATIONS có classpath:db/seed (mặc định ở môi trường dev, xem application.yml).
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Nội dung trang chủ (đặt lịch đầu trang, giới thiệu chung, các cụm ảnh nền)
INSERT INTO site_content (content_key, content, updated_at)
VALUES ('homepage', '{
  "booking": {
    "note": "Đặt lịch sớm và chọn chuyên khoa để được phục vụ tốt hơn",
    "badge": "Đặt lịch online · Xác nhận trong 15 phút",
    "stats": [
      {
        "label": "Chuyên khoa",
        "value": "15+"
      },
      {
        "label": "Bác sĩ",
        "value": "40+"
      },
      {
        "label": "Bệnh nhân đánh giá",
        "value": "4.8/5"
      }
    ],
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
    ]
  },
  "intro": {
    "footer": "© 2026 MediSchedule",
    "address": "125 Nguyễn Văn Linh, Phường Tân Phong, Quận 7, TP.HCM",
    "hotline": "1900 6868",
    "features": [
      {
        "icon": "calendar",
        "title": "Chọn chuyên khoa, bác sĩ và giờ khám",
        "description": "Xem khung giờ còn trống và giữ chỗ ngay."
      },
      {
        "icon": "bell",
        "title": "Biết trước thời gian chờ",
        "description": "Nhận thông báo khi sắp đến lượt khám."
      },
      {
        "icon": "file",
        "title": "Kết quả và đơn thuốc ở một nơi",
        "description": "Tra cứu lại lịch sử khám bất cứ lúc nào."
      }
    ],
    "headline": "Đặt lịch khám nhanh, không phải xếp hàng chờ",
    "brandName": "MediSchedule",
    "subheadline": "Một tài khoản để đặt lịch, theo dõi lượt khám và xem lại hồ sơ sức khỏe của cả gia đình.",
    "workingHours": "T2 – T7: 7:00 – 20:00 · CN: 7:00 – 12:00"
  },
  "sections": [
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
  ]
}'::jsonb, now())
ON CONFLICT (content_key) DO UPDATE SET content = EXCLUDED.content, updated_at = now()
-- Chỉ ghi đè nội dung chưa từng được sửa ở /admin/homepage (updated_by trống = do migration/seed tạo).
-- Máy đã sửa trang chủ giữ nguyên; muốn lấy bản seed mới nhất thì xóa dữ liệu: docker compose down -v
WHERE site_content.updated_by IS NULL;

-- 2. Tài khoản demo cho từng vai trò, mật khẩu: demo@123
--    (tài khoản quản trị admin / admin@123 do backend tự tạo lúc khởi động, xem AdminInitializer)
INSERT INTO users (email, username, password_hash, full_name, role_id)
SELECT v.email, v.username, crypt('demo@123', gen_salt('bf', 10)), v.full_name, r.id
FROM (VALUES
    ('bacsi.demo@medischedule.local', 'bacsi', 'BS. Trần Minh Khoa', 'DOCTOR'),
    ('letan.demo@medischedule.local', 'letan', 'Nguyễn Thu Hà', 'RECEPTIONIST'),
    ('duocsi.demo@medischedule.local', 'duocsi', 'DS. Lê Quang Huy', 'PHARMACIST'),
    ('benhnhan.demo@medischedule.local', 'benhnhan', 'Phạm Ngọc Anh', 'PATIENT')
) AS v(email, username, full_name, role_code)
JOIN roles r ON r.code = v.role_code
ON CONFLICT DO NOTHING;   -- trùng email hoặc username thì bỏ qua

-- 3. Mỗi chuyên khoa (seed ở V8) một phòng khám mẫu; chuyên khoa đã đổi tên/xóa thì bỏ qua, phòng đã có thì giữ nguyên
INSERT INTO room (name, specialty_id)
SELECT v.room, s.id
FROM (VALUES
    ('Nội tổng quát', 'P101'),
    ('Nhi', 'P102'),
    ('Tai Mũi Họng', 'P103'),
    ('Da liễu', 'P104'),
    ('Sản phụ khoa', 'P105'),
    ('Tim mạch', 'P106'),
    ('Tiêu hóa', 'P107'),
    ('Cơ xương khớp', 'P108'),
    ('Mắt', 'P109'),
    ('Răng Hàm Mặt', 'P110')
) AS v(specialty, room)
JOIN specialty s ON s.name = v.specialty
ON CONFLICT DO NOTHING;

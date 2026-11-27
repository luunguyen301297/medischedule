-- Trùng tên chuyên khoa / phòng không phân biệt hoa thường; UNIQUE(name) cũ giữ nguyên.
CREATE UNIQUE INDEX uq_specialty_name_ci ON specialty (lower(name));
CREATE UNIQUE INDEX uq_room_name_ci      ON room (lower(name));

-- Danh mục ban đầu = 10 chuyên khoa đang hiện ở trang chủ.
INSERT INTO specialty (name) VALUES
    ('Nội tổng quát'), ('Nhi'), ('Tai Mũi Họng'), ('Da liễu'), ('Sản phụ khoa'),
    ('Tim mạch'), ('Tiêu hóa'), ('Cơ xương khớp'), ('Mắt'), ('Răng Hàm Mặt')
ON CONFLICT DO NOTHING;

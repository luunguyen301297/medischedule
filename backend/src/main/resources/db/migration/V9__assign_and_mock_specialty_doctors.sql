-- Dữ liệu mẫu: mỗi chuyên khoa có ít nhất 1 bác sĩ (task specialty-doctor).
-- 1) Bác sĩ đang hoạt động chưa có khoa → gán lần lượt vào các khoa chưa có bác sĩ.
-- 2) Khoa vẫn trống → tạo tài khoản bác sĩ mẫu (email @medischedule.test) + hồ sơ staff.
-- ponytail: mật khẩu tài khoản mẫu là bcrypt của chuỗi ngẫu nhiên không lưu ở đâu → không đăng nhập được;
--   cần đăng nhập thì admin tạo tài khoản thật. Chạy trên DB không có bác sĩ/khoa nào thì không làm gì.

-- 1) Gán bác sĩ sẵn có
WITH free_doctors AS (
    SELECT u.id AS user_id, u.full_name, row_number() OVER (ORDER BY u.id) AS rn
    FROM users u
    JOIN roles r ON r.id = u.role_id AND r.code = 'DOCTOR'
    LEFT JOIN staff s ON s.user_id = u.id
    WHERE u.enabled AND s.specialty_id IS NULL
),
empty_specialties AS (
    SELECT sp.id AS specialty_id, row_number() OVER (ORDER BY sp.id) AS rn
    FROM specialty sp
    WHERE NOT EXISTS (SELECT 1 FROM staff s WHERE s.specialty_id = sp.id)
),
pairs AS (
    SELECT d.user_id, d.full_name, e.specialty_id
    FROM free_doctors d JOIN empty_specialties e USING (rn)
)
INSERT INTO staff (user_id, full_name, specialty_id)
SELECT user_id, full_name, specialty_id FROM pairs
ON CONFLICT (user_id) DO UPDATE SET specialty_id = EXCLUDED.specialty_id;

-- 2) Bác sĩ mẫu cho khoa còn trống
-- Tên mẫu bỏ qua tên đã có trong users để không trùng bác sĩ thật; hết tên thì "BS. Mẫu <khoa>"
CREATE TEMP TABLE mock_doctor ON COMMIT DROP AS
WITH empty_specialties AS (
    SELECT sp.id, sp.name, row_number() OVER (ORDER BY sp.id) AS rn
    FROM specialty sp
    WHERE NOT EXISTS (SELECT 1 FROM staff s WHERE s.specialty_id = sp.id)
),
names AS (
    SELECT n.full_name, row_number() OVER (ORDER BY n.ord) AS rn
    FROM unnest(ARRAY['BS. Trần Minh Khoa', 'BS. Lê Thu Hà', 'BS. Phạm Quốc Bảo', 'BS. Hoàng Ngọc Lan',
                      'BS. Vũ Đức Anh', 'BS. Đặng Thị Mai', 'BS. Bùi Văn Long', 'BS. Đỗ Hải Yến',
                      'BS. Ngô Thanh Tùng', 'BS. Dương Minh Châu', 'BS. Lý Hoài Nam', 'BS. Trịnh Bích Ngọc'])
         WITH ORDINALITY AS n(full_name, ord)
    WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.full_name = n.full_name)
)
SELECT e.id AS specialty_id,
       coalesce(n.full_name, 'BS. Mẫu ' || e.name) AS full_name,
       'bs.mau' || e.id || '@medischedule.test' AS email
FROM empty_specialties e LEFT JOIN names n USING (rn);

INSERT INTO users (email, password_hash, full_name, role_id)
SELECT m.email, '$2b$10$rpxBBDJh6GlGn2lhMWZmyOYTNoSrmuipH.5PUhtEnRJOo8y7tYfga', m.full_name, (SELECT id FROM roles WHERE code = 'DOCTOR')
FROM mock_doctor m
ON CONFLICT (email) DO NOTHING;

INSERT INTO staff (user_id, full_name, specialty_id)
SELECT u.id, m.full_name, m.specialty_id
FROM mock_doctor m JOIN users u ON u.email = m.email
ON CONFLICT (user_id) DO UPDATE SET specialty_id = EXCLUDED.specialty_id;

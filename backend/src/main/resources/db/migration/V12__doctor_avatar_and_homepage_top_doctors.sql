-- Trang chủ: ảnh bác sĩ + danh sách "Các bác sĩ chuyên khoa hàng đầu" + số liệu lấy từ hệ thống.
-- Dữ liệu mẫu (mock), chạy lại không đổi kết quả; admin sửa ở /admin/homepage.

-- 1) Ảnh đại diện bác sĩ: đường dẫn công khai (ảnh ngoài hoặc /api/public/files/...), hiện ở trang chủ
ALTER TABLE staff ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- 2) Bác sĩ đang hoạt động chưa có hồ sơ staff → tạo hồ sơ để gắn được ảnh
INSERT INTO staff (user_id, full_name)
SELECT u.id, u.full_name
FROM users u JOIN roles r ON r.id = u.role_id AND r.code = 'DOCTOR'
WHERE u.enabled
ON CONFLICT (user_id) DO NOTHING;

-- 3) Ảnh mẫu cho mọi bác sĩ chưa có ảnh, xoay vòng bộ ảnh Unsplash
WITH pics AS (
    SELECT 'https://images.unsplash.com/photo-' || p.id || '?w=400&h=400&fit=crop&crop=faces&q=70' AS url,
           p.ord - 1 AS idx, count(*) OVER () AS total
    FROM unnest(ARRAY['1559839734-2b71ea197ec2', '1612349317150-e413f6a5b16d', '1622253692010-333f2da6031d',
                      '1594824476967-48c8b964273f', '1537368910025-700350fe46c7', '1582750433449-648ed127bb54',
                      '1651008376811-b90baee60c1f', '1622902046580-2b47f47f5471', '1638202993928-7267aad84c31',
                      '1607990281513-2c110a25bd8c', '1614608682850-e0d6ed316d47', '1527613426441-4da17471b66d',
                      '1591604021695-0c69b7c05981', '1623854767648-e7bb8009f0db'])
         WITH ORDINALITY AS p(id, ord)
),
doctors AS (
    SELECT s.id, row_number() OVER (ORDER BY s.id) - 1 AS rn
    FROM staff s
    JOIN users u ON u.id = s.user_id
    JOIN roles r ON r.id = u.role_id AND r.code = 'DOCTOR'
    WHERE s.avatar_url IS NULL
)
UPDATE staff s SET avatar_url = pics.url
FROM doctors d JOIN pics ON pics.idx = d.rn % pics.total
WHERE s.id = d.id;

-- 4) Nội dung trang chủ (chỉ đổi phần còn đúng chữ mẫu cũ, giữ nguyên phần admin đã sửa)
--    - bỏ mô tả dưới tiêu đề và cam kết "Hủy hoặc đổi lịch..."; "Không cần tạo tài khoản" → "Đặt lịch chỉ bằng số điện thoại"
--    - số liệu Chuyên khoa / Bác sĩ đếm từ hệ thống (source), vẫn hiện dấu "+"
--    - topDoctorIds: 10 bác sĩ đầu tiên (users.id) có ảnh, nếu admin chưa chọn
UPDATE site_content
SET updated_at = now(),
    content = jsonb_set(content, '{booking}', (content -> 'booking') || jsonb_build_object(
        'subtitle',
        CASE WHEN content #>> '{booking,subtitle}' LIKE 'Chọn chuyên khoa và buổi khám phù hợp.%' THEN ''
             ELSE coalesce(content #>> '{booking,subtitle}', '') END,
        'highlights',
        coalesce((SELECT jsonb_agg(CASE WHEN h = 'Không cần tạo tài khoản' THEN 'Đặt lịch chỉ bằng số điện thoại' ELSE h END)
                  FROM jsonb_array_elements_text(content #> '{booking,highlights}') AS h
                  WHERE h <> 'Hủy hoặc đổi lịch miễn phí trước 2 giờ'), '[]'::jsonb),
        'stats',
        coalesce((SELECT jsonb_agg(CASE
                      WHEN st ? 'source' THEN st
                      WHEN st ->> 'label' ILIKE 'Chuyên khoa%' THEN st || '{"source": "specialties"}'
                      WHEN st ->> 'label' ILIKE 'Bác sĩ%' THEN st || '{"source": "doctors"}'
                      ELSE st END ORDER BY i)
                  FROM jsonb_array_elements(content #> '{booking,stats}') WITH ORDINALITY AS x(st, i)), '[]'::jsonb),
        'topDoctorIds',
        coalesce(content #> '{booking,topDoctorIds}',
                 (SELECT coalesce(jsonb_agg(t.user_id ORDER BY t.id), '[]'::jsonb)
                  FROM (SELECT s.id, s.user_id
                        FROM staff s
                        JOIN users u ON u.id = s.user_id AND u.enabled
                        JOIN roles r ON r.id = u.role_id AND r.code = 'DOCTOR'
                        WHERE s.active AND s.avatar_url IS NOT NULL
                        ORDER BY s.id LIMIT 10) t))
    ))
WHERE content_key = 'homepage' AND content ? 'booking';

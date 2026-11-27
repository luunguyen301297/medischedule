-- Cam kết khối đặt lịch đầu trang: "Đặt lịch chỉ bằng số điện thoại" → "Đặt lịch đơn giản",
-- thêm "Có thể lựa chọn các bác sĩ hàng đầu" (nếu chưa có và còn chỗ, tối đa 3 dòng).
-- Chỉ đổi chữ mẫu, giữ nguyên phần admin đã sửa; chạy lại không đổi kết quả.
UPDATE site_content
SET updated_at = now(),
    content = jsonb_set(content, '{booking,highlights}', (
        WITH h AS (
            SELECT coalesce(jsonb_agg(CASE WHEN x = 'Đặt lịch chỉ bằng số điện thoại' THEN 'Đặt lịch đơn giản' ELSE x END
                                      ORDER BY i), '[]'::jsonb) AS arr
            FROM jsonb_array_elements_text(coalesce(content #> '{booking,highlights}', '[]'::jsonb))
                 WITH ORDINALITY AS t(x, i)
        )
        SELECT CASE WHEN arr ? 'Có thể lựa chọn các bác sĩ hàng đầu' OR jsonb_array_length(arr) >= 3 THEN arr
                    ELSE arr || '["Có thể lựa chọn các bác sĩ hàng đầu"]'::jsonb END
        FROM h))
WHERE content_key = 'homepage' AND content ? 'booking';

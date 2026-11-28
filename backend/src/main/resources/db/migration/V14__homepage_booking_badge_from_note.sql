-- Trang chủ: nhãn nhỏ đầu cụm đặt lịch thay bằng câu lưu ý; bỏ dòng lưu ý dưới nút đặt lịch (để trống thì ẩn).
-- Idempotent: chạy lại cho cùng kết quả; không có khối homepage thì không làm gì.
UPDATE site_content
SET content = jsonb_set(
        jsonb_set(content, '{booking,badge}', to_jsonb('Đặt lịch sớm để được phục vụ tốt hơn'::text), true),
        '{booking,note}', to_jsonb(''::text), true),
    updated_at = now()
WHERE content_key = 'homepage'
  AND content ? 'booking';

-- Cụm giới thiệu cạnh form đăng nhập: bỏ tiêu đề lớn, dùng câu mô tả ngắn làm tiêu đề (mô tả ngắn để trống thì ẩn).
-- Idempotent: chỉ đổi khi mô tả ngắn còn nội dung.
UPDATE site_content
SET content = jsonb_set(
        jsonb_set(content, '{intro,headline}', content -> 'intro' -> 'subheadline', true),
        '{intro,subheadline}', to_jsonb(''::text), true),
    updated_at = now()
WHERE content_key = 'homepage'
  AND coalesce(content -> 'intro' ->> 'subheadline', '') <> '';

-- Đăng nhập Google, quên mật khẩu qua email, push notification cho app.
-- Chỉ thêm cột / nới ràng buộc: không mất dữ liệu, mọi dòng cũ đều có password_hash nên CHECK hợp lệ.

ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;   -- NULL khi chỉ đăng nhập Google

ALTER TABLE users
    ADD COLUMN google_sub        VARCHAR(64)  UNIQUE,         -- ID tài khoản Google đã liên kết
    ADD COLUMN reset_token_hash  VARCHAR(100),                -- link quên mật khẩu (lưu hash, không lưu token)
    ADD COLUMN reset_expires_at  TIMESTAMPTZ,
    ADD COLUMN fcm_token         VARCHAR(255),
    ADD CONSTRAINT ck_users_login_method CHECK (password_hash IS NOT NULL OR google_sub IS NOT NULL);

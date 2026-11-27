-- Số điện thoại dùng làm tên đăng nhập nên phải duy nhất (bỏ qua tài khoản chưa có SĐT)
CREATE UNIQUE INDEX uq_users_phone ON users (phone) WHERE phone IS NOT NULL;

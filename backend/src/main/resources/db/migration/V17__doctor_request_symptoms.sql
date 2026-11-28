-- Mô tả triệu chứng người khám nhập khi gửi yêu cầu đặt bác sĩ (không bắt buộc).
ALTER TABLE doctor_request ADD COLUMN IF NOT EXISTS symptoms VARCHAR(500);

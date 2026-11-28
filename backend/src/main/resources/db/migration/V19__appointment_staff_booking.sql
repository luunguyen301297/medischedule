-- Lịch hẹn do nhân viên đặt hộ (menu Lịch hẹn ở khu quản trị). Thiết kế: docs/design/admin-booking-tech-design.md

-- Thời gian khám một lượt của khoa = độ dài slot khi sinh ca khám.
ALTER TABLE specialty ADD COLUMN IF NOT EXISTS slot_minutes SMALLINT NOT NULL DEFAULT 15;
ALTER TABLE specialty DROP CONSTRAINT IF EXISTS ck_specialty_slot_minutes;
ALTER TABLE specialty ADD CONSTRAINT ck_specialty_slot_minutes
    CHECK (slot_minutes BETWEEN 5 AND 120 AND slot_minutes % 5 = 0);

-- Ca khám sinh lười từ phân ca làm việc: một bác sĩ / ngày / ca làm việc có tối đa một ca khám.
-- Dòng cũ (nếu có) để NULL; NULL không đụng nhau trong unique nên index không cần partial.
ALTER TABLE shift ADD COLUMN IF NOT EXISTS work_shift_id BIGINT REFERENCES work_shift (id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_shift_doctor_work_shift ON shift (doctor_id, work_date, work_shift_id);

-- Kênh STAFF = nhân viên đặt hộ; lưu người đặt.
ALTER TABLE appointment DROP CONSTRAINT IF EXISTS appointment_channel_check;
ALTER TABLE appointment DROP CONSTRAINT IF EXISTS ck_appointment_channel;
ALTER TABLE appointment ADD CONSTRAINT ck_appointment_channel
    CHECK (channel IN ('APP', 'GUEST', 'AI', 'WALK_IN', 'STAFF'));
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES users (id);

-- Danh sách lịch hẹn lọc theo khoảng ngày.
CREATE INDEX IF NOT EXISTS idx_appointment_start ON appointment (start_at);

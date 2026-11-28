-- Lịch hẹn không chỉ định bác sĩ: chỉ có ngày + buổi (+ chuyên khoa nếu khách chọn); bác sĩ, ca khám và giờ xếp khi
-- check-in. Thiết kế: docs/design/admin-booking-tech-design.md §9

ALTER TABLE appointment ALTER COLUMN doctor_id DROP NOT NULL;
ALTER TABLE appointment ALTER COLUMN shift_id DROP NOT NULL;
ALTER TABLE appointment ALTER COLUMN start_at DROP NOT NULL;

ALTER TABLE appointment ADD COLUMN IF NOT EXISTS visit_date DATE;
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS work_shift_id BIGINT REFERENCES work_shift (id);
ALTER TABLE appointment ADD COLUMN IF NOT EXISTS specialty_id BIGINT REFERENCES specialty (id);

-- Lịch đã có (luôn có giờ và ca khám) lấy ngày theo giờ VN, buổi theo ca khám
UPDATE appointment SET visit_date = (start_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date WHERE visit_date IS NULL;
UPDATE appointment a SET work_shift_id = s.work_shift_id
FROM shift s
WHERE s.id = a.shift_id AND a.work_shift_id IS NULL;
ALTER TABLE appointment ALTER COLUMN visit_date SET NOT NULL;

-- Bác sĩ, ca khám, giờ hẹn: cùng có (đã xếp) hoặc cùng trống (chờ xếp)
ALTER TABLE appointment DROP CONSTRAINT IF EXISTS ck_appointment_assigned;
ALTER TABLE appointment ADD CONSTRAINT ck_appointment_assigned
    CHECK ((doctor_id IS NULL) = (shift_id IS NULL) AND (doctor_id IS NULL) = (start_at IS NULL));

CREATE INDEX IF NOT EXISTS idx_appointment_visit_date ON appointment (visit_date);

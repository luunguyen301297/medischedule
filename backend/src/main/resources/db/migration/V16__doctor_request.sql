-- Yêu cầu đặt bác sĩ từ hồ sơ bác sĩ trên trang chủ: ngày + buổi mong muốn, chờ nhân sự xếp ca (chưa phải lịch hẹn).
CREATE TABLE doctor_request (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    doctor_user_id  BIGINT       NOT NULL REFERENCES users (id),
    requested_date  DATE         NOT NULL,
    work_shift_id   BIGINT       NOT NULL REFERENCES work_shift (id),
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    booker_user_id  BIGINT       REFERENCES users (id),          -- NULL = khách chưa đăng nhập
    status          VARCHAR(12)  NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'SCHEDULED', 'REJECTED', 'CANCELLED')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Một SĐT không gửi trùng (bác sĩ, ngày, buổi) khi yêu cầu cũ còn chờ.
CREATE UNIQUE INDEX uq_doctor_request_pending ON doctor_request (phone, doctor_user_id, requested_date, work_shift_id)
    WHERE status = 'PENDING';
CREATE INDEX idx_doctor_request_phone_status ON doctor_request (phone, status);         -- giới hạn số yêu cầu chờ
CREATE INDEX idx_doctor_request_doctor_date ON doctor_request (doctor_user_id, requested_date);  -- xếp ca theo bác sĩ

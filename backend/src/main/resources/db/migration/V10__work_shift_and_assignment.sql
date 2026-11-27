-- Ca làm việc chuẩn (mẫu) và phân ca nhân viên theo ngày. Tách biệt bảng shift (ca khám của bác sĩ cho đặt lịch).

CREATE TABLE work_shift (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    color       VARCHAR(7)  NOT NULL CHECK (color ~ '^#[0-9a-fA-F]{6}$'),
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    CHECK (end_time > start_time)   -- không có ca qua đêm
);

CREATE UNIQUE INDEX uq_work_shift_name_ci ON work_shift (lower(name));

-- user_id (không phải staff.id): tài khoản nhân viên có thể chưa có hồ sơ staff.
-- FK work_shift_id chặn xóa ca đã được phân.
CREATE TABLE work_assignment (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    work_date      DATE   NOT NULL,
    work_shift_id  BIGINT NOT NULL REFERENCES work_shift (id),
    user_id        BIGINT NOT NULL REFERENCES users (id),
    CONSTRAINT uq_work_assignment UNIQUE (work_date, work_shift_id, user_id)   -- cũng phục vụ truy vấn theo ngày
);

INSERT INTO work_shift (name, start_time, end_time, color) VALUES
    ('Sáng', '07:00', '11:30', '#0e7490'),
    ('Chiều', '13:00', '17:00', '#059669'),
    ('Tối', '17:30', '21:00', '#d97706')
ON CONFLICT DO NOTHING;

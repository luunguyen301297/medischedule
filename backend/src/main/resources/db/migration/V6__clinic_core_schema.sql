-- Schema nghiệp vụ theo thiết kế CSDL đã duyệt (29 bảng; roles, users, site_content đã có từ V1–V5).
-- Thiết kế: https://claude.ai/artifact/MyPagR729Fb9aD5ZvVPP2g
-- Quy ước: khóa bigint identity, thời gian timestamptz, tiền VND numeric, trạng thái varchar + CHECK.
-- File (ảnh, PDF) nằm ở MinIO; bảng chỉ lưu object key, không lưu URL.

-- ============ 1. Tài khoản & nhân sự ============

CREATE TABLE specialty (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE,
    active  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE room (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(50) NOT NULL UNIQUE,
    specialty_id  BIGINT      REFERENCES specialty (id),
    active        BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE staff (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              BIGINT       NOT NULL UNIQUE REFERENCES users (id),
    full_name            VARCHAR(120) NOT NULL,
    phone                VARCHAR(15),
    specialty_id         BIGINT       REFERENCES specialty (id),   -- các cột chuyên môn chỉ dùng cho bác sĩ
    degree               VARCHAR(50),
    license_no           VARCHAR(30)  UNIQUE,                      -- chứng chỉ hành nghề
    accepts_designation  BOOLEAN      NOT NULL DEFAULT FALSE,
    active               BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ============ 2. Bệnh nhân ============

CREATE TABLE patient (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_code    VARCHAR(20)  NOT NULL UNIQUE,
    owner_user_id   BIGINT       REFERENCES users (id),   -- NULL: khách đặt bằng SĐT, gán khi khách đăng ký
    relation        VARCHAR(10)  NOT NULL DEFAULT 'SELF'
                    CHECK (relation IN ('SELF', 'CHILD', 'PARENT', 'OTHER')),
    full_name       VARCHAR(120) NOT NULL,
    date_of_birth   DATE,
    gender          VARCHAR(6),
    national_id     VARCHAR(12)  UNIQUE,                  -- CCCD
    phone           VARCHAR(15)  NOT NULL,
    address         VARCHAR(255),
    bhyt_number     VARCHAR(15),
    bhyt_valid_to   DATE,
    bhyt_rate       SMALLINT     CHECK (bhyt_rate IN (80, 95, 100)),
    allergy_note    TEXT,
    source          VARCHAR(10)  NOT NULL CHECK (source IN ('GUEST', 'APP', 'RECEPTION')),
    merged_into_id  BIGINT       REFERENCES patient (id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_patient_phone ON patient (phone);
CREATE INDEX idx_patient_owner ON patient (owner_user_id);
CREATE UNIQUE INDEX uq_patient_owner_self ON patient (owner_user_id) WHERE relation = 'SELF';

-- ============ 3. Lịch trực & lượt khám ============

CREATE TABLE shift (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    doctor_id     BIGINT      NOT NULL REFERENCES staff (id),
    room_id       BIGINT      NOT NULL REFERENCES room (id),
    work_date     DATE        NOT NULL,
    start_time    TIME        NOT NULL,
    end_time      TIME        NOT NULL,
    slot_minutes  SMALLINT    NOT NULL CHECK (slot_minutes > 0),
    max_patients  SMALLINT    NOT NULL CHECK (max_patients > 0),
    status        VARCHAR(10) NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    CHECK (end_time > start_time),
    UNIQUE (doctor_id, work_date, start_time),
    UNIQUE (id, doctor_id)   -- đích của FK (shift_id, doctor_id) bên appointment
);

CREATE TABLE shift_request (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    shift_id    BIGINT       NOT NULL REFERENCES shift (id),
    doctor_id   BIGINT       NOT NULL REFERENCES staff (id),
    type        VARCHAR(12)  NOT NULL CHECK (type IN ('SWAP', 'UNAVAILABLE')),
    reason      VARCHAR(255) NOT NULL,
    status      VARCHAR(10)  NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HANDLED')),
    handled_by  BIGINT       REFERENCES staff (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_shift_request_shift ON shift_request (shift_id);

CREATE TABLE voucher (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code              VARCHAR(20) NOT NULL UNIQUE,
    discount_percent  SMALLINT    NOT NULL CHECK (discount_percent BETWEEN 1 AND 100),
    valid_to          DATE        NOT NULL,
    usage_limit       INT         CHECK (usage_limit > 0),
    used_count        INT         NOT NULL DEFAULT 0 CHECK (used_count >= 0)
);

-- Lịch hẹn và cũng là lượt khám; walk-in là dòng channel = WALK_IN.
CREATE TABLE appointment (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_code      VARCHAR(12)  NOT NULL UNIQUE,
    patient_id        BIGINT       NOT NULL REFERENCES patient (id),
    booker_phone      VARCHAR(15)  NOT NULL,
    shift_id          BIGINT       NOT NULL,
    start_at          TIMESTAMPTZ  NOT NULL,
    doctor_id         BIGINT       NOT NULL REFERENCES staff (id),
    is_designated     BOOLEAN      NOT NULL DEFAULT FALSE,
    designation_fee   NUMERIC(12)  NOT NULL DEFAULT 0,      -- chốt giá lúc đặt
    voucher_id        BIGINT       REFERENCES voucher (id),
    channel           VARCHAR(10)  NOT NULL CHECK (channel IN ('APP', 'GUEST', 'AI', 'WALK_IN')),
    status            VARCHAR(12)  NOT NULL DEFAULT 'BOOKED'
                      CHECK (status IN ('BOOKED', 'CHECKED_IN', 'IN_PROGRESS', 'DONE', 'CANCELLED', 'NO_SHOW')),
    queue_number      SMALLINT,                              -- cấp khi check-in
    checked_in_at     TIMESTAMPTZ,
    started_at        TIMESTAMPTZ,
    finished_at       TIMESTAMPTZ,
    reschedule_count  SMALLINT     NOT NULL DEFAULT 0 CHECK (reschedule_count BETWEEN 0 AND 2),
    cancel_reason     VARCHAR(255),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- ponytail: doctor_id lặp lại shift.doctor_id để làm unique chống trùng; FK ghép giữ hai giá trị luôn khớp
    FOREIGN KEY (shift_id, doctor_id) REFERENCES shift (id, doctor_id)
);

-- Chống đặt trùng giờ; khách vãng lai không chiếm slot.
CREATE UNIQUE INDEX uq_appointment_doctor_slot ON appointment (doctor_id, start_at)
    WHERE status <> 'CANCELLED' AND channel <> 'WALK_IN';
CREATE INDEX idx_appointment_booker ON appointment (booker_phone, status);          -- tối đa 2 lịch chờ, đếm no-show
CREATE INDEX idx_appointment_queue ON appointment (shift_id, status, queue_number);  -- hàng đợi, thời gian chờ
CREATE INDEX idx_appointment_patient ON appointment (patient_id);

-- ============ 4. Bệnh án ============

CREATE TABLE medical_record (
    appointment_id  BIGINT      PRIMARY KEY REFERENCES appointment (id),
    symptoms        TEXT        NOT NULL,
    vital_signs     JSONB,                  -- {bp, pulse, temp, spo2, weight}
    icd10_code      VARCHAR(10),
    diagnosis       TEXT        NOT NULL,
    advice          TEXT,
    follow_up_date  DATE,
    signed_at       TIMESTAMPTZ             -- ký xong thì khóa sửa
);

-- ============ 6. Bảng giá (tạo trước vì lab_order trỏ tới) ============

CREATE TABLE service (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    type          VARCHAR(15)  NOT NULL CHECK (type IN ('CONSULT', 'LAB', 'DESIGNATION_FEE')),
    specialty_id  BIGINT       REFERENCES specialty (id),
    price         NUMERIC(12)  NOT NULL CHECK (price >= 0),
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE lab_order (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    appointment_id   BIGINT       NOT NULL REFERENCES appointment (id),
    service_id       BIGINT       NOT NULL REFERENCES service (id),
    status           VARCHAR(10)  NOT NULL DEFAULT 'ORDERED' CHECK (status IN ('ORDERED', 'DONE', 'CANCELLED')),
    result_text      TEXT,
    result_values    JSONB,                -- [{code, value, unit, ref_low, ref_high}]
    result_file_key  VARCHAR(255),         -- object key MinIO
    result_at        TIMESTAMPTZ
);

CREATE INDEX idx_lab_order_appointment ON lab_order (appointment_id);

-- ============ 5. Đơn thuốc & kho ============

CREATE TABLE drug (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name               VARCHAR(200) NOT NULL,
    active_ingredient  VARCHAR(200) NOT NULL,
    unit               VARCHAR(20)  NOT NULL,
    sale_price         NUMERIC(12)  NOT NULL CHECK (sale_price >= 0),
    min_stock          INT          NOT NULL DEFAULT 0,
    active             BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE drug_batch (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    drug_id       BIGINT       NOT NULL REFERENCES drug (id),
    batch_no      VARCHAR(30)  NOT NULL,
    expiry_date   DATE         NOT NULL,
    quantity      INT          NOT NULL CHECK (quantity >= 0),
    import_price  NUMERIC(12)  NOT NULL CHECK (import_price >= 0),
    supplier      VARCHAR(200),
    received_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (drug_id, batch_no)
);

CREATE INDEX idx_drug_batch_fefo ON drug_batch (drug_id, expiry_date);  -- cấp phát lô hết hạn gần nhất trước

CREATE TABLE prescription (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    appointment_id    BIGINT      NOT NULL UNIQUE REFERENCES appointment (id),
    national_rx_code  VARCHAR(20) UNIQUE,     -- mã đơn liên thông
    status            VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SIGNED', 'DISPENSED')),
    signed_at         TIMESTAMPTZ,
    dispensed_by      BIGINT      REFERENCES staff (id),
    dispensed_at      TIMESTAMPTZ
);

CREATE TABLE prescription_item (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prescription_id     BIGINT       NOT NULL REFERENCES prescription (id),
    drug_id             BIGINT       NOT NULL REFERENCES drug (id),
    quantity            INT          NOT NULL CHECK (quantity > 0),
    dosage              VARCHAR(100) NOT NULL,
    days                SMALLINT     NOT NULL CHECK (days > 0),
    substitute_drug_id  BIGINT       REFERENCES drug (id),
    substitute_status   VARCHAR(10)  CHECK (substitute_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_prescription_item_rx ON prescription_item (prescription_id);

-- ============ 6. Thanh toán ============

CREATE TABLE invoice (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_no        VARCHAR(20) NOT NULL UNIQUE,
    appointment_id    BIGINT      REFERENCES appointment (id),   -- NULL khi bán thuốc lẻ
    patient_id        BIGINT      REFERENCES patient (id),
    total             NUMERIC(14) NOT NULL,
    insurance_amount  NUMERIC(14) NOT NULL DEFAULT 0,
    discount_amount   NUMERIC(14) NOT NULL DEFAULT 0,
    payment_method    VARCHAR(6)  CHECK (payment_method IN ('CASH', 'QR', 'CARD')),
    gateway_txn_id    VARCHAR(60) UNIQUE,
    status            VARCHAR(10) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID', 'REFUNDED')),
    einvoice_code     VARCHAR(50),
    cashier_id        BIGINT      REFERENCES staff (id),
    paid_at           TIMESTAMPTZ
);

CREATE INDEX idx_invoice_appointment ON invoice (appointment_id);

CREATE TABLE invoice_item (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id  BIGINT      NOT NULL REFERENCES invoice (id),
    service_id  BIGINT      REFERENCES service (id),
    drug_id     BIGINT      REFERENCES drug (id),
    quantity    INT         NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(12) NOT NULL,
    amount      NUMERIC(14) NOT NULL,
    CHECK (num_nonnulls(service_id, drug_id) = 1)
);

CREATE INDEX idx_invoice_item_invoice ON invoice_item (invoice_id);

-- ============ 7. Sau khám: khảo sát & mạn tính ============

CREATE TABLE feedback (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    appointment_id   BIGINT      NOT NULL UNIQUE REFERENCES appointment (id),
    nps              SMALLINT    NOT NULL CHECK (nps BETWEEN 0 AND 10),
    score_reception  SMALLINT    CHECK (score_reception BETWEEN 1 AND 5),
    score_waiting    SMALLINT    CHECK (score_waiting BETWEEN 1 AND 5),
    score_doctor     SMALLINT    CHECK (score_doctor BETWEEN 1 AND 5),
    score_facility   SMALLINT    CHECK (score_facility BETWEEN 1 AND 5),
    comment          TEXT,
    ticket_status    VARCHAR(10) CHECK (ticket_status IN ('OPEN', 'RESOLVED')),   -- NULL: không cần xử lý
    handled_by       BIGINT      REFERENCES staff (id),
    resolution       TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE care_plan (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id      BIGINT      NOT NULL REFERENCES patient (id),
    doctor_id       BIGINT      NOT NULL REFERENCES staff (id),
    icd10_code      VARCHAR(10) NOT NULL,
    thresholds      JSONB       NOT NULL,   -- [{metric, warn, critical}]
    reminder_times  JSONB,                  -- ["07:00", "19:00"]
    status          VARCHAR(8)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ENDED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_care_plan_patient ON care_plan (patient_id);

CREATE TABLE health_measurement (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    care_plan_id      BIGINT       NOT NULL REFERENCES care_plan (id),
    metric            VARCHAR(12)  NOT NULL CHECK (metric IN ('BP_SYS', 'BP_DIA', 'GLUCOSE', 'WEIGHT')),
    value             NUMERIC(6,1) NOT NULL,
    measured_at       TIMESTAMPTZ  NOT NULL,
    alert_level       VARCHAR(8)   CHECK (alert_level IN ('WARNING', 'CRITICAL')),
    alert_handled_at  TIMESTAMPTZ
);

CREATE INDEX idx_health_measurement_plan ON health_measurement (care_plan_id, metric, measured_at DESC);

-- ============ 8. Hoa hồng bác sĩ ============

CREATE TABLE commission_policy (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    effective_from     DATE         NOT NULL UNIQUE,
    per_visit_amount   NUMERIC(12)  NOT NULL,
    consult_percent    NUMERIC(5,2) NOT NULL CHECK (consult_percent BETWEEN 0 AND 100),
    designation_bonus  NUMERIC(12)  NOT NULL
);

CREATE TABLE doctor_commission (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    doctor_id         BIGINT      NOT NULL REFERENCES staff (id),
    month             DATE        NOT NULL CHECK (EXTRACT(DAY FROM month) = 1),   -- ngày đầu tháng
    policy_id         BIGINT      NOT NULL REFERENCES commission_policy (id),
    visit_count       INT         NOT NULL DEFAULT 0,
    designated_count  INT         NOT NULL DEFAULT 0,
    amount            NUMERIC(14) NOT NULL DEFAULT 0,
    status            VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'LOCKED')),
    dispute_note      TEXT,
    dispute_status    VARCHAR(10) CHECK (dispute_status IN ('OPEN', 'RESOLVED')),
    UNIQUE (doctor_id, month)
);

-- ============ 9. Hệ thống ============

CREATE TABLE notification (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      REFERENCES users (id),
    phone       VARCHAR(15),                 -- gửi cho khách không có tài khoản
    channel     VARCHAR(5)  NOT NULL CHECK (channel IN ('PUSH', 'ZALO', 'SMS')),
    content     TEXT        NOT NULL,
    status      VARCHAR(8)  NOT NULL DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'SENT', 'FAILED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at     TIMESTAMPTZ,
    CHECK (user_id IS NOT NULL OR phone IS NOT NULL)
);

CREATE INDEX idx_notification_queued ON notification (created_at) WHERE status = 'QUEUED';

CREATE TABLE system_setting (
    setting_key  VARCHAR(60)  PRIMARY KEY,
    value        VARCHAR(255) NOT NULL,
    description  VARCHAR(255)
);

INSERT INTO system_setting (setting_key, value, description) VALUES
    ('booking.max_pending_per_phone', '2',   'Số lịch chờ tối đa mỗi SĐT'),
    ('booking.no_show_block_after',   '3',   'Chặn đặt online sau số lần no-show'),
    ('booking.cancel_before_hours',   '2',   'Hủy/đổi lịch trước giờ hẹn tối thiểu (giờ)'),
    ('queue.wait_warning_minutes',    '30',  'Cảnh báo lễ tân khi bệnh nhân chờ quá (phút)');

CREATE TABLE audit_log (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      REFERENCES users (id),
    action      VARCHAR(60) NOT NULL,
    entity      VARCHAR(40) NOT NULL,
    entity_id   BIGINT,
    detail      JSONB,                       -- giá trị trước / sau
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity, entity_id);

-- ============ 10. AI hỗ trợ ============

-- Một lần bệnh nhân gửi ảnh da liễu và/hoặc chỉ số xét nghiệm cho AI Service.
CREATE TABLE ai_analysis (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id        BIGINT       NOT NULL REFERENCES patient (id),
    appointment_id    BIGINT       REFERENCES appointment (id),   -- gắn khi đặt lịch từ AI hoặc đưa vào bệnh án
    type              VARCHAR(10)  NOT NULL CHECK (type IN ('IMAGE', 'LAB', 'COMBINED')),
    symptoms          TEXT,
    image_keys        JSONB,                 -- ["derm/2026/11/abc.jpg", ...] object key MinIO
    lab_values        JSONB,                 -- [{code, value, unit, ref_low, ref_high}]
    image_label       VARCHAR(60),
    image_confidence  NUMERIC(4,3) CHECK (image_confidence BETWEEN 0 AND 1),
    lab_flags         JSONB,                 -- [{code, level, message}]
    advice            TEXT,                  -- lời khuyên LLM
    model_version     VARCHAR(60),
    status            VARCHAR(10)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'DONE', 'FAILED')),
    error_message     VARCHAR(255),
    consent_at        TIMESTAMPTZ  NOT NULL,   -- đồng ý xử lý dữ liệu sức khỏe (NĐ 13/2023)
    reviewed_by       BIGINT       REFERENCES staff (id),
    reviewed_at       TIMESTAMPTZ,
    doctor_note       TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (image_keys IS NOT NULL OR lab_values IS NOT NULL)
);

CREATE INDEX idx_ai_analysis_patient ON ai_analysis (patient_id, created_at DESC);
CREATE INDEX idx_ai_analysis_appointment ON ai_analysis (appointment_id);

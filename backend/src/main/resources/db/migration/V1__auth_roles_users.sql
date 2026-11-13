-- Phân quyền theo vai trò: mỗi user đúng một role.
CREATE TABLE roles (
    id          SMALLINT PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,   -- dùng trong hasRole('...')
    name        VARCHAR(100) NOT NULL
);

INSERT INTO roles (id, code, name) VALUES
    (1, 'ADMIN',        'Quản trị viên'),
    (2, 'DOCTOR',       'Bác sĩ'),
    (3, 'RECEPTIONIST', 'Lễ tân'),
    (4, 'PHARMACIST',   'Dược sĩ'),
    (5, 'PATIENT',      'Bệnh nhân');

CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(120) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,          -- bcrypt
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(15),
    role_id         SMALLINT     NOT NULL REFERENCES roles (id),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_role ON users (role_id);

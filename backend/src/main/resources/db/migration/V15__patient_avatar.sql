-- Ảnh đại diện cho từng hồ sơ bệnh nhân (bản thân, người thân); admin tải lên ở hồ sơ bệnh nhân.
ALTER TABLE patient ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- Màu ca mẫu theo yêu cầu: Chiều cam, Tối tím. Chỉ đổi khi ca còn màu seed của V10 (admin chưa tự đổi).
UPDATE work_shift SET color = '#ea580c' WHERE lower(name) = 'chiều' AND color = '#059669';
UPDATE work_shift SET color = '#7c3aed' WHERE lower(name) = 'tối'   AND color = '#d97706';

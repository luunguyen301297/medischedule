## Định dạng tài liệu BA Spec đầu ra

Mọi file spec ghi vào thư mục `docs/tasks/` phải tuân thủ cấu trúc sau:

```markdown
# BA SPEC: [Tên Tính Năng]

- **Trạng thái:** [Nháp / Đã phê duyệt]
- **Độ phức tạp:** [Nhỏ / Trung bình / Lớn]
- **Domain:** [Tên Domain nghiệp vụ]

## 1. Tổng quan & Mục tiêu

[Mô tả ngắn gọn giá trị nghiệp vụ]

## 2. Danh sách User Stories

- **US-01:** Với vai trò là [vai trò], tôi muốn [mục tiêu/hành động], để [giá trị nghiệp vụ].

## 3. Tiêu chí nghiệm thu (BDD)

- **AC-01 (Luồng chính):** Với điều kiện [điều kiện ban đầu], khi [hành động], thì [kết quả mong đợi].
- **AC-02 (Trường hợp biên):** Với điều kiện [điều kiện ban đầu], khi [hành động], thì [kết quả mong đợi].

## 4. Ảnh hưởng liên Capability (Flow C — Modify và Flow G — Bug Investigation, khi domain có KB)

[Từ `dependency-graph.yaml` → lấy `depended_by` của capability đang sửa, cộng blast radius code-level (tìm caller
theo AGENTS.md §Tìm code) nếu đã xác định symbol cụ thể. Bỏ qua mục này nếu là Full Pipeline, tính năng mới hoặc domain không có KB.]

- **Capability đang sửa:** [tên capability]
- **Các capability có thể bị ảnh hưởng (capability-level, từ dependency-graph.yaml):** [danh sách kèm 1 câu lý do —
  ví dụ: "invoice: đọc trực tiếp trạng thái order"]
- **Blast radius code-level (caller thật trong code, nếu đã xác định symbol cụ thể):** [danh sách caller/dependent thật — file
  + function. Ghi "Chưa xác định code path cụ thể" nếu chưa khoanh vùng được symbol.]
- **Đề xuất phạm vi kiểm thử hồi quy cho QA:** [feature cụ thể trong các capability bị ảnh hưởng]

## 5. Giả định

[Điều BA tự chọn phương án mặc định, chưa được user quyết rõ. User duyệt hoặc đổi ở GATE 3. Không có thì ghi "Không có".]

- **GĐ-01:** [Giả định] — áp dụng cho [US/AC liên quan]. Nếu sai thì [điều gì phải sửa].
```

Trong bản nháp, chỗ còn mơ hồ ghi `[CẦN LÀM RÕ: <câu hỏi>]` ngay tại câu đó. Trước GATE 3 phải hết: hỏi user ở GATE 1,
hoặc chuyển thành `GĐ-n` ở mục 5 và sửa câu gốc theo giả định.

### Quy tắc ngôn ngữ bắt buộc

* **Toàn bộ nội dung BA Spec phải viết bằng tiếng Việt, và ngôn từ trong câu phải tự nhiên.**
* Không sử dụng cấu trúc tiếng Anh như `As a... I want... So that...`.
* User Story bắt buộc sử dụng cấu trúc:
  **"Với vai trò là..., tôi muốn..., để..."**
* Acceptance Criteria bắt buộc sử dụng cấu trúc:
  **"Với điều kiện..., khi..., thì..."**
* Các nhãn như `Happy Path`, `Edge Case` phải được Việt hóa thành **Luồng chính**, **Trường hợp biên** hoặc thuật ngữ
  tiếng Việt tương đương.
* Các thuật ngữ kỹ thuật hoặc tên riêng không cần dịch nếu việc dịch làm mất tính chính xác, ví dụ: `API`, `DB`, `QA`,
  `BA`, `SA`, `capability`, `feature`, `domain`, `KB`, `BDD`, `Full Pipeline`, `Modify`.
* Không dịch tên định danh kỹ thuật, tên file, đường dẫn, tên trường dữ liệu hoặc giá trị cấu hình.
* Khi thuật ngữ tiếng Anh có thể dịch rõ nghĩa mà không ảnh hưởng đến tính chính xác nghiệp vụ, **ưu tiên dùng tiếng
  Việt**.
* Quy tắc này áp dụng cho **tất cả BA Spec được sinh ra về sau**, không chỉ riêng User Stories và Acceptance Criteria.

---


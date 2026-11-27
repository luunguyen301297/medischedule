# TECH REVIEW REPORT: Menu Chuyên khoa (quản lý chuyên khoa và phòng khám)

- **Verdict:** APPROVED
- **Reviewer:** Tech Lead
- **Revision Round:** 1

## 1. Đánh giá chi tiết

- **Khớp BA spec ↔ design:** Đạt. AC-01…AC-24 đều có phần đáp ứng (menu/route mục 5, rule mục 3, public API + landing
  mục 4–5); GĐ-01…GĐ-06 được theo đúng. Không có phần design ngoài scope (404 cho phòng dùng chung truy về "Ngoài phạm
  vi" của spec).
- **Kiến trúc & Pattern:** Đạt. Đúng cấu trúc module `domain/repository/service/web/dto`; controller không chạm
  repository; dùng FK thay vì gọi repository module khác là lựa chọn đúng với quy ước hiện có.
- **Dữ liệu & API Contract:** Đạt. Danh mục nhỏ, không phân trang hợp lý; đếm phòng bằng một query group-by, không N+1.
  Index `lower(name)` vừa chống trùng vừa chống race.
- **Bảo mật & Phân quyền:** Đạt. Admin chặn hai lớp (URL + `@PreAuthorize`); public chỉ lộ `id`, `name` của chuyên khoa
  đang hoạt động.

## 2. Danh sách hành động (Action Items)

1. Bắt `DataIntegrityViolationException` phải ở **trong** method `@Transactional` của service, ngay quanh
   `delete(...)`/`flush()` hoặc `saveAndFlush(...)`, và ném `ApiException` ngay — không dùng tiếp `EntityManager` sau lỗi
   flush (session đã hỏng). Không bắt bằng `@ExceptionHandler` chung: sẽ biến mọi lỗi FK khác thành mã sai.
2. Phân biệt lỗi trùng tên với lỗi khác khi `saveAndFlush`: chỉ map sang `*_EXISTS` khi đã kiểm `exists…` trước đó;
   giữ nguyên kiểm `exists…` để thông báo đúng trong trường hợp thường.
3. Số version migration: `git fetch` và lấy kế tiếp trên `origin/main` (hiện cao nhất là V7 trên nhánh này).
4. Sửa phòng (`PUT /api/admin/rooms/{id}`) khi đổi `specialtyId` sang chuyên khoa đang ngừng mà phòng đang `active=false`
   vẫn phải chặn `SPECIALTY_INACTIVE` (AC-17 chặn "chuyển phòng vào" bất kể trạng thái phòng).
5. Web: sau mutation invalidate đủ ba key nêu trong design; Switch của chuyên khoa hiển thị lỗi 409 bằng
   `message.error` và giữ trạng thái cũ (không cập nhật lạc quan).
6. Unit test bắt buộc cho các nhánh ở bảng mục 3 của design; `npm run typecheck` sạch.

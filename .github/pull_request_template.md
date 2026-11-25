## Thay đổi

<!-- Làm gì, vì sao. Issue: #... Flow (A/B/C/...): -->

## Kiểm

- [ ] Test liên quan đã chạy trên DB `qa_*` (ghi lệnh và kết quả), hoặc ghi rõ phần chưa chạy
- [ ] Web: `npm --prefix web run typecheck` (nếu sửa `web/`)
- [ ] Không sửa migration `V*` cũ; version mới không trùng `origin/main`
- [ ] Chạm "Vùng rủi ro cao" (`.claude/PROJECT/conventions.md`)? Nếu có: đã có người review độc lập
- [ ] Không có `.env`, mật khẩu, token trong diff

## Ghi chú cho người review

<!-- AI đã dùng (nếu có), điểm cần xem kỹ, việc còn lại -->

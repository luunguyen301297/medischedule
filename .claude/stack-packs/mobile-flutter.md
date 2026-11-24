# Stack pack — Mobile: Flutter (Dart)

Áp dụng khi `MOBILE_FRAMEWORK` = Flutter, `MOBILE_LANG` = Dart. Bổ sung cho `mobile-engineer` và `mobile-screen-loop`
(bước 4–6, `references/build-verify.md`). Dự án chưa chạy `flutter create`: điền bảng dưới sau khi khởi tạo; việc khởi
tạo app là một task riêng (Flow F hoặc Full), không tự chạy.

## Lệnh & khoá

| Khoá | Giá trị | Ghi chú |
|---|---|---|
| `MOBILE_FRAMEWORK` | Flutter | version (từ `flutter --version`) ghi ở cột Ghi chú của bảng stack, không ghép vào giá trị |
| `MOBILE_LANG` | Dart | |
| `MOBILE_APP_DIR` | `mobile` | thư mục chứa `pubspec.yaml` |
| `MOBILE_LINT_COMMAND` | `(cd mobile && flutter analyze)` | |
| `MOBILE_TYPECHECK_COMMAND` | `(cd mobile && flutter analyze)` | thêm `flutter test` (cùng `cd`) nếu có test |
| State management / navigation | chọn ở design | ghi ADR (vd Riverpod/Bloc/Provider; go_router/Navigator 2) |
| Client API | sinh từ OpenAPI của backend hoặc viết tay | ghi thư mục + lệnh sinh |
| Máy ảo / toolchain | tên AVD / simulator, đường dẫn SDK | ghi vào README của app |

Kiểm `flutter doctor` trên máy dev trước khi nhận task mobile; SDK chưa cài thì báo user, không tự cài.

## Khởi tạo (task riêng)

- `flutter create --org <reverse-domain> --project-name <ten_app> .` trong `mobile` — `--org` quyết định
  applicationId/bundle id, đổi sau tốn công.
- Client API: nếu backend có OpenAPI, sinh bằng `openapi-generator` (generator `dart-dio`) thay vì viết tay model trùng
  DTO backend; ghi lệnh sinh vào README của app và không sửa tay file sinh ra.
- `.gitignore`: `build/`, `.dart_tool/` (Flutter tạo sẵn khi `create`; kiểm lại nếu thư mục app có trước).

## Quy ước

- Auth theo `.claude/PROJECT/auth.md`: token lưu trong secure storage (vd `flutter_secure_storage`); không lưu token
  trong `SharedPreferences`, không hardcode.
- Thông báo lỗi lấy từ body lỗi của backend (đúng ngôn ngữ backend trả), không tự dịch / tự đặt lại.
- Màu/brand bám design system của web (file theme FE) cho tới khi có design system mobile riêng; khai báo một
  `ThemeData` duy nhất, widget đọc `Theme.of(context)`.
- Thứ tự implement: model (sinh từ OpenAPI hoặc viết) → repository/service → state → navigation → screens → widgets.

## Build + chạy máy ảo (bước 5–6)

- `flutter devices`, `flutter emulators --launch <id>`, `flutter run -d <device>`.
- Backend dev từ máy ảo: Android emulator gọi host qua `10.0.2.2:<cổng backend>`, iOS simulator qua `localhost`. Android
  cần cho phép cleartext HTTP **chỉ ở bản debug** (network security config / `usesCleartextTraffic` trong manifest
  debug).
- Chụp màn hình: `flutter screenshot --out <scratchpad>/s.png` hoặc `adb exec-out screencap -p > <scratchpad>/s.png`.
- Log: `flutter logs`.

## Bẫy

- `ListView` lồng trong `SingleChildScrollView`/`Column` cùng chiều ⇒ lỗi unbounded height; danh sách nhỏ cố định thì
  `Column` + `map`, danh sách dài dùng `CustomScrollView` + slivers (hoặc `shrinkWrap` có cân nhắc hiệu năng).
- Theme sáng/tối: đọc `Theme.of(context)` trong `build`, không chốt màu ở biến top-level.
- `BuildContext` dùng sau `await` phải kiểm `mounted` (`if (!context.mounted) return;`).
- Hot reload không chạy lại `initState`/`main()` — đổi khởi tạo thì hot restart.
- Skill tham khảo Flutter chưa có trong `.claude/skills/_shared/` — kiến thức stack nằm ở pack này.

import type { ThemeConfig } from 'antd'

/**
 * Bảng màu dùng chung cho toàn app. Đổi màu chỉ sửa ở đây:
 *  - CSS dùng biến --ms-* (đẩy vào :root bởi applyCssVars)
 *  - Ant Design dùng theme bên dưới
 * Chủ đạo: xanh cyan y tế (bản thiết kế đầu), dùng cho mọi nút và điểm nhấn.
 */
export const palette = {
  primary: '#0e7490',        // cyan-700, chữ trắng trên nền đạt 5.4:1
  primaryHover: '#155e75',
  primaryActive: '#164e63',
  primarySoft: '#ecfeff',    // nền nhạt cho vùng chọn, nhãn
  brand: '#0e7490',          // nền panel giới thiệu
  brandInk: '#e0f7fa',       // chữ phụ trên nền brand
  // Điểm nhấn dùng chung màu chủ đạo: cả trang chỉ một màu xanh lam cho nút, nhãn, dấu tích
  accent: '#0e7490',
  accentHover: '#155e75',
  accentSoft: '#ecfeff',
  success: '#059669',
  warning: '#d97706',
  error: '#dc2626',
  ink: '#0f2a33',            // chữ chính
  inkSoft: '#3d5a64',        // chữ mô tả (tương phản > 7:1 trên trắng)
  border: '#d5e7ec',
  surface: '#ffffff',
  font: "'Be Vietnam Pro', 'Segoe UI', system-ui, sans-serif",
} as const

const cssVarName = (k: string) => `--ms-${k.replace(/[A-Z]/g, (c) => '-' + c.toLowerCase())}`

export function applyCssVars() {
  const root = document.documentElement.style
  Object.entries(palette).forEach(([k, v]) => root.setProperty(cssVarName(k), v))
}

/** Theme Ant Design: phẳng, không đổ bóng ở bất kỳ component nào. */
export const antTheme: ThemeConfig = {
  token: {
    colorPrimary: palette.primary,
    colorSuccess: palette.success,
    colorWarning: palette.warning,
    colorError: palette.error,
    colorText: palette.ink,
    colorTextDescription: palette.inkSoft,
    colorBorder: palette.border,
    colorBorderSecondary: palette.border,
    colorLink: palette.primary,
    colorLinkHover: palette.primaryHover,
    borderRadius: 8,
    fontFamily: palette.font,
    fontSize: 15,
    controlHeightLG: 46,
    boxShadow: 'none',
    boxShadowSecondary: 'none',
    boxShadowTertiary: 'none',
  },
  components: {
    Button: { primaryShadow: 'none', defaultShadow: 'none', dangerShadow: 'none', fontWeight: 600 },
    Input: { activeShadow: 'none', errorActiveShadow: 'none', warningActiveShadow: 'none' },
    Select: { optionSelectedBg: palette.primarySoft },
    Menu: { itemSelectedBg: palette.primarySoft, itemSelectedColor: palette.primary, itemBorderRadius: 8 },
    Layout: { headerBg: palette.surface, siderBg: palette.surface, bodyBg: palette.surface },
    Table: { headerBg: palette.surface, headerSplitColor: 'transparent' },
    Tabs: { inkBarColor: palette.primary, itemSelectedColor: palette.primary },
  },
}

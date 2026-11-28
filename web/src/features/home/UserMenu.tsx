import { LogoutOutlined } from '@ant-design/icons'
import { Avatar, Divider, Dropdown, theme } from 'antd'
import type { UserResponse } from '@/features/auth'

/** Chữ cái đầu của tên (từ cuối, kiểu tên Việt) làm avatar. */
const initial = (name: string) => name.trim().split(/\s+/).pop()?.charAt(0).toUpperCase() ?? '?'

/** Avatar góc phải thanh điều hướng; bấm mở thông tin tài khoản + đăng xuất. */
export default function UserMenu({ me, onLogout }: { me: UserResponse; onLogout: () => void }) {
  const { token } = theme.useToken()
  return (
    <Dropdown
      trigger={['click']}
      placement="bottomRight"
      menu={{ items: [{ key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true }], onClick: onLogout }}
      dropdownRender={(menu) => (
        <div style={{ background: token.colorBgElevated, borderRadius: token.borderRadiusLG, border: `1px solid ${token.colorBorderSecondary}` }}>
          <div className="user-card">
            <b>{me.fullName}</b>
            <span>{me.email}</span>
            {me.phone && <span>{me.phone}</span>}
          </div>
          <Divider style={{ margin: 0 }} />
          {menu}
        </div>
      )}
    >
      <button type="button" className="nav-user" aria-label="Tài khoản">
        <Avatar size={34}>{initial(me.fullName)}</Avatar>
        <span className="nav-user-name">{me.fullName}</span>
      </button>
    </Dropdown>
  )
}

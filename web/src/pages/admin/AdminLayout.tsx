import { LayoutOutlined, LogoutOutlined, TeamOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Menu, Spin } from 'antd'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { authApi } from '../../api/auth'
import { tokenStore } from '../../api/http'
import { ROLE_LABEL } from '../../api/users'

const MENU = [
  { key: '/admin/staff', icon: <TeamOutlined />, label: 'Tài khoản nhân viên' },
  { key: '/admin/homepage', icon: <LayoutOutlined />, label: 'Thiết lập trang chủ' },
]

/** Khung khu quản trị: chỉ ADMIN vào được. */
export default function AdminLayout() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { data: me, isLoading } = useQuery({ queryKey: ['me'], queryFn: authApi.me })

  if (isLoading) return <Spin fullscreen />
  if (!me) return <Navigate to="/login" replace />
  if (me.role !== 'ADMIN') return <Navigate to="/" replace />

  const logout = () => {
    tokenStore.clear()
    navigate('/login', { replace: true })
  }

  return (
    <div className="admin-shell">
      <aside className="admin-side">
        <div className="brand-logo">
          <span className="brand-mark" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M9 2h6v7h7v6h-7v7H9v-7H2V9h7z" /></svg>
          </span>
          Quản trị
        </div>
        <Menu
          mode="inline"
          selectedKeys={[MENU.find((m) => pathname.startsWith(m.key))?.key ?? '']}
          items={MENU}
          onClick={(e) => navigate(e.key)}
        />
        <div className="admin-user">
          <div>
            <b>{me.fullName}</b>
            <div>{ROLE_LABEL[me.role]}</div>
          </div>
          <Button icon={<LogoutOutlined />} onClick={logout} block>
            Đăng xuất
          </Button>
        </div>
      </aside>
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  )
}

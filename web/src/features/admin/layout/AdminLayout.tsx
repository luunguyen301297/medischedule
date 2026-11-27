import { ApartmentOutlined, LayoutOutlined, LogoutOutlined, SettingOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Menu, Spin } from 'antd'
import { useEffect, useState } from 'react'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { authApi } from '@/features/auth'
import { tokenStore } from '@/shared/api/http'
import '../admin.css'

type MenuItem = { key: string; icon: JSX.Element; label: string; children?: MenuItem[] }

const MENU: MenuItem[] = [
  { key: '/admin/staff', icon: <TeamOutlined />, label: 'Tài khoản nhân viên' },
  { key: '/admin/patients', icon: <UserOutlined />, label: 'Tài khoản bệnh nhân' },
  {
    key: 'settings',
    icon: <SettingOutlined />,
    label: 'Cài đặt',
    children: [
      { key: '/admin/homepage', icon: <LayoutOutlined />, label: 'Trang chủ' },
      { key: '/admin/specialties', icon: <ApartmentOutlined />, label: 'Chuyên khoa' },
    ],
  },
]

/** Mục lá đang khớp đường dẫn, kèm key menu cha (nếu có) để mở sẵn. */
function findActive(pathname: string) {
  for (const m of MENU) {
    if (!m.children) {
      if (pathname.startsWith(m.key)) return { key: m.key, parent: undefined }
      continue
    }
    const child = m.children.find((c) => pathname.startsWith(c.key))
    if (child) return { key: child.key, parent: m.key }
  }
  return { key: '', parent: undefined }
}

/** Khung khu quản trị: chỉ ADMIN vào được. */
export default function AdminLayout() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { data: me, isLoading } = useQuery({ queryKey: ['me'], queryFn: authApi.me })
  const active = findActive(pathname)
  const [openKeys, setOpenKeys] = useState<string[]>([])
  // Menu cha mặc định đóng; đổi trang thì chỉ mở menu cha chứa trang đang active
  useEffect(() => setOpenKeys(active.parent ? [active.parent] : []), [active.parent])

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
          selectedKeys={[active.key]}
          openKeys={openKeys}
          onOpenChange={setOpenKeys}
          items={MENU}
          onClick={(e) => navigate(e.key)}
        />
        <div className="admin-user">
          <div>
            <b>{me.fullName}</b>
            {/* Khu này chỉ ADMIN vào được nên không cần hiện vai trò */}
            <div className="admin-user-sub">{me.username ?? me.email}</div>
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

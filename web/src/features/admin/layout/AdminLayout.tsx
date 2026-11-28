import { ApartmentOutlined, CalendarOutlined, ClockCircleOutlined, ExperimentOutlined, LayoutOutlined, LogoutOutlined, MedicineBoxOutlined, MenuOutlined, SafetyCertificateOutlined, ScheduleOutlined, SettingOutlined, SolutionOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Menu, Spin, Tooltip } from 'antd'
import { useEffect, useState } from 'react'
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { authApi, ROLE_LABEL } from '@/features/auth'
import { contentApi } from '@/features/landing'
import { tokenStore } from '@/shared/api/http'
import '../admin.css'

type MenuItem = { key: string; icon: JSX.Element; label: string; children?: MenuItem[] }

const BOOKINGS = '/admin/bookings'
const RECEPTION = '/admin/reception'
/** Trang lễ tân được vào; các trang khác của khu quản trị chỉ ADMIN */
const RECEPTIONIST_PAGES = [BOOKINGS, RECEPTION]

const MENU: MenuItem[] = [
  { key: BOOKINGS, icon: <ScheduleOutlined />, label: 'Lịch hẹn' },
  { key: RECEPTION, icon: <SolutionOutlined />, label: 'Lễ tân' },
  { key: '/admin/doctor', icon: <MedicineBoxOutlined />, label: 'Bác sĩ' },
  { key: '/admin/pharmacy', icon: <ExperimentOutlined />, label: 'Dược sĩ' },
  { key: '/admin/work-schedule', icon: <CalendarOutlined />, label: 'Lịch làm việc' },
  {
    key: 'accounts',
    icon: <SafetyCertificateOutlined />,
    label: 'Quản trị',
    children: [
      { key: '/admin/staff', icon: <TeamOutlined />, label: 'Nhân viên' },
      { key: '/admin/patients', icon: <UserOutlined />, label: 'Bệnh nhân' },
    ],
  },
  {
    key: 'settings',
    icon: <SettingOutlined />,
    label: 'Cài đặt',
    children: [
      { key: '/admin/homepage', icon: <LayoutOutlined />, label: 'Trang chủ' },
      { key: '/admin/specialties', icon: <ApartmentOutlined />, label: 'Chuyên khoa' },
      { key: '/admin/work-shifts', icon: <ClockCircleOutlined />, label: 'Ca làm việc' },
    ],
  },
]

/** Lễ tân chỉ thấy Lịch hẹn và Lễ tân; quản trị viên thấy đủ. */
const menuFor = (role: string) => (role === 'RECEPTIONIST' ? MENU.filter((m) => RECEPTIONIST_PAGES.includes(m.key)) : MENU)

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

/** Khung khu quản trị: ADMIN vào đủ; RECEPTIONIST chỉ vào Lịch hẹn và Lễ tân. */
export default function AdminLayout() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { data: me, isLoading } = useQuery({ queryKey: ['me'], queryFn: authApi.me })
  // Cùng key với trang Thiết lập trang chủ: lưu tên mới là header đổi ngay
  const { data: homepage } = useQuery({ queryKey: ['homepage'], queryFn: contentApi.homepage })
  const active = findActive(pathname)
  const [openKeys, setOpenKeys] = useState<string[]>([])
  // Sidebar mặc định thu thành cột icon; chọn trang xong tự thu lại, nút 3 gạch để mở / thu
  const [collapsed, setCollapsed] = useState(true)
  // Menu cha mặc định đóng; đổi trang thì chỉ mở menu cha chứa trang đang active
  useEffect(() => setOpenKeys(active.parent ? [active.parent] : []), [active.parent])

  if (isLoading) return <Spin fullscreen />
  if (!me) return <Navigate to="/login" replace />
  if (me.role !== 'ADMIN' && me.role !== 'RECEPTIONIST') return <Navigate to="/" replace />
  if (me.role === 'RECEPTIONIST' && !RECEPTIONIST_PAGES.some((p) => pathname.startsWith(p))) {
    return <Navigate to={BOOKINGS} replace />
  }

  const logout = () => {
    tokenStore.clear()
    navigate('/login', { replace: true })
  }

  return (
    <div className={collapsed ? 'admin-shell is-collapsed' : 'admin-shell'}>
      <aside className="admin-side">
        <div className="admin-side-head">
          {!collapsed && (
            <div className="brand-logo">
              <span className="brand-mark" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M9 2h6v7h7v6h-7v7H9v-7H2V9h7z" /></svg>
              </span>
              {homepage?.intro.brandName}
            </div>
          )}
          <Button
            type="text"
            icon={<MenuOutlined />}
            className="admin-toggle"
            onClick={() => setCollapsed((c) => !c)}
            aria-label={collapsed ? 'Mở menu' : 'Thu gọn menu'}
            aria-expanded={!collapsed}
          />
        </div>
        <Menu
          mode="inline"
          inlineCollapsed={collapsed}
          selectedKeys={[active.key]}
          // Thu gọn thì menu con hiện dạng popup, không ép openKeys để popup tự đóng
          {...(collapsed ? {} : { openKeys, onOpenChange: setOpenKeys })}
          items={menuFor(me.role)}
          onClick={(e) => {
            navigate(e.key)
            setCollapsed(true)
          }}
        />
        <div className="admin-user">
          {!collapsed && (
            <div>
              <b>{me.fullName}</b>
              <div className="admin-user-sub">
                {me.role === 'ADMIN' ? (me.username ?? me.email) : `${ROLE_LABEL[me.role]} · ${me.username ?? me.email}`}
              </div>
            </div>
          )}
          <Tooltip title={collapsed ? `Đăng xuất (${me.fullName})` : undefined} placement="right">
            <Button icon={<LogoutOutlined />} onClick={logout} block aria-label="Đăng xuất">
              {!collapsed && 'Đăng xuất'}
            </Button>
          </Tooltip>
        </div>
      </aside>
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  )
}

import { useQuery } from '@tanstack/react-query'
import { Button, Descriptions, Space, Spin } from 'antd'
import { Navigate, useNavigate } from 'react-router-dom'
import { authApi, ROLE_LABEL } from '@/features/auth'
import { tokenStore } from '@/shared/api/http'

/** Trang tạm sau đăng nhập: hiện người dùng hiện tại. Các khu vực theo vai trò thêm sau. */
export default function HomePage() {
  const navigate = useNavigate()
  const { data: me, isLoading } = useQuery({ queryKey: ['me'], queryFn: authApi.me })

  // Quản trị viên không dùng trang này: vào thẳng khu quản trị
  if (me?.role === 'ADMIN') return <Navigate to="/admin" replace />

  const logout = () => {
    tokenStore.clear()
    navigate('/login', { replace: true })
  }

  return (
    <div style={{ minHeight: '100vh' }}>
      <header
        style={{
          height: 64, padding: '0 32px', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          borderBottom: '1px solid var(--ms-border)', fontWeight: 700, fontSize: 18,
        }}
      >
        MediSchedule
        <Space>
          <Button onClick={logout}>Đăng xuất</Button>
        </Space>
      </header>
      <main style={{ padding: 32 }}>
        {isLoading || !me ? (
          <Spin />
        ) : (
          <Descriptions title="Tài khoản" column={1} bordered size="small" style={{ maxWidth: 560 }}>
            <Descriptions.Item label="Họ tên">{me.fullName}</Descriptions.Item>
            <Descriptions.Item label="Email">{me.email}</Descriptions.Item>
            {me.username && <Descriptions.Item label="Tên đăng nhập">{me.username}</Descriptions.Item>}
            <Descriptions.Item label="Vai trò">{ROLE_LABEL[me.role]}</Descriptions.Item>
          </Descriptions>
        )}
      </main>
    </div>
  )
}

import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { App, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { authApi, type TokenResponse } from '../api'
import RegisterDialog from '../components/RegisterDialog'
import { errorMessage, tokenStore } from '@/shared/api/http'
import { LandingLayout } from '@/features/landing'

/** Trang đăng nhập; đăng ký mở dạng dialog trên trang này (/register mở sẵn dialog). */
export default function LoginPage() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { message } = App.useApp()
  const [loading, setLoading] = useState(false)
  const [registerOpen, setRegisterOpen] = useState(pathname === '/register')

  const enter = (res: TokenResponse) => {
    tokenStore.set(res.accessToken)
    navigate(res.user.role === 'ADMIN' ? '/admin' : '/', { replace: true })
  }

  const closeRegister = () => {
    setRegisterOpen(false)
    if (pathname === '/register') navigate('/login', { replace: true })
  }

  const onFinish = async (v: { login: string; password: string }) => {
    setLoading(true)
    try {
      enter(await authApi.login(v.login, v.password))
    } catch (e) {
      message.error(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <LandingLayout
      title="Đăng nhập"
      subtitle="Chào mừng trở lại. Đăng nhập để đặt lịch và theo dõi lượt khám."
      onRegister={() => setRegisterOpen(true)}
    >
      <Form layout="vertical" size="large" onFinish={onFinish} requiredMark={false}>
        <Form.Item
          name="login"
          label="Email, số điện thoại hoặc tên đăng nhập"
          rules={[{ required: true, whitespace: true, message: 'Nhập email, số điện thoại hoặc tên đăng nhập' }]}
        >
          <Input prefix={<UserOutlined />} placeholder="ten@email.com hoặc 09xx xxx xxx" autoComplete="username" />
        </Form.Item>
        <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
          <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading} style={{ marginTop: 8 }}>
          Đăng nhập
        </Button>
      </Form>
      <div className="auth-switch">
        Chưa có tài khoản?{' '}
        <Button type="link" className="auth-switch-link" onClick={() => setRegisterOpen(true)}>
          Đăng ký ngay
        </Button>
      </div>
      <RegisterDialog open={registerOpen} onClose={closeRegister} onDone={enter} />
    </LandingLayout>
  )
}

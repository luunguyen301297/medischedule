import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { App, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api'
import { errorMessage, tokenStore } from '@/shared/api/http'
import { LandingLayout } from '@/features/landing'

export default function LoginPage() {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const [loading, setLoading] = useState(false)

  const onFinish = async (v: { login: string; password: string }) => {
    setLoading(true)
    try {
      const res = await authApi.login(v.login, v.password)
      tokenStore.set(res.accessToken)
      navigate(res.user.role === 'ADMIN' ? '/admin' : '/', { replace: true })
    } catch (e) {
      message.error(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <LandingLayout title="Đăng nhập" subtitle="Chào mừng trở lại. Đăng nhập để đặt lịch và theo dõi lượt khám.">
      <Form layout="vertical" size="large" onFinish={onFinish} requiredMark={false}>
        <Form.Item
          name="login"
          label="Email hoặc tên đăng nhập"
          rules={[{ required: true, whitespace: true, message: 'Nhập email hoặc tên đăng nhập' }]}
        >
          <Input prefix={<UserOutlined />} placeholder="ten@email.com" autoComplete="username" />
        </Form.Item>
        <Form.Item name="password" label="Mật khẩu" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
          <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading} style={{ marginTop: 8 }}>
          Đăng nhập
        </Button>
      </Form>
      <div className="auth-switch">
        Chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link>
      </div>
    </LandingLayout>
  )
}

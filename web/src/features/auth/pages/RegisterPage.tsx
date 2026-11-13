import { LockOutlined, MailOutlined, PhoneOutlined, UserOutlined } from '@ant-design/icons'
import { App, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi, type RegisterRequest } from '../api'
import { errorCode, errorMessage, fieldErrors, tokenStore } from '@/shared/api/http'
import { LandingLayout } from '@/features/landing'

type FormValues = RegisterRequest & { confirm: string }

/** Bệnh nhân tự tạo tài khoản; backend luôn gán vai trò PATIENT. */
export default function RegisterPage() {
  const navigate = useNavigate()
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()
  const [loading, setLoading] = useState(false)

  const onFinish = async ({ confirm: _confirm, ...v }: FormValues) => {
    setLoading(true)
    try {
      const res = await authApi.register({ ...v, phone: v.phone || undefined })
      tokenStore.set(res.accessToken)
      message.success('Tạo tài khoản thành công')
      navigate('/', { replace: true })
    } catch (e) {
      // Lỗi theo từng ô (validate của backend) hiện ngay dưới ô đó
      const fields = fieldErrors(e)
      if (fields) {
        form.setFields(Object.entries(fields).map(([name, err]) => ({ name: name as keyof FormValues, errors: [err] })))
      } else {
        if (errorCode(e) === 'EMAIL_EXISTS') form.setFields([{ name: 'email', errors: [errorMessage(e)] }])
        else message.error(errorMessage(e))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <LandingLayout title="Tạo tài khoản" subtitle="Dành cho bệnh nhân. Chỉ mất chưa tới một phút.">
      <Form form={form} layout="vertical" size="large" onFinish={onFinish} requiredMark={false}>
        <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, whitespace: true, message: 'Nhập họ và tên' }, { max: 120 }]}>
          <Input prefix={<UserOutlined />} placeholder="Nguyễn Văn A" autoComplete="name" />
        </Form.Item>

        <div className="auth-row">
          <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email', message: 'Nhập email hợp lệ' }]}>
            <Input prefix={<MailOutlined />} placeholder="ten@email.com" autoComplete="email" />
          </Form.Item>
          <Form.Item
            name="phone"
            label="Số điện thoại"
            rules={[{ pattern: /^0\d{9}$/, message: 'Số điện thoại gồm 10 số, bắt đầu bằng 0' }]}
          >
            <Input prefix={<PhoneOutlined />} placeholder="Không bắt buộc" autoComplete="tel" inputMode="tel" maxLength={10} />
          </Form.Item>
        </div>

        <div className="auth-row">
          <Form.Item
            name="password"
            label="Mật khẩu"
            rules={[{ required: true, message: 'Nhập mật khẩu' }, { min: 8, message: 'Ít nhất 8 ký tự' }, { max: 72 }]}
            hasFeedback
          >
            <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirm"
            label="Nhập lại mật khẩu"
            dependencies={['password']}
            hasFeedback
            rules={[
              { required: true, message: 'Nhập lại mật khẩu' },
              ({ getFieldValue }) => ({
                validator: (_, value) =>
                  !value || getFieldValue('password') === value
                    ? Promise.resolve()
                    : Promise.reject(new Error('Mật khẩu không khớp')),
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
          </Form.Item>
        </div>

        <Button type="primary" htmlType="submit" block loading={loading} style={{ marginTop: 8 }}>
          Tạo tài khoản
        </Button>
      </Form>
      <div className="auth-switch">
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </div>
    </LandingLayout>
  )
}

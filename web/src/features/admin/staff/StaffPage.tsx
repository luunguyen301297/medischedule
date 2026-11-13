import { CopyOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Radio, Select, Switch, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useState } from 'react'
import { authApi, ROLE_LABEL, type Role, type UserResponse } from '@/features/auth'
import { errorCode, errorMessage, fieldErrors } from '@/shared/api/http'
import { STAFF_ROLES, usersApi, type CreateUserRequest } from './api'

const ROLE_COLOR: Record<Role, string> = {
  ADMIN: 'geekblue',
  DOCTOR: 'blue',
  RECEPTIONIST: 'cyan',
  PHARMACIST: 'green',
  PATIENT: 'default',
}

/** Mật khẩu tạm 12 ký tự, tránh ký tự dễ nhầm (0/O, 1/l). */
function randomPassword() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$'
  const buf = new Uint32Array(12)
  crypto.getRandomValues(buf)
  return Array.from(buf, (n) => chars[n % chars.length]).join('')
}

const fmtTime = (iso: string | null) => (iso ? new Date(iso).toLocaleString('vi-VN') : 'Chưa đăng nhập')

export default function StaffPage() {
  const { message, modal } = App.useApp()
  const qc = useQueryClient()
  const [role, setRole] = useState<Role | undefined>()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [creating, setCreating] = useState(false)
  const [form] = Form.useForm<CreateUserRequest>()

  const { data: me } = useQuery({ queryKey: ['me'], queryFn: authApi.me })
  const { data, isFetching, refetch } = useQuery({
    queryKey: ['admin-users', role, q, page, size],
    queryFn: () => usersApi.list({ role, q: q || undefined, page, size }),
    placeholderData: keepPreviousData,
  })

  const refresh = () => qc.invalidateQueries({ queryKey: ['admin-users'] })

  const changeRole = useMutation({
    mutationFn: ({ id, role }: { id: number; role: Role }) => usersApi.changeRole(id, role),
    onSuccess: () => { message.success('Đã đổi vai trò'); refresh() },
    onError: (e) => message.error(errorMessage(e)),
  })
  const setEnabled = useMutation({
    mutationFn: ({ id, value }: { id: number; value: boolean }) => usersApi.setEnabled(id, value),
    onSuccess: (u) => { message.success(u.enabled ? 'Đã mở khóa tài khoản' : 'Đã khóa tài khoản'); refresh() },
    onError: (e) => message.error(errorMessage(e)),
  })
  const create = useMutation({
    mutationFn: usersApi.create,
    onSuccess: (u, req) => {
      setCreating(false)
      form.resetFields()
      refresh()
      // Hiện thông tin đăng nhập một lần để admin gửi cho nhân viên
      modal.success({
        title: 'Đã tạo tài khoản',
        width: 460,
        content: (
          <div style={{ display: 'grid', gap: 6, marginTop: 8 }}>
            <div>{u.fullName} · {ROLE_LABEL[u.role]}</div>
            <div>Đăng nhập: <b>{u.username ?? u.email}</b></div>
            <div>
              Mật khẩu: <Typography.Text code copyable={{ icon: <CopyOutlined /> }}>{req.password}</Typography.Text>
            </div>
            <div>Hãy gửi thông tin này cho nhân viên. Mật khẩu sẽ không hiện lại.</div>
          </div>
        ),
      })
    },
    onError: (e) => {
      const fields = fieldErrors(e)
      const code = errorCode(e)
      if (fields) form.setFields(Object.entries(fields).map(([name, err]) => ({ name: name as keyof CreateUserRequest, errors: [err] })))
      else if (code === 'EMAIL_EXISTS') form.setFields([{ name: 'email', errors: [errorMessage(e)] }])
      else if (code === 'USERNAME_EXISTS') form.setFields([{ name: 'username', errors: [errorMessage(e)] }])
      else message.error(errorMessage(e))
    },
  })

  const openCreate = () => {
    form.resetFields()
    form.setFieldsValue({ role: 'DOCTOR', password: randomPassword() })
    setCreating(true)
  }

  const columns: ColumnsType<UserResponse> = [
    {
      title: 'Họ tên',
      dataIndex: 'fullName',
      render: (v: string, u) => (
        <div>
          <div style={{ fontWeight: 600 }}>{v}</div>
          <div style={{ fontSize: 13 }}>{u.email}</div>
        </div>
      ),
    },
    { title: 'Tên đăng nhập', dataIndex: 'username', width: 160, render: (v: string | null) => v ?? '—' },
    { title: 'Số điện thoại', dataIndex: 'phone', width: 140, render: (v: string | null) => v ?? '—' },
    {
      title: 'Vai trò',
      dataIndex: 'role',
      width: 190,
      render: (r: Role, u) =>
        u.id === me?.id ? (
          <Tag color={ROLE_COLOR[r]}>{ROLE_LABEL[r]}</Tag>
        ) : (
          <Select
            id={`row-role-${u.id}`}
            size="small"
            value={r}
            style={{ width: 160 }}
            disabled={changeRole.isPending}
            onChange={(v) => changeRole.mutate({ id: u.id, role: v })}
            options={(Object.keys(ROLE_LABEL) as Role[]).map((k) => ({ value: k, label: ROLE_LABEL[k] }))}
          />
        ),
    },
    {
      title: 'Hoạt động',
      dataIndex: 'enabled',
      width: 110,
      render: (v: boolean, u) => (
        <Switch
          checked={v}
          disabled={u.id === me?.id}
          loading={setEnabled.isPending && setEnabled.variables?.id === u.id}
          onChange={(value) => setEnabled.mutate({ id: u.id, value })}
          aria-label={v ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}
        />
      ),
    },
    { title: 'Đăng nhập gần nhất', dataIndex: 'lastLoginAt', width: 190, render: fmtTime },
  ]

  return (
    <div className="admin-page">
      <div className="admin-head">
        <div>
          <h1>Tài khoản nhân viên</h1>
          <p>Tạo tài khoản cho bác sĩ, lễ tân, dược sĩ, quản trị viên; đổi vai trò hoặc khóa tài khoản.</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          Tạo tài khoản
        </Button>
      </div>

      <div className="admin-toolbar">
        <Input.Search
          id="staff-search"
          allowClear
          placeholder="Tìm theo họ tên, email, tên đăng nhập"
          style={{ width: 360 }}
          onSearch={(v) => { setQ(v.trim()); setPage(0) }}
        />
        <Select
          id="staff-role-filter"
          allowClear
          placeholder="Tất cả vai trò"
          style={{ width: 200 }}
          value={role}
          onChange={(v) => { setRole(v); setPage(0) }}
          options={(Object.keys(ROLE_LABEL) as Role[]).map((k) => ({ value: k, label: ROLE_LABEL[k] }))}
        />
        <Button icon={<ReloadOutlined />} onClick={() => refetch()}>Tải lại</Button>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isFetching}
        pagination={{
          current: page + 1,
          pageSize: size,
          total: data?.page.totalElements ?? 0,
          showSizeChanger: false,
          showTotal: (t) => `${t} tài khoản`,
          onChange: (p, s) => { setPage(p - 1); setSize(s) },
        }}
      />

      <Modal
        title="Tạo tài khoản nhân viên"
        open={creating}
        width={560}
        okText="Tạo tài khoản"
        cancelText="Hủy"
        confirmLoading={create.isPending}
        onOk={() => form.submit()}
        onCancel={() => setCreating(false)}
        destroyOnClose
      >
        <Form
          form={form}
          name="staff"
          layout="vertical"
          requiredMark={false}
          onFinish={(v) => create.mutate({ ...v, username: v.username || undefined, phone: v.phone || undefined })}
          style={{ marginTop: 16 }}
        >
          <Form.Item name="role" label="Vai trò" htmlFor="staff_role_0" rules={[{ required: true }]}>
            <Radio.Group
              name="role"
              optionType="button"
              buttonStyle="solid"
              options={STAFF_ROLES.map((r, i) => ({ value: r, label: ROLE_LABEL[r], id: `staff_role_${i}` }))}
            />
          </Form.Item>
          <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, whitespace: true, message: 'Nhập họ và tên' }, { max: 120 }]}>
            <Input placeholder="BS. Nguyễn Văn A" />
          </Form.Item>
          <div className="auth-row">
            <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email', message: 'Nhập email hợp lệ' }]}>
              <Input placeholder="ten@phongkham.vn" />
            </Form.Item>
            <Form.Item
              name="username"
              label="Tên đăng nhập"
              tooltip="Không bắt buộc. Nhân viên đăng nhập được bằng email hoặc tên này."
              rules={[{ pattern: /^[a-zA-Z0-9._-]{3,50}$/, message: '3–50 ký tự: chữ, số, dấu chấm, gạch' }]}
            >
              <Input placeholder="bs.nguyenvana" />
            </Form.Item>
          </div>
          <div className="auth-row">
            <Form.Item name="phone" label="Số điện thoại" rules={[{ pattern: /^0\d{9}$/, message: '10 số, bắt đầu bằng 0' }]}>
              <Input placeholder="Không bắt buộc" maxLength={10} />
            </Form.Item>
            <Form.Item label="Mật khẩu tạm" required>
              <div style={{ display: 'flex', gap: 8 }}>
                <Form.Item name="password" noStyle rules={[{ required: true, message: 'Nhập mật khẩu' }, { min: 8, message: 'Ít nhất 8 ký tự' }]}>
                  <Input />
                </Form.Item>
                <Button onClick={() => form.setFieldsValue({ password: randomPassword() })}>Tạo mới</Button>
              </div>
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  )
}

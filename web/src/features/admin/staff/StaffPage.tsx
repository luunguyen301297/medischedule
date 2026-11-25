import { CopyOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData, useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Form, Input, Modal, Radio, Select, Spin, Switch, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useRef, useState } from 'react'
import { authApi, ROLE_LABEL, type Role, type UserResponse } from '@/features/auth'
import { errorCode, errorMessage, fieldErrors } from '@/shared/api/http'
import { STAFF_ROLES, usersApi, type CreateUserRequest, type UserGroup } from './api'

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

const PAGE_SIZE = 20

const fmtTime = (iso: string | null) => (iso ? new Date(iso).toLocaleString('vi-VN') : 'Chưa đăng nhập')

const TEXT: Record<UserGroup, { title: string; desc: string }> = {
  staff: {
    title: 'Tài khoản nhân viên',
    desc: 'Tạo tài khoản cho bác sĩ, lễ tân, dược sĩ, quản trị viên; đổi vai trò hoặc khóa tài khoản.',
  },
  patient: {
    title: 'Tài khoản bệnh nhân',
    desc: 'Bệnh nhân tự đăng ký. Tra cứu, khóa hoặc mở khóa tài khoản.',
  },
}

/** Danh sách tài khoản theo nhóm: nhân viên (/admin/staff) hoặc bệnh nhân (/admin/patients). */
export default function StaffPage({ group }: { group: UserGroup }) {
  const isStaff = group === 'staff'
  const { message, modal } = App.useApp()
  const qc = useQueryClient()
  const [role, setRole] = useState<Role | undefined>()
  const [q, setQ] = useState('')
  const [creating, setCreating] = useState(false)
  const [form] = Form.useForm<CreateUserRequest>()

  const { data: me } = useQuery({ queryKey: ['me'], queryFn: authApi.me })
  const { data, isFetching, isFetchingNextPage, hasNextPage, fetchNextPage, refetch } = useInfiniteQuery({
    queryKey: ['admin-users', group, role, q],
    queryFn: ({ pageParam }) => usersApi.list({ group, role, q: q || undefined, page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page.number + 1 < last.page.totalPages ? last.page.number + 1 : undefined),
    placeholderData: keepPreviousData,
  })

  const rows = data?.pages.flatMap((p) => p.content) ?? []

  // Cuộn tới cuối bảng thì tải trang kế tiếp
  const sentinel = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const el = sentinel.current
    if (!el || !hasNextPage) return
    const io = new IntersectionObserver(([e]) => { if (e.isIntersecting && !isFetching) fetchNextPage() }, { rootMargin: '200px' })
    io.observe(el)
    return () => io.disconnect()
  }, [hasNextPage, isFetching, fetchNextPage])

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
        !isStaff || u.id === me?.id ? (
          <Tag color={ROLE_COLOR[r]}>{ROLE_LABEL[r]}</Tag>
        ) : (
          <Select
            id={`row-role-${u.id}`}
            size="small"
            value={r}
            style={{ width: 160 }}
            disabled={changeRole.isPending}
            onChange={(v) => changeRole.mutate({ id: u.id, role: v })}
            options={STAFF_ROLES.map((k) => ({ value: k, label: ROLE_LABEL[k] }))}
          />
        ),
    },
    {
      title: 'Hoạt động',
      dataIndex: 'enabled',
      width: 140,
      onHeaderCell: () => ({ style: { whiteSpace: 'nowrap' } }),
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
          <h1>{TEXT[group].title}</h1>
          <p>{TEXT[group].desc}</p>
        </div>
        {isStaff && (
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Tạo tài khoản
          </Button>
        )}
      </div>

      <div className="admin-toolbar">
        <Input.Search
          id="staff-search"
          allowClear
          placeholder="Tìm theo họ tên, email, tên đăng nhập"
          style={{ width: 360, maxWidth: '100%' }}
          onSearch={(v) => setQ(v.trim())}
        />
        {isStaff && (
          <Select
            id="staff-role-filter"
            allowClear
            placeholder="Tất cả vai trò"
            style={{ width: 200 }}
            value={role}
            onChange={(v) => setRole(v)}
            options={STAFF_ROLES.map((k) => ({ value: k, label: ROLE_LABEL[k] }))}
          />
        )}
        <Button icon={<ReloadOutlined />} onClick={() => refetch()}>Tải lại</Button>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={rows}
        loading={isFetching && !isFetchingNextPage}
        pagination={false}
        scroll={{ x: 'max-content' }}
      />
      <div ref={sentinel} className="admin-list-end">
        {isFetchingNextPage ? <Spin size="small" /> : data && `${rows.length} / ${data.pages[0].page.totalElements} tài khoản`}
      </div>

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

import { EditOutlined } from '@ant-design/icons'
import { Button, Descriptions, Form, Input, Modal, type FormInstance } from 'antd'
import type { NamePath } from 'antd/es/form/interface'
import type { Rule } from 'antd/es/form'
import type { ReactNode } from 'react'
import { ROLE_LABEL, type UserResponse } from '@/features/auth'
import { errorCode, errorMessage, fieldErrors } from '@/shared/api/http'
import type { UpdateUserRequest } from './api'

export const dash = (v: string | number | null | undefined) => (v == null || v === '' ? '—' : v)
export const fmtDate = (d: string | null) => (d ? new Date(d).toLocaleDateString('vi-VN') : '—')
export const blank = (v: string | null | undefined) => (v && v.trim() ? v.trim() : null)

/** Một dòng của bảng hồ sơ: view khi xem, edit (nếu có) khi đang sửa. */
export interface Row {
  key: string
  label: ReactNode
  view: ReactNode
  edit?: ReactNode
}

/** Bảng hồ sơ sửa tại chỗ: đang sửa thì ô giá trị thành control, giữ nguyên bố cục. */
export const InfoTable = ({ title, extra, rows, editing }: { title: ReactNode; extra?: ReactNode; rows: Row[]; editing: boolean }) => (
  <Descriptions
    title={title}
    extra={extra}
    column={1}
    bordered
    size="small"
    labelStyle={{ width: 200 }}
    items={rows.map((r) => ({ key: r.key, label: r.label, children: editing && r.edit ? r.edit : r.view }))}
  />
)

/** Form.Item gọn để đặt trong ô bảng. */
export const cell = (name: NamePath, control: ReactNode, rules?: Rule[], extra?: { valuePropName?: string }) => (
  <Form.Item name={name} rules={rules} style={{ margin: 0 }} {...extra}>
    {control}
  </Form.Item>
)

/** Gắn lỗi backend vào ô: lỗi theo trường (body.fields) hoặc mã lỗi → trường; còn lại báo toast. */
export function applyError(form: FormInstance, e: unknown, codes: Record<string, string>, prefix: (string | number)[], toast: (m: string) => void) {
  const fields = fieldErrors(e)
  const field = codes[errorCode(e) ?? '']
  if (fields) form.setFields(Object.entries(fields).map(([n, err]) => ({ name: [...prefix, n], errors: [err] })))
  else if (field) form.setFields([{ name: [...prefix, field], errors: [errorMessage(e)] }])
  else toast(errorMessage(e))
}

// ---------- Khối tài khoản (dùng chung 2 dialog), giá trị form nằm dưới 'account' ----------

export const ACCOUNT_ERRORS = { EMAIL_EXISTS: 'email', USERNAME_EXISTS: 'username', PHONE_EXISTS: 'phone', PHONE_REQUIRED: 'phone' }

export const accountValues = (u: UserResponse): UpdateUserRequest => ({
  fullName: u.fullName,
  email: u.email,
  username: u.username ?? '',
  phone: u.phone ?? '',
})

export const toUpdateUser = (v: UpdateUserRequest): UpdateUserRequest => ({
  fullName: v.fullName.trim(),
  email: v.email.trim(),
  username: blank(v.username),
  phone: blank(v.phone),
})

export function accountRows(u: UserResponse): Row[] {
  const isPatient = u.role === 'PATIENT'
  const rows: (Row | false)[] = [
    { key: 'fullName', label: 'Họ tên', view: u.fullName, edit: cell(['account', 'fullName'], <Input />, [{ required: true, whitespace: true, message: 'Nhập họ tên' }, { max: 120 }]) },
    !isPatient && { key: 'role', label: 'Vai trò', view: ROLE_LABEL[u.role] },
    { key: 'email', label: 'Email', view: u.email, edit: cell(['account', 'email'], <Input />, [{ required: true, type: 'email', message: 'Nhập email hợp lệ' }]) },
    !isPatient && {
      key: 'username',
      label: 'Tên đăng nhập',
      view: dash(u.username),
      edit: cell(['account', 'username'], <Input placeholder="Không bắt buộc" />, [{ pattern: /^[a-zA-Z0-9._-]{3,50}$/, message: '3–50 ký tự: chữ, số, dấu chấm, gạch' }]),
    },
    {
      key: 'phone',
      label: 'Số điện thoại',
      view: dash(u.phone),
      edit: cell(['account', 'phone'], <Input maxLength={10} placeholder={isPatient ? undefined : 'Không bắt buộc'} />, [
        { required: isPatient, message: 'Nhập số điện thoại' },
        { pattern: /^0\d{9}$/, message: '10 số, bắt đầu bằng 0' },
      ]),
    },
    { key: 'createdAt', label: isPatient ? 'Ngày đăng ký' : 'Ngày tạo', view: fmtDate(u.createdAt) },
  ]
  return rows.filter((r): r is Row => !!r)
}

/** Khung dialog hồ sơ: một nút Sửa duy nhất ở chân; đang sửa thì thành Hủy / Lưu. */
export function ProfileModal(props: {
  title: string
  open: boolean
  width: number
  editing: boolean
  saving: boolean
  onClose: () => void
  onEdit: () => void
  onCancelEdit: () => void
  onSave: () => void
  children: ReactNode
}) {
  const { editing, saving } = props
  return (
    <Modal
      title={props.title}
      open={props.open}
      width={props.width}
      centered
      destroyOnClose
      // Chiều cao cố định, chỉ thân dialog cuộn; tiêu đề và nút ở chân đứng yên
      styles={{ body: { height: 'min(640px, calc(100vh - 200px))', overflowY: 'auto', marginInline: -24, paddingInline: 24 } }}
      onCancel={props.onClose}
      footer={
        editing ? (
          <>
            <Button onClick={props.onCancelEdit} disabled={saving}>Hủy</Button>
            <Button type="primary" loading={saving} onClick={props.onSave}>Lưu</Button>
          </>
        ) : (
          <Button type="primary" icon={<EditOutlined />} onClick={props.onEdit}>Sửa</Button>
        )
      }
    >
      {props.children}
    </Modal>
  )
}

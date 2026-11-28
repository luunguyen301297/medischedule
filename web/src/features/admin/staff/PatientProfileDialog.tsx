import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, Empty, Form, Input, Select, Space, Spin, Tag } from 'antd'
import { useEffect, useState } from 'react'
import type { UserResponse } from '@/features/auth'
import { errorMessage } from '@/shared/api/http'
import AvatarField, { AvatarView } from '@/shared/ui/AvatarField'
import { profilesApi, usersApi, type PatientProfile, type PatientRelation, type PatientRequest, type UpdateUserRequest } from './api'
import {
  ACCOUNT_ERRORS,
  accountRows,
  accountValues,
  applyError,
  blank,
  cell,
  dash,
  fmtDate,
  InfoTable,
  ProfileModal,
  toUpdateUser,
  type Row,
} from './profileShared'

const RELATION_LABEL: Record<PatientRelation, string> = { SELF: 'Bản thân', CHILD: 'Con', PARENT: 'Bố/Mẹ', OTHER: 'Khác' }
const SOURCE_LABEL: Record<NonNullable<PatientProfile['source']>, string> = { GUEST: 'Khách đặt qua SĐT', APP: 'Ứng dụng', RECEPTION: 'Lễ tân' }
const GENDERS = ['Nam', 'Nữ', 'Khác']

/** Hồ sơ trong form; id/patientCode trống = hồ sơ mới thêm. */
type ProfileValues = PatientRequest & { id?: number; patientCode?: string; source?: PatientProfile['source'] }

interface Values {
  account: UpdateUserRequest
  profiles: ProfileValues[]
}

const toRequest = (p: ProfileValues): PatientRequest => ({
  relation: p.relation,
  fullName: (p.fullName ?? '').trim(),
  dateOfBirth: blank(p.dateOfBirth),
  gender: blank(p.gender),
  nationalId: blank(p.nationalId),
  phone: (p.phone ?? '').trim(),
  address: blank(p.address),
  bhytNumber: blank(p.bhytNumber),
  bhytValidTo: blank(p.bhytValidTo),
  bhytRate: p.bhytRate ?? null,
  allergyNote: blank(p.allergyNote),
  avatarUrl: blank(p.avatarUrl),
})

const PROFILE_ERRORS = { NATIONAL_ID_EXISTS: 'nationalId', SELF_EXISTS: 'relation', BHYT_RATE_INVALID: 'bhytRate' }

/** Các dòng của một hồ sơ đi khám; i = vị trí trong form (chỉ dùng khi sửa). */
function profileRows(p: ProfileValues, i: number, relations: PatientRelation[]): Row[] {
  const f = (name: string) => ['profiles', i, name]
  return [
    { key: 'avatar', label: 'Ảnh', view: <AvatarView src={p.avatarUrl} />, edit: cell(f('avatarUrl'), <AvatarField />) },
    { key: 'fullName', label: 'Họ tên', view: p.fullName, edit: cell(f('fullName'), <Input />, [{ required: true, whitespace: true, message: 'Nhập họ tên' }, { max: 120 }]) },
    {
      key: 'relation',
      label: 'Quan hệ',
      view: RELATION_LABEL[p.relation],
      edit: cell(f('relation'), <Select options={relations.map((r) => ({ value: r, label: RELATION_LABEL[r] }))} />, [{ required: true, message: 'Chọn quan hệ' }]),
    },
    { key: 'dob', label: 'Ngày sinh', view: fmtDate(p.dateOfBirth), edit: cell(f('dateOfBirth'), <Input type="date" />) },
    { key: 'gender', label: 'Giới tính', view: dash(p.gender), edit: cell(f('gender'), <Select allowClear options={GENDERS.map((g) => ({ value: g, label: g }))} />) },
    {
      key: 'phone',
      label: 'Số điện thoại',
      view: dash(p.phone),
      edit: cell(f('phone'), <Input maxLength={10} />, [{ required: true, message: 'Nhập số điện thoại' }, { pattern: /^0\d{9}$/, message: '10 số, bắt đầu bằng 0' }]),
    },
    { key: 'nationalId', label: 'CCCD', view: dash(p.nationalId), edit: cell(f('nationalId'), <Input maxLength={12} />, [{ pattern: /^\d{12}$/, message: 'CCCD gồm 12 số' }]) },
    { key: 'address', label: 'Địa chỉ', view: dash(p.address), edit: cell(f('address'), <Input />, [{ max: 255 }]) },
    { key: 'bhytNumber', label: 'Số thẻ BHYT', view: dash(p.bhytNumber), edit: cell(f('bhytNumber'), <Input maxLength={15} />) },
    {
      key: 'bhytRate',
      label: 'Mức hưởng BHYT',
      view: p.bhytRate ? `${p.bhytRate}%` : '—',
      edit: cell(f('bhytRate'), <Select allowClear options={[80, 95, 100].map((r) => ({ value: r, label: `${r}%` }))} />),
    },
    { key: 'bhytValidTo', label: 'Hạn thẻ BHYT', view: fmtDate(p.bhytValidTo), edit: cell(f('bhytValidTo'), <Input type="date" />) },
    { key: 'allergy', label: 'Dị ứng', view: dash(p.allergyNote), edit: cell(f('allergyNote'), <Input.TextArea autoSize={{ minRows: 1, maxRows: 4 }} />) },
    ...(p.id ? [{ key: 'source', label: 'Nguồn tạo', view: p.source ? SOURCE_LABEL[p.source] : '—' }] : []),
  ]
}

const profileTitle = (p: ProfileValues) => (
  <span>
    {p.id ? p.patientCode : 'Hồ sơ mới'} <Tag style={{ marginLeft: 6 }}>{RELATION_LABEL[p.relation] ?? '—'}</Tag>
  </span>
)

/** Hồ sơ bệnh nhân: tài khoản + các hồ sơ đi khám (bản thân, người thân); một nút Sửa, sửa tại chỗ, thêm hồ sơ khi sửa. */
export default function PatientProfileDialog({ user, onClose }: { user: UserResponse | null; onClose: () => void }) {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const [form] = Form.useForm<Values>()
  const [current, setCurrent] = useState(user)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  useEffect(() => {
    setCurrent(user)
    setEditing(false)
  }, [user])

  const key = ['admin-patient-profiles', current?.id]
  const { data, isLoading, error } = useQuery({
    queryKey: key,
    queryFn: () => profilesApi.patients(current!.id),
    enabled: !!current,
  })
  const watched = Form.useWatch('profiles', form) as ProfileValues[] | undefined

  const startEdit = () => {
    form.setFieldsValue({ account: accountValues(current!), profiles: (data ?? []).map((p) => ({ ...p })) })
    setEditing(true)
  }

  const save = async () => {
    const v = form.getFieldsValue(true) as Values
    setSaving(true)
    const fail = (e: unknown, codes: Record<string, string>, prefix: (string | number)[]) => {
      setSaving(false)
      applyError(form, e, codes, prefix, message.error)
    }
    try {
      setCurrent(await usersApi.update(current!.id, toUpdateUser(v.account)))
      qc.invalidateQueries({ queryKey: ['admin-users'] })
    } catch (e) {
      return fail(e, ACCOUNT_ERRORS, ['account'])
    }
    // Lưu lần lượt, chỉ hồ sơ mới hoặc có thay đổi; lưu hỏng giữa chừng thì hồ sơ mới đã tạo được giữ id để lưu lại không tạo trùng
    for (const [i, p] of (v.profiles ?? []).entries()) {
      const req = toRequest(p)
      const orig = data?.find((o) => o.id === p.id)
      if (orig && JSON.stringify(req) === JSON.stringify(toRequest(orig))) continue
      try {
        const saved = p.id ? await profilesApi.updatePatient(p.id, req) : await profilesApi.createPatient({ ...req, ownerUserId: current!.id })
        form.setFieldValue(['profiles', i], { ...saved })
      } catch (e) {
        await qc.invalidateQueries({ queryKey: key })
        return fail(e, PROFILE_ERRORS, ['profiles', i])
      }
    }
    await qc.invalidateQueries({ queryKey: key })
    setSaving(false)
    setEditing(false)
    message.success('Đã lưu hồ sơ')
  }

  // Mỗi tài khoản một hồ sơ "Bản thân": ô quan hệ chỉ cho chọn SELF ở hồ sơ đang giữ nó, hoặc khi chưa hồ sơ nào giữ
  const relationsFor = (i: number) =>
    (Object.keys(RELATION_LABEL) as PatientRelation[]).filter((r) => r !== 'SELF' || !watched?.some((p, j) => j !== i && p?.relation === 'SELF'))

  return (
    <ProfileModal
      title="Hồ sơ bệnh nhân"
      open={!!user}
      width={640}
      editing={editing}
      saving={saving}
      onClose={onClose}
      onEdit={startEdit}
      onCancelEdit={() => setEditing(false)}
      onSave={() => form.submit()}
    >
      {current && (
        <Form form={form} name={`patient-profile-${current.id}`} onFinish={save} requiredMark={false}>
          <div style={{ display: 'grid', gap: 16, marginTop: 12 }}>
            <InfoTable title="Tài khoản" rows={accountRows(current)} editing={editing} />

            {isLoading ? (
              <Spin />
            ) : error ? (
              <div>{errorMessage(error)}</div>
            ) : editing ? (
              <Form.List name="profiles">
                {(fields, { add, remove }) => (
                  <>
                    {fields.map(({ key: k, name }) => {
                      const p = (watched?.[name] ?? form.getFieldValue(['profiles', name])) as ProfileValues
                      return (
                        <InfoTable
                          key={k}
                          title={profileTitle(p)}
                          extra={!p.id && <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => remove(name)}>Bỏ</Button>}
                          rows={profileRows(p, name, relationsFor(name))}
                          editing
                        />
                      )
                    })}
                    <Space>
                      <Button
                        icon={<PlusOutlined />}
                        onClick={() => {
                          const hasSelf = watched?.some((p) => p?.relation === 'SELF')
                          add({ relation: hasSelf ? 'CHILD' : 'SELF', fullName: hasSelf ? '' : current.fullName, phone: current.phone ?? '' })
                        }}
                      >
                        Thêm hồ sơ
                      </Button>
                    </Space>
                  </>
                )}
              </Form.List>
            ) : data?.length ? (
              data.map((p, i) => <InfoTable key={p.id} title={profileTitle(p)} rows={profileRows(p, i, [])} editing={false} />)
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có hồ sơ đi khám" />
            )}
          </div>
        </Form>
      )}
    </ProfileModal>
  )
}

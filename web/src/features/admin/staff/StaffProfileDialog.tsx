import { useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Form, Input, Select, Spin, Switch, Tag } from 'antd'
import { useEffect, useState } from 'react'
import type { UserResponse } from '@/features/auth'
import { errorCode, errorMessage } from '@/shared/api/http'
import AvatarField, { AvatarView } from '@/shared/ui/AvatarField'
import { specialtyApi } from '../specialty/api'
import { profilesApi, usersApi, type StaffProfile, type StaffProfileRequest, type UpdateUserRequest } from './api'
import {
  ACCOUNT_ERRORS,
  accountRows,
  accountValues,
  applyError,
  blank,
  cell,
  dash,
  InfoTable,
  ProfileModal,
  toUpdateUser,
  type Row,
} from './profileShared'

interface Values {
  account: UpdateUserRequest
  profile: StaffProfileRequest
}

const profileValues = (p: StaffProfile | undefined): StaffProfileRequest => ({
  specialtyId: p?.specialtyId ?? null,
  degree: p?.degree ?? '',
  licenseNo: p?.licenseNo ?? '',
  acceptsDesignation: p?.acceptsDesignation ?? false,
  active: p?.active ?? true,
  avatarUrl: p?.avatarUrl ?? null,
})

const toRequest = (v: StaffProfileRequest): StaffProfileRequest => ({
  specialtyId: v.specialtyId ?? null,
  degree: blank(v.degree),
  licenseNo: blank(v.licenseNo),
  acceptsDesignation: !!v.acceptsDesignation,
  active: !!v.active,
  avatarUrl: blank(v.avatarUrl),
})

/** Hồ sơ nhân viên: tài khoản + hồ sơ chuyên môn (bảng staff); một nút Sửa, sửa tại chỗ. */
export default function StaffProfileDialog({ user, onClose }: { user: UserResponse | null; onClose: () => void }) {
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

  const isDoctor = current?.role === 'DOCTOR'
  const key = ['admin-staff-profile', current?.id]
  const { data, isLoading, error } = useQuery({
    queryKey: key,
    queryFn: () => profilesApi.staff(current!.id),
    enabled: !!current,
    retry: false,
  })
  const noProfile = errorCode(error) === 'STAFF_PROFILE_NOT_FOUND'
  const { data: specialties } = useQuery({ queryKey: ['admin-specialties'], queryFn: specialtyApi.list, enabled: editing && isDoctor })

  const startEdit = () => {
    form.setFieldsValue({ account: accountValues(current!), profile: profileValues(data) })
    setEditing(true)
  }

  const save = async () => {
    const v = form.getFieldsValue(true) as Values
    setSaving(true)
    try {
      setCurrent(await usersApi.update(current!.id, toUpdateUser(v.account)))
      qc.invalidateQueries({ queryKey: ['admin-users'] })
    } catch (e) {
      setSaving(false)
      return applyError(form, e, ACCOUNT_ERRORS, ['account'], message.error)
    }
    const req = toRequest(v.profile)
    // Chưa có hồ sơ và không đổi gì thì không tạo hồ sơ rỗng
    if (data || JSON.stringify(req) !== JSON.stringify(toRequest(profileValues(undefined)))) {
      try {
        qc.setQueryData(key, await profilesApi.saveStaff(current!.id, req))
      } catch (e) {
        setSaving(false)
        return applyError(form, e, { LICENSE_EXISTS: 'licenseNo', SPECIALTY_NOT_FOUND: 'specialtyId' }, ['profile'], message.error)
      }
    }
    setSaving(false)
    setEditing(false)
    message.success('Đã lưu hồ sơ')
  }

  const profileRows: Row[] = [
    { key: 'avatar', label: 'Ảnh', view: <AvatarView src={data?.avatarUrl} />, edit: cell(['profile', 'avatarUrl'], <AvatarField />) },
    ...(isDoctor
      ? [
          {
            key: 'specialty',
            label: 'Chuyên khoa',
            view: dash(data?.specialtyName),
            edit: cell(
              ['profile', 'specialtyId'],
              <Select
                allowClear
                placeholder="Chưa có khoa"
                loading={!specialties}
                options={specialties?.filter((s) => s.active || s.id === data?.specialtyId).map((s) => ({ value: s.id, label: s.name }))}
              />,
            ),
          },
          { key: 'degree', label: 'Học hàm / học vị', view: dash(data?.degree), edit: cell(['profile', 'degree'], <Input placeholder="ThS, BSCKI..." />, [{ max: 50 }]) },
          { key: 'license', label: 'Chứng chỉ hành nghề', view: dash(data?.licenseNo), edit: cell(['profile', 'licenseNo'], <Input />, [{ max: 30 }]) },
          {
            key: 'designation',
            label: 'Nhận chỉ định đích danh',
            view: data?.acceptsDesignation ? 'Có' : 'Không',
            edit: cell(['profile', 'acceptsDesignation'], <Switch />, undefined, { valuePropName: 'checked' }),
          },
        ]
      : []),
    {
      key: 'active',
      label: 'Tình trạng',
      view: data?.active === false ? <Tag>Đã nghỉ việc</Tag> : <Tag color="green">Đang làm việc</Tag>,
      edit: cell(['profile', 'active'], <Switch checkedChildren="Đang làm" unCheckedChildren="Nghỉ việc" />, undefined, { valuePropName: 'checked' }),
    },
  ]

  return (
    <ProfileModal
      title="Hồ sơ nhân viên"
      open={!!user}
      width={600}
      editing={editing}
      saving={saving}
      onClose={onClose}
      onEdit={startEdit}
      onCancelEdit={() => setEditing(false)}
      onSave={() => form.submit()}
    >
      {current && (
        <Form form={form} name={`staff-profile-${current.id}`} onFinish={save} requiredMark={false}>
          <div style={{ display: 'grid', gap: 16, marginTop: 12 }}>
            <InfoTable title="Tài khoản" rows={accountRows(current)} editing={editing} />
            {isLoading ? (
              <Spin />
            ) : error && !noProfile ? (
              <div>{errorMessage(error)}</div>
            ) : !data && !editing ? (
              <InfoTable title="Hồ sơ" rows={[{ key: 'none', label: 'Hồ sơ', view: 'Chưa có hồ sơ nhân viên' }]} editing={false} />
            ) : (
              <InfoTable title="Hồ sơ" rows={profileRows} editing={editing} />
            )}
          </div>
        </Form>
      )}
    </ProfileModal>
  )
}

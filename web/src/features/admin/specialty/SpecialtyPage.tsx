import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, Empty, Form, Input, List, Modal, Popconfirm, Select, Switch, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useState } from 'react'
import { errorCode, errorMessage, fieldErrors } from '@/shared/api/http'
import { specialtyApi, type Room, type RoomRequest, type Specialty } from './api'

/** Một form cho cả chuyên khoa và phòng; trường nào không dùng thì bỏ qua */
type CatalogForm = RoomRequest & { doctorUserIds?: number[] }

/** Dòng phụ: tên phòng khi đúng 1 phòng, tên bác sĩ khi đúng 1 bác sĩ, còn lại là số lượng */
const summary = (s: Specialty) =>
  [
    s.roomCount === 1 && s.singleRoomName ? `Phòng ${s.singleRoomName}` : `${s.roomCount} phòng`,
    s.singleDoctorName ?? (s.doctorUserIds.length > 1 ? `${s.doctorUserIds.length} bác sĩ` : null),
  ]
    .filter(Boolean)
    .join(' · ')

/** Hộp thoại đang mở: thêm (không có item) hoặc sửa một chuyên khoa / phòng. */
type Editing = { kind: 'specialty'; item?: Specialty } | { kind: 'room'; item?: Room }

/** Danh mục chuyên khoa (cột trái) và phòng khám của chuyên khoa đang chọn (cột phải). */
export default function SpecialtyPage() {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const [selectedId, setSelectedId] = useState<number>()
  const [editing, setEditing] = useState<Editing | null>(null)
  const [form] = Form.useForm<CatalogForm>()

  const specialties = useQuery({ queryKey: ['admin', 'specialties'], queryFn: specialtyApi.list })
  const list = specialties.data ?? []
  const selected = list.find((s) => s.id === selectedId) ?? list[0]
  const rooms = useQuery({
    queryKey: ['admin', 'rooms', selected?.id],
    queryFn: () => specialtyApi.rooms(selected!.id),
    enabled: !!selected,
  })
  const doctors = useQuery({
    queryKey: ['admin', 'doctors'],
    queryFn: specialtyApi.doctors,
    enabled: editing?.kind === 'specialty',
  })

  // Số phòng, danh sách phòng và ô chọn ở trang chủ phụ thuộc nhau nên làm mới cả ba
  // Danh sách chuyên khoa làm mới trước: chuyên khoa vừa xóa rời khỏi `selected` thì không còn ai gọi lại phòng của nó (404)
  const refresh = async () => {
    await qc.invalidateQueries({ queryKey: ['admin', 'specialties'] })
    const alive = new Set(qc.getQueryData<Specialty[]>(['admin', 'specialties'])?.map((s) => s.id))
    qc.invalidateQueries({ queryKey: ['admin', 'rooms'], predicate: (q) => alive.has(q.queryKey[2] as number) })
    qc.invalidateQueries({ queryKey: ['public', 'specialties'] })
    qc.invalidateQueries({ queryKey: ['admin', 'doctors'] })
  }

  // Bật/tắt và xóa: không cập nhật lạc quan, lỗi 409 thì giữ nguyên trạng thái cũ
  const action = useMutation({
    mutationFn: ({ run }: { run: () => Promise<unknown>; done: string }) => run(),
    // Dùng { }: message.* trả thenable, trả về thì React Query chờ toast đóng mới chạy onSettled
    onSuccess: (_, v) => { message.success(v.done) },
    onError: (e) => { message.error(errorMessage(e)) },
    onSettled: refresh,
  })

  const save = useMutation({
    mutationFn: async ({ target, values }: { target: Editing; values: CatalogForm }) => {
      if (target.kind === 'room') {
        const body = { name: values.name, specialtyId: values.specialtyId }
        await (target.item ? specialtyApi.updateRoom(target.item.id, body) : specialtyApi.createRoom(body))
        return
      }
      const body = { name: values.name, doctorUserIds: values.doctorUserIds ?? [] }
      if (target.item) {
        await specialtyApi.update(target.item.id, body)
      } else {
        setSelectedId((await specialtyApi.create(body)).id)
      }
    },
    onSuccess: () => {
      message.success('Đã lưu')
      setEditing(null)
    },
    onError: (e) => {
      const fields = fieldErrors(e)
      const code = errorCode(e)
      if (fields) form.setFields(Object.entries(fields).map(([name, err]) => ({ name: name as keyof CatalogForm, errors: [err] })))
      else if (code === 'SPECIALTY_EXISTS' || code === 'ROOM_EXISTS') form.setFields([{ name: 'name', errors: [errorMessage(e)] }])
      else if (code === 'DOCTOR_INVALID') form.setFields([{ name: 'doctorUserIds', errors: [errorMessage(e)] }])
      else message.error(errorMessage(e))
    },
    onSettled: refresh,
  })

  const open = (target: Editing) => {
    form.resetFields()
    form.setFieldsValue({
      name: target.item?.name,
      specialtyId: target.kind === 'room' ? (target.item?.specialtyId ?? selected?.id) : undefined,
      doctorUserIds: target.kind === 'specialty' ? (target.item?.doctorUserIds ?? []) : undefined,
    })
    setEditing(target)
  }

  const isRoom = editing?.kind === 'room'
  const maxName = isRoom ? 50 : 100
  const noun = isRoom ? 'phòng khám' : 'chuyên khoa'

  const columns: ColumnsType<Room> = [
    {
      title: 'Tên phòng',
      dataIndex: 'name',
      render: (v: string, r) => (
        <>
          {v} {!r.active && <Tag>Đang ngừng</Tag>}
        </>
      ),
    },
    {
      title: 'Hoạt động',
      dataIndex: 'active',
      width: 120,
      align: 'center',
      render: (v: boolean, r) => (
        <Switch
          checked={v}
          // Chuyên khoa đang ngừng thì không mở lại được phòng
          disabled={action.isPending || (!v && !selected?.active)}
          onChange={(value) =>
            action.mutate({ run: () => specialtyApi.setRoomActive(r.id, value), done: value ? 'Đã mở lại phòng' : 'Đã ngừng phòng' })
          }
          aria-label={`${v ? 'Ngừng' : 'Mở lại'} phòng ${r.name}`}
        />
      ),
    },
    {
      title: 'Thao tác',
      width: 110,
      align: 'center',
      render: (_, r) => (
        <>
          <Button type="text" icon={<EditOutlined />} aria-label={`Sửa phòng ${r.name}`} onClick={() => open({ kind: 'room', item: r })} />
          <Popconfirm
            title={`Xóa phòng ${r.name}?`}
            okText="Xóa"
            cancelText="Hủy"
            onConfirm={() => action.mutate({ run: () => specialtyApi.removeRoom(r.id), done: 'Đã xóa phòng' })}
          >
            <Button type="text" danger icon={<DeleteOutlined />} aria-label={`Xóa phòng ${r.name}`} />
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="admin-page">
      <div className="admin-head">
        <h1>Chuyên khoa</h1>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => open({ kind: 'specialty' })}>
          Thêm chuyên khoa
        </Button>
      </div>

      <div className="admin-scroll">
      {specialties.isError && <Alert type="error" showIcon message={errorMessage(specialties.error)} />}

      <div className="specialty-grid">
        <List
          bordered
          loading={specialties.isLoading}
          dataSource={list}
          locale={{ emptyText: <Empty description="Chưa có chuyên khoa" /> }}
          renderItem={(s) => (
            <List.Item className={`specialty-item ${s.id === selected?.id ? 'is-active' : ''}`}>
              <button type="button" className="specialty-pick" aria-current={s.id === selected?.id} onClick={() => setSelectedId(s.id)}>
                <b>{s.name}</b>
                <span className="specialty-meta">
                  <span className="specialty-sub" title={summary(s)}>{summary(s)}</span>
                  {!s.active && <Tag>Đang ngừng</Tag>}
                </span>
              </button>
              <Switch
                size="small"
                checked={s.active}
                disabled={action.isPending}
                onChange={(value) =>
                  action.mutate({
                    run: () => specialtyApi.setActive(s.id, value),
                    done: value ? 'Đã mở lại chuyên khoa' : 'Đã ngừng chuyên khoa',
                  })
                }
                aria-label={`${s.active ? 'Ngừng' : 'Mở lại'} chuyên khoa ${s.name}`}
              />
              <Button type="text" icon={<EditOutlined />} aria-label={`Sửa chuyên khoa ${s.name}`} onClick={() => open({ kind: 'specialty', item: s })} />
              <Popconfirm
                title={`Xóa chuyên khoa ${s.name}?`}
                okText="Xóa"
                cancelText="Hủy"
                onConfirm={() => action.mutate({ run: () => specialtyApi.remove(s.id), done: 'Đã xóa chuyên khoa' })}
              >
                <Button type="text" danger icon={<DeleteOutlined />} aria-label={`Xóa chuyên khoa ${s.name}`} />
              </Popconfirm>
            </List.Item>
          )}
        />

        {selected && (
          <section>
            <div className="admin-head">
              <h2>Phòng khám · {selected.name}</h2>
              <Button icon={<PlusOutlined />} disabled={!selected.active} onClick={() => open({ kind: 'room' })}>
                Thêm phòng
              </Button>
            </div>
            {!selected.active && <Alert type="info" showIcon message="Chuyên khoa đang ngừng. Hãy mở lại chuyên khoa để thêm hoặc mở lại phòng." />}
            {rooms.isError && <Alert type="error" showIcon message={errorMessage(rooms.error)} />}
            <Table
              rowKey="id"
              columns={columns}
              dataSource={rooms.data}
              loading={rooms.isLoading}
              pagination={false}
              locale={{ emptyText: <Empty description="Chưa có phòng khám" /> }}
              // ponytail: chỉ cuộn ngang khi có dòng; bảng rỗng mà có scroll.x thì AntD vẫn vẽ thanh cuộn
              scroll={rooms.data?.length ? { x: 'max-content' } : undefined}
            />
          </section>
        )}
      </div>
      </div>

      <Modal
        title={`${editing?.item ? 'Sửa' : 'Thêm'} ${noun}`}
        open={!!editing}
        okText="Lưu"
        cancelText="Hủy"
        confirmLoading={save.isPending}
        onOk={() => form.submit()}
        onCancel={() => setEditing(null)}
        forceRender
      >
        <Form
          form={form}
          name="catalog"
          layout="vertical"
          requiredMark={false}
          onFinish={(values) => editing && !save.isPending && save.mutate({ target: editing, values })}
        >
          <Form.Item
            name="name"
            label={`Tên ${noun}`}
            rules={[
              { required: true, whitespace: true, message: `Nhập tên ${noun}` },
              { max: maxName, message: `Tối đa ${maxName} ký tự` },
            ]}
          >
            <Input autoComplete="off" />
          </Form.Item>
          {/* Thêm phòng thì thuộc chuyên khoa đang chọn; chỉ khi sửa mới chuyển chuyên khoa */}
          <Form.Item name="specialtyId" label="Chuyên khoa" hidden={!isRoom || !editing?.item}>
            <Select
              options={list
                .filter((s) => s.active || (editing?.kind === 'room' && s.id === editing.item?.specialtyId))
                .map((s) => ({ value: s.id, label: s.name }))}
            />
          </Form.Item>
          {!isRoom && (
            <Form.Item name="doctorUserIds" label="Bác sĩ">
              <Select
                mode="multiple"
                allowClear
                optionFilterProp="label"
                placeholder="Chọn bác sĩ"
                loading={doctors.isLoading}
                notFoundContent={doctors.isLoading ? undefined : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có tài khoản bác sĩ" />}
                // Bác sĩ đang ở khoa khác ghi kèm tên khoa; lưu thì chuyển sang khoa này
                options={doctors.data?.map((d) => ({
                  value: d.userId,
                  label: d.specialtyId && d.specialtyId !== editing?.item?.id ? `${d.fullName} · ${d.specialtyName}` : d.fullName,
                }))}
              />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  )
}

import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, App, Button, ColorPicker, Empty, Form, Input, Modal, Popconfirm, Switch, Table, Tag, TimePicker } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { Color } from 'antd/es/color-picker'
import dayjs, { type Dayjs } from 'dayjs'
import { useState } from 'react'
import { errorCode, errorMessage, fieldErrors } from '@/shared/api/http'
import { palette } from '@/shared/theme/theme'
import { shiftTime, workShiftApi, type WorkShift } from './api'

type ShiftForm = { name: string; time: [Dayjs, Dayjs]; color: string }

/** Cài đặt › Ca làm việc: danh sách ca chuẩn dùng khi phân lịch nhân viên. */
export default function WorkShiftPage() {
  const { message } = App.useApp()
  const qc = useQueryClient()
  // undefined = đóng, null = thêm mới
  const [editing, setEditing] = useState<WorkShift | null>()
  const [form] = Form.useForm<ShiftForm>()

  const shifts = useQuery({ queryKey: ['admin', 'work-shifts'], queryFn: workShiftApi.list })
  const refresh = () => qc.invalidateQueries({ queryKey: ['admin', 'work-shifts'] })

  const action = useMutation({
    mutationFn: ({ run }: { run: () => Promise<unknown>; done: string }) => run(),
    onSuccess: (_, v) => { message.success(v.done) },
    onError: (e) => { message.error(errorMessage(e)) },
    onSettled: refresh,
  })

  const save = useMutation({
    mutationFn: ({ target, values }: { target: WorkShift | null; values: ShiftForm }) => {
      const body = {
        name: values.name,
        startTime: values.time[0].format('HH:mm'),
        endTime: values.time[1].format('HH:mm'),
        color: values.color,
      }
      return target ? workShiftApi.update(target.id, body) : workShiftApi.create(body)
    },
    onSuccess: () => {
      message.success('Đã lưu')
      setEditing(undefined)
    },
    onError: (e) => {
      const fields = fieldErrors(e)
      const code = errorCode(e)
      if (fields) form.setFields(Object.entries(fields).map(([name, err]) => ({ name: name === 'name' || name === 'color' ? name : 'time', errors: [err] })))
      else if (code === 'WORK_SHIFT_EXISTS') form.setFields([{ name: 'name', errors: [errorMessage(e)] }])
      else if (code === 'INVALID_TIME_RANGE') form.setFields([{ name: 'time', errors: [errorMessage(e)] }])
      else message.error(errorMessage(e))
    },
    onSettled: refresh,
  })

  const open = (item: WorkShift | null) => {
    form.resetFields()
    form.setFieldsValue(
      item
        ? { name: item.name, time: [dayjs(item.startTime, 'HH:mm:ss'), dayjs(item.endTime, 'HH:mm:ss')], color: item.color }
        : { color: palette.primary },
    )
    setEditing(item)
  }

  const columns: ColumnsType<WorkShift> = [
    {
      title: 'Ca',
      dataIndex: 'name',
      render: (v: string, s) => (
        <span className="ws-name">
          {/* Màu ca là dữ liệu do admin chọn, không phải màu giao diện */}
          <span className="ws-dot" style={{ background: s.color }} aria-hidden="true" />
          {v} {!s.active && <Tag>Đang tắt</Tag>}
        </span>
      ),
    },
    { title: 'Giờ làm', width: 160, render: (_, s) => shiftTime(s) },
    {
      title: 'Hoạt động',
      dataIndex: 'active',
      width: 120,
      align: 'center',
      render: (v: boolean, s) => (
        <Switch
          checked={v}
          disabled={action.isPending}
          onChange={(value) => action.mutate({ run: () => workShiftApi.setActive(s.id, value), done: value ? 'Đã bật ca' : 'Đã tắt ca' })}
          aria-label={`${v ? 'Tắt' : 'Bật'} ca ${s.name}`}
        />
      ),
    },
    {
      title: 'Thao tác',
      width: 110,
      align: 'center',
      render: (_, s) => (
        <>
          <Button type="text" icon={<EditOutlined />} aria-label={`Sửa ca ${s.name}`} onClick={() => open(s)} />
          <Popconfirm
            title={`Xóa ca ${s.name}?`}
            okText="Xóa"
            cancelText="Hủy"
            onConfirm={() => action.mutate({ run: () => workShiftApi.remove(s.id), done: 'Đã xóa ca' })}
          >
            <Button type="text" danger icon={<DeleteOutlined />} aria-label={`Xóa ca ${s.name}`} />
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="admin-page">
      <div className="admin-head">
        <div>
          <h1>Ca làm việc</h1>
          <p>Các ca chuẩn dùng khi phân lịch làm việc cho nhân viên.</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => open(null)}>
          Thêm ca
        </Button>
      </div>

      <div className="admin-scroll">
        {shifts.isError && <Alert type="error" showIcon message={errorMessage(shifts.error)} />}
        <Table
          rowKey="id"
          columns={columns}
          dataSource={shifts.data}
          loading={shifts.isLoading}
          pagination={false}
          locale={{ emptyText: <Empty description="Chưa có ca làm việc" /> }}
          scroll={shifts.data?.length ? { x: 'max-content' } : undefined}
        />
      </div>

      <Modal
        title={`${editing ? 'Sửa' : 'Thêm'} ca làm việc`}
        open={editing !== undefined}
        okText="Lưu"
        cancelText="Hủy"
        confirmLoading={save.isPending}
        onOk={() => form.submit()}
        onCancel={() => setEditing(undefined)}
        forceRender
      >
        <Form
          form={form}
          name="work-shift"
          layout="vertical"
          requiredMark={false}
          onFinish={(values) => editing !== undefined && !save.isPending && save.mutate({ target: editing, values })}
        >
          <Form.Item
            name="name"
            label="Tên ca"
            rules={[
              { required: true, whitespace: true, message: 'Nhập tên ca' },
              { max: 50, message: 'Tối đa 50 ký tự' },
            ]}
          >
            <Input autoComplete="off" placeholder="VD: Sáng" />
          </Form.Item>
          <Form.Item
            name="time"
            label="Giờ làm"
            rules={[
              { required: true, message: 'Chọn giờ bắt đầu và kết thúc' },
              {
                validator: (_, v?: [Dayjs, Dayjs]) =>
                  !v || v[1].isAfter(v[0]) ? Promise.resolve() : Promise.reject(new Error('Giờ kết thúc phải sau giờ bắt đầu')),
              },
            ]}
          >
            <TimePicker.RangePicker format="HH:mm" minuteStep={5} order={false} placeholder={['Bắt đầu', 'Kết thúc']} />
          </Form.Item>
          <Form.Item name="color" label="Màu hiển thị" getValueFromEvent={(c: Color) => c.toHexString()}>
            <ColorPicker disabledAlpha showText />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

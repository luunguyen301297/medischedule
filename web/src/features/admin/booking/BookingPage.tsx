import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App, Button, DatePicker, Empty, Form, Input, Modal, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs, { type Dayjs } from 'dayjs'
import { useState } from 'react'
import { errorMessage } from '@/shared/api/http'
import BookingCreateDialog from './BookingCreateDialog'
import {
  CHANNEL_LABEL,
  receptionApi,
  STATUS_LABEL,
  type AppointmentRow,
  type AppointmentStatus,
} from './api'

const PAGE_SIZE = 20

const STATUS_TAG: Record<AppointmentStatus, string> = {
  BOOKED: 'processing',
  CHECKED_IN: 'warning',
  IN_PROGRESS: 'warning',
  DONE: 'success',
  CANCELLED: 'default',
  NO_SHOW: 'error',
}

const hhmm = (iso: string) => dayjs(iso).format('HH:mm')
/** Giờ hẹn khi đã xếp; chưa xếp thì hiện buổi */
const timeText = (a: AppointmentRow) =>
  a.startAt && a.endAt
    ? `${hhmm(a.startAt)}–${hhmm(a.endAt)}`
    : `${a.workShiftName ?? ''} ${a.workShiftStart ?? ''}–${a.workShiftEnd ?? ''}`

/** Menu Lịch hẹn: danh sách lịch hẹn của khách, đặt hộ và hủy lịch (ADMIN, RECEPTIONIST). */
export default function BookingPage() {
  const { message } = App.useApp()
  const qc = useQueryClient()
  const [range, setRange] = useState<[Dayjs, Dayjs]>([dayjs(), dayjs()])
  const [doctorUserId, setDoctorUserId] = useState<number>()
  const [status, setStatus] = useState<AppointmentStatus>()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)
  const [cancelling, setCancelling] = useState<AppointmentRow | null>(null)
  const [cancelForm] = Form.useForm<{ reason: string }>()

  const filter = {
    from: range[0].format('YYYY-MM-DD'),
    to: range[1].format('YYYY-MM-DD'),
    doctorUserId,
    status,
    q,
    page,
    size: PAGE_SIZE,
  }
  const list = useQuery({
    queryKey: ['reception', 'appointments', filter],
    queryFn: () => receptionApi.list(filter),
    placeholderData: keepPreviousData,
  })
  const doctors = useQuery({ queryKey: ['reception', 'doctors'], queryFn: receptionApi.doctors, staleTime: 60_000 })

  const cancel = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => receptionApi.cancel(id, reason),
    onSuccess: (a) => {
      message.success(`Đã hủy lịch ${a.bookingCode}`)
      setCancelling(null)
      qc.invalidateQueries({ queryKey: ['reception'] })
    },
    onError: (e) => message.error(errorMessage(e)),
  })

  // Đổi bộ lọc thì về trang đầu
  const refilter = <T,>(set: (v: T) => void) => (v: T) => {
    set(v)
    setPage(0)
  }

  const columns: ColumnsType<AppointmentRow> = [
    {
      title: 'Giờ hẹn',
      key: 'time',
      width: 120,
      render: (_, a) => (
        <div>
          <b>{timeText(a)}</b>
          {range[0].format('YYYY-MM-DD') !== range[1].format('YYYY-MM-DD') && (
            <div className="booking-sub">{dayjs(a.visitDate).format('DD/MM/YYYY')}</div>
          )}
        </div>
      ),
    },
    { title: 'Mã lịch', dataIndex: 'bookingCode', width: 110, render: (v: string) => <span className="booking-code">{v}</span> },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      render: (_, a) => (
        <div>
          <b>{a.patientName}</b>
          <div className="booking-sub">{a.phone} · {a.patientCode}</div>
        </div>
      ),
    },
    {
      title: 'Bác sĩ',
      key: 'doctor',
      render: (_, a) => (
        <div>
          {a.doctorName ?? <span className="booking-sub">Chờ xếp bác sĩ</span>}
          <div className="booking-sub">
            {[a.specialtyName, a.roomName, a.designated ? 'Chỉ định' : null].filter(Boolean).join(' · ')}
          </div>
        </div>
      ),
    },
    {
      title: 'Kênh đặt',
      key: 'channel',
      render: (_, a) => (
        <div>
          {CHANNEL_LABEL[a.channel]}
          {a.createdByName && <div className="booking-sub">{a.createdByName}</div>}
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 150,
      render: (_, a) => (
        <div>
          <Tag color={STATUS_TAG[a.status]}>{STATUS_LABEL[a.status]}</Tag>
          {a.cancelReason && <div className="booking-sub">{a.cancelReason}</div>}
        </div>
      ),
    },
    { title: 'Đặt lúc', dataIndex: 'createdAt', width: 150, render: (v: string) => dayjs(v).format('HH:mm DD/MM/YYYY') },
    {
      title: '',
      key: 'actions',
      width: 80,
      align: 'center',
      render: (_, a) =>
        a.status === 'BOOKED' && (
          <Button
            danger
            type="text"
            onClick={() => {
              cancelForm.resetFields()
              setCancelling(a)
            }}
            aria-label={`Hủy lịch ${a.bookingCode}`}
          >
            Hủy
          </Button>
        ),
    },
  ]

  const data = list.data
  return (
    <div className="admin-page">
      <div className="admin-head">
        <h1>Lịch hẹn</h1>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreating(true)}>
          Đặt lịch hộ
        </Button>
      </div>

      <div className="admin-toolbar">
        <DatePicker.RangePicker
          id="booking-range"
          allowClear={false}
          format="DD/MM/YYYY"
          value={range}
          onChange={(v) => v?.[0] && v[1] && refilter(setRange)([v[0], v[1]])}
        />
        <Select
          id="booking-doctor"
          allowClear
          showSearch
          optionFilterProp="label"
          placeholder="Tất cả bác sĩ"
          style={{ width: 220 }}
          value={doctorUserId}
          onChange={refilter(setDoctorUserId)}
          options={doctors.data?.map((d) => ({ value: d.userId, label: d.fullName }))}
        />
        <Select
          id="booking-status"
          allowClear
          placeholder="Tất cả trạng thái"
          style={{ width: 180 }}
          value={status}
          onChange={refilter(setStatus)}
          options={(Object.keys(STATUS_LABEL) as AppointmentStatus[]).map((k) => ({ value: k, label: STATUS_LABEL[k] }))}
        />
        <Input.Search
          id="booking-search"
          allowClear
          placeholder="Tìm theo tên, SĐT, mã lịch"
          style={{ width: 280, maxWidth: '100%' }}
          onSearch={(v) => refilter(setQ)(v.trim())}
        />
        <Button icon={<ReloadOutlined />} onClick={() => list.refetch()}>Tải lại</Button>
      </div>

      <div className="admin-scroll">
        <Table
          rowKey="id"
          columns={columns}
          dataSource={data?.content}
          loading={list.isFetching}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText: (
              <Empty description={list.isError ? errorMessage(list.error) : 'Không có lịch hẹn'}>
                {!list.isError && (
                  <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreating(true)}>
                    Đặt lịch hộ
                  </Button>
                )}
              </Empty>
            ),
          }}
          pagination={{
            current: page + 1,
            pageSize: PAGE_SIZE,
            total: data?.page.totalElements ?? 0,
            showSizeChanger: false,
            showTotal: (t) => `${t} lịch hẹn`,
            onChange: (p) => setPage(p - 1),
          }}
        />
      </div>

      <BookingCreateDialog open={creating} onClose={() => setCreating(false)} />

      <Modal
        title={cancelling ? `Hủy lịch ${cancelling.bookingCode}` : ''}
        open={!!cancelling}
        okText="Hủy lịch"
        okButtonProps={{ danger: true, loading: cancel.isPending }}
        cancelText="Đóng"
        onCancel={() => setCancelling(null)}
        onOk={() => cancelForm.submit()}
        destroyOnClose
      >
        {cancelling && (
          <p>
            {cancelling.patientName} · {timeText(cancelling)} {dayjs(cancelling.visitDate).format('DD/MM/YYYY')}
            {cancelling.doctorName && ` · ${cancelling.doctorName}`}
          </p>
        )}
        <Form
          form={cancelForm}
          layout="vertical"
          onFinish={(v) => cancelling && cancel.mutate({ id: cancelling.id, reason: v.reason.trim() })}
        >
          <Form.Item
            name="reason"
            label="Lý do hủy"
            rules={[
              { required: true, whitespace: true, message: 'Nhập lý do hủy' },
              { max: 255, message: 'Lý do tối đa 255 ký tự' },
            ]}
          >
            <Input.TextArea id="booking-cancel-reason" rows={3} maxLength={255} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

import {
  CalendarOutlined, CheckCircleFilled, LockOutlined, MedicineBoxOutlined, PhoneOutlined, RightOutlined,
  SafetyCertificateOutlined, TeamOutlined, UserOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { App, Button, Descriptions, Form, Input, Modal } from 'antd'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { type ReactNode, useEffect, useState } from 'react'
import { bookingApi } from '../api'
import { DateStrip, hhmm, SessionTiles } from './BookingPickers'

interface BookingForm {
  specialtyId?: number
  doctorUserId?: number
  date: string     // YYYY-MM-DD
  shiftId: number
  fullName?: string
  phone?: string
  symptoms?: string
}

/** Người đặt đã đăng nhập: đặt cho mình thì lấy luôn họ tên / số điện thoại của tài khoản. */
export interface Booker { fullName: string; phone: string | null }

interface PickOption { value: number; label: string; hint?: string }

/** Nút chọn (chuyên khoa, bác sĩ); bấm mở hộp thoại danh sách, chọn xong tự đóng. Mục "Không chọn" để bỏ chọn. */
function OptionPicker({ id, icon, title, placeholder, emptyText, options, blocked, value, onChange }: {
  id?: string; icon: ReactNode; title: string; placeholder: string; emptyText: string
  options: PickOption[]; blocked?: string; value?: number; onChange?: (v?: number) => void
}) {
  const [open, setOpen] = useState(false)
  const selected = options.find((o) => o.value === value)
  const pick = (v?: number) => { onChange?.(v); setOpen(false) }
  return (
    <>
      <button
        id={id}
        type="button"
        className={`spec-trigger ${selected ? 'has-value' : ''}`}
        aria-haspopup="dialog"
        disabled={!!blocked}
        onClick={() => setOpen(true)}
      >
        {icon}
        <span className="spec-trigger-text">{blocked ?? selected?.label ?? placeholder}</span>
        <RightOutlined aria-hidden="true" className="spec-trigger-arrow" />
      </button>
      <Modal title={title} open={open} onCancel={() => setOpen(false)} footer={null} width={600} centered>
        {options.length === 0 ? (
          <p className="spec-empty">{emptyText}</p>
        ) : (
          <div className="spec-grid" role="radiogroup" aria-label={title}>
            <button
              type="button"
              role="radio"
              aria-checked={value == null}
              className={`spec-option ${value == null ? 'is-active' : ''}`}
              onClick={() => pick(undefined)}
            >
              <span>Không chọn</span>
              {value == null && <CheckCircleFilled aria-hidden="true" />}
            </button>
            {options.map((o) => (
              <button
                key={o.value}
                type="button"
                role="radio"
                aria-checked={value === o.value}
                className={`spec-option ${value === o.value ? 'is-active' : ''}`}
                onClick={() => pick(o.value)}
              >
                <span>{o.label}{o.hint && <small className="spec-option-hint">{o.hint}</small>}</span>
                {value === o.value && <CheckCircleFilled aria-hidden="true" />}
              </button>
            ))}
          </div>
        )}
      </Modal>
    </>
  )
}

/**
 * Thẻ đặt lịch nhanh ở đầu trang. Khách nhập họ tên + số điện thoại; bệnh nhân đã đăng nhập (`booker`) đặt cho mình
 * không cần nhập, đặt cho người khác thì mở hộp thoại chứa form khách.
 * Buổi khám lấy từ ca làm việc đang bật; bác sĩ lọc theo phân ca của ngày + buổi (và khoa nếu đã chọn).
 * Gửi lịch vẫn là mẫu: backend đặt lịch (module booking) chưa có.
 */
export default function BookingWidget({ specialties, loadFailed, note, booker, title = 'Đặt lịch khám', formName = 'booking', confirmFirst = !!booker }: {
  specialties?: { id: number; name: string }[]; loadFailed: boolean; note: string
  booker?: Booker; title?: string; formName?: string
  /** Hiện hộp xác nhận thông tin trước khi gửi (người đã đăng nhập), tránh bấm nhầm / gửi lặp */
  confirmFirst?: boolean
}) {
  const { modal } = App.useApp()
  // Chuyên khoa không bắt buộc: tải lỗi chỉ khóa ô chuyên khoa
  const specBlocked = loadFailed ? 'Không tải được chuyên khoa' : specialties?.length === 0 ? 'Chưa có chuyên khoa' : undefined
  const [form] = Form.useForm<BookingForm>()
  const [sending, setSending] = useState(false)
  const [done, setDone] = useState<{ summary: string; phone?: string | null } | null>(null)
  const [forOther, setForOther] = useState(false)

  const shiftsQ = useQuery({ queryKey: ['public', 'booking-shifts'], queryFn: bookingApi.shifts, staleTime: 60_000 })
  const shifts = shiftsQ.data ?? []
  const shiftBlocked = shiftsQ.isError ? 'Không tải được buổi khám, thử lại sau'
    : shiftsQ.data?.length === 0 ? 'Phòng khám chưa mở buổi khám nào' : undefined

  const date = Form.useWatch('date', form)
  const shiftId = Form.useWatch('shiftId', form)
  const specialtyId = Form.useWatch('specialtyId', form)
  const doctorUserId = Form.useWatch('doctorUserId', form)

  const doctorsQ = useQuery({
    queryKey: ['public', 'booking-doctors', date, shiftId, specialtyId],
    queryFn: () => bookingApi.doctors(date, shiftId, specialtyId),
    enabled: !!date && !!shiftId,
  })
  const doctors = doctorsQ.data ?? []

  // Ca mặc định là ca đầu tiên khi danh sách ca tải xong
  useEffect(() => {
    if (shifts.length && !shifts.some((s) => s.id === form.getFieldValue('shiftId'))) {
      form.setFieldValue('shiftId', shifts[0].id)
    }
  }, [shifts, form])

  // Đổi ngày / buổi / khoa mà bác sĩ đã chọn không còn trong danh sách thì bỏ chọn
  useEffect(() => {
    if (doctorUserId != null && doctorsQ.data && !doctorsQ.data.some((d) => d.userId === doctorUserId)) {
      form.setFieldValue('doctorUserId', undefined)
    }
  }, [doctorsQ.data, doctorUserId, form])

  // Chọn bác sĩ thì tự điền khoa của bác sĩ đó
  const pickDoctor = (v?: number) => {
    form.setFieldValue('doctorUserId', v)
    const sp = doctors.find((d) => d.userId === v)?.specialtyId
    if (sp != null) form.setFieldValue('specialtyId', sp)
  }

  const send = async (v: BookingForm, summary: string) => {
    setSending(true)
    // TODO: gọi POST /api/public/bookings khi module booking có API
    await new Promise((r) => setTimeout(r, 700))
    setSending(false)
    setDone({ summary, phone: booker ? booker.phone : v.phone })
  }

  const submit = (v: BookingForm) => {
    const specialty = specialties?.find((s) => s.id === v.specialtyId)?.name
    const doctor = doctors.find((d) => d.userId === v.doctorUserId)?.fullName
    const shift = shifts.find((s) => s.id === v.shiftId)
    const day = dayjs(v.date).locale('vi').format('dddd DD/MM/YYYY')
    const summary = [specialty, doctor, `buổi ${shift?.name.toLowerCase() ?? ''}, ${day}`].filter(Boolean).join(' · ')
    if (!confirmFirst) return send(v, summary)

    const name = booker?.fullName ?? v.fullName
    const phone = booker ? booker.phone : v.phone
    modal.confirm({
      title: 'Xác nhận đặt lịch khám',
      icon: <CalendarOutlined style={{ color: 'var(--ms-primary)' }} />,
      width: 480,
      centered: true,
      okText: 'Xác nhận đặt lịch',
      cancelText: 'Sửa lại',
      content: (
        <Descriptions column={1} size="small" className="booking-confirm">
          <Descriptions.Item label="Người khám">{name}</Descriptions.Item>
          {phone && <Descriptions.Item label="Số điện thoại">{phone}</Descriptions.Item>}
          <Descriptions.Item label="Chuyên khoa">{specialty ?? 'Chưa chọn'}</Descriptions.Item>
          <Descriptions.Item label="Bác sĩ">{doctor ?? 'Phòng khám sắp xếp'}</Descriptions.Item>
          <Descriptions.Item label="Ngày khám">{day}</Descriptions.Item>
          <Descriptions.Item label="Buổi">
            {shift ? `${shift.name} (${hhmm(shift.startTime)} – ${hhmm(shift.endTime)})` : ''}
          </Descriptions.Item>
          <Descriptions.Item label="Triệu chứng">{v.symptoms?.trim() || 'Không có'}</Descriptions.Item>
        </Descriptions>
      ),
      // Trả promise: nút xác nhận quay chờ, không bấm lặp được
      onOk: () => send(v, summary),
    })
  }

  return (
    <div className="booking-card-inner">
      <div className="booking-card-head">
        <span className="booking-card-icon" aria-hidden="true"><CalendarOutlined /></span>
        <h2>{title}</h2>
      </div>

      {done ? (
        <div className="booking-done" role="status">
          <CheckCircleFilled className="booking-done-icon" />
          <h3>Đã gửi yêu cầu đặt lịch</h3>
          <p>{done.summary}</p>
          {done.phone
            ? <p>Phòng khám sẽ gọi số <b>{done.phone}</b> để xác nhận trong 15 phút.</p>
            : <p>Phòng khám sẽ liên hệ với bạn để xác nhận trong 15 phút.</p>}
          <Button onClick={() => { setDone(null); form.resetFields() }}>Đặt thêm lịch khác</Button>
        </div>
      ) : (
        <Form
          form={form}
          name={formName}
          layout="vertical"
          requiredMark={false}
          onFinish={submit}
          initialValues={{ date: dayjs().add(1, 'day').format('YYYY-MM-DD'), shiftId: shifts[0]?.id }}
        >
          <div className="booking-row">
            <Form.Item name="specialtyId">
              <OptionPicker
                icon={<MedicineBoxOutlined aria-hidden="true" />}
                title="Chọn chuyên khoa"
                placeholder="Chọn chuyên khoa"
                emptyText="Chưa có chuyên khoa"
                blocked={specBlocked}
                options={(specialties ?? []).map((s) => ({ value: s.id, label: s.name }))}
              />
            </Form.Item>
            <Form.Item name="doctorUserId">
              <OptionPicker
                icon={<UserOutlined aria-hidden="true" />}
                title="Bác sĩ trực buổi đã chọn"
                placeholder="Chọn bác sĩ"
                emptyText={doctorsQ.isError ? 'Không tải được danh sách bác sĩ' : 'Chưa có bác sĩ trực buổi này'}
                options={doctors.map((d) => ({ value: d.userId, label: d.fullName, hint: d.specialtyName ?? undefined }))}
                onChange={pickDoctor}
              />
            </Form.Item>
          </div>

          <Form.Item name="date" label="Ngày khám" rules={[{ required: true, message: 'Chọn ngày khám' }]}>
            <DateStrip />
          </Form.Item>

          <Form.Item
            name="shiftId"
            rules={[{ required: true, message: 'Chọn buổi khám' }]}
            help={shiftBlocked}
            validateStatus={shiftBlocked ? 'error' : undefined}
          >
            <SessionTiles shifts={shifts} date={date} />
          </Form.Item>

          <div className="booking-divider" aria-hidden="true" />
          {!booker && <div className="booking-row">
            <Form.Item name="fullName" rules={[{ required: true, whitespace: true, message: 'Nhập họ và tên' }]}>
              <Input size="large" prefix={<UserOutlined />} placeholder="Họ và tên" aria-label="Họ và tên" autoComplete="name" />
            </Form.Item>
            <Form.Item
              name="phone"
              rules={[{ required: true, message: 'Nhập số điện thoại' }, { pattern: /^0\d{9}$/, message: '10 số, bắt đầu bằng 0' }]}
            >
              <Input size="large" prefix={<PhoneOutlined />} placeholder="Số điện thoại" aria-label="Số điện thoại" inputMode="tel" maxLength={10} autoComplete="tel" />
            </Form.Item>
          </div>}

          <Form.Item name="symptoms" rules={[{ max: 500, message: 'Tối đa 500 ký tự' }]}>
            <Input.TextArea
              placeholder="Mô tả triệu chứng của bạn"
              aria-label="Mô tả triệu chứng"
              autoSize={{ minRows: 2, maxRows: 4 }}
              maxLength={500}
            />
          </Form.Item>

          <Button
            htmlType="submit"
            block
            size="large"
            className="btn-accent booking-submit"
            loading={sending}
            disabled={!!shiftBlocked || shiftsQ.isLoading}
          >
            {booker ? 'Đặt lịch khám cho bạn' : 'Đặt lịch khám'}
          </Button>
          {booker && (
            <Button block size="large" className="booking-other" icon={<TeamOutlined />} onClick={() => setForOther(true)}>
              Đặt lịch cho người khác
            </Button>
          )}
          <div className="booking-trust">
            <span><LockOutlined /> Bảo mật thông tin</span>
            <span><SafetyCertificateOutlined /> Không thu phí đặt lịch</span>
          </div>
          {note && <p className="booking-note">{note}</p>}
        </Form>
      )}
      {booker && (
        <Modal open={forOther} onCancel={() => setForOther(false)} footer={null} width={600} centered destroyOnClose className="booking-modal">
          <BookingWidget specialties={specialties} loadFailed={loadFailed} note={note} title="Đặt lịch cho người khác" formName="booking-other" confirmFirst />
        </Modal>
      )}
    </div>
  )
}

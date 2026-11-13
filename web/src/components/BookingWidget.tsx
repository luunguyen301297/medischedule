import {
  CalendarOutlined, CheckCircleFilled, LockOutlined, MedicineBoxOutlined, PhoneOutlined, RightOutlined,
  SafetyCertificateOutlined, UserOutlined,
} from '@ant-design/icons'
import { Button, Form, Input, Modal } from 'antd'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { useState } from 'react'

const SESSIONS = [
  { value: 'Sáng', time: '7:00 – 11:30' },
  { value: 'Chiều', time: '13:00 – 17:00' },
  { value: 'Tối', time: '17:30 – 20:00' },
] as const

type Session = (typeof SESSIONS)[number]['value']

interface BookingForm {
  specialty: string
  date: string     // YYYY-MM-DD
  session: Session
  fullName: string
  phone: string
}

const WEEKDAY = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

/** Dải 7 ngày tới để chọn nhanh. */
function DateStrip({ id, value, onChange }: { id?: string; value?: string; onChange?: (v: string) => void }) {
  const days = Array.from({ length: 7 }, (_, i) => dayjs().add(i, 'day'))
  return (
    <div className="date-strip" role="radiogroup" aria-label="Ngày khám">
      {days.map((d, i) => {
        const v = d.format('YYYY-MM-DD')
        return (
          <button
            key={v}
            id={i === 0 ? id : undefined}
            type="button"
            role="radio"
            aria-checked={value === v}
            className={`date-chip ${value === v ? 'is-active' : ''}`}
            onClick={() => onChange?.(v)}
          >
            <span className="date-dow">{i === 0 ? 'Hôm nay' : i === 1 ? 'Mai' : WEEKDAY[d.day()]}</span>
            <span className="date-num">{d.format('DD')}</span>
            <span className="date-mon">Th{d.format('M')}</span>
          </button>
        )
      })}
    </div>
  )
}

/** Một nút chọn chuyên khoa; bấm mở hộp thoại danh sách, chọn xong tự đóng. */
function SpecialtyPicker({ id, options, value, onChange }: { id?: string; options: string[]; value?: string; onChange?: (v: string) => void }) {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button
        id={id}
        type="button"
        className={`spec-trigger ${value ? 'has-value' : ''}`}
        aria-haspopup="dialog"
        onClick={() => setOpen(true)}
      >
        <MedicineBoxOutlined aria-hidden="true" />
        <span>{value ?? 'Chọn chuyên khoa'}</span>
        <RightOutlined aria-hidden="true" className="spec-trigger-arrow" />
      </button>
      <Modal
        title="Chọn chuyên khoa"
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
        width={600}
        centered
      >
        <div className="spec-grid" role="radiogroup" aria-label="Chuyên khoa">
          {options.map((s) => (
            <button
              key={s}
              type="button"
              role="radio"
              aria-checked={value === s}
              className={`spec-option ${value === s ? 'is-active' : ''}`}
              onClick={() => { onChange?.(s); setOpen(false) }}
            >
              <span>{s}</span>
              {value === s && <CheckCircleFilled aria-hidden="true" />}
            </button>
          ))}
        </div>
      </Modal>
    </>
  )
}

/** Ba ô buổi khám kèm khung giờ. */
function SessionTiles({ id, value, onChange }: { id?: string; value?: Session; onChange?: (v: Session) => void }) {
  return (
    <div className="session-tiles" role="radiogroup" aria-label="Buổi khám">
      {SESSIONS.map((s, i) => (
        <button
          key={s.value}
          id={i === 0 ? id : undefined}
          type="button"
          role="radio"
          aria-checked={value === s.value}
          className={`session-tile ${value === s.value ? 'is-active' : ''}`}
          onClick={() => onChange?.(s.value)}
        >
          <b>{s.value}</b>
          <span>{s.time}</span>
        </button>
      ))}
    </div>
  )
}

/**
 * Thẻ đặt lịch nhanh ở đầu trang (khách không cần tài khoản).
 * Hiện chỉ là giao diện: backend đặt lịch (module booking) chưa có, bấm gửi sẽ hiện màn hình xác nhận mẫu.
 */
export default function BookingWidget({ specialties, note }: { specialties: string[]; note: string }) {
  const [form] = Form.useForm<BookingForm>()
  const [sending, setSending] = useState(false)
  const [done, setDone] = useState<BookingForm | null>(null)

  const submit = async (v: BookingForm) => {
    setSending(true)
    // TODO: gọi POST /api/public/bookings khi module booking có API
    await new Promise((r) => setTimeout(r, 700))
    setSending(false)
    setDone(v)
  }

  return (
    <div className="booking-card-inner">
      <div className="booking-card-head">
        <span className="booking-card-icon" aria-hidden="true"><CalendarOutlined /></span>
        <h2>Đặt lịch khám</h2>
      </div>

      {done ? (
        <div className="booking-done" role="status">
          <CheckCircleFilled className="booking-done-icon" />
          <h3>Đã gửi yêu cầu đặt lịch</h3>
          <p>
            {done.specialty} · buổi {done.session.toLowerCase()}, {dayjs(done.date).locale('vi').format('dddd DD/MM/YYYY')}
          </p>
          <p>Phòng khám sẽ gọi số <b>{done.phone}</b> để xác nhận trong 15 phút.</p>
          <Button onClick={() => { setDone(null); form.resetFields() }}>Đặt thêm lịch khác</Button>
        </div>
      ) : (
        <Form
          form={form}
          name="booking"
          layout="vertical"
          requiredMark={false}
          onFinish={submit}
          initialValues={{ session: 'Sáng', date: dayjs().add(1, 'day').format('YYYY-MM-DD') }}
        >
          <Form.Item name="specialty" rules={[{ required: true, message: 'Chọn chuyên khoa' }]}>
            <SpecialtyPicker options={specialties} />
          </Form.Item>

          <Form.Item name="date" label="Ngày khám" rules={[{ required: true, message: 'Chọn ngày khám' }]}>
            <DateStrip />
          </Form.Item>

          <Form.Item name="session">
            <SessionTiles />
          </Form.Item>

          <div className="booking-row">
            <Form.Item name="fullName" label="Họ và tên" rules={[{ required: true, whitespace: true, message: 'Nhập họ và tên' }]}>
              <Input size="large" prefix={<UserOutlined />} placeholder="Nguyễn Văn A" autoComplete="name" />
            </Form.Item>
            <Form.Item
              name="phone"
              label="Số điện thoại"
              rules={[{ required: true, message: 'Nhập số điện thoại' }, { pattern: /^0\d{9}$/, message: '10 số, bắt đầu bằng 0' }]}
            >
              <Input size="large" prefix={<PhoneOutlined />} placeholder="09xx xxx xxx" inputMode="tel" maxLength={10} autoComplete="tel" />
            </Form.Item>
          </div>

          <Button htmlType="submit" block size="large" className="btn-accent booking-submit" loading={sending}>
            Đặt lịch khám
          </Button>
          <div className="booking-trust">
            <span><LockOutlined /> Bảo mật thông tin</span>
            <span><SafetyCertificateOutlined /> Không thu phí đặt lịch</span>
          </div>
          {note && <p className="booking-note">{note}</p>}
        </Form>
      )}
    </div>
  )
}

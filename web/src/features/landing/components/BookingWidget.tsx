import {
  CalendarOutlined, CheckCircleFilled, LockOutlined, MedicineBoxOutlined, PhoneOutlined, RightOutlined,
  SafetyCertificateOutlined, UserOutlined,
} from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { Button, Form, Input, Modal } from 'antd'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { type ReactNode, useEffect, useState } from 'react'
import { bookingApi, type BookingShift } from '../api'

interface BookingForm {
  specialtyId?: number
  doctorUserId?: number
  date: string     // YYYY-MM-DD
  shiftId: number
  fullName: string
  phone: string
}

interface PickOption { value: number; label: string; hint?: string }

const WEEKDAY = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
const hhmm = (t: string) => t.slice(0, 5)

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

/** Ô buổi khám theo ca làm việc đang bật; ca đã kết thúc của hôm nay bị khóa. */
function SessionTiles({ id, shifts, date, value, onChange }: {
  id?: string; shifts: BookingShift[]; date?: string; value?: number; onChange?: (v: number) => void
}) {
  const isToday = date === dayjs().format('YYYY-MM-DD')
  const now = dayjs().format('HH:mm')
  return (
    <div className="session-tiles" role="radiogroup" aria-label="Buổi khám">
      {shifts.map((s, i) => (
        <button
          key={s.id}
          id={i === 0 ? id : undefined}
          type="button"
          role="radio"
          aria-checked={value === s.id}
          disabled={isToday && hhmm(s.endTime) <= now}
          className={`session-tile ${value === s.id ? 'is-active' : ''}`}
          onClick={() => onChange?.(s.id)}
        >
          <b>{s.name}</b>
          <span>{hhmm(s.startTime)} – {hhmm(s.endTime)}</span>
        </button>
      ))}
    </div>
  )
}

/**
 * Thẻ đặt lịch nhanh ở đầu trang (khách không cần tài khoản).
 * Buổi khám lấy từ ca làm việc đang bật; bác sĩ lọc theo phân ca của ngày + buổi (và khoa nếu đã chọn).
 * Gửi lịch vẫn là mẫu: backend đặt lịch (module booking) chưa có.
 */
export default function BookingWidget({ specialties, loadFailed, note }: {
  specialties?: { id: number; name: string }[]; loadFailed: boolean; note: string
}) {
  // Chuyên khoa không bắt buộc: tải lỗi chỉ khóa ô chuyên khoa
  const specBlocked = loadFailed ? 'Không tải được chuyên khoa' : specialties?.length === 0 ? 'Chưa có chuyên khoa' : undefined
  const [form] = Form.useForm<BookingForm>()
  const [sending, setSending] = useState(false)
  const [done, setDone] = useState<{ summary: string; phone: string } | null>(null)

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

  const submit = async (v: BookingForm) => {
    setSending(true)
    // TODO: gọi POST /api/public/bookings khi module booking có API
    await new Promise((r) => setTimeout(r, 700))
    setSending(false)
    const doctor = doctors.find((d) => d.userId === v.doctorUserId)
    const shift = shifts.find((s) => s.id === v.shiftId)
    const summary = [
      specialties?.find((s) => s.id === v.specialtyId)?.name,
      doctor?.fullName,
      `buổi ${shift?.name.toLowerCase() ?? ''}, ${dayjs(v.date).locale('vi').format('dddd DD/MM/YYYY')}`,
    ].filter(Boolean).join(' · ')
    setDone({ summary, phone: v.phone })
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
          <p>{done.summary}</p>
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

          <Button
            htmlType="submit"
            block
            size="large"
            className="btn-accent booking-submit"
            loading={sending}
            disabled={!!shiftBlocked || shiftsQ.isLoading}
          >
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

import { CheckCircleFilled, PhoneOutlined, UserOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { App, Button, Descriptions, Form, Input, Modal } from 'antd'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { useEffect, useState } from 'react'
import { errorMessage, fieldErrors } from '@/shared/api/http'
import { AvatarView } from '@/shared/ui/AvatarField'
import { bookingApi, doctorRequestApi, type DoctorRequestResult, type PublicDoctor } from '../api'
import { DateStrip, hhmm, SessionTiles } from './BookingPickers'
import type { Booker } from './BookingWidget'

interface Values { date: string; shiftId: number; fullName?: string; phone?: string; symptoms?: string }

/**
 * Đặt đích danh một bác sĩ: chọn ngày + buổi mong muốn, gửi yêu cầu để nhân sự xếp ca sau (chưa phải lịch hẹn).
 * Bệnh nhân đã đăng nhập (`booker`) không nhập họ tên / SĐT; khách nhập hai ô này. Luôn hỏi xác nhận trước khi gửi.
 */
export default function DoctorRequestDialog({ doctor, booker, open, onClose }: {
  doctor: PublicDoctor; booker?: Booker; open: boolean; onClose: () => void
}) {
  const { modal } = App.useApp()
  const [form] = Form.useForm<Values>()
  const [done, setDone] = useState<DoctorRequestResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  const shiftsQ = useQuery({ queryKey: ['public', 'booking-shifts'], queryFn: bookingApi.shifts, staleTime: 60_000, enabled: open })
  const shifts = shiftsQ.data ?? []
  const shiftBlocked = shiftsQ.isError ? 'Không tải được buổi khám, thử lại sau'
    : shiftsQ.data?.length === 0 ? 'Phòng khám chưa mở buổi khám nào' : undefined
  const date = Form.useWatch('date', form)

  useEffect(() => {
    if (shifts.length && !shifts.some((s) => s.id === form.getFieldValue('shiftId'))) form.setFieldValue('shiftId', shifts[0].id)
  }, [shifts, form])

  const close = () => {
    onClose()
    setDone(null)
    setError(null)
  }

  const send = async (v: Values) => {
    setError(null)
    const body = { doctorUserId: doctor.userId, date: v.date, workShiftId: v.shiftId, symptoms: v.symptoms?.trim() || undefined }
    try {
      setDone(booker
        ? await doctorRequestApi.patient(body)
        : await doctorRequestApi.guest({ ...body, fullName: v.fullName?.trim(), phone: v.phone?.trim() }))
    } catch (e) {
      const fields = fieldErrors(e)
      if (fields) form.setFields(Object.entries(fields).map(([name, err]) => ({ name: name as keyof Values, errors: [err] })))
      else setError(errorMessage(e))
    }
  }

  const submit = (v: Values) => {
    const shift = shifts.find((s) => s.id === v.shiftId)
    modal.confirm({
      title: 'Xác nhận đặt bác sĩ',
      icon: <UserOutlined style={{ color: 'var(--ms-primary)' }} />,
      width: 480,
      centered: true,
      okText: 'Xác nhận gửi',
      cancelText: 'Sửa lại',
      content: (
        <Descriptions column={1} size="small" className="booking-confirm">
          <Descriptions.Item label="Bác sĩ">{doctor.fullName}</Descriptions.Item>
          <Descriptions.Item label="Chuyên khoa">{doctor.specialtyName ?? '—'}</Descriptions.Item>
          <Descriptions.Item label="Ngày khám">{dayjs(v.date).locale('vi').format('dddd DD/MM/YYYY')}</Descriptions.Item>
          <Descriptions.Item label="Buổi">{shift ? `${shift.name} (${hhmm(shift.startTime)} – ${hhmm(shift.endTime)})` : ''}</Descriptions.Item>
          <Descriptions.Item label="Người khám">{booker?.fullName ?? v.fullName}</Descriptions.Item>
          <Descriptions.Item label="Số điện thoại">{booker ? booker.phone ?? '—' : v.phone}</Descriptions.Item>
          <Descriptions.Item label="Triệu chứng">{v.symptoms?.trim() || 'Không có'}</Descriptions.Item>
        </Descriptions>
      ),
      // Trả promise: nút xác nhận quay chờ, không bấm lặp được
      onOk: () => send(v),
    })
  }

  return (
    <Modal open={open} onCancel={close} footer={null} width={600} centered destroyOnClose className="booking-modal">
      <div className="booking-card-head">
        <AvatarView src={doctor.avatarUrl} size={50} />
        <div>
          <h2>Đặt bác sĩ {doctor.fullName}</h2>
          <p>{doctor.specialtyName ?? 'Chọn ngày và buổi bạn muốn khám'}</p>
        </div>
      </div>

      {done ? (
        <div className="booking-done" role="status">
          <CheckCircleFilled className="booking-done-icon" />
          <h3>Đã gửi yêu cầu đặt bác sĩ</h3>
          <p>{done.doctorName} · buổi {done.workShiftName.toLowerCase()}, {dayjs(done.date).locale('vi').format('dddd DD/MM/YYYY')}</p>
          <p>Phòng khám sẽ sắp xếp ca và gọi số <b>{done.phone}</b> để xác nhận.</p>
          <Button onClick={close}>Đóng</Button>
        </div>
      ) : (
        <Form
          form={form}
          name={`doctor-request-${doctor.userId}`}
          layout="vertical"
          requiredMark={false}
          onFinish={submit}
          initialValues={{ date: dayjs().add(1, 'day').format('YYYY-MM-DD'), shiftId: shifts[0]?.id }}
        >
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
          {!booker && (
            <div className="booking-row">
              <Form.Item name="fullName" rules={[{ required: true, whitespace: true, message: 'Nhập họ và tên' }, { max: 120 }]}>
                <Input size="large" prefix={<UserOutlined />} placeholder="Họ và tên" aria-label="Họ và tên" autoComplete="name" />
              </Form.Item>
              <Form.Item
                name="phone"
                rules={[{ required: true, message: 'Nhập số điện thoại' }, { pattern: /^0\d{9}$/, message: '10 số, bắt đầu bằng 0' }]}
              >
                <Input size="large" prefix={<PhoneOutlined />} placeholder="Số điện thoại" aria-label="Số điện thoại" inputMode="tel" maxLength={10} autoComplete="tel" />
              </Form.Item>
            </div>
          )}

          <Form.Item name="symptoms" rules={[{ max: 500, message: 'Tối đa 500 ký tự' }]}>
            <Input.TextArea
              placeholder="Mô tả triệu chứng của bạn"
              aria-label="Mô tả triệu chứng"
              autoSize={{ minRows: 2, maxRows: 4 }}
              maxLength={500}
            />
          </Form.Item>

          {error && <p className="doctor-request-error" role="alert">{error}</p>}
          <Button
            htmlType="submit"
            block
            size="large"
            className="btn-accent booking-submit"
            disabled={!!shiftBlocked || shiftsQ.isLoading}
          >
            Gửi yêu cầu
          </Button>
          <p className="booking-note">Phòng khám sẽ sắp xếp ca cho bác sĩ và gọi lại để xác nhận giờ khám.</p>
        </Form>
      )}
    </Modal>
  )
}

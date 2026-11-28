import { CalendarOutlined } from '@ant-design/icons'
import { Button, Descriptions, Modal } from 'antd'
import { useState } from 'react'
import { AvatarView } from '@/shared/ui/AvatarField'
import type { PublicDoctor } from '../api'
import type { Booker } from './BookingWidget'
import DoctorRequestDialog from './DoctorRequestDialog'

const dash = (v?: string | null) => v || '—'

/** Hồ sơ chuyên môn bác sĩ cho người xem trang chủ: không có tình trạng làm việc, không lộ email / SĐT. */
export default function DoctorProfileDialog({ doctor, booker, onClose }: {
  doctor: PublicDoctor | null; booker?: Booker; onClose: () => void
}) {
  const [requesting, setRequesting] = useState(false)
  return (
    <>
    <Modal title="Hồ sơ bác sĩ" open={!!doctor} onCancel={onClose} footer={null} width={520} centered>
      {doctor && (
        <div className="doctor-profile">
          <div className="doctor-profile-head">
            <AvatarView src={doctor.avatarUrl} size={96} />
            <div>
              <h3>{doctor.fullName}</h3>
              {doctor.specialtyName && <span>{doctor.specialtyName}</span>}
            </div>
          </div>
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="Chuyên khoa">{dash(doctor.specialtyName)}</Descriptions.Item>
            <Descriptions.Item label="Học hàm / học vị">{dash(doctor.degree)}</Descriptions.Item>
            <Descriptions.Item label="Chứng chỉ hành nghề">{dash(doctor.licenseNo)}</Descriptions.Item>
            <Descriptions.Item label="Nhận chỉ định đích danh">{doctor.acceptsDesignation ? 'Có' : 'Không'}</Descriptions.Item>
          </Descriptions>
          <Button type="primary" size="large" block icon={<CalendarOutlined />} onClick={() => setRequesting(true)}>
            Đặt bác sĩ này
          </Button>
        </div>
      )}
    </Modal>
    {doctor && (
      <DoctorRequestDialog doctor={doctor} booker={booker} open={requesting} onClose={() => { setRequesting(false); onClose() }} />
    )}
    </>
  )
}

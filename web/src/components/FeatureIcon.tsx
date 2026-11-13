import {
  BellOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  FileSearchOutlined,
  HeartOutlined,
  MedicineBoxOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'
import type { FeatureIcon as IconKey } from '../api/content'

export const FEATURE_ICONS: Record<IconKey, { label: string; node: ReactNode }> = {
  calendar: { label: 'Lịch hẹn', node: <CalendarOutlined /> },
  clock: { label: 'Đồng hồ', node: <ClockCircleOutlined /> },
  file: { label: 'Hồ sơ', node: <FileSearchOutlined /> },
  bell: { label: 'Thông báo', node: <BellOutlined /> },
  heart: { label: 'Sức khỏe', node: <HeartOutlined /> },
  team: { label: 'Đội ngũ', node: <TeamOutlined /> },
  safety: { label: 'An toàn', node: <SafetyCertificateOutlined /> },
  medicine: { label: 'Thuốc', node: <MedicineBoxOutlined /> },
}

export default function FeatureIcon({ name }: { name: IconKey }) {
  return FEATURE_ICONS[name]?.node ?? <HeartOutlined />
}

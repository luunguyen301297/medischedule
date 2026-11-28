import { Empty } from 'antd'

/** Trang giữ chỗ cho khu chưa làm (Lễ tân, Bác sĩ): có menu trước, nội dung làm ở task sau. */
export default function ComingSoonPage({ title }: { title: string }) {
  return (
    <div className="admin-page">
      <div className="admin-head">
        <h1>{title}</h1>
      </div>
      <div className="admin-scroll">
        <Empty style={{ marginTop: 64 }} description="Chức năng đang được xây dựng" />
      </div>
    </div>
  )
}

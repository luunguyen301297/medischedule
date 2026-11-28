import { DeleteOutlined, UploadOutlined, UserOutlined } from '@ant-design/icons'
import { App, Avatar, Button, Upload } from 'antd'
import { useState } from 'react'
import { filesApi } from '@/shared/api/files'
import { errorMessage } from '@/shared/api/http'

const MAX_MB = 5

/** Ảnh tròn chỉ xem; chưa có ảnh thì hiện biểu tượng người. */
export function AvatarView({ src, size = 64 }: { src?: string | null; size?: number }) {
  return <Avatar size={size} src={src || undefined} icon={<UserOutlined />} />
}

/**
 * Ô ảnh hồ sơ dùng trong Form.Item: xem trước, tải ảnh lên (MinIO qua backend), gỡ ảnh.
 * Chỉ đổi giá trị trong form; lưu thật khi bấm Lưu của dialog (Hủy thì giữ ảnh cũ).
 */
export default function AvatarField({ id, value, onChange }: { id?: string; value?: string | null; onChange?: (v: string | null) => void }) {
  const { message } = App.useApp()
  const [uploading, setUploading] = useState(false)

  const upload = async (file: File) => {
    if (!file.type.startsWith('image/')) {
      message.error('Chỉ nhận tệp ảnh (JPG, PNG, WEBP)')
      return Upload.LIST_IGNORE
    }
    if (file.size > MAX_MB * 1024 * 1024) {
      message.error(`Ảnh tối đa ${MAX_MB} MB`)
      return Upload.LIST_IGNORE
    }
    setUploading(true)
    try {
      onChange?.(await filesApi.uploadImage(file))
    } catch (e) {
      message.error(errorMessage(e))
    } finally {
      setUploading(false)
    }
    return false // tự xử lý, không để Upload gửi request riêng
  }

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <AvatarView src={value} />
      <Upload id={id} name="file" accept="image/jpeg,image/png,image/webp" showUploadList={false} beforeUpload={upload}>
        <Button size="small" icon={<UploadOutlined />} loading={uploading}>{value ? 'Đổi ảnh' : 'Tải ảnh lên'}</Button>
      </Upload>
      {value && <Button size="small" danger icon={<DeleteOutlined />} onClick={() => onChange?.(null)}>Gỡ ảnh</Button>}
    </div>
  )
}

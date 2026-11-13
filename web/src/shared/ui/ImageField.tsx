import { UploadOutlined } from '@ant-design/icons'
import { App, Button, Input, Upload } from 'antd'
import { useState } from 'react'
import { filesApi } from '@/shared/api/files'
import { errorMessage } from '@/shared/api/http'

/**
 * Ô chọn ảnh dùng trong Form.Item: dán đường dẫn hoặc tải ảnh lên (lưu MinIO), có ảnh xem trước.
 * Form.Item tự truyền value / onChange.
 */
export default function ImageField({ id, value, onChange }: { id?: string; value?: string; onChange?: (v: string) => void }) {
  const { message } = App.useApp()
  const [uploading, setUploading] = useState(false)

  const upload = async (file: File) => {
    setUploading(true)
    try {
      onChange?.(await filesApi.uploadImage(file))
      message.success('Đã tải ảnh lên')
    } catch (e) {
      message.error(errorMessage(e))
    } finally {
      setUploading(false)
    }
    return false // tự xử lý, không để Upload gửi request riêng
  }

  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <div style={{ display: 'flex', gap: 8 }}>
        <Input id={id} value={value} onChange={(e) => onChange?.(e.target.value)} placeholder="Dán đường dẫn ảnh hoặc bấm Tải ảnh lên" allowClear />
        <Upload id={id ? `${id}_file` : undefined} name="file" accept="image/jpeg,image/png,image/webp" showUploadList={false} beforeUpload={upload}>
          <Button icon={<UploadOutlined />} loading={uploading}>Tải ảnh lên</Button>
        </Upload>
      </div>
      {value && (
        <div
          role="img"
          aria-label="Xem trước ảnh nền"
          style={{
            height: 120, borderRadius: 8, border: '1px solid var(--ms-border)',
            backgroundImage: `url("${value}")`, backgroundSize: 'cover', backgroundPosition: 'center',
          }}
        />
      )}
    </div>
  )
}

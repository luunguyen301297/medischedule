import { http } from './http'

export const filesApi = {
  /** Upload ảnh lên MinIO qua backend, trả về đường dẫn công khai /api/public/files/site/... */
  uploadImage: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post<{ url: string }>('/admin/files', fd).then((r) => r.data.url)
  },
}

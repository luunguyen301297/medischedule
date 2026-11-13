import axios from 'axios'

const TOKEN_KEY = 'ms_token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

/** Gọi backend qua proxy /api của Vite; tự gắn JWT, hết hạn thì về trang đăng nhập. */
export const http = axios.create({ baseURL: '/api' })

http.interceptors.request.use((cfg) => {
  const t = tokenStore.get()
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

http.interceptors.response.use(
  (r) => r,
  (err) => {
    const onAuthPage = location.pathname.startsWith('/login') || location.pathname.startsWith('/register')
    if (err.response?.status === 401 && !onAuthPage) {
      tokenStore.clear()
      location.href = '/login'
    }
    return Promise.reject(err)
  },
)

/** Lấy thông báo lỗi tiếng Việt từ body {code, message} của backend. */
export const errorMessage = (err: unknown): string =>
  (axios.isAxiosError(err) && err.response?.data?.message) || 'Không kết nối được máy chủ'

/** Mã lỗi ổn định từ backend (vd. EMAIL_EXISTS) để xử lý theo từng trường hợp. */
export const errorCode = (err: unknown): string | undefined =>
  (axios.isAxiosError(err) && err.response?.data?.code) || undefined

/** Lỗi validate theo từng trường (body.fields) nếu backend trả về. */
export const fieldErrors = (err: unknown): Record<string, string> | null =>
  (axios.isAxiosError(err) && err.response?.data?.fields) || null

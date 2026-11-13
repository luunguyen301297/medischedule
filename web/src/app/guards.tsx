import type { ReactElement } from 'react'
import { Navigate } from 'react-router-dom'
import { tokenStore } from '@/shared/api/http'

/** Chưa đăng nhập thì về trang đăng nhập. Kiểm vai trò làm trong layout từng khu (vd. AdminLayout). */
export function RequireAuth({ children }: { children: ReactElement }) {
  return tokenStore.get() ? children : <Navigate to="/login" replace />
}

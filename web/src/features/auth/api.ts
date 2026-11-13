import { http } from '@/shared/api/http'

export type Role = 'ADMIN' | 'DOCTOR' | 'RECEPTIONIST' | 'PHARMACIST' | 'PATIENT'

export interface UserResponse {
  id: number
  email: string
  username: string | null
  fullName: string
  phone: string | null
  role: Role
  enabled: boolean
  lastLoginAt: string | null
  createdAt: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserResponse
}

export interface RegisterRequest {
  fullName: string
  email: string
  phone?: string
  password: string
}

export const authApi = {
  /** login: email hoặc tên đăng nhập */
  login: (login: string, password: string) =>
    http.post<TokenResponse>('/auth/login', { login, password }).then((r) => r.data),
  register: (body: RegisterRequest) => http.post<TokenResponse>('/auth/register', body).then((r) => r.data),
  me: () => http.get<UserResponse>('/auth/me').then((r) => r.data),
}

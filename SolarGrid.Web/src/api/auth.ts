import api from './client'

export type LoginRequest = {
  identifier: string
  password: string
}

export type LoginResponse = {
  success: boolean
  message: string
  userType?: string | null
  userId?: string | null
  fullName?: string | null
  token?: string | null
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>('/api/auth/login', data)
  return response.data
}

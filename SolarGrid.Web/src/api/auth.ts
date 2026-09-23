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

export type RegisterRequest = {
  userType: string
  username?: string | null
  nic?: string | null
  password: string
  fullName: string
  email: string
  phoneNumber: string
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>('/api/auth/login', data)
  return response.data
}

export async function register(data: RegisterRequest): Promise<LoginResponse> {
  const response = await api.post<LoginResponse>('/api/auth/register', data)
  return response.data
}

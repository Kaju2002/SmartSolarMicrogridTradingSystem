import api from './client'
import type { LoginResponse } from './auth'

export type PendingUser = {
  id: string
  userType: string
  nic?: string | null
  username?: string | null
  fullName: string
  email: string
  phoneNumber: string
  status: string
  createdAt: string
}

export type UpdateUserStatusRequest = {
  userId: string
  newStatus: 'Active' | 'Deactivated'
}

export async function getPendingUsers(): Promise<PendingUser[]> {
  const response = await api.get<PendingUser[]>('/api/auth/pending-users')
  return response.data
}

export async function updateUserStatus(
  data: UpdateUserStatusRequest,
): Promise<LoginResponse> {
  const response = await api.put<LoginResponse>('/api/auth/update-status', data)
  return response.data
}

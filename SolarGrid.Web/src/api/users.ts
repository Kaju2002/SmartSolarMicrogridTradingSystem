import api from './client'
import type { LoginResponse } from './auth'

export type AppUser = {
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

/** @deprecated use AppUser — kept for PendingApprovalsPage */
export type PendingUser = AppUser

export type UpdateUserStatusRequest = {
  userId: string
  newStatus: 'Active' | 'Deactivated'
}

export async function getPendingUsers(): Promise<AppUser[]> {
  const response = await api.get<AppUser[]>('/api/auth/pending-users')
  return response.data
}

export async function getUsers(userType?: string): Promise<AppUser[]> {
  const response = await api.get<AppUser[]>('/api/auth/users', {
    params: userType ? { userType } : undefined,
  })
  return response.data
}

export async function updateUserStatus(
  data: UpdateUserStatusRequest,
): Promise<LoginResponse> {
  const response = await api.put<LoginResponse>('/api/auth/update-status', data)
  return response.data
}

export type UserProfile = AppUser & {
  updatedAt?: string | null
}

export type UpdateProfileRequest = {
  fullName: string
  email: string
  phoneNumber: string
}

export async function getProfile(userId: string): Promise<UserProfile> {
  const response = await api.get<UserProfile>(`/api/auth/profile/${userId}`)
  return response.data
}

export async function updateProfile(
  userId: string,
  data: UpdateProfileRequest,
): Promise<LoginResponse> {
  const response = await api.put<LoginResponse>(
    `/api/auth/profile/${userId}`,
    data,
  )
  return response.data
}

export async function requestDeactivation(
  userId: string,
): Promise<LoginResponse> {
  const response = await api.put<LoginResponse>(
    `/api/auth/request-deactivation/${userId}`,
  )
  return response.data
}

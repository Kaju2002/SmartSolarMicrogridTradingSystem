import api from './client'

export type Station = {
  id: string
  stationName: string
  latitude: number
  longitude: number
  capacityKWh: number
  batterySlots: number
  availableSlots: number
  openTime: string
  closeTime: string
  status: string
  createdBy?: string | null
  createdAt: string
  updatedAt?: string | null
}

export type CreateStationRequest = {
  stationName: string
  latitude: number
  longitude: number
  capacityKWh: number
  batterySlots: number
  openTime: string
  closeTime: string
  createdBy: string
}

export type UpdateStationRequest = {
  capacityKWh?: number
  batterySlots?: number
  openTime?: string
  closeTime?: string
}

export type StationResponse = {
  success: boolean
  message: string
  stationId?: string | null
  stationName?: string | null
}

export async function getStations(): Promise<Station[]> {
  const response = await api.get<Station[]>('/api/stations')
  return response.data
}

export async function createStation(
  data: CreateStationRequest,
): Promise<StationResponse> {
  const response = await api.post<StationResponse>('/api/stations', data)
  return response.data
}

export async function updateStation(
  id: string,
  data: UpdateStationRequest,
): Promise<StationResponse> {
  const response = await api.put<StationResponse>(`/api/stations/${id}`, data)
  return response.data
}

export async function deactivateStation(id: string): Promise<StationResponse> {
  const response = await api.put<StationResponse>(
    `/api/stations/${id}/deactivate`,
  )
  return response.data
}

import api from './client'

export type Reservation = {
  id: string
  prosumerNic: string
  stationId: string
  bookingSlotId?: string | null
  reservationDateTime: string
  status: string
  qrCode?: string | null
  approvedBy?: string | null
  createdAt: string
  lastModifiedAt?: string | null
}

export type ReservationResponse = {
  success: boolean
  message: string
  reservationId?: string | null
  status?: string | null
  reservationDateTime?: string | null
  qrCode?: string | null
}

export async function getReservations(): Promise<Reservation[]> {
  const response = await api.get<Reservation[]>('/api/reservations')
  return response.data
}

export async function approveReservation(
  id: string,
): Promise<ReservationResponse> {
  const response = await api.put<ReservationResponse>(
    `/api/reservations/${id}/approve`,
  )
  return response.data
}

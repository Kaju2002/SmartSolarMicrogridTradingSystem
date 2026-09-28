import api from './client'

export type Reservation = {
  id: string
  prosumerNic: string
  stationId: string
  bookingSlotId?: string | null
  reservationDateTime: string
  // 0 on bookings made before kWh was added
  requestedKWh?: number
  // LKR, worked out by the API when booked
  estimatedCost?: number
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
  requestedKWh?: number | null
  estimatedCost?: number | null
  qrCode?: string | null
}

// "10 kWh · LKR 450", or null for old bookings without kWh
export function formatEnergy(reservation: Reservation): string | null {
  const kWh = reservation.requestedKWh ?? 0
  if (kWh <= 0) return null
  const cost = reservation.estimatedCost ?? 0
  return `${kWh.toLocaleString()} kWh · LKR ${cost.toLocaleString()}`
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

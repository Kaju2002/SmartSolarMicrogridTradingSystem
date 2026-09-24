import api from './client'
import type { ReservationResponse } from './reservations'

export type VerifyQrRequest = {
  qrCode: string
}

export async function verifyQr(
  data: VerifyQrRequest,
): Promise<ReservationResponse> {
  const response = await api.post<ReservationResponse>(
    '/api/verification/scan-qr',
    data,
  )
  return response.data
}

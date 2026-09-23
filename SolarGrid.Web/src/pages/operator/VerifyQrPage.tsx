import OperatorPlaceholder from './OperatorPlaceholder'

export default function OperatorVerifyQrPage() {
  return (
    <OperatorPlaceholder
      title="Verify QR"
      subtitle="Scan or paste a booking QR to finalize transfer."
      nextStep="Step 4 — call POST /api/verification/scan-qr and show success or error."
    />
  )
}

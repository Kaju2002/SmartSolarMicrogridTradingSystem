export const ROLES = {
  Backoffice: 'Backoffice',
  GridOperator: 'GridOperator',
  Prosumer: 'Prosumer',
} as const

export type AppRole = (typeof ROLES)[keyof typeof ROLES]

export function homePathForRole(userType?: string | null): string {
  const role = (userType || '').trim()

  if (role.toLowerCase() === ROLES.Backoffice.toLowerCase()) {
    return '/backoffice'
  }

  if (role.toLowerCase() === ROLES.GridOperator.toLowerCase()) {
    return '/operator'
  }

  // Prosumer (or unknown) — web portal is staff-only
  return '/unauthorized'
}

export function matchesRole(
  userType: string | null | undefined,
  allowed: string[],
): boolean {
  const role = (userType || '').trim().toLowerCase()
  return allowed.some((a) => a.toLowerCase() === role)
}

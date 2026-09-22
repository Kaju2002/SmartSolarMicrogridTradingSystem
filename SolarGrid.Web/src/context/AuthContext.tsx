import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

export type AuthUser = {
  userId?: string | null
  fullName?: string | null
  userType?: string | null
}

type AuthContextValue = {
  token: string | null
  user: AuthUser | null
  isAuthenticated: boolean
  login: (token: string, user: AuthUser, keepLoggedIn: boolean) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function readStoredAuth(): { token: string | null; user: AuthUser | null } {
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token')
  const raw =
    localStorage.getItem('user') || sessionStorage.getItem('user')

  if (!token || !raw) {
    return { token: null, user: null }
  }

  try {
    return { token, user: JSON.parse(raw) as AuthUser }
  } catch {
    return { token, user: null }
  }
}

function clearStorage() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const initial = readStoredAuth()
  const [token, setToken] = useState<string | null>(initial.token)
  const [user, setUser] = useState<AuthUser | null>(initial.user)

  const login = useCallback(
    (newToken: string, newUser: AuthUser, keepLoggedIn: boolean) => {
      clearStorage()
      const storage = keepLoggedIn ? localStorage : sessionStorage
      storage.setItem('token', newToken)
      storage.setItem('user', JSON.stringify(newUser))
      setToken(newToken)
      setUser(newUser)
    },
    [],
  )

  const logout = useCallback(() => {
    clearStorage()
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token),
      login,
      logout,
    }),
    [token, user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}

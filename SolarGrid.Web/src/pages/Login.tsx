import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { login as loginApi } from '../api/auth'
import loginHero from '../assets/login-hero.png'
import { useAuth } from '../context/AuthContext'
import { homePathForRole } from '../utils/roles'

export default function Login() {
  const navigate = useNavigate()
  const { isAuthenticated, login, user } = useAuth()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [keepLoggedIn, setKeepLoggedIn] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (isAuthenticated) {
    return <Navigate to={homePathForRole(user?.userType)} replace />
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')

    if (!identifier.trim() || !password) {
      setError('Identifier and password are required.')
      return
    }

    setLoading(true)
    try {
      const result = await loginApi({
        identifier: identifier.trim(),
        password,
      })

      if (!result.success || !result.token) {
        setError(result.message || 'Login failed.')
        return
      }

      login(
        result.token,
        {
          userId: result.userId,
          fullName: result.fullName,
          userType: result.userType,
        },
        keepLoggedIn,
      )
      navigate(homePathForRole(result.userType), { replace: true })
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const msg =
          (err.response?.data as { message?: string } | undefined)?.message ||
          err.message
        setError(msg || 'Login failed.')
      } else {
        setError('Login failed. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative flex h-screen w-full flex-col lg:flex-row">
      {/* Left — form */}
      <div className="flex w-full flex-1 flex-col bg-white lg:w-1/2">
        <div className="mx-auto flex w-full max-w-lg flex-1 flex-col justify-center px-6 py-10">
          <div className="mb-8">
            <h1 className="mb-2 text-3xl font-semibold text-ink sm:text-4xl">
              Sign In
            </h1>
            <p className="text-sm text-muted">
              Enter your username or NIC and password to sign in!
            </p>
          </div>

          {/* Social buttons */}
          <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-3">
            <button
              type="button"
              className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg bg-gray-100 px-4 py-3 text-sm font-normal text-ink transition hover:bg-gray-200 hover:text-ink"
            >
              <svg
                className="shrink-0"
                width="20"
                height="20"
                viewBox="0 0 20 20"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M18.7511 10.1944C18.7511 9.47495 18.6915 8.94995 18.5626 8.40552H10.1797V11.6527H15.1003C15.0011 12.4597 14.4654 13.675 13.2749 14.4916L13.2582 14.6003L15.9087 16.6126L16.0924 16.6305C17.7788 15.1041 18.7511 12.8583 18.7511 10.1944Z"
                  fill="#4285F4"
                />
                <path
                  d="M10.1788 18.75C12.5895 18.75 14.6133 17.9722 16.0915 16.6305L13.274 14.4916C12.5201 15.0068 11.5081 15.3666 10.1788 15.3666C7.81773 15.3666 5.81379 13.8402 5.09944 11.7305L4.99473 11.7392L2.23868 13.8295L2.20264 13.9277C3.67087 16.786 6.68674 18.75 10.1788 18.75Z"
                  fill="#34A853"
                />
                <path
                  d="M5.10014 11.7305C4.91165 11.186 4.80257 10.6027 4.80257 9.99992C4.80257 9.3971 4.91165 8.81379 5.09022 8.26935L5.08523 8.1534L2.29464 6.02954L2.20333 6.0721C1.5982 7.25823 1.25098 8.5902 1.25098 9.99992C1.25098 11.4096 1.5982 12.7415 2.20333 13.9277L5.10014 11.7305Z"
                  fill="#FBBC05"
                />
                <path
                  d="M10.1789 4.63331C11.8554 4.63331 12.9864 5.34303 13.6312 5.93612L16.1511 3.525C14.6035 2.11528 12.5895 1.25 10.1789 1.25C6.68676 1.25 3.67088 3.21387 2.20264 6.07218L5.08953 8.26943C5.81381 6.15972 7.81776 4.63331 10.1789 4.63331Z"
                  fill="#EB4335"
                />
              </svg>
              Sign in with Google
            </button>

            <button
              type="button"
              className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg bg-gray-100 px-4 py-3 text-sm font-normal text-ink transition hover:bg-gray-200 hover:text-ink"
            >
              <svg
                className="shrink-0"
                width="21"
                height="20"
                viewBox="0 0 21 20"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M15.6705 1.875H18.4272L12.4002 8.75833L19.4897 18.125H13.9422L9.59717 12.4442L4.62554 18.125H1.86721L8.31523 10.7625L1.51221 1.875H7.20054L11.128 7.0675L15.6705 1.875ZM14.703 16.475H16.2305L6.37054 3.43833H4.73137L14.703 16.475Z"
                  fill="#000000"
                />
              </svg>
              Sign in with X
            </button>
          </div>

          {/* Or divider */}
          <div className="relative py-3 sm:py-5">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-line" />
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="bg-white px-5 text-muted">Or</span>
            </div>
          </div>

          <form className="space-y-5" onSubmit={handleSubmit}>
            {error ? (
              <div className="rounded-lg border border-error-500/30 bg-error-500/10 px-4 py-3 text-sm text-error-500">
                {error}
              </div>
            ) : null}

            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">
                Identifier<span className="text-error-500">*</span>
              </label>
              <input
                type="text"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                placeholder="Username or NIC"
                disabled={loading}
                className="h-11 w-full rounded-lg border border-line bg-transparent px-4 text-sm text-ink outline-none placeholder:text-muted focus:border-brand-500 focus:ring-3 focus:ring-brand-500/15 disabled:opacity-60"
              />
            </div>

            <div>
              <label className="mb-1.5 block text-sm font-medium text-ink">
                Password<span className="text-error-500">*</span>
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  disabled={loading}
                  className="h-11 w-full rounded-lg border border-line bg-transparent py-2.5 ps-4 pe-11 text-sm text-ink outline-none placeholder:text-muted focus:border-brand-500 focus:ring-3 focus:ring-brand-500/15 disabled:opacity-60"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute top-1/2 right-4 -translate-y-1/2 text-muted"
                  aria-label="Toggle password"
                >
                  {showPassword ? (
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path
                        fillRule="evenodd"
                        clipRule="evenodd"
                        d="M4.638 3.577a.75.75 0 0 0-1.061 1.061l1.276 1.276A8.3 8.3 0 0 0 2.415 9.46c-.054.157-.054.328 0 .486A8.56 8.56 0 0 0 10 15.362a8.5 8.5 0 0 0 3.5-.74l1.862 1.862a.75.75 0 0 0 1.061-1.061L4.638 3.577Zm7.723 9.844-1.913-1.913a1.87 1.87 0 0 1-2.315-2.315L5.919 6.979A6.5 6.5 0 0 0 3.923 9.702C4.868 12.137 7.234 13.862 10 13.862c.833 0 1.629-.156 2.361-.441Zm3.716-3.719a6.4 6.4 0 0 1-1.257 2.029l1.061 1.061a8.3 8.3 0 0 0 1.704-2.847c.054-.158.054-.329 0-.486A8.56 8.56 0 0 0 10 4.043c-.865 0-1.698.137-2.478.39l1.229 1.23c.404-.079.822-.12 1.249-.12 2.767 0 5.132 1.725 6.077 4.16Z"
                        fill="#98A2B3"
                      />
                    </svg>
                  ) : (
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path
                        fillRule="evenodd"
                        clipRule="evenodd"
                        d="M10 13.862c-2.766 0-5.132-1.725-6.077-4.16C4.868 7.268 7.234 5.543 10 5.543s5.132 1.725 6.077 4.16C15.132 12.137 12.766 13.862 10 13.862ZM10 4.043C6.482 4.043 3.495 6.309 2.415 9.459c-.054.158-.054.329 0 .487C3.495 13.096 6.482 15.362 10 15.362s6.505-2.266 7.585-5.416c.054-.158.054-.329 0-.487C16.505 6.309 13.518 4.043 10 4.043Zm-.008 3.801a1.858 1.858 0 1 0 .015 3.716h.015a1.858 1.858 0 0 0-.015-3.716h-.015Z"
                        fill="#98A2B3"
                      />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between">
              <label className="flex cursor-pointer items-center gap-2 text-sm text-ink">
                <input
                  type="checkbox"
                  checked={keepLoggedIn}
                  onChange={(e) => setKeepLoggedIn(e.target.checked)}
                  className="size-4 rounded border-line accent-brand-500"
                />
                Keep me logged in
              </label>
              <span className="cursor-default text-sm text-brand-500">
                Forgot password?
              </span>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="flex w-full items-center justify-center rounded-lg bg-brand-500 px-4 py-3 text-sm font-medium text-white transition hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {loading ? 'Signing in…' : 'Sign In'}
            </button>
          </form>

          <div className="mt-5 text-center text-sm text-muted">
            Don&apos;t have an account?{' '}
            <span className="cursor-default text-brand-500">Sign Up</span>
          </div>
        </div>
      </div>

      {/* Right — full-height brand panel over the smart solar home artwork */}
      <div className="hidden h-full lg:flex lg:w-1/2">
        <div className="relative flex h-full w-full flex-col overflow-hidden bg-brand-950">
          <img
            src={loginHero}
            alt=""
            aria-hidden="true"
            className="absolute inset-0 h-full w-full object-cover object-center"
          />
          {/* Night sky fades to navy at the top so the white text stays readable */}
          <div className="absolute inset-x-0 top-0 h-3/5 bg-linear-to-b from-brand-950/95 via-brand-950/60 to-transparent" />

          <div className="relative z-10 px-10 pt-10 xl:px-14 xl:pt-14">
            <div className="mb-6 flex items-center gap-3">
              {/* White tile so the cube's black face stands out on the navy panel */}
              <div className="flex size-12 items-center justify-center rounded-xl bg-white p-1.5 shadow-sm">
                <img src="/brand/logo-icon.png" alt="" className="size-full object-contain" />
              </div>
              <span className="text-xl font-semibold text-white">SolarGrid</span>
            </div>

            <h2 className="max-w-md text-3xl leading-tight font-semibold text-white xl:text-4xl">
              Clean solar energy, managed in one place.
            </h2>
            <p className="mt-3 max-w-md text-sm leading-relaxed text-white/80 xl:text-base">
              Backoffice and Grid Operator portal for approving prosumers,
              running solar stations and verifying energy bookings.
            </p>

            {/* Short windows would push the pills onto the house, so drop them there */}
            <ul className="mt-6 flex max-w-md flex-wrap gap-2 [@media(max-height:720px)]:hidden">
              {['Prosumer approvals', 'Station management', 'QR booking check'].map(
                (feature) => (
                  <li
                    key={feature}
                    className="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/15 px-3 py-1.5 text-xs font-medium text-white backdrop-blur"
                  >
                    <span className="size-1.5 rounded-full bg-brand-500" />
                    {feature}
                  </li>
                ),
              )}
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
}

import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'

import { BRAND } from '../brand'
import { useAuth } from '../auth/authContext'
import { HOME_BY_ROLE } from '../auth/roles'
import Avatar from '../components/ui/Avatar'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Logo from '../components/ui/Logo'
import PasswordInput from '../components/ui/PasswordInput'
import { ROLE_LABEL } from '../components/ui/styles'

/** Created by DemoDataSeeder on the backend. One click beats typing on a projector. */
const DEMO_ACCOUNTS = [
  { name: 'Rahim Ahmed', email: 'student@uiu.ac.bd', role: 'STUDENT' },
  { name: 'Dr. Farhana Islam', email: 'faculty@uiu.ac.bd', role: 'FACULTY' },
  { name: 'Nasir Uddin', email: 'invigilator@uiu.ac.bd', role: 'INVIGILATOR' },
]
const DEMO_PASSWORD = 'password123'

export default function LoginPage() {
  const { user, loading, login } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  if (loading) {
    return <div className="grid h-full place-items-center text-ink-400">Loading...</div>
  }

  // Already signed in: no reason to show the form again.
  if (user) {
    return <Navigate to={HOME_BY_ROLE[user.role] ?? '/student'} replace />
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    setFieldErrors({})

    try {
      const me = await login(email.trim(), password)
      navigate(HOME_BY_ROLE[me.role] ?? '/student', { replace: true })
    } catch (err) {
      setError(err.message)
      setFieldErrors(err.fieldErrors ?? {})
    } finally {
      setSubmitting(false)
    }
  }

  function fillDemoAccount(account) {
    setEmail(account.email)
    setPassword(DEMO_PASSWORD)
    setError(null)
    setFieldErrors({})
  }

  return (
    <div className="grid min-h-full grid-cols-1 lg:grid-cols-[1.05fr_1fr]">
      {/* ------------------------------------------------------------------
          Left: what the product is. Collapses to a compact banner on small
          screens so a phone gets straight to the form.
          ------------------------------------------------------------------ */}
      <section className="relative isolate overflow-hidden bg-ink-950 px-6 py-10 text-white sm:px-10 lg:flex lg:flex-col lg:justify-between lg:px-14 lg:py-14">
        {/* Depth: one accent bloom, plus a faint engineering grid. */}
        <div aria-hidden="true" className="bg-hairline-grid absolute inset-0 -z-10 opacity-60" />
        <div
          aria-hidden="true"
          className="absolute -top-40 -left-24 -z-10 h-[32rem] w-[32rem] rounded-full bg-brand-600/25 blur-[100px]"
        />

        <div className="flex items-center gap-2.5">
          <Logo size={30} className="text-brand-400" />
          <span className="text-[0.9375rem] font-semibold tracking-tight">{BRAND.name}</span>
        </div>

        <div className="mt-8 lg:mt-0">
          <p className="text-xs font-medium tracking-[0.18em] text-white/40 uppercase">
            {BRAND.institution}
          </p>
          <h1 className="mt-3 max-w-md text-3xl font-semibold tracking-tight text-balance lg:text-[2.5rem] lg:leading-[1.1]">
            {BRAND.tagline}
          </h1>
          <p className="mt-4 max-w-md text-[0.9375rem] leading-relaxed text-white/55">
            {BRAND.subline}
          </p>
        </div>

        <ul className="mt-8 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-white/35 lg:mt-10">
          {BRAND.assurances.map((assurance, index) => (
            <li key={assurance} className="flex items-center gap-2">
              {index > 0 && <span aria-hidden="true">&middot;</span>}
              {assurance}
            </li>
          ))}
        </ul>
      </section>

      {/* ------------------------------------------------------------------
          Right: the form.
          ------------------------------------------------------------------ */}
      <section className="flex items-center justify-center bg-white px-6 py-12 sm:px-10">
        <div className="w-full max-w-[22rem]">
          <h2 className="text-2xl font-semibold tracking-tight text-ink-900">Sign in</h2>
          <p className="mt-1.5 text-sm text-ink-500">Use the account issued by your department.</p>

          <form className="mt-8 flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
            <Input
              id="email"
              name="email"
              type="email"
              label="Email"
              placeholder="you@uiu.ac.bd"
              autoComplete="username"
              autoFocus
              required
              value={email}
              error={fieldErrors.email}
              onChange={(event) => {
                setEmail(event.target.value)
                setError(null)
              }}
            />

            <PasswordInput
              id="password"
              name="password"
              label="Password"
              placeholder="Your password"
              autoComplete="current-password"
              required
              value={password}
              error={fieldErrors.password}
              onChange={(event) => {
                setPassword(event.target.value)
                setError(null)
              }}
            />

            {/* One region for anything the server rejected outright. */}
            {error && (
              <p
                role="alert"
                className="flex items-start gap-2.5 rounded-lg border border-danger-200 bg-danger-50 px-3.5 py-3 text-sm text-danger-700"
              >
                <svg
                  className="mt-0.5 h-4 w-4 shrink-0"
                  viewBox="0 0 16 16"
                  fill="none"
                  aria-hidden="true"
                >
                  <circle cx="8" cy="8" r="6.75" stroke="currentColor" strokeWidth="1.5" />
                  <path
                    d="M8 4.75v3.75M8 11.1h.007"
                    stroke="currentColor"
                    strokeWidth="1.5"
                    strokeLinecap="round"
                  />
                </svg>
                {error}
              </p>
            )}

            <Button type="submit" size="lg" fullWidth loading={submitting} className="mt-1">
              {submitting ? 'Signing in' : 'Sign in'}
            </Button>
          </form>

          <div className="mt-9">
            <div className="flex items-center gap-3">
              <span className="h-px flex-1 bg-ink-200" />
              <span className="text-[0.6875rem] font-semibold tracking-wider text-ink-400 uppercase">
                Demo accounts
              </span>
              <span className="h-px flex-1 bg-ink-200" />
            </div>

            <ul className="mt-4 flex flex-col gap-1.5">
              {DEMO_ACCOUNTS.map((account) => (
                <li key={account.email}>
                  <button
                    type="button"
                    onClick={() => fillDemoAccount(account)}
                    className="group flex w-full items-center gap-3 rounded-lg border border-transparent px-2.5 py-2 text-left transition-colors hover:border-ink-200 hover:bg-ink-50"
                  >
                    <Avatar name={account.name} role={account.role} size="sm" />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium text-ink-800">
                        {account.name}
                      </span>
                      <span className="block truncate text-xs text-ink-500">
                        {ROLE_LABEL[account.role]} &middot; {account.email}
                      </span>
                    </span>
                    <svg
                      className="h-4 w-4 shrink-0 text-ink-300 transition-colors group-hover:text-ink-500"
                      viewBox="0 0 16 16"
                      fill="none"
                      aria-hidden="true"
                    >
                      <path
                        d="m6 3.5 4.5 4.5L6 12.5"
                        stroke="currentColor"
                        strokeWidth="1.6"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </button>
                </li>
              ))}
            </ul>

            <p className="mt-3 px-2.5 text-xs text-ink-400">
              Selecting an account fills the form. Password:{' '}
              <span className="tabular font-medium text-ink-500">{DEMO_PASSWORD}</span>
            </p>
          </div>
        </div>
      </section>
    </div>
  )
}

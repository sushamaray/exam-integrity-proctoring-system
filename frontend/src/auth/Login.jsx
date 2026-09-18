import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import Button from '../shared/ui/Button.jsx'
import Field from '../shared/ui/Field.jsx'
import AuthLayout, { FormError } from './AuthLayout.jsx'
import DemoLogins from './DemoLogins.jsx'
import { homeFor, useAuth } from './context.js'

export default function Login() {
  const { user, loading, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [busy, setBusy] = useState(false)

  const navigate = useNavigate()
  const location = useLocation()

  if (!loading && user) {
    return <Navigate to={location.state?.from ?? homeFor(user)} replace />
  }

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    setBusy(true)
    try {
      const signedIn = await login(email, password)
      navigate(location.state?.from ?? homeFor(signedIn), { replace: true })
    } catch (err) {
      setFieldErrors(err.fieldErrors ?? {})
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const fillDemo = ({ email: demoEmail, password: demoPassword }) => {
    setEmail(demoEmail)
    setPassword(demoPassword)
    setError(null)
    setFieldErrors({})
  }

  return (
    <>
      <AuthLayout title="Sign in" subtitle="Use your university email and password.">
        <form className="flex flex-col gap-3.5" onSubmit={submit}>
          <FormError>{error}</FormError>

          <Field
            label="Email"
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="0112210001@bscse.uiu.ac.bd"
            error={fieldErrors.email}
          />
          <Field
            label="Password"
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            error={fieldErrors.password}
          />

          <Button type="submit" disabled={busy} className="mt-2 w-full">
            {busy ? 'Signing in…' : 'Sign in'}
          </Button>

          <p className="m-0 mt-1 text-center text-sm text-ink/55">
            New student? <Link to="/register">Create an account</Link>
          </p>
        </form>
      </AuthLayout>

      <DemoLogins onPick={fillDemo} />
    </>
  )
}

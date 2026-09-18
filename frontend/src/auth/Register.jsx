import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Button from '../shared/ui/Button.jsx'
import Field from '../shared/ui/Field.jsx'
import AuthLayout, { FormError } from './AuthLayout.jsx'
import { useAuth } from './context.js'

const EMPTY = { fullName: '', studentId: '', email: '', password: '', confirm: '' }

export default function Register() {
  const { register } = useAuth()
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setFieldErrors({})

    if (form.password !== form.confirm) {
      setFieldErrors({ confirm: 'Passwords do not match' })
      return
    }

    setBusy(true)
    try {
      await register({
        fullName: form.fullName,
        studentId: form.studentId,
        email: form.email,
        password: form.password,
      })
      navigate('/student', { replace: true })
    } catch (err) {
      setFieldErrors(err.fieldErrors ?? {})
      setError(Object.keys(err.fieldErrors ?? {}).length ? null : err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Create your account" subtitle="Students register here using their university email.">
      <form className="flex flex-col gap-3.5" onSubmit={submit}>
        <FormError>{error}</FormError>

        <Field
          label="Full name"
          required
          autoComplete="name"
          value={form.fullName}
          onChange={set('fullName')}
          placeholder="Tanvir Rahman"
          error={fieldErrors.fullName}
        />
        <Field
          label="Student ID"
          required
          inputMode="numeric"
          value={form.studentId}
          onChange={set('studentId')}
          placeholder="0112210001"
          error={fieldErrors.studentId}
        />
        <Field
          label="Email"
          type="email"
          required
          autoComplete="email"
          value={form.email}
          onChange={set('email')}
          placeholder="0112210001@bscse.uiu.ac.bd"
          error={fieldErrors.email}
        />
        <Field
          label="Password"
          type="password"
          required
          autoComplete="new-password"
          value={form.password}
          onChange={set('password')}
          placeholder="••••••••"
          hint="At least 8 characters."
          error={fieldErrors.password}
        />
        <Field
          label="Confirm password"
          type="password"
          required
          autoComplete="new-password"
          value={form.confirm}
          onChange={set('confirm')}
          placeholder="••••••••"
          error={fieldErrors.confirm}
        />

        <Button type="submit" disabled={busy} className="mt-2 w-full">
          {busy ? 'Creating account…' : 'Create account'}
        </Button>

        <p className="m-0 mt-1 text-center text-sm text-ink/55">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </AuthLayout>
  )
}

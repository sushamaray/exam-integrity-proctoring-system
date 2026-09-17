import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '../../shared/ui/Button.jsx'
import Card from '../../shared/ui/Card.jsx'
import { login } from '../api.js'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()

  const submit = async (e, dest) => {
    e.preventDefault()
    setBusy(true)
    await login(email, password)
    setBusy(false)
    navigate(dest)
  }

  return (
    <div className="min-h-screen flex bg-paper text-ink">
      <div className="hidden md:flex w-[420px] shrink-0 flex-col justify-between bg-navy-dark text-paper p-10 relative overflow-hidden">
        <div
          className="absolute inset-0 opacity-[0.06]"
          style={{
            backgroundImage:
              'linear-gradient(to right, #fff 1px, transparent 1px), linear-gradient(to bottom, #fff 1px, transparent 1px)',
            backgroundSize: '44px 44px',
          }}
        />
        <div className="relative flex items-center gap-2.5">
          <span className="w-[30px] h-[30px] rounded-[8px] bg-paper flex items-center justify-center
            text-navy-dark text-sm font-bold shrink-0">C</span>
          <span className="text-lg font-semibold tracking-[-0.015em]">Cohort</span>
        </div>
        <div className="relative">
          <h1 className="text-3xl font-semibold leading-tight tracking-[-0.02em] mb-3">
            Proctored assessment,<br />without the theatre.
          </h1>
          <p className="text-paper/70 leading-relaxed max-w-[34ch]">
            Timers, answer palettes and evidence-first review — the same event stream powers
            the student exam and the faculty console.
          </p>
        </div>
        <div className="relative text-paper/40 text-xs">© Cohort · CSE 4103</div>
      </div>

      <div className="flex-1 flex items-center justify-center p-6">
        <Card className="w-full max-w-[380px] p-7">
          <h2 className="m-0 mb-1 text-xl font-semibold tracking-[-0.015em]">Sign in</h2>
          <p className="m-0 mb-6 text-ink/55 text-sm">Use your university email and password.</p>

          <form className="flex flex-col gap-3.5" onSubmit={(e) => submit(e, '/student')}>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs font-medium text-ink/66">Email</span>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="nadia.rahman@uni.edu"
                className="h-11 rounded-[6px] border border-navy/18 bg-surface px-3.5 text-sm
                  focus-visible:outline-2 focus-visible:outline-navy"
              />
            </label>
            <label className="flex flex-col gap-1.5">
              <span className="text-xs font-medium text-ink/66">Password</span>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="h-11 rounded-[6px] border border-navy/18 bg-surface px-3.5 text-sm
                  focus-visible:outline-2 focus-visible:outline-navy"
              />
            </label>

            <Button type="submit" disabled={busy} className="mt-2 w-full">
              {busy ? 'Signing in…' : 'Sign in as student'}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={busy}
              onClick={(e) => submit(e, '/faculty')}
              className="w-full"
            >
              Continue as faculty (demo)
            </Button>
          </form>
        </Card>
      </div>
    </div>
  )
}

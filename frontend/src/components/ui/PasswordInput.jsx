import { useState } from 'react'

import Input from './Input'

/**
 * A password field with a reveal toggle.
 *
 * Worth having on a screen that will be driven from a lectern: mistyping a
 * password into a projected login and getting no way to check it is a bad
 * thirty seconds in front of an audience.
 */
export default function PasswordInput({ id, ...props }) {
  const [visible, setVisible] = useState(false)

  return (
    <Input
      {...props}
      id={id}
      type={visible ? 'text' : 'password'}
      trailing={
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          aria-label={visible ? 'Hide password' : 'Show password'}
          aria-pressed={visible}
          className="rounded-md px-2 py-1.5 text-xs font-medium text-ink-500 transition-colors hover:bg-ink-100 hover:text-ink-800"
        >
          {visible ? 'Hide' : 'Show'}
        </button>
      }
    />
  )
}

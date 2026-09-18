const PASSWORD = 'password123'

const ACCOUNTS = [
  { key: 'F', label: 'faculty (Dr. Farhana Islam)', email: 'faculty@uiu.ac.bd' },
  { key: '1', label: 'student 1 (Tanvir Rahman)', email: '0112210001@bscse.uiu.ac.bd' },
  { key: '2', label: 'student 2 (Nusrat Hossain)', email: '0112210002@bscse.uiu.ac.bd' },
  { key: '3', label: 'student 3 (Sadia Chowdhury)', email: '0112210003@bscse.uiu.ac.bd' },
]

export default function DemoLogins({ onPick }) {
  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col items-end gap-1.5">
      <span className="text-[10px] font-semibold tracking-[0.1em] uppercase text-ink/38">
        Demo logins
      </span>
      <div className="flex gap-1.5">
        {ACCOUNTS.map((account) => (
          <button
            key={account.key}
            type="button"

            title={`Fill in ${account.label}: ${account.email}`}
            aria-label={`Fill in the ${account.label} demo account`}
            onClick={() => onPick({ email: account.email, password: PASSWORD })}

            className="w-9 h-9 inline-flex items-center justify-center rounded-[6px]
              border border-navy/18 bg-surface text-sm font-semibold text-ink/66
              transition-colors hover:bg-navy/7 hover:text-navy
              focus-visible:outline-2 focus-visible:outline-navy"
          >
            {account.key}
          </button>
        ))}
      </div>
    </div>
  )
}

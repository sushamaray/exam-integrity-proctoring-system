import { useEffect, useMemo, useState } from 'react'
import CodeEditor from '../../shared/ui/CodeEditor.jsx'
import Console from './Console.jsx'
import { SELECT } from './CodingExam.jsx'
import {
  listLanguages,
  preparePracticeConsole,
  sendPracticeConsoleInput,
  startPracticeConsole,
  stopPracticeConsole,
} from '../api.js'

const SOURCE_KEY = 'aoop.codelab.source'
const LANGUAGE_KEY = 'aoop.codelab.language'

/** Each one reads a number and prints something back, so Run does something on first sight. */
const TEMPLATES = {
  C: `#include <stdio.h>

int main(void) {
    int n;
    printf("Enter a number: ");
    if (scanf("%d", &n) != 1) {
        return 1;
    }

    printf("You typed %d\\n", n);
    return 0;
}
`,
  CPP: `#include <iostream>

int main() {
    int n;
    std::cout << "Enter a number: ";
    if (!(std::cin >> n)) {
        return 1;
    }

    std::cout << "You typed " << n << std::endl;
    return 0;
}
`,
  PYTHON: `n = input("Enter a number: ")

print("You typed " + n)
`,
}

const DEFAULT_LANGUAGE = 'C'

/** One key per language, so switching never overwrites the other scratchpad. */
const keyFor = (language) => `${SOURCE_KEY}.${language}`

/** Reads a scratchpad, tolerating a browser that refuses storage entirely. */
function restore(language) {
  try {
    return window.localStorage.getItem(keyFor(language)) ?? TEMPLATES[language] ?? ''
  } catch {
    return TEMPLATES[language] ?? ''
  }
}

function restoreLanguage() {
  try {
    const last = window.localStorage.getItem(LANGUAGE_KEY)
    return last in TEMPLATES ? last : DEFAULT_LANGUAGE
  } catch {
    return DEFAULT_LANGUAGE
  }
}

/**
 * The compiler with no exam behind it: write code, run it, type at it.
 *
 * Nothing here is timed, graded, proctored or sent anywhere to be stored. The source lives in this
 * browser's localStorage only, so it survives a refresh without the server ever holding a student's
 * practice work -- one entry per language, because switching language to try something is not a
 * reason to lose what you were writing in the other.
 */
export default function PracticeLab({ selector }) {
  const [language, setLanguage] = useState(restoreLanguage)
  const [sources, setSources] = useState(() => ({ [language]: restore(language) }))
  const [languages, setLanguages] = useState([])

  const source = sources[language] ?? ''

  useEffect(() => {
    let alive = true
    listLanguages().then((list) => alive && setLanguages(list)).catch(() => {})
    return () => { alive = false }
  }, [])

  useEffect(() => {
    try {
      window.localStorage.setItem(keyFor(language), source)
      window.localStorage.setItem(LANGUAGE_KEY, language)
    } catch {
      // Private window, or storage is full or blocked. Losing the scratchpad on refresh is a far
      // smaller problem than refusing to let somebody type.
    }
  }, [language, source])

  // Each language's buffer is loaded the first time it is picked, and kept in memory after that.
  const pick = (next) => {
    setSources((all) => (next in all ? all : { ...all, [next]: restore(next) }))
    setLanguage(next)
  }

  const write = (text) => setSources((all) => ({ ...all, [language]: text }))

  const endpoints = useMemo(() => ({
    prepare: preparePracticeConsole,
    start: startPracticeConsole,
    send: sendPracticeConsoleInput,
    stop: stopPracticeConsole,
  }), [])

  const options = languages.length > 0
    ? languages
    : [{ id: 'C', label: 'C (GCC)', available: true }]

  return (
    <div className="flex flex-col h-screen">
      <div className="flex items-center justify-between gap-4 px-5 py-3 border-b border-navy/14 bg-surface">
        <span className="font-semibold tracking-[-0.01em]">Code lab</span>
        <div className="flex items-center gap-5">
          {selector}
          <select
            value={language}
            onChange={(e) => pick(e.target.value)}
            aria-label="Language"
            className={SELECT}
          >
            {options.map((option) => (
              <option key={option.id} value={option.id} disabled={!option.available}>
                {option.label}{option.available ? '' : ' · soon'}
              </option>
            ))}
          </select>
        </div>
      </div>

      <main className="flex-1 min-h-0 flex flex-col p-5 gap-3">
        <p className="m-0 text-ink/55 max-w-[80ch]">
          Somewhere to try things out. Nothing here is timed or marked, and your code stays in this
          browser. Press Tab to complete what you are typing, or to indent.
        </p>

        <div
          className="grid gap-3 flex-1 min-h-0"
          style={{ gridTemplateColumns: 'minmax(0,1.5fr) minmax(300px,1fr)' }}
        >
          <CodeEditor value={source} onChange={write} language={language} className="min-h-0" />
          <Console endpoints={endpoints} language={language} source={source} />
        </div>
      </main>
    </div>
  )
}

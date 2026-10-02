export const weakTopics = [
  { label: 'Concurrency', pct: 41 },
  { label: 'Generics', pct: 58 },
  { label: 'Design patterns', pct: 84 },
]

export const questions = [
  {
    id: 1,
    bloom: 'Apply',
    topic: 'Design patterns',
    text: 'A logging subsystem must notify three independent consumers whenever an event is recorded, and consumers are registered at runtime. Which pattern applies?',
    options: [
      { id: 'a', text: 'Observer' },
      { id: 'b', text: 'Decorator' },
      { id: 'c', text: 'Strategy' },
      { id: 'd', text: 'Factory Method' },
    ],
    correct: 'a',
    explanation: 'Consumers register at runtime and are notified of state changes — that is Observer by definition.',
  },
  {
    id: 2,
    bloom: 'Understand',
    topic: 'Generics',
    text: 'What does type erasure mean for a generic class at runtime in the JVM?',
    options: [
      { id: 'a', text: 'Type parameters are replaced with their bound (or Object)' },
      { id: 'b', text: 'Generic classes are compiled twice, once per type argument' },
      { id: 'c', text: 'The JVM keeps full reified type information' },
      { id: 'd', text: 'Generic methods cannot be overloaded' },
    ],
    correct: 'a',
    explanation: 'The compiler erases type parameters to their bound and inserts casts, so a single bytecode class serves every instantiation.',
  },
  {
    id: 3,
    bloom: 'Apply',
    topic: 'Concurrency',
    text: 'Two threads increment a shared counter without synchronization. Which statement is true?',
    options: [
      { id: 'a', text: 'Updates can be lost due to a race condition' },
      { id: 'b', text: 'The JVM guarantees atomicity for int increments' },
      { id: 'c', text: 'volatile alone would make the increment atomic' },
      { id: 'd', text: 'The compiler rejects the code' },
    ],
    correct: 'a',
    explanation: 'Increment is read-modify-write; without synchronization the two threads can interleave and lose an update.',
  },
  {
    id: 4,
    bloom: 'Remember',
    topic: 'Collections',
    text: 'Which interface guarantees iteration order equal to insertion order and allows duplicates?',
    options: [
      { id: 'a', text: 'List' },
      { id: 'b', text: 'Set' },
      { id: 'c', text: 'SortedMap' },
      { id: 'd', text: 'Deque only' },
    ],
    correct: 'a',
    explanation: 'List preserves insertion order and permits duplicate elements by contract.',
  },
  {
    id: 5,
    bloom: 'Analyze',
    topic: 'Exceptions',
    text: 'A finally block contains a return statement. What happens to an exception thrown earlier in the try block?',
    options: [
      { id: 'a', text: 'It is swallowed — the finally return wins' },
      { id: 'b', text: 'It propagates after finally completes' },
      { id: 'c', text: 'A compile error is raised' },
      { id: 'd', text: 'Both the exception and the return value are returned' },
    ],
    correct: 'a',
    explanation: 'A return in finally discards any exception in flight from the try block — a classic pitfall.',
  },
  {
    id: 6,
    bloom: 'Apply',
    topic: 'Design patterns',
    text: 'You need to add logging around an existing service without modifying its class or subclassing it. Which pattern fits best?',
    options: [
      { id: 'a', text: 'Proxy / Decorator' },
      { id: 'b', text: 'Singleton' },
      { id: 'c', text: 'Builder' },
      { id: 'd', text: 'Visitor' },
    ],
    correct: 'a',
    explanation: 'A proxy or decorator wraps the object and adds behaviour transparently, without touching its source.',
  },
  {
    id: 7,
    bloom: 'Understand',
    topic: 'Inheritance',
    text: 'Why does Java disallow multiple inheritance of classes but allow it for interfaces?',
    options: [
      { id: 'a', text: 'To avoid the diamond problem on state and constructors' },
      { id: 'b', text: 'Interfaces cannot declare methods' },
      { id: 'c', text: 'Classes cannot implement interfaces' },
      { id: 'd', text: 'It is a JVM bytecode limitation only' },
    ],
    correct: 'a',
    explanation: 'Multiple class inheritance creates ambiguity over inherited state; interfaces (until default methods) carried no state to conflict.',
  },
  {
    id: 8,
    bloom: 'Apply',
    topic: 'Concurrency',
    text: 'Which keyword ensures visibility of a field\'s latest value across threads without providing mutual exclusion?',
    options: [
      { id: 'a', text: 'volatile' },
      { id: 'b', text: 'synchronized' },
      { id: 'c', text: 'transient' },
      { id: 'd', text: 'final' },
    ],
    correct: 'a',
    explanation: 'volatile guarantees happens-before visibility but does not make compound operations atomic.',
  },
]

export const proctoringReadiness = [
  { label: 'Browser', detail: 'Chrome · supported', ok: true },
  { label: 'Network stability', detail: '38ms · 0 drops in 30s', ok: true },
  { label: 'Fullscreen permission', detail: 'Granted', ok: true },
  { label: 'Second display', detail: 'One external display detected', ok: false },
]

export const recordedEvents = [
  'Whether this tab has focus',
  'Paste, copy and cut events in answer fields',
  'Typing cadence, sampled — not your keystrokes',
  'Time spent per question',
  'A heartbeat every 5 seconds',
]

export const notRecordedEvents = ['Camera', 'Microphone', 'Your screen', 'Anything outside this tab']

export const practiceTopics = [
  { key: 'patterns', label: 'Design patterns' },
  { key: 'generics', label: 'Generics' },
  { key: 'concurrency', label: 'Concurrency' },
  { key: 'collections', label: 'Collections' },
  { key: 'exceptions', label: 'Exceptions' },
]


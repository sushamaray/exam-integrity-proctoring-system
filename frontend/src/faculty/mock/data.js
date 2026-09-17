export const invigilator = { name: 'Dr. S. Rahman', role: 'Invigilator', initials: 'SR' }

export const examRows = [
  { id: 'e1', name: 'CSE 4103 Midterm', when: 'Today 14:00 · 60 min', state: 'Live', pool: '120 / 6', students: '38 of 40', flags: '4 open', action: 'Monitor', to: '/faculty/monitor' },
  { id: 'e2', name: 'CSE 4103 Lab Final', when: 'Thursday 09:00 · 120 min', state: 'Scheduled', pool: '18 / 3', students: '40 enrolled', flags: '—', action: 'Edit', to: '/faculty' },
  { id: 'e3', name: 'Quiz 3 · Concurrency', when: 'Next Monday 11:00 · 20 min', state: 'Draft', pool: '40 / 4', students: '40 enrolled', flags: '—', action: 'Edit', to: '/faculty' },
  { id: 'e4', name: 'Quiz 2 · Design patterns', when: '8 March · released', state: 'Completed', pool: '60 / 5', students: '39 submitted', flags: '2 upheld · 1 dismissed', action: 'Item analysis', to: '/faculty/analysis' },
  { id: 'e5', name: 'Quiz 1 · Inheritance', when: '19 February · released', state: 'Completed', pool: '50 / 4', students: '40 submitted', flags: 'None', action: 'Item analysis', to: '/faculty/analysis' },
]

export const examHeader = {
  code: 'CSE 4103 Midterm',
  active: 38,
  flagged: 4,
  elapsed: '21:04 elapsed',
}

const STUDENT_NAMES = [
  'Rahim', 'Karim', 'Nadia', 'Sabbir', 'Tanvir', 'Mitu', 'Imran', 'Farhan', 'Ayesha', 'Sumaiya',
  'Zahid', 'Rifat', 'Tasnim', 'Shuvo', 'Anika', 'Jubayer', 'Mehedi', 'Nusrat', 'Arif', 'Sadia',
  'Rony', 'Puja', 'Hasib', 'Labib', 'Tuhin', 'Raisa', 'Omar', 'Sanjida', 'Fahim', 'Tamim',
  'Joya', 'Niloy', 'Bushra', 'Sami', 'Ruhul', 'Ishrat', 'Noman', 'Dipto',
]

const SPECIAL = {
  Rahim: [82, 'escalate', 'paste 240ch'],
  Karim: [44, 'review', 'blur 4m'],
  Sabbir: [38, 'review', 'devtools'],
  Tanvir: [31, 'review', 'time anomaly'],
}

export const tiles = STUDENT_NAMES.map((name, i) => {
  const sp = SPECIAL[name]
  const risk = sp ? sp[0] : (i * 7 + 3) % 18
  const tier = sp ? sp[1] : 'clear'
  const last = sp ? sp[2] : '—'
  return { id: `s-${i}`, name, risk, tier, last }
})

export const monitorFilters = [
  { key: 'all', label: 'All' },
  { key: 'review', label: 'Review' },
  { key: 'escalate', label: 'Escalate' },
]

export const cohortNote = {
  headline: 'Cohort: 11% tab-switched in the last 5 minutes',
  detail: 'Baseline for this exam is 9%. Nothing systemic. Individual events are being scored against this line, not an absolute threshold.',
}

export const queueStats = [
  { value: 33, label: 'Auto-cleared', tone: 'plain' },
  { value: 3, label: 'Awaiting review', tone: 'review' },
  { value: 1, label: 'Escalated', tone: 'escalate' },
  { value: 4, label: 'Similarity pairs', tone: 'plain' },
]

export const queueRows = [
  { id: '8f21c', name: 'Rahim Uddin', risk: 82, tier: 'escalate', signals: 'Paste 240 chars · idle → burst · blur', state: 'Under review' },
  { id: 'a09e4', name: 'Karim Hasan', risk: 44, tier: 'review', signals: 'Blur 4m · answer time anomaly', state: 'Raised' },
  { id: 'c7d10', name: 'Sabbir Ahmed', risk: 38, tier: 'review', signals: 'Devtools opened · heartbeat gap 11s', state: 'Raised' },
  { id: 'd2b83', name: 'Tanvir Islam', risk: 31, tier: 'review', signals: 'Hard question answered in 8s', state: 'Raised' },
  { id: 'e5517', name: 'Nadia Rahman', risk: 12, tier: 'clear', signals: 'Tab switch at cohort minute 12 (discounted)', state: 'Dismissed' },
  { id: 'f8a22', name: 'Mitu Barua', risk: 9, tier: 'clear', signals: 'None correlated', state: 'Auto-cleared' },
  { id: 'b3c66', name: 'Imran Kabir', risk: 4, tier: 'clear', signals: 'No events recorded', state: 'Auto-cleared' },
]

export const sessionDetail = {
  id: '8f21c',
  name: 'Rahim Uddin',
  risk: 82,
  tier: 'escalate',
  raisedAt: '14:41',
  state: 'Under review',
  marks: [
    { label: 'typing stopped 14:02', left: 3.3, hot: false },
    { label: 'blur 14:09', left: 15, hot: false },
    { label: 'idle 14:31', left: 44.5, hot: true },
    { label: 'focus return 14:38', left: 49.5, hot: true },
    { label: 'paste 14:41', left: 55, hot: true },
    { label: 'submit 14:52', left: 86, hot: false },
  ],
  clusterStart: 44,
  clusterWidth: 13,
  tree: [
    { label: 'Session risk', weight: '82', conf: 'escalate', depth: 0 },
    { label: 'Q7 · short answer', weight: '0.70', conf: 'high', depth: 1 },
    { label: 'Paste, 240 chars', weight: '0.45', conf: 'high', depth: 2 },
    { label: 'Idle 29m → burst', weight: '0.25', conf: 'med', depth: 2 },
    { label: 'Q12 · multiple choice', weight: '0.20', conf: 'low', depth: 1 },
    { label: 'Focus loss ×2', weight: '0.10', conf: 'low', depth: 2 },
    { label: 'Typing rhythm shift', weight: '0.10', conf: 'low', depth: 2 },
  ],
  rawEvents: [
    { t: '14:41:06', text: 'paste · 240 chars · field q7', hot: true },
    { t: '14:38:44', text: 'focus regained', hot: false },
    { t: '14:31:02', text: 'typing idle threshold crossed', hot: true },
    { t: '14:09:17', text: 'window blur', hot: false },
    { t: '14:02:51', text: 'keydown cadence sample · 41 wpm', hot: false },
    { t: '14:00:00', text: 'session opened · heartbeat ok', hot: false },
  ],
  annotation: {
    text: '"My brother walked into the room at 14:32. The paste was my own draft from the scratch pad."',
    meta: 'Submitted 15:04 · dismissed flags feed back as calibration data',
  },
  baseline: 'This student pauses a median 22s before each answer. Idle events are scored against that, not the cohort mean.',
}

export const collusionGraph = {
  question: 'Q7 short answer · 38 submissions',
  threshold: 0.78,
  nodes: [
    { x: 150, y: 96, id: 'Rahim', on: true }, { x: 252, y: 142, id: 'Karim', on: true },
    { x: 178, y: 204, id: 'Nadia', on: true }, { x: 470, y: 112, id: 'Sabbir', on: true },
    { x: 556, y: 182, id: 'Tanvir', on: true }, { x: 348, y: 250, id: 'Mitu', on: false },
    { x: 398, y: 56, id: 'Imran', on: false }, { x: 72, y: 158, id: 'Farhan', on: false },
    { x: 604, y: 74, id: 'Ayesha', on: false }, { x: 268, y: 320, id: 'Sumaiya', on: false },
    { x: 660, y: 268, id: 'Zahid', on: false }, { x: 104, y: 296, id: 'Rifat', on: false },
  ],
  edges: [['Rahim', 'Karim', 0.91], ['Karim', 'Nadia', 0.84], ['Rahim', 'Nadia', 0.79], ['Sabbir', 'Tanvir', 0.88]],
  pairs: [
    { pair: 'Rahim ↔ Karim', score: '0.91', detail: 'Q7 · 3 matching mistakes', evidence: 'Both name the pattern "Chain of Command" and repeat the same truncated example in the same order.' },
    { pair: 'Sabbir ↔ Tanvir', score: '0.88', detail: 'Q7 · 2 matching mistakes', evidence: 'Identical clause ordering with one substituted synonym per sentence.' },
    { pair: 'Karim ↔ Nadia', score: '0.84', detail: 'Q7 · no matching mistakes', evidence: 'Shared phrasing on the testability argument. Weaker — both may have used the same lecture slide.' },
    { pair: 'Rahim ↔ Nadia', score: '0.79', detail: 'Q7 · transitive through Karim', evidence: 'Just above threshold. Shown because it closes a three-node cluster.' },
  ],
}

export const answerDiff = {
  pairLabel: 'Rahim Uddin ↔ Karim Hasan',
  meta: 'Q7 · cosine 0.91 · 3 matching mistakes · matching spans underlined',
  left: {
    name: 'Rahim Uddin',
    meta: 'Submitted 14:52 · 212 words',
    segments: [
      { text: 'The ', hot: false },
      { text: 'Chain of Command pattern', hot: true },
      { text: ' is used because ', hot: false },
      { text: 'each handler can decide to handle the request or pass it forward', hot: true },
      { text: ', and this means every stage is testable on its own. ', hot: false },
      { text: 'For example the filter stage does not need to know about scoring at all.', hot: true },
      { text: ' A single handler would couple them together.', hot: false },
    ],
  },
  right: {
    name: 'Karim Hasan',
    meta: 'Submitted 14:54 · 208 words',
    segments: [
      { text: 'We use the ', hot: false },
      { text: 'Chain of Command pattern', hot: true },
      { text: ' here since ', hot: false },
      { text: 'each handler can decide to handle the request or pass it forward', hot: true },
      { text: ', so each stage is testable on its own. ', hot: false },
      { text: 'For example the filter stage does not need to know about scoring at all.', hot: true },
      { text: ' One big handler would tie them together.', hot: false },
    ],
  },
  mistake: 'Both name the pattern "Chain of Command". The correct name is Chain of Responsibility. Shared errors carry more weight than shared correct phrasing, and the scorer weights them accordingly.',
}

export const items = [
  { n: 3, text: 'Which access modifier permits subclass access but not package access?', diff: 0.91, disc: 0.12, dist: [91, 4, 3, 2] },
  { n: 7, text: 'An event pipeline with independently testable reorderable stages describes…', diff: 0.62, disc: 0.48, dist: [18, 9, 62, 11] },
  { n: 9, text: 'Which collection guarantees insertion order and permits duplicates?', diff: 0.74, disc: 0.41, dist: [74, 12, 9, 5] },
  { n: 12, text: 'Attaching confidence adjustment to a detector without modifying it…', diff: 0.38, disc: -0.22, dist: [31, 38, 19, 12] },
  { n: 18, text: 'Output of the given generic method with a bounded wildcard', diff: 0.29, disc: 0.52, dist: [12, 29, 44, 15] },
  { n: 23, text: 'Which statement about volatile and synchronized is correct?', diff: 0.44, disc: 0.09, dist: [24, 44, 20, 12] },
  { n: 31, text: 'Identify the compile error in the given inheritance hierarchy', diff: 0.56, disc: 0.37, dist: [56, 18, 15, 11] },
  { n: 40, text: 'Which pattern does the risk-score tree implement?', diff: 0.68, disc: 0.44, dist: [68, 14, 11, 7] },
]

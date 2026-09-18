export const METHOD_LABEL = {
  TFIDF_COSINE: 'Written answer · TF-IDF cosine',
  WRONG_ANSWER: 'Matching wrong answers',
}

const WIDTH = 740
const HEIGHT = 380

export function layoutGraph(graph) {
  const clusters = new Map()
  for (const node of graph.nodes) {
    if (node.cluster == null) continue
    if (!clusters.has(node.cluster)) clusters.set(node.cluster, [])
    clusters.get(node.cluster).push(node)
  }

  const groups = [...clusters.values()]
  const perRow = Math.min(groups.length, 4)
  const rows = Math.max(1, Math.ceil(groups.length / Math.max(1, perRow)))
  const positioned = []
  groups.forEach((members, i) => {
    const row = Math.floor(i / perRow)
    const col = i % perRow
    const inRow = Math.min(perRow, groups.length - row * perRow)
    const cx = ((col + 1) * WIDTH) / (inRow + 1)
    const cy = ((row + 1) * HEIGHT) / (rows + 1)
    const radius = members.length === 2 ? 55 : 40 + 14 * members.length
    members.forEach((node, j) => {
      const angle = (2 * Math.PI * j) / members.length - Math.PI / 2
      positioned.push({
        id: node.sessionId,
        label: node.student.split(' ')[0],
        name: node.student,
        x: Math.round(cx + radius * Math.cos(angle)),
        y: Math.round(cy + radius * Math.sin(angle) * 0.7),
      })
    })
  })

  const thresholdOf = (method) => graph.thresholds?.[method] ?? 0
  const edges = graph.edges.map((e) => ({
    ...e,

    width: 2 + Math.max(0, (e.score - thresholdOf(e.method)) / Math.max(0.01, 1 - thresholdOf(e.method))) * 8,
  }))

  return {
    nodes: positioned,
    edges,
    unconnected: graph.nodes.filter((n) => n.cluster == null).length,
  }
}

export function pairCards(graph) {
  const name = Object.fromEntries(graph.nodes.map((n) => [n.sessionId, n.student]))
  return graph.edges.map((e) => ({
    id: e.pairId,
    pair: `${name[e.a] ?? e.a} ↔ ${name[e.b] ?? e.b}`,
    score: e.score.toFixed(e.method === 'WRONG_ANSWER' ? 3 : 2),
    detail: e.questionText ? `${METHOD_LABEL[e.method]} · ${e.questionText}` : METHOD_LABEL[e.method],
    evidence: e.detail,
  }))
}

export function segments(text, spans, side) {
  if (!text) return []
  const ranges = spans
    .map((s) => (side === 'a' ? [s.aStart, s.aEnd] : [s.bStart, s.bEnd]))
    .sort((x, y) => x[0] - y[0])
  const out = []
  let at = 0
  for (const [start, end] of ranges) {
    if (start > at) out.push({ text: text.slice(at, start), hot: false })
    out.push({ text: text.slice(start, end), hot: true })
    at = end
  }
  if (at < text.length) out.push({ text: text.slice(at), hot: false })
  return out
}

import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'

/**
 * Streams one running program's output.
 *
 * Modelled on faculty/hooks/useRiskSocket.js. Readiness is reported once the SUBSCRIBE frame has
 * been sent -- deliberately not via a STOMP receipt, because Spring's simple broker does not
 * acknowledge subscriptions at all and waiting for one would hang forever.
 *
 * Closing the remaining race is the server's job: launching waits for the subscription to appear
 * before it starts the program, so nothing printed at startup is lost.
 *
 * Readiness is tracked as "ready for this run id" rather than a boolean, so a stale true cannot
 * survive into the next run.
 */
export function useConsoleSocket(runId, onEvent) {
  const [readyFor, setReadyFor] = useState(null)
  const handler = useRef(onEvent)

  useEffect(() => {
    handler.current = onEvent
  }, [onEvent])

  useEffect(() => {
    if (!runId) return undefined

    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${scheme}://${window.location.host}/ws`,
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe(`/topic/console/${runId}`, (message) =>
          handler.current(JSON.parse(message.body)))
        setReadyFor(runId)
      },
    })
    client.activate()

    return () => {
      client.deactivate()
    }
  }, [runId])

  return runId != null && readyFor === runId
}

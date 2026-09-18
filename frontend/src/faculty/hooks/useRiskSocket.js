import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'

export function useRiskSocket(examId, onTiles) {
  const [connected, setConnected] = useState(false)
  const handler = useRef(onTiles)

  useEffect(() => {
    handler.current = onTiles
  }, [onTiles])

  useEffect(() => {
    if (examId == null) return
    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const client = new Client({
      brokerURL: `${scheme}://${window.location.host}/ws`,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        client.subscribe(`/topic/exams/${examId}/risk`, (message) => {
          handler.current(JSON.parse(message.body))
        })
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    })
    client.activate()
    return () => {
      client.deactivate()
      setConnected(false)
    }
  }, [examId])

  return connected
}

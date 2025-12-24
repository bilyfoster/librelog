import { useEffect, useRef, useState, useCallback } from 'react'
import { useAuth } from '../contexts/AuthContext'

interface CollaborationMessage {
  type: string
  user_id?: string
  username?: string
  cursor?: any
  spot?: any
  locked?: boolean
  field?: string
  users?: Array<{ user_id?: string; username: string }>
  timestamp?: string
}

interface UseCollaborationOptions {
  logId: number
  onCursorUpdate?: (userId: number, username: string, cursor: any) => void
  onSpotUpdate?: (userId: number, username: string, spot: any) => void
  onLogLock?: (userId: number, username: string, locked: boolean) => void
  onUserJoined?: (userId: number, username: string) => void
  onUserLeft?: (userId: number, username: string) => void
  onUsersList?: (users: Array<{ user_id?: string; username: string }>) => void
}

export const useCollaboration = (options: UseCollaborationOptions) => {
  const { logId, onCursorUpdate, onSpotUpdate, onLogLock, onUserJoined, onUserLeft, onUsersList } = options
  const { token } = useAuth()
  const wsRef = useRef<WebSocket | null>(null)
  const [isConnected, setIsConnected] = useState(false)
  const [users, setUsers] = useState<Array<{ user_id?: string; username: string }>>([])
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const reconnectAttempts = useRef(0)
  const maxReconnectAttempts = 5

  const connect = useCallback(() => {
    if (!token || !logId) return

    // Determine WebSocket URL
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const wsUrl = `${protocol}//${host}/api/collaboration/ws/${logId}?token=${token}`

    try {
      const ws = new WebSocket(wsUrl)
      wsRef.current = ws

      ws.onopen = () => {
        setIsConnected(true)
        reconnectAttempts.current = 0
        console.log('Collaboration WebSocket connected')
      }

      ws.onmessage = (event) => {
        try {
          const message: CollaborationMessage = JSON.parse(event.data)
          
          switch (message.type) {
            case 'user_joined':
              if (message.user_id && message.username && onUserJoined) {
                onUserJoined(message.user_id, message.username)
              }
              break
            
            case 'user_left':
              if (message.user_id && message.username && onUserLeft) {
                onUserLeft(message.user_id, message.username)
              }
              break
            
            case 'users_list':
              if (message.users) {
                setUsers(message.users)
                if (onUsersList) {
                  onUsersList(message.users)
                }
              }
              break
            
            case 'cursor_update':
              if (message.user_id && message.username && message.cursor && onCursorUpdate) {
                onCursorUpdate(message.user_id, message.username, message.cursor)
              }
              break
            
            case 'spot_update':
              if (message.user_id && message.username && message.spot && onSpotUpdate) {
                onSpotUpdate(message.user_id, message.username, message.spot)
              }
              break
            
            case 'log_lock':
              if (message.user_id && message.username && message.locked !== undefined && onLogLock) {
                onLogLock(message.user_id, message.username, message.locked)
              }
              break
            
            case 'pong':
              // Heartbeat response
              break
            
            default:
              console.warn('Unknown collaboration message type:', message.type)
          }
        } catch (error) {
          console.error('Error parsing collaboration message:', error)
        }
      }

      ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        setIsConnected(false)
      }

      ws.onclose = () => {
        setIsConnected(false)
        console.log('Collaboration WebSocket disconnected')
        
        // Attempt to reconnect
        if (reconnectAttempts.current < maxReconnectAttempts) {
          reconnectAttempts.current++
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 30000)
          reconnectTimeoutRef.current = setTimeout(() => {
            console.log(`Reconnecting... (attempt ${reconnectAttempts.current})`)
            connect()
          }, delay)
        } else {
          console.error('Max reconnection attempts reached')
        }
      }
    } catch (error) {
      console.error('Error creating WebSocket:', error)
      setIsConnected(false)
    }
  }, [token, logId, onCursorUpdate, onSpotUpdate, onLogLock, onUserJoined, onUserLeft, onUsersList])

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
    
    if (wsRef.current) {
      wsRef.current.close()
      wsRef.current = null
    }
    
    setIsConnected(false)
  }, [])

  const sendMessage = useCallback((message: any) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message))
    } else {
      console.warn('WebSocket is not connected')
    }
  }, [])

  const sendCursorUpdate = useCallback((cursor: any) => {
    sendMessage({
      type: 'cursor_update',
      cursor,
    })
  }, [sendMessage])

  const sendSpotUpdate = useCallback((spot: any) => {
    sendMessage({
      type: 'spot_update',
      spot,
    })
  }, [sendMessage])

  const sendLogLock = useCallback((locked: boolean) => {
    sendMessage({
      type: 'log_lock',
      locked,
    })
  }, [sendMessage])

  const sendTyping = useCallback((field: string) => {
    sendMessage({
      type: 'typing',
      field,
    })
  }, [sendMessage])

  // Heartbeat to keep connection alive
  useEffect(() => {
    if (!isConnected) return

    const heartbeatInterval = setInterval(() => {
      sendMessage({ type: 'ping' })
    }, 30000) // Send ping every 30 seconds

    return () => clearInterval(heartbeatInterval)
  }, [isConnected, sendMessage])

  useEffect(() => {
    connect()
    return () => {
      disconnect()
    }
  }, [connect, disconnect])

  return {
    isConnected,
    users,
    sendCursorUpdate,
    sendSpotUpdate,
    sendLogLock,
    sendTyping,
    reconnect: connect,
  }
}


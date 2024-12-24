interface WsMessage {
  type: string
  data?: string
  channel?: string
  device_uuid?: string
}

const wsUrl = `${window.location.protocol === 'https' ? 'wws' : 'ws'}://${import.meta.env.VITE_REMOTE_HOST}:9801/rtc-ws`
export function createClientTransferWebsocket(deviceId: string) {
  const ws = new WebSocket(wsUrl)
  return new Promise<{ws: WebSocket, addMessageListener: (listener: (event: MessageEvent) => void) => void}>((resolve) => {
    ws.addEventListener('open', () => {
      console.log('connect op')
      const data: WsMessage = {
        type: 'request_new_channel',
        device_uuid: deviceId
      }
      ws.send(JSON.stringify(data))
    })
    let l: ((event: MessageEvent) => void) | undefined
    const messageListener = (event: MessageEvent) => {
      console.log('new data', event)
      if (typeof event.data === 'string') {
        const data: WsMessage = JSON.parse(event.data)
        if (data.type === 'new_channel_success') {
          resolve({ws, addMessageListener: (listener => {
            l = listener
          })})
          return
        }
        l?.(event)
      }
    }
    ws.addEventListener('message', messageListener)
  })
}

export function createListenServer(deviceId: string, onNewConnectListener: (ws: WebSocket) => void) {
  const ws = new WebSocket(wsUrl)
  ws.addEventListener('open', () => {
    ws.send(JSON.stringify({
      type: 'server_register',
      device_uuid: deviceId
    }))
  })
  ws.addEventListener('message', (event) => {
    if (typeof event.data === 'string') {
      const data: WsMessage = JSON.parse(event.data)
      if (data.type === 'request_new_channel') {
        const channelId = data.channel
        const newWs = new WebSocket(wsUrl)
        const openListener = () => {
          newWs.send(JSON.stringify({
            type: 'response_new_channel',
            channel: channelId,
            device_uuid: deviceId
          }))
          newWs.removeEventListener('open', openListener)
          onNewConnectListener(newWs)
        }
        newWs.addEventListener('open', openListener)
      }
    }
  })
  return new Promise<WebSocket>(resolve => {
    const wsOpenListener = () => {
      ws.removeEventListener('open', wsOpenListener)
      resolve(ws)
    }
    ws.addEventListener('open', wsOpenListener)
  })
  
}
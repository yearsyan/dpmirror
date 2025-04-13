import { useCallback, useEffect, useRef, useState } from 'react'
import './App.css'
import { createClientTransferWebsocket, createListenServer } from './utils/rtc'

const deviceId = '454ce4bc-9c0a-4965-9828-7b26736dba53'

const iceServers: RTCIceServer[] = [
  {
    urls: `turn:${import.meta.env.VITE_REMOTE_HOST}:9802`,
    username: '0',
    credential: 'testkey'
  }
]

function Server() {
  const [text, setText] = useState('')
  useEffect(() => {
    createListenServer(deviceId, async (ws) => {

      console.log('new ws')
      ws.addEventListener('close', () => {
        console.log('close')
      })

      const stream = await navigator.mediaDevices.getDisplayMedia({
        video: true
      })

      const peerConnection = new RTCPeerConnection({
        iceServers: iceServers
      })

      stream.getTracks().forEach(track => {
        if (track.kind === 'video') {
          peerConnection.addTrack(track)
        }
      })

      peerConnection.addEventListener('icecandidate', event => {
        if (!event.candidate) {
          return
        }
        ws.send(JSON.stringify({
          type: 'candidate',
          candidate: event.candidate
        }))
      })

      peerConnection.addEventListener('connectionstatechange', event => {
        if (peerConnection.connectionState === 'connected') {
          console.log('connected!!', event)
          setText('Connected')
        }
      })

      ws.addEventListener('message', (event) => {
        if (!(typeof event.data === 'string')) {
          return
        }
        const e = JSON.parse(event.data)
        if (e.type === 'answer') {
          console.log('rece ans', event)
          peerConnection.setRemoteDescription(new RTCSessionDescription(e.sdp))
          console.log('set remote')
        } else if (e.type === 'candidate') {
          peerConnection.addIceCandidate(new RTCIceCandidate(e.candidate))
        }
      })

      await peerConnection.setLocalDescription(await peerConnection.createOffer())
      console.log('set local')
      console.log(peerConnection.localDescription)
      ws.send(JSON.stringify({
        type: 'offer',
        sdp: peerConnection.localDescription
      }))
    })
  }, [])

  return (
    <>
      <h1 className="text-3xl font-bold underline">
        Hello world, Server
      </h1>
      <span>{text}</span>
    </>
  )
}

function Client() {
  const ref = useRef<HTMLVideoElement>(null)
  const [isConnect, setConnectState] = useState(false)

  const connect = useCallback(() => {
    console.log('create ws')
    createClientTransferWebsocket(deviceId).then(async ({ ws: newWs, addMessageListener }) => {
      console.log('new ws')
      const peerConnection = new RTCPeerConnection({
        iceServers: iceServers
      })


      peerConnection.addEventListener('track', event => {
        console.log('new track', event, ref.current)
        if (ref.current) {
          const stream = new MediaStream();
          console.log(event.track.getSettings())
          event.track.addEventListener('ended', () => {
            console.log('enddd')
          })
          stream.addTrack(event.track);
          ref.current.srcObject = stream
        }
      })

      peerConnection.addEventListener('datachannel', event => {
        const dataChannel = event.channel;

        dataChannel.onopen = () => console.log("DataChannel is open");
        dataChannel.onmessage = (event) => console.log("Received:", event.data);
      })

      peerConnection.addEventListener('icecandidate', event => {
        if (!event.candidate) {
          return
        }
        newWs.send(JSON.stringify({
          type: 'candidate',
          candidate: event.candidate
        }))
      })

      peerConnection.addEventListener('connectionstatechange', event => {
        if (peerConnection.connectionState === 'connected') {
          console.log('connected!!', event)
          ref.current?.play()
        } else if (peerConnection.connectionState === 'disconnected') {
          setConnectState(false)
        }
      })
      addMessageListener(async e => {
        if (typeof e.data !== 'string') {
          return
        }
        const event = JSON.parse(e.data)
        if (event.type === 'offer') {
          console.log('rece: ', e)
          peerConnection.setRemoteDescription(new RTCSessionDescription(event.sdp))
          console.log('set remote')
          peerConnection.createAnswer().then(answer => {
            peerConnection.setLocalDescription(answer)
            newWs.send(JSON.stringify({
              type: 'answer',
              sdp: answer
            }))
            console.log('set local')
          })
        } else if (event.type === 'answer') {
          console.log('rece ans', event)
          peerConnection.setRemoteDescription(new RTCSessionDescription(event.sdp))
          console.log('set remote')
        } else if (event.type === 'candidate') {
          console.log('candiate')
          peerConnection.addIceCandidate(new RTCIceCandidate(event.candidate))
        } else if (event.type === 'ready') {
          await peerConnection.setLocalDescription(await peerConnection.createOffer())
          console.log('set local')
          console.log(peerConnection.localDescription)
          newWs.send(JSON.stringify({
            type: 'offer',
            sdp: peerConnection.localDescription
          }))
        }
      })
    })
  }, [])

  return (
    <div className='flex items-center flex-col h-screen '>
      <h1 className="text-xl">
        client
      </h1>
      <video ref={ref} className="flex-1 w-screen"></video>
      {!isConnect &&<button onClick={() => {
        if (!isConnect) {
          connect()
        }
      }}>connect</button> }
    </div>
  )
}

function App() {
  const isClient = new URL(window.location.href).searchParams.get('client') === '1'
  return isClient ? <Client /> : <Server />
}

export default App

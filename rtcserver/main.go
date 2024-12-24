package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"github.com/gorilla/websocket"
	"github.com/pion/turn/v4"
	"github.com/yearsyan/rtcserver/config"
	"github.com/yearsyan/rtcserver/util"
	"io"
	"log"
	"net"
	"net/http"
	"strconv"
	"sync"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

type Message struct {
	MessageType string `json:"type"`
	MessageData string `json:"data"`
	Channel     string `json:"channel"`
	DeviceUuid  string `json:"device_uuid"`
}

var serverMap sync.Map
var channelConnections sync.Map

const (
	ServerRegister     = "server_register"
	RequestNewChannel  = "request_new_channel"
	NewChannelSuccess  = "new_channel_success"
	ResponseNewChannel = "response_new_channel"
)

func handleWebSocket(w http.ResponseWriter, r *http.Request) {
	log.Println("new ws path request " + r.RemoteAddr)
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("upgrader.Upgrade: %v", err)
		return
	}

	log.Println("upgrader.Upgrade ok, remote: " + r.RemoteAddr)
	for {
		messageType, message, e := conn.ReadMessage()
		if e != nil {
			if errors.Is(e, io.EOF) {
				break
			}
			fmt.Println("Read error:", e)
			break
		}

		if messageType == websocket.TextMessage {
			var msgModel Message
			e = json.Unmarshal(message, &msgModel)
			if e != nil {
				continue
			}

			switch msgModel.MessageType {

			case ServerRegister:
				fmt.Println("Register uuid: " + msgModel.DeviceUuid)
				if !config.GetConfig().Allow(msgModel.DeviceUuid) {
					fmt.Println("Forbidden uuid: " + msgModel.DeviceUuid)
					_ = conn.Close()
					return
				}
				serverMap.Store(msgModel.DeviceUuid, conn)
				return

			case RequestNewChannel:
				channelID := util.GenerateChannelID()
				channelConnections.Store(channelID, map[string]*websocket.Conn{
					"server": nil,
					"client": conn,
				})

				sendMessageToDevice(&serverMap, msgModel.DeviceUuid, Message{
					MessageType: RequestNewChannel,
					MessageData: "",
					Channel:     channelID,
				})
				return

			case ResponseNewChannel:
				transferData(msgModel, conn)
				return
			}
		}
	}
}

func transferData(msg Message, sender *websocket.Conn) {
	channelID := msg.Channel
	if connections, ok := channelConnections.Load(channelID); ok {
		connectionsMap := connections.(map[string]*websocket.Conn)
		defer sender.Close()

		// Determine the target connection
		targetConn := connectionsMap["client"]
		if targetConn == nil {
			return
		}
		defer targetConn.Close()
		connectionsMap["server"] = sender
		e := targetConn.WriteJSON(Message{
			MessageType: NewChannelSuccess,
			MessageData: "",
			Channel:     channelID,
		})

		if e != nil {
			return
		}

		var wg sync.WaitGroup
		wg.Add(2)
		go func() {
			defer wg.Done()
			for {
				msgType, newMsg, readError := sender.ReadMessage()
				if readError != nil {
					return
				}
				writeErr := targetConn.WriteMessage(msgType, newMsg)
				if writeErr != nil {
					return
				}
			}
		}()

		go func() {
			defer wg.Done()
			for {
				msgType, newMsg, readError := targetConn.ReadMessage()
				if readError != nil {
					return
				}
				writeErr := sender.WriteMessage(msgType, newMsg)
				if writeErr != nil {
					return
				}
			}
		}()
		wg.Wait()
		fmt.Println("channel close")
		channelConnections.Delete(channelID)

	} else {
		_ = sender.Close()
	}
}

func sendMessageToDevice(store *sync.Map, deviceUuid string, message Message) {
	if conn, ok := store.Load(deviceUuid); ok {
		_ = conn.(*websocket.Conn).WriteJSON(message)
	} else {
		fmt.Println("Device not found:", deviceUuid)
	}
}

func main() {

	config.InitConfig()
	turnPort := strconv.Itoa(config.GetConfig().TURN.Port)
	wsAddr := "0.0.0.0:" + strconv.Itoa(config.GetConfig().WebSocket.Port)
	realm := "yearsyan.github.com"

	udpListener, err := net.ListenPacket("udp4", "0.0.0.0:"+turnPort)
	if err != nil {
		log.Panicf("Failed to create TURN server listener: %s", err)
	}

	publicIP, err := util.GetPublicIP()
	if err != nil {
		panic(err)
	}
	fmt.Println("server public ip: " + publicIP)
	s, err := turn.NewServer(turn.ServerConfig{
		Realm: realm,
		AuthHandler: func(username string, realm string, srcAddr net.Addr) ([]byte, bool) { // nolint: revive
			dummyKey := turn.GenerateAuthKey(username, realm, config.GetConfig().TURN.Key)
			return dummyKey, true
		},
		PacketConnConfigs: []turn.PacketConnConfig{
			{
				PacketConn: udpListener,
				RelayAddressGenerator: &turn.RelayAddressGeneratorStatic{
					RelayAddress: net.ParseIP(publicIP),
					Address:      "0.0.0.0",
				},
			},
		},
	})

	defer s.Close()
	http.HandleFunc(config.GetConfig().WebSocket.Path, handleWebSocket)
	if err := http.ListenAndServe(wsAddr, nil); err != nil {
		fmt.Println("Server error:", err)
	}
}

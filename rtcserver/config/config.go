package config

import (
	"encoding/json"
	"os"
)

// Config 定义配置文件的结构体
type Config struct {
	AllowDeviceUUID []string `json:"allow_device_uuid"`
	TURN            struct {
		Port int    `json:"port"`
		Key  string `json:"key"`
	} `json:"turn"`
	WebSocket struct {
		Port int    `json:"port"`
		Path string `json:"path"`
	} `json:"websocket"`
	allowDev map[string]bool
}

func parseConfig(filePath string) (*Config, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	// 创建配置结构体实例
	var config Config

	// 解析 JSON 配置
	decoder := json.NewDecoder(file)
	if err := decoder.Decode(&config); err != nil {
		return nil, err
	}

	if config.AllowDeviceUUID != nil && len(config.AllowDeviceUUID) > 0 {
		config.allowDev = make(map[string]bool)
		for _, uuid := range config.AllowDeviceUUID {
			config.allowDev[uuid] = true
		}
	}

	return &config, nil
}

var c Config

func InitConfig() {
	cPtr, err := parseConfig("config.json")
	if err != nil {
		panic(err)
	}
	c = *cPtr
}

func GetConfig() *Config {
	return &c
}

func (conf *Config) Allow(uuid string) bool {
	if conf.allowDev == nil {
		return false
	}
	return conf.allowDev[uuid]
}

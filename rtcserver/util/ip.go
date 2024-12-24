package util

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"
)

type IPResponse struct {
	IP string `json:"ip"`
}

// GetPublicIP https://api.ip.sb/jsonip
func GetPublicIP() (string, error) {
	resp, err := http.Get("https://ipinfo.io/")
	if err != nil {
		return "", fmt.Errorf("request fail: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("API Status Error: %s", resp.Status)
	}

	var ipResp IPResponse
	if err := json.NewDecoder(resp.Body).Decode(&ipResp); err != nil {
		return "", fmt.Errorf("decode fail: %v", err)
	}

	return ipResp.IP, nil
}

func GetLocalIP() (string, error) {
	interfaces, err := net.Interfaces()
	if err != nil {
		return "", fmt.Errorf("failed to get network interfaces: %v", err)
	}

	for _, iface := range interfaces {
		if iface.Flags&net.FlagUp == 0 || iface.Flags&net.FlagLoopback != 0 {
			continue
		}

		addrs, err := iface.Addrs()
		if err != nil {
			return "", fmt.Errorf("failed to get addresses for interface %s: %v", iface.Name, err)
		}

		for _, addr := range addrs {
			ipNet, ok := addr.(*net.IPNet)
			if ok && !ipNet.IP.IsLoopback() && ipNet.IP.To4() != nil {
				return ipNet.IP.String(), nil
			}
		}
	}

	return "", fmt.Errorf("no active network interface found")
}

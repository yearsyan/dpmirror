package util

import (
	"crypto/rand"
	"encoding/hex"
)

func GenerateChannelID() string {
	bytes := make([]byte, 32)
	if _, err := rand.Read(bytes); err != nil {
		return ""
	}
	return hex.EncodeToString(bytes)
}

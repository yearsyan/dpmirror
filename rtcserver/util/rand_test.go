package util

import (
	"encoding/hex"
	"testing"
)

func TestGenerateChannelID(t *testing.T) {
	// Generate a channel ID
	channelID := GenerateChannelID()

	// Check if the result is not empty
	if channelID == "" {
		t.Fatalf("GenerateChannelID() returned an empty string")
	}

	// Check if the length of the result is correct (64 hex characters for 32 bytes)
	if len(channelID) != 64 {
		t.Errorf("Expected length 64, got %d", len(channelID))
	}

	// Check if the result is valid hexadecimal
	_, err := hex.DecodeString(channelID)
	if err != nil {
		t.Errorf("GenerateChannelID() returned invalid hexadecimal string: %v", err)
	}
}

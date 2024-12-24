# Go parameters
GOCMD=go
GOBUILD=$(GOCMD) build
GOCLEAN=$(GOCMD) clean
GOTEST=$(GOCMD) test
GOGET=$(GOCMD) get
BINARY_NAME=rtcserver
BINARY_UNIX=$(BINARY_NAME)_linux_amd64

# Default target executed when no arguments are provided to make
.PHONY: all
all: test build

# Build the binary
.PHONY: build
build:
	$(GOBUILD) -o $(BINARY_NAME) -v

# Run tests
.PHONY: test
test:
	$(GOTEST) -v ./...

# Clean the build
.PHONY: clean
clean:
	$(GOCLEAN)
	rm -f $(BINARY_NAME)
	rm -f $(BINARY_UNIX)

# Install dependencies
.PHONY: deps
deps:
	$(GOGET) ./...

# Cross-compile for Linux x86-64
.PHONY: build-linux
build-linux:
	GOOS=linux GOARCH=amd64 $(GOBUILD) -o $(BINARY_UNIX) -v

# Run the application
.PHONY: run
run:
	$(GOCMD) run main.go

# Format the code
.PHONY: fmt
fmt:
	$(GOCMD) fmt ./...

# Lint the code (if golangci-lint is installed)
.PHONY: lint
lint:
	golangci-lint run ./...

# Help message
.PHONY: help
help:
	@echo "Makefile targets:"
	@echo "  all         - Run tests and build the binary"
	@echo "  build       - Build the binary for current platform"
	@echo "  test        - Run tests"
	@echo "  clean       - Clean up generated files"
	@echo "  deps        - Install dependencies"
	@echo "  build-linux - Build the binary for Linux x86-64"
	@echo "  run         - Run the application"
	@echo "  fmt         - Format the code"
	@echo "  lint        - Lint the code"

APP=slow-io
IMAGE=slow-io:local
PORT?=18080

.PHONY: run build docker-build docker-run tidy

tidy:
	cd slow-io && go mod tidy

build: tidy
	cd slow-io && go build -o ../bin/$(APP)

run: tidy
	PORT=$(PORT) BASE_DELAY_MS=50 go run slow-io/main.go

docker-build:
	docker build -t $(IMAGE) -f slow-io/Dockerfile .

docker-run: docker-build
	docker run --rm -p $(PORT):8080 -e BASE_DELAY_MS=50 $(IMAGE)

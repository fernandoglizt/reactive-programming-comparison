# Multi-stage build for go-rxgo application
FROM golang:1.22-alpine AS builder
RUN apk add --no-cache git
WORKDIR /app

COPY reactive/go-rxgo/go.mod reactive/go-rxgo/go.sum ./
RUN go mod download

COPY reactive/go-rxgo/ .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags "-s -w" \
    -trimpath \
    -o /go-rxgo \
    ./cmd/server

FROM scratch
COPY --from=builder /go-rxgo /go-rxgo
EXPOSE 8083

ENV PORT=8083
ENV MAX_COUNT=200000
ENV BATCH_CONCURRENCY=4
ENV ITEM_CONCURRENCY=64
ENV DOWNSTREAM_TIMEOUT_MS=2000
ENV RETRY_ATTEMPTS=1
ENV LOG_LEVEL=INFO

ENTRYPOINT ["/go-rxgo"]

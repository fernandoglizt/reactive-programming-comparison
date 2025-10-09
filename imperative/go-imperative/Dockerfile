# Multi-stage build for go-imperative application
FROM golang:1.22-alpine AS builder
RUN apk add --no-cache git
WORKDIR /app

COPY imperative/go-imperative/go.mod imperative/go-imperative/go.sum ./
RUN go mod download || true

COPY imperative/go-imperative/ .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags "-s -w" \
    -trimpath \
    -o /go-imperative \
    ./cmd/server

FROM scratch
COPY --from=builder /go-imperative /go-imperative
EXPOSE 8093

ENV PORT=8093
ENV MAX_COUNT=200000
ENV CONCURRENCY=128
ENV DOWNSTREAM_TIMEOUT_MS=2000
ENV RETRY_ATTEMPTS=1
ENV LOG_LEVEL=INFO

ENTRYPOINT ["/go-imperative"]


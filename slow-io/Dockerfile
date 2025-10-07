# Etapa de build
FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY slow-io/go.mod .
RUN go mod download
COPY slow-io/. .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /slow-io

# Etapa final (imagem pequena)
FROM alpine:3.20
ENV PORT=8080 \
    BASE_DELAY_MS=50 \
    INSTANCE_ID=""
EXPOSE 8080
COPY --from=builder /slow-io /usr/local/bin/slow-io
ENTRYPOINT ["/usr/local/bin/slow-io"]

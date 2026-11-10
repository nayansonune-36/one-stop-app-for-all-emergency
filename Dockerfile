# Multi-stage build for S.E.N.T.I.N.E.L. Go Backend
FROM golang:1.22-alpine AS builder

WORKDIR /src

# Copy Go module definition
COPY backend/go.mod ./
RUN go mod download

# Copy source files
COPY backend/ ./

# Build statically compiled binary with stripped debug symbols
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /bin/sentinel-backend .

# Production runtime container
FROM alpine:3.20

RUN apk --no-cache add ca-certificates tzdata

WORKDIR /app

COPY --from=builder /bin/sentinel-backend /app/sentinel-backend
COPY --from=builder /src/prompts /app/prompts

RUN chmod -R 755 /app

EXPOSE 8080

ENV PORT=8080

USER nobody:nobody

ENTRYPOINT ["/app/sentinel-backend"]

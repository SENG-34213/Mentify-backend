# Redis integration

Redis is an optional shared backend capability for caching, short-lived coordination,
and idempotency. It is not the source of truth for business data and this change does
not implement API rate limiting.

## Local startup

Start Redis with the cache profile:

```bash
docker compose --profile app-cache up -d redis
```

Configure the services through `.env`:

```dotenv
REDIS_ENABLED=true
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=change-me-redis-password
REDIS_DATABASE=0
```

The default key namespace is `mentify:v1`. Cache entries have a TTL and database-backed
requests fall back safely when Redis is unavailable. Redis locks are only coordination
helpers; database transactions and constraints remain authoritative.

## Shared services

`common-lib` exposes:

- `RedisCacheService` for JSON cache-aside reads, writes, TTLs, and invalidation.
- `RedisLockService` for token-owned short-lived distributed locks.
- `RedisIdempotencyService` for short-lived operation claims and results.
- `RedisKey` for namespaced key construction.

The notification service currently caches notification pages and unread counts and
invalidates those entries after ingestion, read, bulk-read, and deletion operations.

## Production requirements

- Use a managed Redis deployment or a protected private Redis network.
- Replace the development password and enable TLS where supported.
- Monitor memory, evictions, command latency, connection failures, and hit ratio.
- Keep sensitive values out of keys and cache private responses with user-scoped keys.
- Use Redis Pub/Sub or Streams for WebSocket fan-out only when multi-instance deployment
  is enabled.

# Flash Sale Lab — Distributed Software Engineering

A hands-on lab project for the **Distributed Software Engineering** course. The goal is to build a simplified high-concurrency flash-sale (limited-time purchase) system that demonstrates key distributed concepts such as cache pre-loading, optimistic locking, and idempotent order placement.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| ORM | MyBatis 3 (annotation-based) |
| Database | MySQL 8 |
| Cache | Redis |
| Build | Maven |

## Quick Start

```bash
# 1. Create the database
mysql -u root -p < docs/schema.sql

# 2. Start Redis
redis-server

# 3. Run the application
mvn spring-boot:run
```

Open `http://localhost:9090/login.html` in your browser.

## Database Design

The system uses **six tables** following a hot/cold data separation pattern:

| Table | Purpose |
|-------|---------|
| `t_member` | Registered members (salted MD5 password) |
| `t_seller` | Seller / shop information |
| `t_product` | Regular product catalog |
| `t_flash_item` | Flash-sale product config (with `version` for optimistic lock) |
| `t_trade_order` | Completed trade orders |
| `t_flash_guard` | Idempotency guard — unique index on `(member_id, product_id)` |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/v1/account/sign-up` | Member registration |
| `POST` | `/v1/account/sign-in` | Member login |
| `GET` | `/v1/product/flash/active` | List active flash-sale products |
| `POST` | `/v1/flash/snap` | Place a flash-sale order |
| `GET` | `/v1/flash/my-orders` | Query member's order history |

## Architecture Highlights

1. **Redis inventory pre-loading** — On startup, all active flash items' stock counts are cached in Redis to avoid hitting the DB on every request.
2. **Two-phase stock deduction** — Redis `DECR` acts as a fast-path filter; the database uses an optimistic-lock (`version` field) CAS update as the source of truth.
3. **Triple idempotency guard** — Redis Set check → DB unique-index constraint → transactional rollback ensures no duplicate orders.

## License

For educational use only.

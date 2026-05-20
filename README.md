# jobskillsmatcher-be

Spring Boot backend for Job Skills Matcher.

## Run it with Docker

Local dev (just spins up Postgres + pgAdmin so you can run the app from your IDE):

```bash
docker compose up -d
```

- Postgres on `localhost:5433` (user `jsm`, password `jsm`, db `jobskillsmatcher`)
- pgAdmin on http://localhost:5050 (`admin@jsm.local` / `admin`)

Full stack (backend + db together, used for deploys):

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

## Environment variables

Drop these in a `.env` file at the repo root before running the prod compose. Only `JWT_SECRET` is strictly required — the rest have sensible defaults.

| Variable | What it does | Default |
|---|---|---|
| `JWT_SECRET` | Signing secret for JWTs. **Must be ≥ 32 chars.** | _required_ |
| `JWT_EXPIRY_HOURS` | Token lifetime | `24` |
| `DB_NAME` / `DB_USER` / `DB_PASS` | Postgres credentials | `jobskillsmatcher` / `jsm` / `jsm` |
| `PUBLIC_ORIGIN` | Frontend origin allowed by CORS | `http://localhost` |
| `PUBLIC_PORT` | Host port the web container binds to | `80` |
| `RAPIDAPI_KEY` | For external job-listing APIs | empty |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth sign-in | empty |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASS` | SMTP for notifications | `localhost:25` |

If you skip `JWT_SECRET` the prod compose will refuse to start, which is on purpose.

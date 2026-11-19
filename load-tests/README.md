# Load tests (k6)

Scripts to measure how the back end behaves with many people at the same time. Run them **only on your own
machine** (Docker), never against a published site.

## Scenarios

| Script | What it does | Needs a session |
| --- | --- | --- |
| `buscar.js` | Caregiver list, one profile and the "busy caregivers" lookup | No (public) |
| `login.js` | `POST /login` | No |
| `solicitudes.js` | A family creates requests (`POST /solicitudes`) and reads `GET /solicitudes/mis-solicitudes` | Yes (logs in once) |

Each script goes up slowly from 10 to 100 virtual users (about 5 minutes) and fails if more than 1% of the
requests fail or if the 95th percentile (p95) is too slow. Use `PERFIL=rapido` for a 20 second smoke test.

## Before running

1. Start the database and the back end (`docker compose up -d` and `./mvnw spring-boot:run`).
2. The login is limited by IP (5 per minute). For the test, start the back end with a high limit:
   `RATE_LIMIT_MAX_REQUESTS=1000000` (and put it back to normal afterwards).
3. Make sure there is at least one caregiver profile and a family user. By default the scripts use
   `familia@test.com` / `Password123!`; change them with `FAMILIA_EMAIL` and `FAMILIA_PASSWORD`.

## Running with k6 installed

```
k6 run load-tests/buscar.js
k6 run -e PERFIL=rapido load-tests/login.js
k6 run --summary-export=load-tests/resultados/solicitudes.json load-tests/solicitudes.js
```

## Running with Docker (nothing to install)

From the root of the back end repo, in Git Bash on Windows:

```
MSYS_NO_PATHCONV=1 docker run --rm -i -v "$PWD/load-tests:/scripts" \
  -e BASE_URL=http://host.docker.internal:8080/api/v1 \
  grafana/k6 run --summary-export=/scripts/resultados/buscar.json /scripts/buscar.js
```

Use `-e PERFIL=rapido` to make it short, and change `buscar.js` for the other scripts. The `resultados/` folder is
ignored by Git: copy the numbers that matter (average, p95, errors) to the README or the wiki.

## Cleaning up

`solicitudes.js` stores real requests. When you finish, reset the local data (for example
`docker compose down -v` and start again).

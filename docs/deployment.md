# Deployment guide (back end)

How to run the Acompáñame back end on a server with Docker Compose.

```
Internet ──► Caddy (80/443, HTTPS) ──► back (Spring Boot, :8080)  ──► MySQL (:3306)
                 only /api/*             internal network only        internal network only
```

Only Caddy is reachable from the internet. MySQL and the back end live on an internal Docker
network: port 3306 is **not** published, and `/actuator/health` is **not** proxied.

The front end is deployed separately (Cloudflare Pages) and talks to `https://<API_DOMAIN>/api/v1`.

## Files

| File | What it is |
| --- | --- |
| `Dockerfile` | Builds the back-end image (always starts with the `prod` profile). |
| `docker-compose.prod.yml` | MySQL + back end + Caddy. |
| `deploy/Caddyfile` | HTTPS and reverse proxy for `/api/*` only. |
| `.env.prod.example` | Every variable the production profile needs. Copy it to `.env` on the server. |
| `docs/sql/schema-completo.sql` | Full schema. MySQL runs it the first time the data volume is empty. |
| `deploy/backup-mysql.sh` | Daily `mysqldump` backup with 14-day retention (reads `.env`, or `ENV_FILE=<file>`). |

## Server requirements

* Linux with Docker Engine and the Compose plugin (`docker compose version`).
* Ports 80 and 443 open (firewall and, on Oracle Cloud, the VCN security list).
* RAM: **2 GB or more recommended** (MySQL ~400 MB + JVM ~600 MB). With 1 GB set
  `BACKEND_MEM_LIMIT=512m` and add swap.
* A DNS `A` record for the API domain (for example `api.example.com`) pointing to the server IP.
  Caddy requests the HTTPS certificate by itself, but only once the DNS record resolves to the server.

## First deployment

```bash
git clone https://github.com/AndreaVaGo/acompaname-backend.git
cd acompaname-backend

cp .env.prod.example .env
nano .env                      # fill in EVERY value (see comments in the file)
chmod 600 .env

docker compose -f docker-compose.prod.yml up -d --build
```

The first build takes several minutes (it downloads the Maven dependencies).

Check it:

```bash
docker compose -f docker-compose.prod.yml ps          # mysql, backend: healthy; caddy: running
docker compose -f docker-compose.prod.yml logs backend --tail 20   # "Started AcompanameBackendApplication"
curl https://<API_DOMAIN>/api/v1/roles                # 200 with the roles
curl -i https://<API_DOMAIN>/actuator/health          # must be 404 (not published)
```

Things that must be true:

* `JPA_DDL_AUTO` is **not** set: production runs with `validate`, so the schema must match the code.
* The administrator account is created at startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Sign in,
  change the password from the web app, then remove `ADMIN_PASSWORD` from `.env`.
* `FICHAJE_VERIFICAR_LUGAR` stays `false` until the privacy policy explains it.

## Updating

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

If the release changes the schema, apply the migration script from `docs/sql/` **before** starting
the new version (production uses `validate`: a mismatch stops the application at startup, it never
alters tables). Whoever changes an entity also updates `docs/sql/schema-completo.sql`.

## Backups

```bash
sh deploy/backup-mysql.sh                     # creates backups/acompaname-YYYY-MM-DD-HHMM.sql.gz
crontab -e                                    # every night at 03:30:
30 3 * * * sh /home/<user>/acompaname-backend/deploy/backup-mysql.sh >> /home/<user>/backup.log 2>&1
```

* Backups contain personal data: the folder is `700`, files are `600`, and `backups/` is git-ignored.
* Also copy them **off the server** (another machine or an object-storage bucket). If the server is lost,
  backups stored on it are lost too.
* Accounts deleted from the app (anonymised) can still exist in backups until they expire (14 days).

Restore (the dump contains `DROP TABLE IF EXISTS` + `CREATE TABLE` + the data, so it replaces the tables):

```bash
docker compose -f docker-compose.prod.yml stop backend
gunzip -c backups/acompaname-<date>.sql.gz | docker compose -f docker-compose.prod.yml exec -T mysql \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
docker compose -f docker-compose.prod.yml start backend
```

To rehearse it without touching real data, restore into a throw-away database
(`CREATE DATABASE restaurada;` and use it instead of `"$MYSQL_DATABASE"`) and run `SHOW TABLES`.
Try a restore once before you need it.

## Pending decisions and tasks

* **Log retention of 30 days.** The privacy policy says IP logs are kept 30 days. Docker rotates logs by
  **size** (`20m` × 3 files per service), which does not guarantee 30 days. Add a daily time-based
  rotation on the host (for example `logrotate` with `daily`, `rotate 30` and `copytruncate` over
  `/var/lib/docker/containers/*/*-json.log`) and check the result.
* **Name the providers** (hosting and SMTP) in the privacy policy.
* **Front end:** Cloudflare Pages build with `VITE_API_URL=https://<API_DOMAIN>/api/v1` and
  `VITE_TURNSTILE_SITE_KEY`; the same front address goes in `CORS_ORIGINS` and `FRONTEND_URL`.
* **Single instance.** The nightly clean-up task and the in-memory rate limiter assume one back-end
  instance. Do not scale the `backend` service to several replicas without moving them out of memory.

#!/bin/sh
# Copia de seguridad de la base de datos de producción: un .sql.gz con fecha en backups/.
# Se ejecuta desde cualquier sitio:   sh deploy/backup-mysql.sh
# Por defecto lee las variables de .env (la raíz del proyecto). Otro archivo:  ENV_FILE=.env.otro sh deploy/backup-mysql.sh
# Cada noche con cron (3:30):         30 3 * * * sh /ruta/a/acompaname-backend/deploy/backup-mysql.sh
#
# Las copias llevan datos personales: la carpeta backups/ es solo para quien administra el servidor
# (permiso 700) y NO se sube a GitHub. Hay que copiarlas también FUERA del servidor
# (si el servidor se pierde, las copias que están dentro se pierden con él).
set -eu

cd "$(dirname "$0")/.."
DIAS_QUE_SE_GUARDAN=14
ENV_FILE="${ENV_FILE:-.env}"

mkdir -p backups
chmod 700 backups
DESTINO="backups/acompaname-$(date +%Y-%m-%d-%H%M).sql.gz"

# Las variables salen del propio contenedor de MySQL (no hace falta leer el .env aquí).
# --single-transaction: copia coherente sin bloquear la base de datos mientras la app sigue funcionando
docker compose --env-file "$ENV_FILE" -f docker-compose.prod.yml exec -T mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines "$MYSQL_DATABASE"' \
  | gzip > "$DESTINO"

# Una copia vacía o diminuta es una copia rota: se avisa en vez de darla por buena
if [ "$(wc -c < "$DESTINO")" -lt 1000 ]; then
  echo "ERROR: la copia $DESTINO está vacía o incompleta" >&2
  rm -f "$DESTINO"
  exit 1
fi

chmod 600 "$DESTINO"
find backups -name 'acompaname-*.sql.gz' -mtime +"$DIAS_QUE_SE_GUARDAN" -delete
echo "Copia creada: $DESTINO"

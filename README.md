# easyschedule

monorepo con:

- `backend/`: spring boot + gradle
- `frontend/`: angular

## versiones recomendadas

- java: `17`
- node: `20.19.0`
- npm: `10.8.2`
- angular cli: `20.3.19`

## 1) requisitos previos

verifica versiones:

```bash
java -version
node -v
npm -v
```

## 2) base de datos postgresql

la app espera, por defecto:

- db: `easyschedule`
- usuario: `postgres`
- password: `postgres`
- puerto: `5432`

crea la base manualmente:

```sql
create database "easyschedule";
```

variables disponibles (opcionales):

- `db_url`
- `db_username`
- `db_password`
- `jpa_ddl_auto`
- `cors_allowed_origins`

referencia: `backend/.env.example`

### ejecutar schema + seeds

al iniciar el backend, los seeds se ejecutan automaticamente solo si la base esta vacia (o si el esquema no existe).
si ya hay datos, se omiten.

tambien puedes ejecutarlos manualmente bajo demanda:

```bash
cd backend
chmod +x run-seeds.sh
./run-seeds.sh
```

variables opcionales para el script:

- `db_host` (default: `localhost`)
- `db_port` (default: `5432`)
- `db_name` (default: `easyschedule`)
- `db_user` (default: `postgres`)
- `db_password` (default: `postgres`)
- `with_schema` (default: `true`)

## 3) levantar backend

```bash
cd backend
./gradlew bootrun
```

en windows powershell, si `./gradlew` no funciona:

```powershell
.\gradlew.bat bootrun
```

backend: `http://localhost:8080`

aclaracion: al iniciar el servicio, es normal que la consola de logs se detenga alrededor del 80%. Este comportamiento es propio de Spring Boot durante el arranque; no indica un error y el servicio ya se encuentra en ejecución.

pruebas manuales de endpoints (http files):

- `backend/src/main/resources/http/test.http`
- `backend/src/main/resources/http/estudiante.http`

## 4) levantar frontend

```bash
cd frontend
npm ci
npm start
```

frontend: `http://localhost:4200`

## 5) verificaciones rapidas

```bash
cd backend
./gradlew test

cd ../frontend
npm test -- --watch=false --browsers=chromeheadless
npm run build
```

## 6) coverage

backend (jacoco):

```bash
cd backend
./gradlew test jacocotestreport
```

frontend (karma/istanbul):

```bash
cd frontend
npm test -- --watch=false --browsers=chromeheadless --code-coverage
```

reportes:

- backend: `backend/build/reports/jacoco/test/html/index.html`
- frontend: `frontend/coverage/frontend/index.html`

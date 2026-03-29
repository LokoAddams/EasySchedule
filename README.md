# EasySchedule

Monorepo con:

- `backend/`: Spring Boot + Gradle
- `frontend/`: Angular

## Versiones recomendadas

- Java: `17`
- Node: `20.19.0`
- npm: `10.8.2`
- Angular CLI: `20.3.19`

## 1) Requisitos previos

Verifica versiones:

```bash
java -version
node -v
npm -v
```

## 2) Base de datos PostgreSQL

La app espera, por defecto:

- DB: `EasySchedule`
- Usuario: `postgres`
- Password: `postgres`
- Puerto: `5432`

Crea la base manualmente:

```sql
CREATE DATABASE "EasySchedule";
```

Variables disponibles (opcionales):

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JPA_DDL_AUTO`
- `CORS_ALLOWED_ORIGINS`

Referencia: `backend/.env.example`

## 3) Levantar backend

```bash
cd backend
./gradlew bootRun
```

En Windows PowerShell, si `./gradlew` no funciona:

```powershell
.\gradlew.bat bootRun
```

Backend: `http://localhost:8080`

Pruebas manuales de endpoints (HTTP files):

- `backend/src/main/resources/http/test.http`
- `backend/src/main/resources/http/estudiante.http`

## 4) Levantar frontend

```bash
cd frontend
npm ci
npm start
```

Frontend: `http://localhost:4200`

## 5) Verificaciones rapidas

```bash
cd backend
./gradlew test

cd ../frontend
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```

## 6) Coverage

Backend (JaCoCo):

```bash
cd backend
./gradlew test jacocoTestReport
```

Frontend (Karma/Istanbul):

```bash
cd frontend
npm test -- --watch=false --browsers=ChromeHeadless --code-coverage
```

Reportes:

- Backend: `backend/build/reports/jacoco/test/html/index.html`
- Frontend: `frontend/coverage/frontend/index.html`

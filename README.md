# EasySchedule

Monorepo con:

- `backend/`: Spring Boot + Gradle
- `frontend/`: Angular

## Versiones objetivo

- Java: 17.0.18
- Node: 20.19.0
- npm: 10.8.2
- Angular CLI: 20.3.19

## 1) Preparar entorno

### Node y npm

Verifica versiones instaladas:

```bash
node -v
npm -v
```

Versiones requeridas:

- Node: 20.19.0
- npm: 10.8.2

### Java 17

El backend usa toolchain Java 17. Verifica:

```bash
java -version
```

Si tienes varias versiones de Java, usa Java 17 para ejecutar Gradle/Spring Boot.

### PostgreSQL

Config por defecto en `backend/src/main/resources/application.properties`:

- DB: `EasySchedule`
- Usuario: `postgres`
- Password: `postgres`

Puedes sobrescribir por variables de entorno:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JPA_DDL_AUTO`
- `CORS_ALLOWED_ORIGINS`

Referencia: `backend/.env.example`

## 2) Levantar backend

```bash
cd backend
./gradlew bootRun
```

Healthcheck: `http://localhost:8080/health`

## 3) Levantar frontend

```bash
cd frontend
npm install
npm start
```

Frontend: `http://localhost:4200`

## 4) Verificaciones rapidas

```bash
cd frontend && npm run build

cd backend && ./gradlew test
```

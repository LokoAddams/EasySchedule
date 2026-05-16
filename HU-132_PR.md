# PR - HU-132: Validar Carnet de Identidad en Perfil (Bolivia)

## Contexto

Esta historia implementa una validacion de carnet de identidad alineada a formatos usados en Bolivia para el modulo de perfil.

Se reemplazo la regla anterior (muy generica) por una validacion mas realista, consistente entre frontend y backend, con mensajes claros para el usuario y normalizacion del valor antes de persistir.

## Historia de Usuario

**Como** estudiante autenticado  
**Quiero** que el campo Carnet de Identidad solo permita formato valido  
**Para** mantener consistencia e integridad de los datos

## Ajuste aplicado a criterios de aceptacion

Se actualizo la interpretacion del criterio "solo digitos" para contemplar formato CI Bolivia real:

- Base numerica obligatoria de **6 a 10 digitos**.
- Complemento opcional de **1 a 2** caracteres alfanumericos (ej: `-1A` o `1A`).
- Extension opcional de departamento: `LP, CB, SC, OR, PT, TJ, CH, BN, PD`.
- Rechazo de formatos alfabeticos, mixtos invalidos, extensiones no permitidas y simbolos fuera de formato.

## Formato soportado

Patron funcional:

`[6-10 digitos][complemento opcional][extension opcional]`

Ejemplos validos:

- `123456`
- `1234567`
- `1234567-1A`
- `1234567 LP`
- `1234567-1A LP`

Ejemplos invalidos:

- `93267` (5 digitos)
- `9326` (4 digitos)
- `abc123`
- `1234567 XX`
- `12@3456`

## Cambios tecnicos

### Frontend

Archivos modificados:

- `frontend/src/app/features/perfil/perfil-validators.ts`
- `frontend/src/app/features/perfil/perfil.ts`
- `frontend/src/app/features/perfil/perfil.spec.ts`
- `frontend/src/assets/i18n/es.json`
- `frontend/src/assets/i18n/en.json`
- `frontend/src/assets/i18n/pt.json`

Cambios clave:

- Nueva regex para CI Bolivia en `carnetIdentidadValidator`.
- Longitud maxima ajustada a 16 para formatos con complemento/ext.
- Normalizacion antes de envio: trim, espacios simples y mayusculas.
- Manejo especifico de `400` por formato de carnet invalido con toast dedicado.
- Mensajes i18n actualizados en ES/EN/PT con ejemplos claros.
- Cobertura de pruebas para casos validos e invalidos del carnet.

### Backend

Archivos modificados:

- `backend/src/main/java/com/easyschedule/backend/estudiante/dto/PerfilUpdateRequest.java`
- `backend/src/main/java/com/easyschedule/backend/estudiante/dto/EstudianteUpdateRequest.java`
- `backend/src/main/java/com/easyschedule/backend/estudiante/service/EstudianteService.java`
- `backend/src/test/java/com/easyschedule/backend/estudiante/controller/EstudianteControllerTest.java`
- `backend/src/test/java/com/easyschedule/backend/estudiante/service/EstudianteServiceTest.java`

Cambios clave:

- Se agrego validacion por `@Pattern` para formato CI Bolivia.
- Se ajusto `@Size` a `min=6` y `max=16`.
- Normalizacion de carnet al guardar: trim, espacios simples y mayusculas.
- Tests de controller para rechazo de extension invalida.
- Tests de service para verificar normalizacion (`lp` -> `LP`).

## Pruebas ejecutadas

### Frontend

Comando:

```bash
npm test -- --watch=false --browsers=ChromeHeadless --include="src/app/features/perfil/perfil.spec.ts"
```

Resultado:

- 10 tests ejecutados
- 10 tests exitosos

### Backend

Comando:

```bash
./gradlew test --tests "com.easyschedule.backend.estudiante.controller.EstudianteControllerTest" --tests "com.easyschedule.backend.estudiante.service.EstudianteServiceTest"
```

Resultado:

- Build exitoso
- Tests de controller y service en verde

## Validacion manual recomendada (QA)

- [ ] Guardar con `123456` (valido).
- [ ] Guardar con `1234567-1A LP` (valido).
- [ ] Verificar normalizacion a mayusculas (`1234567 lp` -> `1234567 LP`).
- [ ] Probar `93267` y confirmar rechazo (minimo 6 digitos).
- [ ] Probar `abc123` y confirmar rechazo.
- [ ] Probar `1234567 XX` y confirmar rechazo.
- [ ] Verificar mensaje comprensible cuando backend responde formato invalido.

## Evidencia visual

> El autor del PR adjuntara capturas en GitHub.

Capturas sugeridas:

1. Caso valido con extension (`1234567-1A LP`).
2. Caso invalido de 5 digitos (`93267`).
3. Caso invalido con extension incorrecta (`XX`).
4. Confirmacion de guardado exitoso y valor normalizado.

## Riesgo e impacto

- **Impacto esperado:** Bajo-medio, acotado al flujo de perfil y validacion de DTOs.
- **Riesgo principal:** Datos antiguos con formato no compatible podrian requerir correccion manual al editar.
- **Mitigacion:** mensajes claros de error + normalizacion automatica de formato permitido.

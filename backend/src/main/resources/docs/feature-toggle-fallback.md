# Feature Toggle Fallback

Si la lectura de feature toggles falla temporalmente, el backend no debe cortar el servicio completo. El sistema intenta leer la configuracion activa en cada request y, ante una excepcion, registra un evento `FEATURE_TOGGLES_FALLBACK` en logs y aplica defaults seguros.

Defaults seguros:

- `malla`: habilitado, porque permite mantener disponible la configuracion academica principal.
- `tomaMaterias`: habilitado, porque evita indisponibilidad del flujo academico principal.
- `ofertasImport`: deshabilitado, porque la importacion modifica datos academicos de forma masiva y es mas segura apagada ante incertidumbre.

El fallback no se cachea. Cuando la configuracion vuelve a estar disponible, la siguiente lectura usa automaticamente los valores configurados.

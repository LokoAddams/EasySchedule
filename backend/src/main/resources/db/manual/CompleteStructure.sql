BEGIN;

-- =========================================================
-- EXTENSION UTIL
-- =========================================================
-- Útil para que username y correo sean case-insensitive.
CREATE EXTENSION IF NOT EXISTS citext;

-- =========================================================
-- TABLA: malla
-- =========================================================
CREATE TABLE malla (
    id              BIGSERIAL PRIMARY KEY,
    carrera         VARCHAR(120) NOT NULL,
    universidad     VARCHAR(150) NOT NULL,
    version         VARCHAR(30)  NOT NULL,

    CONSTRAINT uq_malla UNIQUE (carrera, universidad, version)
);

COMMENT ON TABLE malla IS 'Malla curricular base.';
COMMENT ON COLUMN malla.carrera IS 'Carrera a la que pertenece la malla.';
COMMENT ON COLUMN malla.universidad IS 'Universidad mostrada en la interfaz.';
COMMENT ON COLUMN malla.version IS 'Versión de la malla, 
ya que una misma carrera tiene varias mallas, por ejemplo: 2024, 2017,etc.';

INSERT INTO malla (carrera, universidad, version)
VALUES ('carrera default', 'universidad default', 'version default')
ON CONFLICT (carrera, universidad, version) DO NOTHING;


-- =========================================================
-- TABLA: estudiante
-- =========================================================
CREATE TABLE estudiante (
    id                  BIGSERIAL PRIMARY KEY,
    username            CITEXT NOT NULL UNIQUE,
    nombre              VARCHAR(100) NOT NULL,
    apellido            VARCHAR(100) NOT NULL,
    correo              CITEXT NOT NULL UNIQUE,
    password_hash       TEXT NOT NULL,
    carnet_identidad    VARCHAR(30) NOT NULL UNIQUE,
    fecha_nacimiento    DATE NOT NULL,
    fecha_registro      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    semestre_actual     SMALLINT NOT NULL CHECK (semestre_actual BETWEEN 1 AND 50),
    carrera             VARCHAR(120) NOT NULL,
    malla_id            BIGINT NOT NULL,

    -- Cardinalidad: muchos estudiantes -> una malla
    CONSTRAINT fk_estudiante_malla
        FOREIGN KEY (malla_id)
        REFERENCES malla(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

COMMENT ON TABLE estudiante IS 'Usuario estudiante del sistema.';
COMMENT ON COLUMN estudiante.semestre_actual IS 'Semestre actual del estudiante.';
COMMENT ON COLUMN estudiante.carrera IS 'Se mantiene redundante para mostrar y validar con la malla.';
COMMENT ON COLUMN estudiante.malla_id IS 'FK a la malla seleccionada por el estudiante.';

CREATE INDEX idx_estudiante_malla_id ON estudiante(malla_id);


-- =========================================================
-- TABLA: materia
-- =========================================================
CREATE TABLE materia (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(30) NOT NULL UNIQUE,
    nombre              VARCHAR(150) NOT NULL,
    semestre_sugerido   SMALLINT NOT NULL CHECK (semestre_sugerido BETWEEN 1 AND 50),
    creditos            SMALLINT NOT NULL CHECK (creditos > 0)
);

COMMENT ON TABLE materia IS 'Catálogo base de materias.';
COMMENT ON COLUMN materia.semestre_sugerido IS 'Semestre sugerido dentro de una malla.';
COMMENT ON COLUMN materia.creditos IS 'Cantidad de créditos de la materia.';


-- =========================================================
-- TABLA: malla_materia
-- =========================================================
CREATE TABLE malla_materia (
    id          BIGSERIAL PRIMARY KEY,
    malla_id    BIGINT NOT NULL,
    materia_id  BIGINT NOT NULL,

    -- Cardinalidad: muchas filas de unión -> una malla
    CONSTRAINT fk_malla_materia_malla_id
        FOREIGN KEY (malla_id)
        REFERENCES malla(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    -- Cardinalidad: muchas filas de unión -> una materia
    CONSTRAINT fk_malla_materia_materia_id
        FOREIGN KEY (materia_id)
        REFERENCES materia(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT uq_malla_materia UNIQUE (malla_id, materia_id)
);

COMMENT ON TABLE malla_materia IS 'Tabla puente para la relación N:M entre malla y materia.';

CREATE INDEX idx_malla_materia_malla_id   ON malla_materia(malla_id);
CREATE INDEX idx_malla_materia_materia_id ON malla_materia(materia_id);


-- =========================================================
-- TABLA: prerequisitos
-- =========================================================
CREATE TABLE prerequisitos (
    id                  BIGSERIAL PRIMARY KEY,
    id_materia          BIGINT NOT NULL,
    id_prerequisito     BIGINT NOT NULL,

    -- Cardinalidad: muchas relaciones de prerequisito -> una materia destino
    CONSTRAINT fk_prereq_materia
        FOREIGN KEY (id_materia)
        REFERENCES materia(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    -- Cardinalidad: muchas relaciones de prerequisito -> una materia prerequisito
    CONSTRAINT fk_prereq_prerequisito
        FOREIGN KEY (id_prerequisito)
        REFERENCES materia(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT ck_prereq_distintos CHECK (id_materia <> id_prerequisito),
    CONSTRAINT uq_prereq UNIQUE (id_materia, id_prerequisito)
);

COMMENT ON TABLE prerequisitos IS 'Autorelación N:M entre materias y sus prerequisitos.';

CREATE INDEX idx_prereq_materia_id      ON prerequisitos(id_materia);
CREATE INDEX idx_prereq_prerequisito_id ON prerequisitos(id_prerequisito);


-- =========================================================
-- TABLA: toma_materia
-- =========================================================
CREATE TABLE toma_materia (
    id                  BIGSERIAL PRIMARY KEY,
    estudiante_id       BIGINT NOT NULL,
    materia_id          BIGINT NOT NULL,
    estado              VARCHAR(20) NOT NULL,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Cardinalidad: muchas tomas -> un estudiante
    CONSTRAINT fk_toma_materia_estudiante
        FOREIGN KEY (estudiante_id)
        REFERENCES estudiante(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    -- Cardinalidad: muchas tomas -> una materia
    CONSTRAINT fk_toma_materia_materia
        FOREIGN KEY (materia_id)
        REFERENCES materia(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT ck_toma_materia_estado
        CHECK (estado IN ('aprobada', 'pendiente', 'cursando')),

    CONSTRAINT uq_toma_materia UNIQUE (estudiante_id, materia_id)
);

COMMENT ON TABLE toma_materia IS 'Estado académico del estudiante respecto a una materia.';

CREATE INDEX idx_toma_materia_estudiante_id ON toma_materia(estudiante_id);
CREATE INDEX idx_toma_materia_materia_id    ON toma_materia(materia_id);


-- =========================================================
-- TABLA: ofertas
-- =========================================================
CREATE TABLE ofertas (
    id                  BIGSERIAL PRIMARY KEY,
    estudiante_id       BIGINT NOT NULL,
    materia_id          BIGINT NOT NULL,
    semestre            VARCHAR(30) NOT NULL,
    horario_json        JSONB NOT NULL,
    docente             VARCHAR(150),
    aula                VARCHAR(100),
    fecha_creacion      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Cardinalidad: muchas ofertas -> un estudiante
    CONSTRAINT fk_ofertas_estudiante
        FOREIGN KEY (estudiante_id)
        REFERENCES estudiante(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    -- Cardinalidad: muchas ofertas -> una materia
    CONSTRAINT fk_ofertas_materia
        FOREIGN KEY (materia_id)
        REFERENCES materia(id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT uq_ofertas UNIQUE (estudiante_id, materia_id, semestre),
    CONSTRAINT ck_ofertas_horario_json_array
        CHECK (jsonb_typeof(horario_json) = 'array')
);

COMMENT ON TABLE ofertas IS 'Ofertas cargadas manualmente por cada estudiante.';
COMMENT ON COLUMN ofertas.horario_json IS 'JSONB con arreglo de bloques: [{"dia":"Lunes","inicio":"07:00","fin":"08:45"}].';

CREATE INDEX idx_ofertas_estudiante_id ON ofertas(estudiante_id);
CREATE INDEX idx_ofertas_materia_id    ON ofertas(materia_id);
CREATE INDEX idx_ofertas_semestre      ON ofertas(semestre);

-- Útil si luego quieres buscar dentro del JSON
CREATE INDEX idx_ofertas_horario_json_gin ON ofertas USING GIN (horario_json);


-- =========================================================
-- TABLA: horarios_recomendados
-- =========================================================
CREATE TABLE horarios_recomendados (
    id                  BIGSERIAL PRIMARY KEY,
    estudiante_id       BIGINT NOT NULL,
    semestre            VARCHAR(30) NOT NULL,
    json_resultado      JSONB NOT NULL,
    fecha_generacion    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Cardinalidad: muchos horarios recomendados -> un estudiante
    CONSTRAINT fk_horarios_recomendados_estudiante
        FOREIGN KEY (estudiante_id)
        REFERENCES estudiante(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

COMMENT ON TABLE horarios_recomendados IS 'Respuestas de horarios generadas por la IA.';
COMMENT ON COLUMN horarios_recomendados.json_resultado IS 'JSONB con el horario recomendado completo devuelto por la IA.';

CREATE INDEX idx_horarios_recomendados_estudiante_id ON horarios_recomendados(estudiante_id);
CREATE INDEX idx_horarios_recomendados_semestre      ON horarios_recomendados(semestre);
CREATE INDEX idx_horarios_recomendados_json_gin      ON horarios_recomendados USING GIN (json_resultado);


-- =========================================================
-- FUNCION GENERICA: actualizar fecha_actualizacion
-- =========================================================
CREATE OR REPLACE FUNCTION fn_set_fecha_actualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =========================================================
-- TRIGGER: actualizar fecha_actualizacion en toma_materia
-- =========================================================
CREATE TRIGGER trg_toma_materia_fecha_actualizacion
BEFORE UPDATE ON toma_materia
FOR EACH ROW
EXECUTE FUNCTION fn_set_fecha_actualizacion();


-- =========================================================
-- TRIGGER: actualizar fecha_actualizacion en ofertas
-- =========================================================
CREATE TRIGGER trg_ofertas_fecha_actualizacion
BEFORE UPDATE ON ofertas
FOR EACH ROW
EXECUTE FUNCTION fn_set_fecha_actualizacion();


-- =========================================================
-- FUNCION: validar que carrera de estudiante coincida con malla
-- =========================================================
CREATE OR REPLACE FUNCTION fn_validar_carrera_estudiante_malla()
RETURNS TRIGGER AS $$
DECLARE
    v_carrera_malla VARCHAR(120);
BEGIN
    SELECT carrera
    INTO v_carrera_malla
    FROM malla
    WHERE id = NEW.malla_id;

    IF v_carrera_malla IS NULL THEN
        RAISE EXCEPTION 'La malla % no existe.', NEW.malla_id;
    END IF;

    IF NEW.carrera <> v_carrera_malla THEN
        RAISE EXCEPTION
            'La carrera del estudiante (%) debe coincidir con la carrera de la malla (%).',
            NEW.carrera, v_carrera_malla;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_validar_carrera_estudiante_malla
BEFORE INSERT OR UPDATE OF carrera, malla_id ON estudiante
FOR EACH ROW
EXECUTE FUNCTION fn_validar_carrera_estudiante_malla();


-- =========================================================
-- FUNCION: sincronizar carrera de estudiantes si cambia la malla
-- =========================================================
CREATE OR REPLACE FUNCTION fn_sincronizar_carrera_desde_malla()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE estudiante
    SET carrera = NEW.carrera
    WHERE malla_id = NEW.id
      AND carrera IS DISTINCT FROM NEW.carrera;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_sincronizar_carrera_desde_malla
AFTER UPDATE OF carrera ON malla
FOR EACH ROW
EXECUTE FUNCTION fn_sincronizar_carrera_desde_malla();




-- =========================================================
-- FUNCION: validar estructura del horario_json
-- Cada elemento debe tener:
--   dia: texto válido
--   inicio: HH:MM
--   fin: HH:MM
-- =========================================================
CREATE OR REPLACE FUNCTION fn_validar_horario_json()
RETURNS TRIGGER AS $$
DECLARE
    v_bloque JSONB;
    v_dia    TEXT;
    v_inicio TEXT;
    v_fin    TEXT;
BEGIN
    IF jsonb_typeof(NEW.horario_json) <> 'array' THEN
        RAISE EXCEPTION 'horario_json debe ser un arreglo JSON.';
    END IF;

    FOR v_bloque IN
        SELECT value
        FROM jsonb_array_elements(NEW.horario_json)
    LOOP
        IF jsonb_typeof(v_bloque) <> 'object' THEN
            RAISE EXCEPTION 'Cada bloque de horario_json debe ser un objeto.';
        END IF;

        IF NOT (v_bloque ? 'dia' AND v_bloque ? 'inicio' AND v_bloque ? 'fin') THEN
            RAISE EXCEPTION 'Cada bloque de horario_json debe tener dia, inicio y fin.';
        END IF;

        v_dia    := v_bloque->>'dia';
        v_inicio := v_bloque->>'inicio';
        v_fin    := v_bloque->>'fin';

        IF v_dia NOT IN ('Lunes', 'Martes', 'Miercoles', 'Miércoles', 'Jueves', 'Viernes', 'Sabado', 'Sábado', 'Domingo') THEN
            RAISE EXCEPTION 'Día inválido en horario_json: %', v_dia;
        END IF;

        IF v_inicio !~ '^(?:[01]\d|2[0-3]):[0-5]\d$' THEN
            RAISE EXCEPTION 'Hora de inicio inválida en horario_json: %', v_inicio;
        END IF;

        IF v_fin !~ '^(?:[01]\d|2[0-3]):[0-5]\d$' THEN
            RAISE EXCEPTION 'Hora de fin inválida en horario_json: %', v_fin;
        END IF;

        IF v_inicio >= v_fin THEN
            RAISE EXCEPTION 'La hora de inicio (%) debe ser menor que la hora fin (%).', v_inicio, v_fin;
        END IF;
    END LOOP;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_validar_horario_json_ofertas
BEFORE INSERT OR UPDATE OF horario_json ON ofertas
FOR EACH ROW
EXECUTE FUNCTION fn_validar_horario_json();


-- =========================================================
-- FUNCION: eliminar horarios recomendados de otros semestres
-- al insertar uno nuevo para el estudiante
-- =========================================================
CREATE OR REPLACE FUNCTION fn_limpiar_horarios_otro_semestre()
RETURNS TRIGGER AS $$
BEGIN
    DELETE FROM horarios_recomendados
    WHERE estudiante_id = NEW.estudiante_id
      AND semestre <> NEW.semestre
      AND id <> NEW.id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_limpiar_horarios_otro_semestre
AFTER INSERT ON horarios_recomendados
FOR EACH ROW
EXECUTE FUNCTION fn_limpiar_horarios_otro_semestre();


-- =========================================================
-- FUNCION UTIL EXTRA:
-- evitar prerequisito directo inverso
-- A -> B y B -> A al mismo tiempo
-- =========================================================
CREATE OR REPLACE FUNCTION fn_evitar_prerequisito_inverso_directo()
RETURNS TRIGGER AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM prerequisitos p
        WHERE p.id_materia = NEW.id_prerequisito
          AND p.id_prerequisito = NEW.id_materia
    )
    INTO v_existe;

    IF v_existe THEN
        RAISE EXCEPTION
            'No se permite prerequisito inverso directo entre materia % y materia %.',
            NEW.id_materia, NEW.id_prerequisito;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_evitar_prerequisito_inverso_directo
BEFORE INSERT OR UPDATE OF id_materia, id_prerequisito ON prerequisitos
FOR EACH ROW
EXECUTE FUNCTION fn_evitar_prerequisito_inverso_directo();

COMMIT;
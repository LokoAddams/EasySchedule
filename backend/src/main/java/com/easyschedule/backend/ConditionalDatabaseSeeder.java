package com.easyschedule.backend;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ConditionalDatabaseSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalDatabaseSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public ConditionalDatabaseSeeder(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!shouldRunSeeders()) {
            logger.info("Seeders omitidos: ya existen datos en la base de datos.");
            return;
        }

        logger.info("Base vacia o sin esquema. Ejecutando schema y seeders...");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(false);
        populator.setIgnoreFailedDrops(true);
        populator.addScript(new ClassPathResource("db/schema.sql"));
        populator.addScript(new ClassPathResource("db/seed/seed_universidades.sql"));
        populator.addScript(new ClassPathResource("db/seed/seed_carreras.sql"));
        populator.addScript(new ClassPathResource("db/seed/seed_mallas.sql"));
        populator.addScript(new ClassPathResource("db/seed/seed_materias.sql"));
        populator.addScript(new ClassPathResource("db/seed/seed_malla_materia.sql"));
        populator.addScript(new ClassPathResource("db/seed/seed_prerequisitos.sql"));
        populator.execute(dataSource);

        logger.info("Schema y seeders ejecutados correctamente.");
    }

    private boolean shouldRunSeeders() {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM universidades", Long.class);
            return count == null || count == 0L;
        } catch (DataAccessException ex) {
            logger.info("No se pudo consultar universidades; se ejecutara inicializacion SQL.");
            return true;
        }
    }
}
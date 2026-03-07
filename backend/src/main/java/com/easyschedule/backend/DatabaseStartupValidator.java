package com.easyschedule.backend;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Component;

@Component
public class DatabaseStartupValidator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseStartupValidator.class);

    private final DataSource dataSource;

    public DatabaseStartupValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(2)) {
                throw new IllegalStateException("La conexion a PostgreSQL no es valida.");
            }
            logger.info(AnsiOutput.toString(AnsiColor.GREEN, "Conexion a PostgreSQL establecida correctamente.", AnsiColor.DEFAULT));
        } catch (SQLException e) {
            logger.error("No se pudo conectar a PostgreSQL al iniciar el servidor.", e);
            throw new IllegalStateException("Error de conexion a la base de datos.", e);
        }
    }
}

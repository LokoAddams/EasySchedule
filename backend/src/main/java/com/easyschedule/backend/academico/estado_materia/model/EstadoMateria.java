package com.easyschedule.backend.academico.estado_materia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "estado_materia_estudiante")
public class EstadoMateria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "malla_materia_id", nullable = false)
    private Long mallaMateriaId;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    public EstadoMateria() {
    }

    public EstadoMateria(Long userId, Long mallaMateriaId, String estado) {
        this.userId = userId;
        this.mallaMateriaId = mallaMateriaId;
        this.estado = estado;
        this.fechaActualizacion = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMallaMateriaId() {
        return mallaMateriaId;
    }

    public void setMallaMateriaId(Long mallaMateriaId) {
        this.mallaMateriaId = mallaMateriaId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(OffsetDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}

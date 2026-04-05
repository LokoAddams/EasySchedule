package com.easyschedule.backend.academico.malla.model;

import com.easyschedule.backend.estudiante.model.Estudiante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mallas", uniqueConstraints = {
    @UniqueConstraint(name = "uq_mallas_carrera_version", columnNames = {"carrera_id", "version"})
})
public class Malla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carrera_id", nullable = false)
    private Long carreraId;

    @Column(length = 150)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "malla")
    private List<Estudiante> estudiantes = new ArrayList<>();

    @OneToMany(mappedBy = "malla")
    private List<MallaMateria> mallaMaterias = new ArrayList<>();

    public Malla() {
    }

    public Malla(Long id, Long carreraId, String nombre, String version) {
        this.id = id;
        this.carreraId = carreraId;
        this.nombre = nombre;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCarreraId() {
        return carreraId;
    }

    public void setCarreraId(Long carreraId) {
        this.carreraId = carreraId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Estudiante> getEstudiantes() {
        return estudiantes;
    }

    public void setEstudiantes(List<Estudiante> estudiantes) {
        this.estudiantes = estudiantes;
    }

    public List<MallaMateria> getMallaMaterias() {
        return mallaMaterias;
    }

    public void setMallaMaterias(List<MallaMateria> mallaMaterias) {
        this.mallaMaterias = mallaMaterias;
    }
}

package com.easyschedule.backend.academico.materia.model;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "prerequisitos", uniqueConstraints = {
    @UniqueConstraint(name = "uq_prereq", columnNames = {"malla_materia_id", "prereq_malla_materia_id"})
})
public class Prerequisito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "malla_materia_id", nullable = false)
    private MallaMateria mallaMateria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prereq_malla_materia_id", nullable = false)
    private MallaMateria prerequisito;

    public Prerequisito() {
    }

    public Prerequisito(Long id, MallaMateria mallaMateria, MallaMateria prerequisito) {
        this.id = id;
        this.mallaMateria = mallaMateria;
        this.prerequisito = prerequisito;
    }

    @PrePersist
    @PreUpdate
    public void validarDiferentes() {
        if (mallaMateria != null && prerequisito != null && mallaMateria.getId() != null && mallaMateria.getId().equals(prerequisito.getId())) {
            throw new IllegalArgumentException("Una materia no puede ser prerequisito de si misma.");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MallaMateria getMallaMateria() {
        return mallaMateria;
    }

    public void setMallaMateria(MallaMateria mallaMateria) {
        this.mallaMateria = mallaMateria;
    }

    public MallaMateria getPrerequisito() {
        return prerequisito;
    }

    public void setPrerequisito(MallaMateria prerequisito) {
        this.prerequisito = prerequisito;
    }
}

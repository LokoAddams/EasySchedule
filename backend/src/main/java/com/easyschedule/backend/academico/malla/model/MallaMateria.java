package com.easyschedule.backend.academico.malla.model;

import com.easyschedule.backend.academico.materia.model.Materia;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "malla_materia", uniqueConstraints = {
        @UniqueConstraint(name = "uq_malla_materia", columnNames = { "malla_id", "materia_id" })
})
public class MallaMateria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "malla_id", nullable = false)
    private Malla malla;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @Column(name = "semestre_sugerido", nullable = false)
    private Short semestreSugerido;

    public MallaMateria() {
    }

    public MallaMateria(Long id, Malla malla, Materia materia) {
        this.id = id;
        this.malla = malla;
        this.materia = materia;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Malla getMalla() {
        return malla;
    }

    public void setMalla(Malla malla) {
        this.malla = malla;
    }

    public Materia getMateria() {
        return materia;
    }

    public void setMateria(Materia materia) {
        this.materia = materia;
    }

    public Short getSemestreSugerido() {
        return semestreSugerido;
    }

    public void setSemestreSugerido(Short semestreSugerido) {
        this.semestreSugerido = semestreSugerido;
    }
}

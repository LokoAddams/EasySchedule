package com.easyschedule.backend.academico.seleccion.dto;

public class SeleccionResponse {
    private Long id;
    private Long materiaId;
    private String materiaNombre;
    private Long ofertaMateriaId;
    private String paralelo;
    private String docente;
    private String horarioJson;
    private String aula;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMateriaId() {
        return materiaId;
    }

    public void setMateriaId(Long materiaId) {
        this.materiaId = materiaId;
    }

    public String getMateriaNombre() {
        return materiaNombre;
    }

    public void setMateriaNombre(String materiaNombre) {
        this.materiaNombre = materiaNombre;
    }

    public Long getOfertaMateriaId() {
        return ofertaMateriaId;
    }

    public void setOfertaMateriaId(Long ofertaMateriaId) {
        this.ofertaMateriaId = ofertaMateriaId;
    }

    public String getParalelo() {
        return paralelo;
    }

    public void setParalelo(String paralelo) {
        this.paralelo = paralelo;
    }

    public String getDocente() {
        return docente;
    }

    public void setDocente(String docente) {
        this.docente = docente;
    }

    public String getHorarioJson() {
        return horarioJson;
    }

    public void setHorarioJson(String horarioJson) {
        this.horarioJson = horarioJson;
    }

    public String getAula() {
        return aula;
    }

    public void setAula(String aula) {
        this.aula = aula;
    }
}

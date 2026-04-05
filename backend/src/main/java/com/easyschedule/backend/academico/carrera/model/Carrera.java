package com.easyschedule.backend.academico.carrera.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "carreras")
public class Carrera {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "universidad_id", nullable = false)
	private Long universidadId;

	@Column(nullable = false, length = 150)
	private String nombre;

	@Column(nullable = false, length = 30)
	private String codigo;

	@Column(nullable = false)
	private boolean active = true;

	public Long getId() {
		return id;
	}

	public Long getUniversidadId() {
		return universidadId;
	}

	public String getNombre() {
		return nombre;
	}

	public String getCodigo() {
		return codigo;
	}

	public boolean isActive() {
		return active;
	}
}

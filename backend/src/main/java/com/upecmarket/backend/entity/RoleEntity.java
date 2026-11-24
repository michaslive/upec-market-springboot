package com.upecmarket.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class RoleEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Short id;

	@Column(nullable = false, unique = true, length = 20)
	private String name;

	protected RoleEntity() {
	}

	public Short getId() { return id; }
	public String getName() { return name; }
}

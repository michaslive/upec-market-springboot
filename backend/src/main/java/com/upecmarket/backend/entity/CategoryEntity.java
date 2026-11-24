package com.upecmarket.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class CategoryEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String name;

	@Column(nullable = false, unique = true, length = 90)
	private String slug;

	@Column(nullable = false)
	private boolean active;

	protected CategoryEntity() {
	}

	public Long getId() { return id; }
	public String getName() { return name; }
	public String getSlug() { return slug; }
	public boolean isActive() { return active; }
}

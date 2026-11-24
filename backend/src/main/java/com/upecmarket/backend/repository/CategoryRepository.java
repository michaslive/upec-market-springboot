package com.upecmarket.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.upecmarket.backend.entity.CategoryEntity;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
	Optional<CategoryEntity> findByIdAndActiveTrue(Long id);
}

package com.upecmarket.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.upecmarket.backend.entity.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
	@EntityGraph(attributePaths = {"category", "seller"})
	@Query("""
			SELECT p FROM ProductEntity p
			WHERE p.status = 'approved' AND p.deletedAt IS NULL
			ORDER BY p.createdAt DESC
			""")
	List<ProductEntity> findPublic(@Param("search") String search, @Param("category") String category, Pageable pageable);

	@EntityGraph(attributePaths = {"category", "seller"})
	Optional<ProductEntity> findByIdAndStatusAndDeletedAtIsNull(Long id, String status);

	@EntityGraph(attributePaths = {"category", "seller"})
	List<ProductEntity> findBySeller_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long sellerId);

	@EntityGraph(attributePaths = {"category", "seller"})
	List<ProductEntity> findByStatusOrderByCreatedAtAsc(String status);

	@EntityGraph(attributePaths = {"category", "seller"})
	Optional<ProductEntity> findByIdAndSeller_IdAndDeletedAtIsNull(Long id, Long sellerId);
}

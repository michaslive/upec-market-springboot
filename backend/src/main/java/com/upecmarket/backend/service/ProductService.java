package com.upecmarket.backend.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.upecmarket.backend.ApiException;
import com.upecmarket.backend.AuthUser;
import com.upecmarket.backend.dto.ProductRequest;
import com.upecmarket.backend.dto.ProductResponse;
import com.upecmarket.backend.entity.CategoryEntity;
import com.upecmarket.backend.entity.ProductEntity;
import com.upecmarket.backend.entity.UserAccountEntity;
import com.upecmarket.backend.repository.CategoryRepository;
import com.upecmarket.backend.repository.ProductRepository;
import com.upecmarket.backend.repository.UserAccountRepository;

@Service
public class ProductService {
	private static final Set<String> EDITABLE_STATUSES = Set.of("pending", "approved", "rejected");
	private final ProductRepository products;
	private final CategoryRepository categories;
	private final UserAccountRepository users;
	private final JdbcTemplate jdbc;
	private final ImageStorageService imageStorage;

	public ProductService(ProductRepository products, CategoryRepository categories, UserAccountRepository users,
			JdbcTemplate jdbc, ImageStorageService imageStorage) {
		this.products = products;
		this.categories = categories;
		this.users = users;
		this.jdbc = jdbc;
		this.imageStorage = imageStorage;
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> publicProducts(String query, String category) {
		String search = query == null || query.isBlank() ? null : query.trim();
		String categoryFilter = category == null || category.isBlank() ? null : category.trim();
		return products.findPublic(search, categoryFilter, PageRequest.of(0, 100))
				.stream()
				.filter(product -> search == null || (
						product.getTitle().toLowerCase().contains(search.toLowerCase())
						|| product.getDescription().toLowerCase().contains(search.toLowerCase())
						|| product.getCategory().getName().toLowerCase().contains(search.toLowerCase())))
				.filter(product -> categoryFilter == null || (
						product.getCategory().getName().equalsIgnoreCase(categoryFilter)
						|| product.getCategory().getSlug().equalsIgnoreCase(categoryFilter)))
				.map(ProductResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public ProductResponse publicProduct(long id) {
		return ProductResponse.from(products.findByIdAndStatusAndDeletedAtIsNull(id, "approved")
				.orElseThrow(() -> ApiException.notFound("El producto no existe o no está publicado.")));
	}

	@Transactional
	public ProductResponse create(AuthUser actor, ProductRequest request) {
		UserAccountEntity seller = users.findById(actor.id())
				.orElseThrow(() -> ApiException.notFound("La cuenta ya no existe."));
		CategoryEntity category = category(request.categoryId());
		ProductEntity product = new ProductEntity(seller, category, request.title().trim(),
				request.description().trim(), request.price(), request.normalizedConditionType(),
				request.normalizedListingType(), validateImageUrl(request.imageUrl()));
		return ProductResponse.from(products.save(product));
	}

	@Transactional
	public ProductResponse update(long id, AuthUser actor, ProductRequest request) {
		ProductEntity product = ownedProduct(id, actor.id());
		if (!EDITABLE_STATUSES.contains(product.getStatus())) {
			throw ApiException.conflict("No se puede editar un producto reservado o vendido.");
		}
		String imageUrl = request.imageUrl() == null ? product.getImageUrl() : validateImageUrl(request.imageUrl());
		product.update(category(request.categoryId()), request.title().trim(), request.description().trim(),
				request.price(), request.normalizedConditionType(), request.normalizedListingType(), imageUrl);
		return ProductResponse.from(product);
	}

	@Transactional
	public void delete(long id, AuthUser actor) {
		ProductEntity product = ownedProduct(id, actor.id());
		if (!EDITABLE_STATUSES.contains(product.getStatus())) {
			throw ApiException.conflict("No se puede eliminar una publicación reservada o transaccionada.");
		}
		product.softDelete();
		jdbc.update("DELETE FROM cart_items WHERE product_id = ?", id);
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> myProducts(AuthUser actor) {
		return products.findBySeller_IdAndDeletedAtIsNullOrderByCreatedAtDesc(actor.id())
				.stream().map(ProductResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> pendingProducts(AuthUser actor) {
		requireAdmin(actor);
		return products.findByStatusOrderByCreatedAtAsc("pending")
				.stream().map(ProductResponse::from).toList();
	}

	@Transactional
	public void moderate(long id, AuthUser actor, String decision, String note) {
		requireAdmin(actor);
		if (!Set.of("approved", "rejected").contains(decision)) {
			throw ApiException.badRequest("El estado debe ser approved o rejected.");
		}
		ProductEntity product = products.findById(id)
				.orElseThrow(() -> ApiException.notFound("La publicación no existe."));
		if (!"pending".equals(product.getStatus())) {
			throw ApiException.conflict("El producto ya fue revisado.");
		}
		if ("approved".equals(decision)) product.approve(actor.id());
		else product.reject(actor.id(), note);
	}

	@Transactional
	public ProductResponse uploadImage(long id, AuthUser actor, MultipartFile image) {
		ProductEntity product = ownedProduct(id, actor.id());
		if (!EDITABLE_STATUSES.contains(product.getStatus())) {
			throw ApiException.conflict("No se puede cambiar la imagen de un producto reservado o vendido.");
		}
		product.setImageUrl(imageStorage.store(image));
		return ProductResponse.from(product);
	}

	private ProductEntity ownedProduct(long id, long ownerId) {
		return products.findByIdAndSeller_IdAndDeletedAtIsNull(id, ownerId)
				.orElseThrow(() -> ApiException.notFound("La publicación no existe o no te pertenece."));
	}

	private CategoryEntity category(long id) {
		return categories.findByIdAndActiveTrue(id)
				.orElseThrow(() -> ApiException.badRequest("La categoría no existe o está inactiva."));
	}

	private static String validateImageUrl(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) return null;
		String normalized = imageUrl.trim();
		boolean validUrl = normalized.matches("(?i)^https?://[^\\s]+$")
				|| normalized.matches("(?i)^/?uploads/[^\\s]+$");
		if (!validUrl || normalized.length() > 1000) {
			throw ApiException.badRequest("La imagen debe ser una URL HTTP/HTTPS o una ruta /uploads/ de hasta 1000 caracteres.");
		}
		return normalized;
	}

	private static void requireAdmin(AuthUser actor) {
		if (!actor.hasRole("ADMIN") && !actor.hasRole("SUPER_ADMIN")) {
			throw ApiException.forbidden("Se requiere el rol administrador.");
		}
	}
}

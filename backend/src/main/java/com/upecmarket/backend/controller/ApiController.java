package com.upecmarket.backend.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.upecmarket.backend.ApiException;
import com.upecmarket.backend.AuthService;
import com.upecmarket.backend.AuthUser;
import com.upecmarket.backend.dto.ProductRequest;
import com.upecmarket.backend.dto.ProductResponse;
import com.upecmarket.backend.dto.ProfileUpdateRequest;
import com.upecmarket.backend.dto.LoginRequest;
import com.upecmarket.backend.dto.RegisterRequest;
import com.upecmarket.backend.service.ProductService;
import com.upecmarket.backend.service.ProfileService;
import com.upecmarket.backend.service.PaymentProofStorageService;

@RestController
@RequestMapping("/api")
@Tag(name = "Marketplace", description = "Autenticación, catálogo, compras, chat, reseñas y administración")
public class ApiController {
	private static final Set<String> TRANSACTION_STATUSES =
			Set.of("pending_payment", "paid", "in_transit", "delivered", "exchanged", "cancelled");
	private final JdbcTemplate jdbc;
	private final AuthService auth;
	private final ProductService productService;
	private final ProfileService profileService;
	private final PaymentProofStorageService paymentProofStorage;
	private final long reservationMinutes;

	public ApiController(JdbcTemplate jdbc, AuthService auth, ProductService productService, ProfileService profileService,
			PaymentProofStorageService paymentProofStorage,
			@Value("${app.reservation.duration-minutes}") long reservationMinutes) {
		this.jdbc = jdbc;
		this.auth = auth;
		this.productService = productService;
		this.profileService = profileService;
		this.paymentProofStorage = paymentProofStorage;
		this.reservationMinutes = Math.max(1, Math.min(reservationMinutes, 30));
	}

	@GetMapping("/health")
	public Map<String, String> health() {
		jdbc.queryForObject("SELECT 1", Integer.class);
		return Map.of("status", "ok", "database", "ok");
	}

	@GetMapping("/payment-config")
	public Map<String, Object> paymentConfig() {
		Map<String, String> settings = jdbc.query(
				"SELECT setting_key, setting_value FROM system_settings",
				result -> {
					Map<String, String> values = new java.util.HashMap<>();
					while (result.next()) values.put(result.getString("setting_key"), result.getString("setting_value"));
					return values;
				});
		double feePercent = parseDouble(settings.get("payment.platform_fee_percent"), 0);
		double feeFixed = parseDouble(settings.get("payment.platform_fee_fixed"), 0);
		List<Map<String, String>> banks = List.of(
				Map.of(
						"name", settings.getOrDefault("payment.bank1_name", "Banco Pichincha"),
						"account", settings.getOrDefault("payment.bank1_account", "220000001234"),
						"holder", settings.getOrDefault("payment.bank1_holder", "UPEC Market Demo")),
				Map.of(
						"name", settings.getOrDefault("payment.bank2_name", "Banco Guayaquil"),
						"account", settings.getOrDefault("payment.bank2_account", "210000009876"),
						"holder", settings.getOrDefault("payment.bank2_holder", "UPEC Market Demo")));
		return Map.of(
				"feePercent", feePercent,
				"feeFixed", feeFixed,
				"banks", banks,
				"deunaLabel", settings.getOrDefault("payment.deuna_label", "UPEC MARKET"));
	}

	@PostMapping("/auth/register")
	public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
		return auth.register(Map.of("email", request.email(), "password", request.password(), "fullName", request.fullName()));
	}

	@PostMapping("/auth/login")
	public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
		return auth.login(Map.of("email", request.email(), "password", request.password()));
	}

	@GetMapping("/auth/me")
	public Map<String, Object> me(@RequestAttribute("authenticatedUser") AuthUser user) {
		return auth.currentUser(user);
	}

	@PatchMapping("/auth/me")
	public Map<String, Object> updateProfile(@RequestAttribute("authenticatedUser") AuthUser user,
			@Valid @RequestBody ProfileUpdateRequest request) {
		return profileService.update(user, request);
	}

	@PostMapping("/auth/logout")
	public Map<String, String> logout(@RequestAttribute("rawToken") String token) {
		auth.logout(token);
		return Map.of("status", "signed_out");
	}

	@GetMapping("/categories")
	public List<Map<String, Object>> categories() {
		return jdbc.queryForList("SELECT id, name, slug FROM categories WHERE active = TRUE ORDER BY name");
	}

	@GetMapping("/products")
	public List<ProductResponse> products(@RequestParam(required = false) String q,
			@RequestParam(required = false) String category) {
		return productService.publicProducts(q, category);
	}

	@GetMapping("/products/{id}")
	public ProductResponse product(@PathVariable long id) {
		return productService.publicProduct(id);
	}

	@PostMapping("/products")
	public ProductResponse createProduct(@RequestAttribute("authenticatedUser") AuthUser user,
			@Valid @RequestBody ProductRequest request) {
		return productService.create(user, request);
	}

	@PutMapping("/products/{id}")
	public ProductResponse updateProduct(@PathVariable long id, @RequestAttribute("authenticatedUser") AuthUser user,
			@Valid @RequestBody ProductRequest request) {
		return productService.update(id, user, request);
	}

	@DeleteMapping("/products/{id}")
	public Map<String, String> deleteProduct(@PathVariable long id, @RequestAttribute("authenticatedUser") AuthUser user) {
		productService.delete(id, user);
		return Map.of("status", "deleted");
	}

	@GetMapping("/products/mine")
	public List<ProductResponse> myProducts(@RequestAttribute("authenticatedUser") AuthUser user) {
		return productService.myProducts(user);
	}

	@GetMapping("/admin/products/pending")
	public List<ProductResponse> pendingProducts(@RequestAttribute("authenticatedUser") AuthUser user) {
		return productService.pendingProducts(user);
	}

	@PatchMapping("/admin/products/{id}/moderation")
	public Map<String, String> moderateProduct(@PathVariable long id,
			@RequestAttribute("authenticatedUser") AuthUser user, @RequestBody Map<String, Object> body) {
		String decision = AuthService.requiredText(body, "status").toLowerCase();
		productService.moderate(id, user, decision, optionalText(body, "note", null));
		return Map.of("status", decision);
	}

	@GetMapping("/cart")
	public List<Map<String, Object>> cart(@RequestAttribute("authenticatedUser") AuthUser user) {
		return jdbc.queryForList("""
				SELECT p.id AS productId, p.title, p.price, p.status, p.reservation_expires_at AS reservationExpiresAt,
				       p.image_url AS imageUrl, u.full_name AS sellerName
				FROM cart_items ci JOIN products p ON p.id = ci.product_id
				JOIN users u ON u.id = p.seller_id
				WHERE ci.user_id = ? AND p.status IN ('approved', 'reserved')
				ORDER BY ci.added_at DESC
				""", user.id());
	}

	@PostMapping("/cart/{productId}")
	public Map<String, String> addToCart(@PathVariable long productId,
			@RequestAttribute("authenticatedUser") AuthUser user) {
		Map<String, Object> product = findProduct(productId);
		if (!"approved".equals(product.get("status"))) {
			throw ApiException.conflict("El producto no está disponible para añadir al carrito.");
		}
		if (((Number) product.get("seller_id")).longValue() == user.id()) {
			throw ApiException.badRequest("No puedes añadir tu propia publicación al carrito.");
		}
		jdbc.update("""
				INSERT INTO cart_items (user_id, product_id) VALUES (?, ?)
				ON CONFLICT (user_id, product_id) DO NOTHING
				""", user.id(), productId);
		return Map.of("status", "added", "reserved", "false");
	}

	@DeleteMapping("/cart/{productId}")
	public Map<String, String> removeFromCart(@PathVariable long productId,
			@RequestAttribute("authenticatedUser") AuthUser user) {
		jdbc.update("DELETE FROM cart_items WHERE user_id = ? AND product_id = ?", user.id(), productId);
		jdbc.update("""
				UPDATE products SET status = 'approved', reserved_by = NULL, reservation_expires_at = NULL
				WHERE id = ? AND status = 'reserved' AND reserved_by = ?
				""", productId, user.id());
		return Map.of("status", "removed");
	}

	@PostMapping("/cart/{productId}/reserve")
	@Transactional
	public Map<String, Object> reserve(@PathVariable long productId,
			@RequestAttribute("authenticatedUser") AuthUser user) {
		int updated = jdbc.update("""
				UPDATE products SET status = 'reserved', reserved_by = ?,
			  reservation_expires_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 minute')
				WHERE id = ? AND status = 'approved' AND seller_id <> ? AND deleted_at IS NULL
				""", user.id(), reservationMinutes, productId, user.id());
		if (updated == 0) {
			throw ApiException.conflict("El producto ya no está disponible para reservar.");
		}
		jdbc.update("""
				INSERT INTO cart_items (user_id, product_id) VALUES (?, ?)
				ON CONFLICT (user_id, product_id) DO NOTHING
				""", user.id(), productId);
		return Map.of("status", "reserved", "expiresInMinutes", reservationMinutes);
	}

	@PostMapping("/transactions")
	@Transactional
	public Map<String, Object> createTransaction(@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestBody Map<String, Object> body) {
		long productId = longValue(body.get("productId"), "productId");
		String type = optionalText(body, "type", "purchase");
		if (!Set.of("purchase", "exchange").contains(type)) {
			throw ApiException.badRequest("El tipo debe ser purchase o exchange.");
		}
		List<Map<String, Object>> products = jdbc.queryForList("""
				SELECT id, seller_id, price, listing_type FROM products
				WHERE id = ? AND status = 'reserved' AND reserved_by = ?
				  AND reservation_expires_at > CURRENT_TIMESTAMP
				FOR UPDATE
				""", productId, user.id());
		if (products.isEmpty()) {
			throw ApiException.conflict("Reserva expirada o inexistente. Reserva de nuevo para continuar.");
		}
		Map<String, Object> product = products.getFirst();
		BigDecimal amount = (BigDecimal) product.get("price");
		BigDecimal platformFee = calculatePlatformFee(amount);
		BigDecimal sellerPayout = amount.subtract(platformFee).max(BigDecimal.ZERO);
		if ("exchange".equals(type) && !"exchange".equals(product.get("listing_type"))
				&& !"both".equals(product.get("listing_type"))) {
			throw ApiException.badRequest("Este producto no está publicado para intercambio.");
		}
		if ("purchase".equals(type) && !"sale".equals(product.get("listing_type"))
				&& !"both".equals(product.get("listing_type"))) {
			throw ApiException.badRequest("Este producto no está publicado para venta.");
		}
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			var statement = connection.prepareStatement("""
					INSERT INTO transactions (product_id, buyer_id, seller_id, transaction_type, status, amount,
					          exchange_details, platform_fee, seller_payout, payout_status)
					VALUES (?, ?, ?, ?, 'pending_payment', ?, ?, ?, ?, 'pending')
					""", java.sql.Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, productId);
			statement.setLong(2, user.id());
			statement.setLong(3, ((Number) product.get("seller_id")).longValue());
			statement.setString(4, type);
			statement.setBigDecimal(5, amount);
			statement.setString(6, optionalText(body, "exchangeDetails", null));
			statement.setBigDecimal(7, platformFee);
			statement.setBigDecimal(8, sellerPayout);
			return statement;
		}, keys);
		return Map.of("id", generatedId(keys), "status", "pending_payment", "type", type);
	}

	@PostMapping("/transactions/{id}/simulate-payment")
	@Transactional
	public Map<String, String> simulatePayment(@PathVariable long id,
			@RequestAttribute("authenticatedUser") AuthUser user, @RequestBody(required = false) Map<String, Object> body) {
		Map<String, Object> transaction = transactionForUpdate(id, user.id());
		if (user.id() != ((Number) transaction.get("buyer_id")).longValue()) {
			throw ApiException.forbidden("Solo el comprador puede completar el pago.");
		}
		if (!"purchase".equals(transaction.get("transaction_type"))
				|| !"pending_payment".equals(transaction.get("status"))) {
			throw ApiException.conflict("Esta transacción no admite pago simulado.");
		}
		String paymentMethod = body == null ? "card_simulation"
				: optionalText(body, "method", "card_simulation");
		if (!Set.of("card_simulation", "transfer_simulation", "deuna_simulation").contains(paymentMethod)) {
			throw ApiException.badRequest("El método de pago simulado no es válido.");
		}
		int updated = jdbc.update("""
				UPDATE products SET status = 'sold', reserved_by = NULL, reservation_expires_at = NULL
				WHERE id = ? AND status = 'reserved' AND reserved_by = ?
				""", transaction.get("product_id"), user.id());
		if (updated == 0) {
			throw ApiException.conflict("La reserva expiró antes del pago.");
		}
		jdbc.update("""
				UPDATE transactions SET status = 'paid', payment_method = ?,
				  payment_reference = 'SIM-' || id::text, payout_status = 'pending' WHERE id = ?
				""", paymentMethod, id);
		jdbc.update("DELETE FROM cart_items WHERE product_id = ?", transaction.get("product_id"));
		return Map.of("status", "paid", "paymentMethod", paymentMethod, "reference", "SIM-" + id);
	}

	@PostMapping(value = "/transactions/{id}/payment-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Transactional
	public Map<String, String> uploadPaymentProof(@PathVariable long id,
			@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestParam("proofNumber") String proofNumber,
			@RequestParam("file") MultipartFile file) {
		Map<String, Object> transaction = transactionForUpdate(id, user.id());
		if (user.id() != ((Number) transaction.get("buyer_id")).longValue()) {
			throw ApiException.forbidden("Solo el comprador puede subir el comprobante.");
		}
		if (!"pending_payment".equals(transaction.get("status"))) {
			throw ApiException.conflict("Esta transacción ya no admite comprobantes.");
		}
		String number = proofNumber == null ? "" : proofNumber.trim();
		if (number.isBlank() || number.length() > 120) {
			throw ApiException.badRequest("Ingresa un número de comprobante válido.");
		}
		String proofUrl = paymentProofStorage.store(file);
		jdbc.update("""
				UPDATE transactions SET payment_method = 'transfer_simulation',
				  payment_reference = ?, payment_proof_url = ?, payout_status = 'pending_verification' WHERE id = ?
				""", number, proofUrl, id);
		return Map.of("status", "pending_verification", "paymentReference", number, "proofUrl", proofUrl);
	}

	@GetMapping("/transactions")
	public List<Map<String, Object>> transactions(@RequestAttribute("authenticatedUser") AuthUser user) {
		return jdbc.queryForList("""
				SELECT t.id, t.product_id AS productId, p.title, t.buyer_id AS buyerId, t.seller_id AS sellerId,
				       t.transaction_type AS type, t.status, t.amount, t.payment_method AS paymentMethod,
				       t.payment_reference AS paymentReference, t.payment_proof_url AS paymentProofUrl,
				       t.platform_fee AS platformFee, t.seller_payout AS sellerPayout, t.payout_status AS payoutStatus,
				       t.created_at AS createdAt, buyer.full_name AS buyerName, seller.full_name AS sellerName
				FROM transactions t JOIN products p ON p.id = t.product_id
				JOIN users buyer ON buyer.id = t.buyer_id JOIN users seller ON seller.id = t.seller_id
				WHERE t.buyer_id = ? OR t.seller_id = ? ORDER BY t.created_at DESC
				""", user.id(), user.id());
	}

	@PatchMapping("/transactions/{id}/status")
	@Transactional
	public Map<String, String> updateTransactionStatus(@PathVariable long id,
			@RequestAttribute("authenticatedUser") AuthUser user, @RequestBody Map<String, Object> body) {
		String status = AuthService.requiredText(body, "status").toLowerCase();
		if (!TRANSACTION_STATUSES.contains(status)) {
			throw ApiException.badRequest("Estado de transacción inválido.");
		}
		Map<String, Object> transaction = transactionForUpdate(id, user.id());
		long buyerId = ((Number) transaction.get("buyer_id")).longValue();
		long sellerId = ((Number) transaction.get("seller_id")).longValue();
		if (user.id() != buyerId && user.id() != sellerId) {
			throw ApiException.forbidden("No participas en esta transacción.");
		}
		String previous = (String) transaction.get("status");
		boolean allowed = switch (status) {
			case "cancelled" -> previous.equals("pending_payment") && user.id() == buyerId;
			case "in_transit" -> previous.equals("paid") && user.id() == sellerId;
			case "delivered" -> previous.equals("in_transit") && user.id() == buyerId;
			case "exchanged" -> previous.equals("pending_payment") && "exchange".equals(transaction.get("transaction_type"));
			default -> false;
		};
		if (!allowed) {
			throw ApiException.conflict("No se permite esa transición de estado.");
		}
		jdbc.update("UPDATE transactions SET status = ? WHERE id = ?", status, id);
		if (Set.of("cancelled", "delivered", "exchanged").contains(status)) {
			String productStatus = switch (status) {
				case "delivered" -> "sold";
				case "exchanged" -> "exchanged";
				default -> "approved";
			};
			jdbc.update("""
					UPDATE products SET status = ?, reserved_by = NULL, reservation_expires_at = NULL
					WHERE id = ? AND status = 'reserved'
					""", productStatus, transaction.get("product_id"));
			if (!"cancelled".equals(status)) {
				jdbc.update("DELETE FROM cart_items WHERE product_id = ?", transaction.get("product_id"));
			}
		}
		return Map.of("status", status);
	}

	@PostMapping("/messages")
	public Map<String, Object> sendMessage(@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestBody Map<String, Object> body) {
		long recipientId = longValue(body.get("recipientId"), "recipientId");
		String message = AuthService.requiredText(body, "body");
		if (message.length() > 5000 || recipientId == user.id()) {
			throw ApiException.badRequest("El mensaje no es válido.");
		}
		if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ? AND account_status = 'active'",
				Integer.class, recipientId) == 0) {
			throw ApiException.notFound("El destinatario no existe.");
		}
		Long transactionId = body.get("transactionId") == null ? null : longValue(body.get("transactionId"), "transactionId");
		if (transactionId != null) {
			Map<String, Object> transaction = transactionForUpdate(transactionId, user.id());
			long buyer = ((Number) transaction.get("buyer_id")).longValue();
			long seller = ((Number) transaction.get("seller_id")).longValue();
			if (user.id() != buyer && user.id() != seller || recipientId != buyer && recipientId != seller) {
				throw ApiException.forbidden("El chat de una transacción solo está disponible para sus participantes.");
			}
		}
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			var statement = connection.prepareStatement("""
					INSERT INTO messages (transaction_id, sender_id, recipient_id, body) VALUES (?, ?, ?, ?)
					""", java.sql.Statement.RETURN_GENERATED_KEYS);
			if (transactionId == null) statement.setNull(1, java.sql.Types.BIGINT);
			else statement.setLong(1, transactionId);
			statement.setLong(2, user.id());
			statement.setLong(3, recipientId);
			statement.setString(4, message);
			return statement;
		}, keys);
		return Map.of("id", generatedId(keys), "status", "sent");
	}

	@GetMapping("/messages")
	public List<Map<String, Object>> messages(@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestParam long withUserId) {
		jdbc.update("""
				UPDATE messages SET read_at = CURRENT_TIMESTAMP
				WHERE sender_id = ? AND recipient_id = ? AND read_at IS NULL
				""", withUserId, user.id());
		return jdbc.queryForList("""
				SELECT id, transaction_id AS transactionId, sender_id AS senderId, recipient_id AS recipientId,
				       body, read_at AS readAt, created_at AS createdAt
				FROM messages WHERE (sender_id = ? AND recipient_id = ?)
				  OR (sender_id = ? AND recipient_id = ?)
				ORDER BY created_at LIMIT 300
				""", user.id(), withUserId, withUserId, user.id());
	}

	@GetMapping("/messages/conversations")
	public List<Map<String, Object>> conversations(@RequestAttribute("authenticatedUser") AuthUser user) {
		return jdbc.queryForList("""
				SELECT other.id AS userId, other.full_name AS fullName, other.profile_photo_url AS profilePhotoUrl,
				       MAX(m.created_at) AS lastMessageAt,
				       (ARRAY_AGG(m.body ORDER BY m.created_at DESC))[1] AS lastMessage,
				       SUM(CASE WHEN m.recipient_id = ? AND m.read_at IS NULL THEN 1 ELSE 0 END) AS unreadCount
				FROM messages m
				JOIN users other ON other.id = CASE WHEN m.sender_id = ? THEN m.recipient_id ELSE m.sender_id END
				WHERE m.sender_id = ? OR m.recipient_id = ?
				GROUP BY other.id, other.full_name, other.profile_photo_url
				ORDER BY lastMessageAt DESC
				""", user.id(), user.id(), user.id(), user.id());
	}

	@PostMapping("/reviews")
	@Transactional
	public Map<String, String> review(@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestBody Map<String, Object> body) {
		long transactionId = longValue(body.get("transactionId"), "transactionId");
		int rating = Math.toIntExact(longValue(body.get("rating"), "rating"));
		if (rating < 1 || rating > 5) throw ApiException.badRequest("La calificación debe estar entre 1 y 5.");
		Map<String, Object> transaction = transactionForUpdate(transactionId, user.id());
		if (!Set.of("delivered", "exchanged").contains(transaction.get("status"))) {
			throw ApiException.conflict("Solo puedes reseñar una transacción finalizada.");
		}
		long buyer = ((Number) transaction.get("buyer_id")).longValue();
		long seller = ((Number) transaction.get("seller_id")).longValue();
		long reviewee = user.id() == buyer ? seller : buyer;
		jdbc.update("INSERT INTO reviews (transaction_id, reviewer_id, reviewee_id, rating, comment) VALUES (?, ?, ?, ?, ?)",
				transactionId, user.id(), reviewee, rating, optionalText(body, "comment", null));
		return Map.of("status", "created");
	}

	@GetMapping("/users/{id}/profile")
	public Map<String, Object> userProfile(@PathVariable long id) {
		List<Map<String, Object>> profiles = jdbc.queryForList("""
				SELECT u.id, u.full_name AS fullName, u.profile_photo_url AS profilePhotoUrl,
				       u.created_at AS memberSince, ROUND(AVG(r.rating), 2) AS trustIndex,
				       COUNT(r.id) AS reviewCount
				FROM users u LEFT JOIN reviews r ON r.reviewee_id = u.id
				WHERE u.id = ? AND u.account_status = 'active'
				GROUP BY u.id
				""", id);
		if (profiles.isEmpty()) throw ApiException.notFound("El perfil no existe.");
		return profiles.getFirst();
	}

	@PostMapping("/reports")
	public Map<String, Object> report(@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestBody Map<String, Object> body) {
		Long productId = nullableLong(body.get("productId"), "productId");
		Long reportedUserId = nullableLong(body.get("reportedUserId"), "reportedUserId");
		if (productId == null && reportedUserId == null) {
			throw ApiException.badRequest("Indica el perfil o publicación que deseas reportar.");
		}
		String reason = AuthService.requiredText(body, "reason");
		if (productId != null && !exists("SELECT COUNT(*) FROM products WHERE id = ?", productId)
				|| reportedUserId != null && !exists("SELECT COUNT(*) FROM users WHERE id = ?", reportedUserId)) {
			throw ApiException.notFound("La publicación o el usuario reportado no existe.");
		}
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			var statement = connection.prepareStatement("""
					INSERT INTO reports (reporter_id, reported_user_id, product_id, reason, details)
					VALUES (?, ?, ?, ?, ?)
					""", java.sql.Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, user.id());
			if (reportedUserId == null) statement.setNull(2, java.sql.Types.BIGINT);
			else statement.setLong(2, reportedUserId);
			if (productId == null) statement.setNull(3, java.sql.Types.BIGINT);
			else statement.setLong(3, productId);
			statement.setString(4, reason);
			statement.setString(5, optionalText(body, "details", null));
			return statement;
		}, keys);
		return Map.of("id", generatedId(keys), "status", "open");
	}

	@GetMapping("/admin/reports")
	public List<Map<String, Object>> reports(@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestParam(required = false) String status) {
		requireAdmin(user);
		if (status == null || status.isBlank()) {
			return jdbc.queryForList("""
					SELECT r.id, r.reporter_id AS reporterId, reporter.full_name AS reporterName,
					       r.reported_user_id AS reportedUserId, reported.full_name AS reportedUserName,
					       r.product_id AS productId, p.title AS productTitle, r.reason, r.details, r.status, r.created_at AS createdAt
					FROM reports r JOIN users reporter ON reporter.id = r.reporter_id
					LEFT JOIN users reported ON reported.id = r.reported_user_id
					LEFT JOIN products p ON p.id = r.product_id ORDER BY r.created_at DESC LIMIT 200
					""");
		}
		return jdbc.queryForList("""
				SELECT r.id, r.reporter_id AS reporterId, reporter.full_name AS reporterName,
				       r.reported_user_id AS reportedUserId, reported.full_name AS reportedUserName,
				       r.product_id AS productId, p.title AS productTitle, r.reason, r.details, r.status, r.created_at AS createdAt
				FROM reports r JOIN users reporter ON reporter.id = r.reporter_id
				LEFT JOIN users reported ON reported.id = r.reported_user_id
				LEFT JOIN products p ON p.id = r.product_id WHERE r.status = ? ORDER BY r.created_at DESC LIMIT 200
				""", status);
	}

	@PatchMapping("/admin/reports/{id}")
	public Map<String, String> resolveReport(@PathVariable long id,
			@RequestAttribute("authenticatedUser") AuthUser user, @RequestBody Map<String, Object> body) {
		requireAdmin(user);
		String status = AuthService.requiredText(body, "status");
		if (!Set.of("investigating", "resolved", "dismissed").contains(status)) {
			throw ApiException.badRequest("Estado de reporte inválido.");
		}
		int updated = jdbc.update("""
				UPDATE reports SET status = ?, resolved_by = ?, resolved_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", status, user.id(), id);
		if (updated == 0) throw ApiException.notFound("El reporte no existe.");
		return Map.of("status", status);
	}

	@PostMapping("/support")
	public Map<String, Object> createSupportRequest(@RequestAttribute("authenticatedUser") AuthUser user,
			@RequestBody Map<String, Object> body) {
		String subject = AuthService.requiredText(body, "subject");
		String message = AuthService.requiredText(body, "message");
		jdbc.update("INSERT INTO support_requests (user_id, email, subject, message) VALUES (?, ?, ?, ?)",
				user.id(), user.email(), subject, message);
		return Map.of("status", "open");
	}

	@GetMapping("/admin/users")
	public List<Map<String, Object>> users(@RequestAttribute("authenticatedUser") AuthUser user) {
		requireAdmin(user);
		return jdbc.queryForList("""
				SELECT u.id, u.email, u.full_name AS fullName, u.account_status AS accountStatus,
				       u.payout_bank AS payoutBank, u.payout_account AS payoutAccount,
				       u.payout_holder AS payoutHolder, u.payout_identifier AS payoutIdentifier,
				       u.created_at AS createdAt,
				       STRING_AGG(r.name, ',' ORDER BY r.name) AS roles
				FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.id
				LEFT JOIN roles r ON r.id = ur.role_id
				GROUP BY u.id ORDER BY u.created_at DESC LIMIT 500
				""");
	}

	@PatchMapping("/admin/users/{id}/status")
	public Map<String, String> changeUserStatus(@PathVariable long id,
			@RequestAttribute("authenticatedUser") AuthUser actor, @RequestBody Map<String, Object> body) {
		requireAdmin(actor);
		String status = AuthService.requiredText(body, "status");
		if (!Set.of("active", "restricted", "suspended").contains(status)) {
			throw ApiException.badRequest("Estado de cuenta inválido.");
		}
		requireCanManageUser(actor, id);
		int updated = jdbc.update("UPDATE users SET account_status = ? WHERE id = ?", status, id);
		if (updated == 0) throw ApiException.notFound("El usuario no existe.");
		return Map.of("accountStatus", status);
	}

	@PatchMapping("/admin/users/{id}/roles")
	@Transactional
	public Map<String, Object> changeUserRoles(@PathVariable long id,
			@RequestAttribute("authenticatedUser") AuthUser actor, @RequestBody Map<String, Object> body) {
		requireSuperAdmin(actor);
		if (!(body.get("roles") instanceof List<?> requestedRoles) || requestedRoles.isEmpty()
				|| requestedRoles.stream().anyMatch(role -> !(role instanceof String))) {
			throw ApiException.badRequest("Envía una lista de roles válida.");
		}
		if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id) == 0) {
			throw ApiException.notFound("El usuario no existe.");
		}
		Set<String> validRoles = Set.of("BUYER", "SELLER", "ADMIN", "SUPER_ADMIN");
		List<String> roles = requestedRoles.stream().map(Object::toString).map(String::toUpperCase).distinct().toList();
		if (!validRoles.containsAll(roles)) throw ApiException.badRequest("La lista contiene roles inválidos.");
		List<String> currentRoles = jdbc.queryForList("""
				SELECT r.name FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ?
				""", String.class, id);
		if (currentRoles.contains("SUPER_ADMIN") && !roles.contains("SUPER_ADMIN")) {
			int superAdminCount = jdbc.queryForObject("""
					SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id = ur.role_id
					WHERE r.name = 'SUPER_ADMIN'
					""", Integer.class);
			if (superAdminCount <= 1) {
				throw ApiException.conflict("Asigna otro Super Admin antes de retirar el último rol Super Admin.");
			}
		}
		jdbc.update("DELETE FROM user_roles WHERE user_id = ?", id);
		for (String role : roles) jdbc.update("""
				INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = ?
				""", id, role);
		return Map.of("userId", id, "roles", roles);
	}

	@PutMapping("/admin/settings/{key}")
	public Map<String, String> updateSetting(@PathVariable String key,
			@RequestAttribute("authenticatedUser") AuthUser user, @RequestBody Map<String, Object> body) {
		requireAdmin(user);
		String password = AuthService.requiredText(body, "password");
		String value = AuthService.requiredText(body, "value");
		if (!auth.passwordMatches(user.id(), password)) {
			throw ApiException.unauthorized("La contraseña de confirmación es incorrecta.");
		}
		if (!key.matches("[a-zA-Z0-9._-]{1,100}") || value.length() > 5000) {
			throw ApiException.badRequest("La clave o el valor de configuración no son válidos.");
		}
		jdbc.update("""
				INSERT INTO system_settings (setting_key, setting_value, updated_by) VALUES (?, ?, ?)
				ON CONFLICT (setting_key) DO UPDATE
				SET setting_value = EXCLUDED.setting_value,
				    updated_by = EXCLUDED.updated_by,
				    updated_at = CURRENT_TIMESTAMP
				""", key, value, user.id());
		jdbc.update("""
				INSERT INTO security_audit_log (actor_id, action, target_type, details)
				VALUES (?, 'system_setting_updated', 'system_setting', jsonb_build_object('key', ?))
				""", user.id(), key);
		return Map.of("status", "updated");
	}

	@GetMapping("/admin/settings")
	public List<Map<String, Object>> settings(@RequestAttribute("authenticatedUser") AuthUser user) {
		requireAdmin(user);
		return jdbc.queryForList("""
				SELECT setting_key AS settingKey,
				       CASE WHEN LOWER(setting_key) REGEXP '(secret|password|token|key)'
				            THEN '[REDACTED]' ELSE setting_value END AS settingValue,
				       updated_at AS updatedAt
				FROM system_settings ORDER BY setting_key
				""");
	}

	private Map<String, Object> transactionForUpdate(long transactionId, long requesterId) {
		List<Map<String, Object>> rows = jdbc.queryForList("""
				SELECT id, product_id, buyer_id, seller_id, transaction_type, status, amount
				FROM transactions WHERE id = ? AND (buyer_id = ? OR seller_id = ?) FOR UPDATE
				""", transactionId, requesterId, requesterId);
		if (rows.isEmpty()) throw ApiException.notFound("La transacción no existe o no tienes acceso.");
		return rows.getFirst();
	}

	private Map<String, Object> ownedProduct(long productId, long ownerId) {
		List<Map<String, Object>> rows = jdbc.queryForList(
				"SELECT id, status FROM products WHERE id = ? AND seller_id = ? AND deleted_at IS NULL",
				productId, ownerId);
		if (rows.isEmpty()) throw ApiException.notFound("La publicación no existe o no te pertenece.");
		return rows.getFirst();
	}

	private Map<String, Object> findProduct(long productId) {
		List<Map<String, Object>> rows = jdbc.queryForList(
				"SELECT id, seller_id, status FROM products WHERE id = ? AND deleted_at IS NULL", productId);
		if (rows.isEmpty()) throw ApiException.notFound("El producto no existe.");
		return rows.getFirst();
	}

	private void requireCategory(long categoryId) {
		if (jdbc.queryForObject("SELECT COUNT(*) FROM categories WHERE id = ? AND active = TRUE", Integer.class, categoryId) == 0) {
			throw ApiException.badRequest("La categoría no existe o está inactiva.");
		}
	}

	private void requireAdmin(AuthUser user) {
		if (!user.hasRole("ADMIN") && !user.hasRole("SUPER_ADMIN")) {
			throw ApiException.forbidden("Se requiere el rol administrador.");
		}
	}

	private void requireSuperAdmin(AuthUser user) {
		if (!user.hasRole("SUPER_ADMIN")) throw ApiException.forbidden("Se requiere el rol Super Admin.");
	}

	private void requireCanManageUser(AuthUser actor, long targetId) {
		List<String> roles = jdbc.queryForList("""
				SELECT r.name FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ?
				""", String.class, targetId);
		if (roles.isEmpty()) throw ApiException.notFound("El usuario no existe.");
		if (roles.contains("SUPER_ADMIN") && actor.id() != targetId) {
			throw ApiException.forbidden("La cuenta Super Admin no puede ser modificada por otro administrador.");
		}
		if (roles.contains("ADMIN") && !actor.hasRole("SUPER_ADMIN")) {
			throw ApiException.forbidden("Solo Super Admin puede modificar cuentas de administrador.");
		}
		if (actor.id() == targetId && (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN"))) {
			throw ApiException.forbidden("No puedes cambiar el estado de tu propia cuenta privilegiada.");
		}
	}

	private boolean exists(String sql, long id) {
		return jdbc.queryForObject(sql, Integer.class, id) > 0;
	}

	private static long generatedId(KeyHolder keys) {
		Number key = keys.getKeyList().isEmpty() ? null : (Number) keys.getKeyList().getFirst().values().stream()
				.filter(Number.class::isInstance).map(Number.class::cast).findFirst().orElse(null);
		if (key == null) throw new IllegalStateException("La base de datos no devolvió el identificador generado.");
		return key.longValue();
	}

	private static double parseDouble(String value, double fallback) {
		if (value == null) return fallback;
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private BigDecimal calculatePlatformFee(BigDecimal amount) {
		double percent = paymentSettingDouble("payment.platform_fee_percent", 0);
		double fixed = paymentSettingDouble("payment.platform_fee_fixed", 0);
		BigDecimal percentageFee = amount.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
		return percentageFee.add(BigDecimal.valueOf(fixed).setScale(2, java.math.RoundingMode.HALF_UP))
				.min(amount).max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);
	}

	private double paymentSettingDouble(String key, double fallback) {
		List<String> values = jdbc.queryForList("SELECT setting_value FROM system_settings WHERE setting_key = ?", String.class, key);
		return values.isEmpty() ? fallback : parseDouble(values.getFirst(), fallback);
	}

	private static long longValue(Object value, String field) {
		try {
			if (value instanceof Number number) return number.longValue();
			return Long.parseLong(String.valueOf(value));
		} catch (NumberFormatException | NullPointerException exception) {
			throw ApiException.badRequest("El campo '" + field + "' debe ser un número entero.");
		}
	}

	private static Long nullableLong(Object value, String field) {
		return value == null ? null : longValue(value, field);
	}

	private static BigDecimal decimalValue(Object value, String field) {
		try {
			return new BigDecimal(String.valueOf(value));
		} catch (NumberFormatException | NullPointerException exception) {
			throw ApiException.badRequest("El campo '" + field + "' debe ser numérico.");
		}
	}

	private static String optionalText(Map<String, Object> body, String key, String fallback) {
		Object value = body.get(key);
		if (value == null) return fallback;
		if (!(value instanceof String text)) throw ApiException.badRequest("El campo '" + key + "' debe ser texto.");
		String result = text.trim();
		if (result.isEmpty()) return fallback;
		if (result.length() > 5000) throw ApiException.badRequest("El campo '" + key + "' es demasiado largo.");
		return result;
	}

	private static boolean isHttpUrl(String value) {
		return value.matches("(?i)^https?://[^\\s]+$");
	}
}

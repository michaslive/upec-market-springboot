package com.upecmarket.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class ProductEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "seller_id", nullable = false)
	private UserAccountEntity seller;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private CategoryEntity category;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "condition_type", nullable = false, length = 20)
	private String conditionType;

	@Column(name = "listing_type", nullable = false, length = 20)
	private String listingType;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "image_url", length = 1000)
	private String imageUrl;

	@Column(name = "reserved_by")
	private Long reservedBy;

	@Column(name = "reservation_expires_at")
	private LocalDateTime reservationExpiresAt;

	@Column(name = "reviewed_by")
	private Long reviewedBy;

	@Column(name = "reviewed_at")
	private LocalDateTime reviewedAt;

	@Column(name = "review_note", length = 500)
	private String reviewNote;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	protected ProductEntity() {
	}

	public ProductEntity(UserAccountEntity seller, CategoryEntity category, String title, String description,
			BigDecimal price, String conditionType, String listingType, String imageUrl) {
		this.seller = seller;
		this.category = category;
		this.title = title;
		this.description = description;
		this.price = price;
		this.conditionType = conditionType;
		this.listingType = listingType;
		this.imageUrl = imageUrl;
		this.status = "pending";
	}

	public void update(CategoryEntity category, String title, String description, BigDecimal price,
			String conditionType, String listingType, String imageUrl) {
		this.category = category;
		this.title = title;
		this.description = description;
		this.price = price;
		this.conditionType = conditionType;
		this.listingType = listingType;
		this.imageUrl = imageUrl;
		this.status = "pending";
		this.reviewedBy = null;
		this.reviewedAt = null;
		this.reviewNote = null;
	}

	public void approve(long reviewerId) {
		this.status = "approved";
		this.reviewedBy = reviewerId;
		this.reviewedAt = LocalDateTime.now();
		this.reviewNote = null;
	}

	public void reject(long reviewerId, String note) {
		this.status = "rejected";
		this.reviewedBy = reviewerId;
		this.reviewedAt = LocalDateTime.now();
		this.reviewNote = note;
	}

	public void softDelete() {
		this.status = "deleted";
		this.deletedAt = LocalDateTime.now();
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
		this.status = "pending";
		this.reviewedBy = null;
		this.reviewedAt = null;
		this.reviewNote = null;
	}
	public Long getId() { return id; }
	public UserAccountEntity getSeller() { return seller; }
	public CategoryEntity getCategory() { return category; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public BigDecimal getPrice() { return price; }
	public String getConditionType() { return conditionType; }
	public String getListingType() { return listingType; }
	public String getStatus() { return status; }
	public String getImageUrl() { return imageUrl; }
	public Long getReservedBy() { return reservedBy; }
	public LocalDateTime getReservationExpiresAt() { return reservationExpiresAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}

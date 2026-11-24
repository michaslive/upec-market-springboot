package com.upecmarket.backend.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccountEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Column(name = "profile_photo_url", length = 500)
	private String profilePhotoUrl;

	@Column(name = "payout_bank", length = 80)
	private String payoutBank;

	@Column(name = "payout_account", length = 120)
	private String payoutAccount;

	@Column(name = "payout_holder", length = 120)
	private String payoutHolder;

	@Column(name = "payout_identifier", length = 120)
	private String payoutIdentifier;

	@Column(name = "account_status", nullable = false, length = 20)
	private String accountStatus;

	@Column(name = "two_factor_secret", length = 255)
	private String twoFactorSecret;

	@Column(name = "two_factor_enabled", nullable = false)
	private boolean twoFactorEnabled;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	@ManyToMany
	@JoinTable(name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<RoleEntity> roles = new HashSet<>();

	protected UserAccountEntity() {
	}

	public Long getId() { return id; }
	public String getEmail() { return email; }
	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getProfilePhotoUrl() { return profilePhotoUrl; }
	public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
	public String getPayoutBank() { return payoutBank; }
	public void setPayoutBank(String payoutBank) { this.payoutBank = payoutBank; }
	public String getPayoutAccount() { return payoutAccount; }
	public void setPayoutAccount(String payoutAccount) { this.payoutAccount = payoutAccount; }
	public String getPayoutHolder() { return payoutHolder; }
	public void setPayoutHolder(String payoutHolder) { this.payoutHolder = payoutHolder; }
	public String getPayoutIdentifier() { return payoutIdentifier; }
	public void setPayoutIdentifier(String payoutIdentifier) { this.payoutIdentifier = payoutIdentifier; }
	public String getAccountStatus() { return accountStatus; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public Set<RoleEntity> getRoles() { return Set.copyOf(roles); }
}

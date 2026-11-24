package com.upecmarket.backend.service;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.upecmarket.backend.ApiException;
import com.upecmarket.backend.AuthUser;
import com.upecmarket.backend.dto.ProfileUpdateRequest;
import com.upecmarket.backend.entity.UserAccountEntity;
import com.upecmarket.backend.repository.UserAccountRepository;

@Service
public class ProfileService {
	private final UserAccountRepository users;
	private final JdbcTemplate jdbc;
	private final ImageStorageService imageStorage;

	public ProfileService(UserAccountRepository users, JdbcTemplate jdbc, ImageStorageService imageStorage) {
		this.users = users;
		this.jdbc = jdbc;
		this.imageStorage = imageStorage;
	}

	@Transactional
	public Map<String, Object> update(AuthUser actor, ProfileUpdateRequest request) {
		UserAccountEntity user = findUser(actor.id());
		user.setFullName(request.fullName().trim());
		user.setPayoutBank(blankToNull(request.payoutBank()));
		user.setPayoutAccount(blankToNull(request.payoutAccount()));
		user.setPayoutHolder(blankToNull(request.payoutHolder()));
		user.setPayoutIdentifier(blankToNull(request.payoutIdentifier()));
		users.flush();
		return currentProfile(actor);
	}

	@Transactional
	public Map<String, Object> uploadPhoto(AuthUser actor, MultipartFile photo) {
		UserAccountEntity user = findUser(actor.id());
		user.setProfilePhotoUrl(imageStorage.store(photo));
		users.flush();
		return currentProfile(actor);
	}

	private UserAccountEntity findUser(long id) {
		return users.findById(id).orElseThrow(() -> ApiException.notFound("La cuenta ya no existe."));
	}

	private Map<String, Object> currentProfile(AuthUser actor) {
		Map<String, Object> profile = jdbc.queryForMap("""
				SELECT id, email, full_name AS fullName, profile_photo_url AS profilePhotoUrl,
				       account_status AS accountStatus, payout_bank AS payoutBank,
				       payout_account AS payoutAccount, payout_holder AS payoutHolder,
				       payout_identifier AS payoutIdentifier, created_at AS createdAt
				FROM users WHERE id = ?
				""", actor.id());
		profile.put("roles", actor.roles());
		return profile;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}

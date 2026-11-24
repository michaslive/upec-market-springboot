package com.upecmarket.backend;

import java.util.Set;

public record AuthUser(long id, String email, String fullName, Set<String> roles) {
	public boolean hasRole(String role) {
		return roles.contains(role);
	}
}

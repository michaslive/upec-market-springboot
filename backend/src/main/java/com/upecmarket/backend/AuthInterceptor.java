package com.upecmarket.backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
	private final AuthService authService;

	public AuthInterceptor(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String path = request.getRequestURI();
		String method = request.getMethod();
		if (isPublic(method, path)) {
			return true;
		}
		String authorization = request.getHeader("Authorization");
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw ApiException.unauthorized("Se requiere una sesión Bearer válida.");
		}
		AuthUser user = authService.authenticate(authorization.substring(7).trim());
		if (user == null) {
			throw ApiException.unauthorized("La sesión expiró o no es válida.");
		}
		request.setAttribute("authenticatedUser", user);
		request.setAttribute("rawToken", authorization.substring(7).trim());
		return true;
	}

	private boolean isPublic(String method, String path) {
		return method.equals("OPTIONS") || path.equals("/api/health")
				|| (method.equals("POST") && (path.equals("/api/auth/register") || path.equals("/api/auth/login")))
				|| (method.equals("GET") && (path.equals("/api/products") || path.matches("/api/products/\\d+")
						|| path.equals("/api/categories") || path.equals("/api/payment-config")
						|| path.matches("/api/users/\\d+/profile")));
	}
}

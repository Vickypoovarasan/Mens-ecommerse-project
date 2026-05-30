package com.example.ecommerse.auth;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.ecommerse.repo.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String auth = request.getHeader("Authorization");
		if (auth != null && auth.startsWith("Bearer ")) {
			String token = auth.substring("Bearer ".length());
			try {
				Claims claims = Jwts.parser()
						.verifyWith(jwtService.key())
						.build()
						.parseSignedClaims(token)
						.getPayload();

				String subject = claims.getSubject();
				String role = claims.get("role", String.class);
				if (subject != null && role != null) {
					Long userId = Long.parseLong(subject);
					boolean exists = userRepository.existsById(userId);
					if (exists) {
						Authentication a = new UsernamePasswordAuthenticationToken(
								userId,
								null,
								java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role)));
						SecurityContextHolder.getContext().setAuthentication(a);
					}
				}
			} catch (Exception ignored) {
				// ignore invalid token; request stays unauthenticated
			}
		}

		filterChain.doFilter(request, response);
	}
}


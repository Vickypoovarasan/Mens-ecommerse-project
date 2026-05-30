package com.example.ecommerse.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerse.repo.UserRepository;

@RestController
@RequestMapping("/api")
public class MeController {

	private final UserRepository userRepository;

	public MeController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping("/me")
	public MeResponse me(Authentication authentication) {
		Long userId = (Long) authentication.getPrincipal();
		return userRepository.findById(userId)
				.map(u -> new MeResponse(
						u.getId(),
						u.getEmail(),
						u.getUsername(),
						u.getRole(),
						u.isVerified(),
						u.getAddress()))
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	public record MeResponse(
			Long id,
			String email,
			String username,
			String role,
			boolean verified,
			String address) {
	}
}


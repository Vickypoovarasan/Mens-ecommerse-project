package com.example.ecommerse.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.ecommerse.domain.User;
import com.example.ecommerse.repo.UserRepository;

@Component
public class AdminUserInitializer implements ApplicationRunner {

	private static final String ADMIN_EMAIL = "admin@atelier.local";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AdminUserInitializer(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
			return;
		}
		User admin = new User();
		admin.setUsername("admin");
		admin.setEmail(ADMIN_EMAIL);
		admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
		admin.setRole("ADMIN");
		admin.setVerified(true);
		userRepository.save(admin);
	}
}

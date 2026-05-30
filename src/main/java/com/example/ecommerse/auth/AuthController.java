package com.example.ecommerse.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerse.auth.dto.RegisterRequest;
import com.example.ecommerse.auth.dto.RegisterResponse;
import com.example.ecommerse.auth.dto.LoginRequest;
import com.example.ecommerse.auth.dto.LoginResponse;
import com.example.ecommerse.auth.dto.ResendOtpRequest;
import com.example.ecommerse.auth.dto.ResendOtpResponse;
import com.example.ecommerse.auth.dto.ForgotPasswordRequest;
import com.example.ecommerse.auth.dto.ForgotPasswordResponse;
import com.example.ecommerse.auth.dto.ResetPasswordRequest;
import com.example.ecommerse.auth.dto.ResetPasswordResponse;
import com.example.ecommerse.auth.dto.VerifyOtpRequest;
import com.example.ecommerse.auth.dto.VerifyOtpResponse;

import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

	private final OtpService otpService;
	private final AuthService authService;
	private final PasswordResetService passwordResetService;

	public AuthController(OtpService otpService, AuthService authService, PasswordResetService passwordResetService) {
		this.otpService = otpService;
		this.authService = authService;
		this.passwordResetService = passwordResetService;
	}

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
		otpService.register(req);
		return ResponseEntity.ok(new RegisterResponse("Registered. OTP sent to your email."));
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
		otpService.verifyOtp(req.email(), req.otp());
		return ResponseEntity.ok(new VerifyOtpResponse("OTP verified. Account activated."));
	}

	@PostMapping("/resend-otp")
	public ResponseEntity<ResendOtpResponse> resendOtp(@Valid @RequestBody ResendOtpRequest req) {
		otpService.resendOtp(req.email());
		return ResponseEntity.ok(new ResendOtpResponse("OTP resent to your email."));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
		String message = passwordResetService.requestReset(req.email());
		return ResponseEntity.ok(new ForgotPasswordResponse(message));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
		passwordResetService.resetPassword(req);
		return ResponseEntity.ok(new ResetPasswordResponse("Password updated. You can log in now."));
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
		AuthService.LoginResult result = authService.login(req.email(), req.password());
		setRefreshCookie(response, result.refreshToken());
		return ResponseEntity.ok(result.response());
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = readRefreshCookie(request);
		AuthService.LoginResult result = authService.refresh(refreshToken);
		setRefreshCookie(response, result.refreshToken());
		return ResponseEntity.ok(result.response());
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = readRefreshCookie(request);
		authService.logout(refreshToken);
		clearRefreshCookie(response);
		return ResponseEntity.noContent().build();
	}

	private static void setRefreshCookie(HttpServletResponse response, String token) {
		Cookie cookie = new Cookie("refresh_token", token);
		cookie.setHttpOnly(true);
		cookie.setSecure(false);
		cookie.setPath("/api/auth");
		cookie.setMaxAge((int) Duration.ofDays(30).toSeconds());
		response.addCookie(cookie);
	}

	private static void clearRefreshCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie("refresh_token", "");
		cookie.setHttpOnly(true);
		cookie.setSecure(false);
		cookie.setPath("/api/auth");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	private static String readRefreshCookie(jakarta.servlet.http.HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			throw new IllegalArgumentException("Missing refresh token cookie");
		}
		for (Cookie c : cookies) {
			if ("refresh_token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
				return c.getValue();
			}
		}
		throw new IllegalArgumentException("Missing refresh token cookie");
	}
}


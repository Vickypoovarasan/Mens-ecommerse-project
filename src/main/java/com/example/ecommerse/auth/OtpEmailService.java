package com.example.ecommerse.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends OTP by SMTP when configured; otherwise logs to console (existing dev behavior).
 */
@Service
public class OtpEmailService {

	private static final Logger log = LoggerFactory.getLogger(OtpEmailService.class);

	private final JavaMailSender mailSender;

	@Value("${spring.mail.host:}")
	private String mailHost;

	@Value("${app.mail.from:noreply@atelier.local}")
	private String fromAddress;

	@Value("${app.mail.enabled:false}")
	private boolean mailEnabledFlag;

	public OtpEmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Async
	public void sendOtp(String toEmail, String otp, String purpose) {
		if (isSmtpConfigured()) {
			try {
				SimpleMailMessage message = new SimpleMailMessage();
				message.setFrom(fromAddress);
				message.setTo(toEmail);
				message.setSubject("Atelier — your verification code");
				message.setText(buildBody(otp, purpose));
				mailSender.send(message);
				log.info("OTP email sent to {} ({})", toEmail, purpose);
				return;
			} catch (Exception ex) {
				log.warn("SMTP send failed for {}: {} — falling back to console OTP", toEmail, ex.getMessage());
			}
		}
		log.info("EMAIL_SIMULATION OTP for {} is {} ({})", toEmail, otp, purpose);
	}

	private boolean isSmtpConfigured() {
		if (mailEnabledFlag) {
			return mailHost != null && !mailHost.isBlank();
		}
		return mailHost != null && !mailHost.isBlank()
				&& fromAddress != null && !fromAddress.isBlank();
	}

	private static String buildBody(String otp, String purpose) {
		return """
				Hello,

				Your Atelier verification code is: %s

				Purpose: %s
				This code expires in 5 minutes.

				If you did not request this, you can ignore this email.

				— Atelier Menswear
				""".formatted(otp, purpose);
	}
}

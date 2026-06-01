CREATE TABLE IF NOT EXISTS idempotency_keys (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  idem_key VARCHAR(80) NOT NULL,
  request_hash VARBINARY(32) NOT NULL,
  response_json JSON NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_idem_user_key (user_id, idem_key),
  KEY idx_idem_user (user_id),
  CONSTRAINT fk_idem_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

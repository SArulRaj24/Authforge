CREATE TABLE audit_logs (
                            id BIGSERIAL PRIMARY KEY,

                            user_id BIGINT REFERENCES app_users(id)
                                               ON DELETE SET NULL,

                            event_type VARCHAR(50) NOT NULL,

                            ip_address VARCHAR(45),

                            user_agent VARCHAR(500),

                            details TEXT,

                            created_at TIMESTAMP WITH TIME ZONE
                                NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id
    ON audit_logs(user_id);

CREATE INDEX idx_audit_logs_event_type
    ON audit_logs(event_type);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs(created_at);
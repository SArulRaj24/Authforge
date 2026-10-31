CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,

                                user_id BIGINT NOT NULL
                                    REFERENCES app_users(id)
                                        ON DELETE CASCADE,

                                token_hash VARCHAR(64) NOT NULL UNIQUE,

                                family_id UUID NOT NULL,

                                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                created_at TIMESTAMP WITH TIME ZONE
                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                revoked_at TIMESTAMP WITH TIME ZONE,

                                replaced_by_token_id BIGINT
                                    REFERENCES refresh_tokens(id)
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_family_id
    ON refresh_tokens(family_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at);
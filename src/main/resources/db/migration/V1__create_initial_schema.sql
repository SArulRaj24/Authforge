CREATE TABLE app_users (
                           id BIGSERIAL PRIMARY KEY,

                           email VARCHAR(255) NOT NULL UNIQUE,

                           password_hash VARCHAR(255),

                           full_name VARCHAR(100) NOT NULL,

                           provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',

                           enabled BOOLEAN NOT NULL DEFAULT TRUE,

                           created_at TIMESTAMP WITH TIME ZONE
                               NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           updated_at TIMESTAMP WITH TIME ZONE
                               NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,

                       name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL REFERENCES app_users(id),
                            role_id BIGINT NOT NULL REFERENCES roles(id),

                            PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (name)
VALUES ('ROLE_USER'), ('ROLE_ADMIN');
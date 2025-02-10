INSERT INTO users(created_at, updated_at, full_name, email, password, role, enabled)
VALUES (
    NOW(),
    NOW(),
    'Admin User',
    'admin@example.com',
    '$2a$10$vsnP07.J4FcUUjsCDc6f0uoJ8jgTPZeCJ8e7BgN5ZktaLw1whNlhG',
    'ADMIN',
    TRUE
);

CREATE TABLE admin_password_reset_requests (
    request_id UUID PRIMARY KEY,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

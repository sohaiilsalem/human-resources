-- Runs once when the container's data volume is created (as the owner/superuser from POSTGRES_USER).
-- The application role: can log in, owns nothing, so row-level security applies to it.
create role hr_app login password 'hr_app';

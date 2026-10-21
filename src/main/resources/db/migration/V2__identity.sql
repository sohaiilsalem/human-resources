-- Identity: tenant tables, isolated by row-level security on app.tenant_id.

create table users
(
    id            uuid primary key,
    tenant_id     uuid         not null references tenants (id),
    email         varchar(254) not null,
    full_name     varchar(200) not null,
    password_hash varchar(100) not null,
    locale        varchar(5)   not null,
    status        varchar(20)  not null,
    employee_id   uuid,
    last_login_at timestamptz,
    created_at    timestamptz  not null,
    created_by    uuid,
    updated_at    timestamptz  not null,
    updated_by    uuid,
    version       bigint       not null,
    unique (tenant_id, email)
);

-- No tenant_id of its own: visible only through a user row the current tenant can see.
create table user_roles
(
    user_id uuid        not null references users (id) on delete cascade,
    role    varchar(30) not null,
    primary key (user_id, role)
);

create table refresh_tokens
(
    id          uuid primary key,
    tenant_id   uuid        not null references tenants (id),
    user_id     uuid        not null references users (id) on delete cascade,
    token_hash  varchar(64) not null unique,
    expires_at  timestamptz not null,
    revoked_at  timestamptz,
    replaced_by uuid,
    created_at  timestamptz not null,
    created_by  uuid,
    updated_at  timestamptz not null,
    updated_by  uuid,
    version     bigint      not null
);
create index refresh_tokens_user_idx on refresh_tokens (tenant_id, user_id);

alter table users enable row level security;
alter table users force row level security;
create policy tenant_isolation on users
    using (tenant_id = app_current_tenant())
    with check (tenant_id = app_current_tenant());

alter table user_roles enable row level security;
alter table user_roles force row level security;
create policy tenant_isolation on user_roles
    using (exists (select 1 from users u where u.id = user_id))
    with check (exists (select 1 from users u where u.id = user_id));

alter table refresh_tokens enable row level security;
alter table refresh_tokens force row level security;
create policy tenant_isolation on refresh_tokens
    using (tenant_id = app_current_tenant())
    with check (tenant_id = app_current_tenant());

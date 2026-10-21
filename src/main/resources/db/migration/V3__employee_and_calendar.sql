-- Minimal employee master and work calendar (extended in Phase 1 — Core HR).

create table employees
(
    id               uuid primary key,
    tenant_id        uuid         not null references tenants (id),
    code             varchar(30)  not null,
    full_name_en     varchar(200) not null,
    full_name_ar     varchar(200) not null,
    national_id      varchar(14),
    gender           varchar(10)  not null,
    birth_date       date         not null,
    hire_date        date         not null,
    status           varchar(20)  not null,
    termination_date date,
    manager_id       uuid references employees (id),
    disability       boolean      not null,
    created_at       timestamptz  not null,
    created_by       uuid,
    updated_at       timestamptz  not null,
    updated_by       uuid,
    version          bigint       not null,
    unique (tenant_id, code)
);
create index employees_manager_idx on employees (tenant_id, manager_id);

alter table users
    add constraint users_employee_fk foreign key (employee_id) references employees (id);
create unique index users_employee_uq on users (tenant_id, employee_id) where employee_id is not null;

create table work_calendars
(
    id         uuid primary key,
    tenant_id  uuid        not null unique references tenants (id),
    rest_days  varchar(70) not null,
    created_at timestamptz not null,
    created_by uuid,
    updated_at timestamptz not null,
    updated_by uuid,
    version    bigint      not null
);

create table public_holidays
(
    id         uuid primary key,
    tenant_id  uuid         not null references tenants (id),
    start_date date         not null,
    end_date   date         not null,
    name_en    varchar(200) not null,
    name_ar    varchar(200) not null,
    created_at timestamptz  not null,
    created_by uuid,
    updated_at timestamptz  not null,
    updated_by uuid,
    version    bigint       not null,
    check (end_date >= start_date)
);
create index public_holidays_range_idx on public_holidays (tenant_id, start_date, end_date);

do $$
    declare
        t text;
    begin
        foreach t in array array ['employees', 'work_calendars', 'public_holidays']
            loop
                execute format('alter table %I enable row level security', t);
                execute format('alter table %I force row level security', t);
                execute format('create policy tenant_isolation on %I using (tenant_id = app_current_tenant())'
                                   || ' with check (tenant_id = app_current_tenant())', t);
            end loop;
    end
$$;

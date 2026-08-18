create table users (
    id             bigserial primary key,
    name           varchar(100)   not null,
    email          varchar(150)   not null unique,
    monthly_income numeric(12, 2) not null check (monthly_income > 0),
    created_at     timestamp      not null
);

create table lender (
    id                 bigserial primary key,
    name               varchar(100)   not null,
    available_amount   numeric(14, 2) not null check (available_amount >= 0),
    min_income         numeric(12, 2) not null,
    base_interest_rate numeric(5, 2)  not null,
    max_tenure_months  integer        not null,
    active             boolean        not null default true
);

create table loan_application (
    id               bigserial primary key,
    user_id          bigint         not null references users (id),
    amount           numeric(14, 2) not null check (amount > 0),
    tenure_months    integer        not null check (tenure_months > 0),
    status           varchar(24)    not null,
    rejection_reason varchar(255),
    created_at       timestamp      not null
);

create index idx_loan_application_user on loan_application (user_id);

create table loan_offer (
    id             bigserial primary key,
    application_id bigint         not null references loan_application (id),
    lender_id      bigint         not null references lender (id),
    offered_amount numeric(14, 2) not null,
    interest_rate  numeric(5, 2)  not null,
    status         varchar(16)    not null,
    created_at     timestamp      not null
);

create index idx_loan_offer_application on loan_offer (application_id);

-- An application can have at most one accepted offer. This is the database-level
-- backstop for the accept flow; the service also takes a row lock on the
-- application before it writes.
create unique index idx_loan_offer_one_accepted
    on loan_offer (application_id)
    where status = 'ACCEPTED';

create table loan (
    id             bigserial primary key,
    application_id bigint         not null unique references loan_application (id),
    lender_id      bigint         not null references lender (id),
    principal      numeric(14, 2) not null,
    interest_rate  numeric(5, 2)  not null,
    tenure_months  integer        not null,
    emi_amount     numeric(12, 2) not null,
    status         varchar(16)    not null,
    disbursed_at   timestamp      not null
);

create table repayment (
    id                bigserial primary key,
    loan_id           bigint         not null references loan (id),
    instalment_number integer        not null,
    due_date          date           not null,
    amount            numeric(12, 2) not null,
    status            varchar(16)    not null,
    paid_at           timestamp,
    unique (loan_id, instalment_number)
);

create index idx_repayment_loan on repayment (loan_id);

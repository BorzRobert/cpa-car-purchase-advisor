-- Schema domeniului (Faza 4). SQL standard, fara tipuri specifice unui motor: aceeasi migrare trebuie sa treaca pe H2
-- (mod PostgreSQL, local si in teste) si pe PostgreSQL (Neon). Din acest motiv:
--   * id-urile datelor unui vizitator sunt UUID generate de aplicatie (fara secvente/identity, care difera intre motoare);
--   * coloanele de text liber sunt VARCHAR(n), nu TEXT (pe H2, TEXT devine CLOB si Hibernate il respinge la validare);
--   * JSON-ul din `message.metadata` este text simplu, nu `jsonb`/`json` (tipuri diferite pe cele doua motoare).

create table visitor_session (
    id           uuid primary key,
    token_hash   varchar(64) not null unique,   -- SHA-256 hex; tokenul brut nu se salveaza niciodata
    created_at   timestamp with time zone not null,
    last_seen_at timestamp with time zone not null
);

create table buyer_profile (
    session_id     uuid primary key references visitor_session (id) on delete cascade,
    budget_amount  numeric(12, 2),
    budget_kind    varchar(10),
    financing_mode varchar(10),
    yearly_km      integer,
    city_percent   integer,
    passengers     integer,
    body_types     varchar(200),    -- valori enum separate prin virgula
    powertrains    varchar(200),    -- valori enum separate prin virgula
    home_charging  boolean,
    condition_pref varchar(10),
    must_haves     varchar(1000),   -- elemente curatate (fara \n), separate prin \n
    deal_breakers  varchar(1000),
    updated_at     timestamp with time zone not null,
    constraint ck_profile_budget_kind check (budget_kind is null or budget_kind in ('TOTAL', 'MONTHLY')),
    constraint ck_profile_financing check (financing_mode is null or financing_mode in ('CASH', 'FINANCE')),
    constraint ck_profile_condition check (condition_pref is null or condition_pref in ('NEW', 'USED', 'ANY')),
    constraint ck_profile_budget check (budget_amount is null or budget_amount >= 0),
    constraint ck_profile_km check (yearly_km is null or yearly_km between 0 and 200000),
    constraint ck_profile_city check (city_percent is null or city_percent between 0 and 100),
    constraint ck_profile_passengers check (passengers is null or passengers between 1 and 9)
);

create table conversation (
    id         uuid primary key,
    session_id uuid not null references visitor_session (id) on delete cascade,
    title      varchar(120),
    summary    varchar(4000),       -- rezumatul rulant pentru memoria din Faza 5; gol pana atunci
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
create index ix_conversation_session on conversation (session_id, updated_at);

create table message (
    id              uuid primary key,
    conversation_id uuid not null references conversation (id) on delete cascade,
    message_role    varchar(10) not null,
    content         varchar(20000) not null,
    metadata        varchar(20000),  -- JSON ca text (tokeni, apeluri de unelte, carduri); folosit din Faza 5
    created_at      timestamp with time zone not null,
    constraint ck_message_role check (message_role in ('USER', 'ASSISTANT'))
);
create index ix_message_conversation on message (conversation_id, created_at);

create table vehicle (
    id                varchar(60) primary key,    -- slug stabil, de exemplu `vw-golf`
    make              varchar(40) not null,
    model             varchar(60) not null,
    body_type         varchar(20) not null,
    seats             integer not null,
    boot_litres       integer not null,
    euro_ncap_stars   integer,                    -- NULL = necunoscut, nu „0 stele”
    reliability_notes varchar(500) not null,
    constraint ck_vehicle_body check (body_type in ('HATCHBACK', 'SEDAN', 'ESTATE', 'SUV', 'MPV')),
    constraint ck_vehicle_seats check (seats between 2 and 9),
    constraint ck_vehicle_stars check (euro_ncap_stars is null or euro_ncap_stars between 0 and 5)
);

create table vehicle_variant (
    id                         varchar(80) primary key,
    vehicle_id                 varchar(60) not null references vehicle (id),
    name                       varchar(80) not null,
    powertrain                 varchar(20) not null,
    price_eur                  integer not null,          -- pret de lista orientativ, cu TVA, vehicul nou
    power_kw                   integer not null,
    consumption_l_100km        numeric(4, 1),             -- NULL la electrice
    consumption_kwh_100km      numeric(4, 1),             -- NULL la vehicule fara baterie externa
    electric_range_km          integer,
    battery_kwh                numeric(5, 1),
    co2_g_km                   integer not null,
    annual_insurance_eur       integer not null,          -- estimare RCA + CASCO
    annual_road_tax_eur        integer not null,
    annual_maintenance_eur     integer not null,
    residual_value_pct_5y      integer not null,          -- valoarea ramasa dupa 5 ani, in % din pretul nou
    data_as_of                 date not null,
    constraint ck_variant_powertrain check (powertrain in ('PETROL', 'DIESEL', 'HYBRID', 'PLUG_IN_HYBRID', 'ELECTRIC')),
    constraint ck_variant_price check (price_eur > 0),
    constraint ck_variant_residual check (residual_value_pct_5y between 0 and 100)
);
create index ix_variant_vehicle on vehicle_variant (vehicle_id);

create table shortlist_item (
    id         uuid primary key,
    session_id uuid not null references visitor_session (id) on delete cascade,
    variant_id varchar(80) not null references vehicle_variant (id),
    note       varchar(500),
    created_at timestamp with time zone not null,
    constraint uq_shortlist_session_variant unique (session_id, variant_id)
);

-- Contabilitatea de consum NU se sterge odata cu sesiunea (`on delete set null`): altfel „sterge-mi datele” + o sesiune
-- noua ar reseta bugetul global zilnic. Randurile ramase nu mai pot fi legate de nimeni.
create table usage_log (
    id            uuid primary key,
    session_id    uuid references visitor_session (id) on delete set null,
    usage_date    date not null,                -- ziua UTC la care se raporteaza bugetul
    model         varchar(60),
    input_tokens  integer not null,
    output_tokens integer not null,
    created_at    timestamp with time zone not null,
    constraint ck_usage_tokens check (input_tokens >= 0 and output_tokens >= 0)
);
create index ix_usage_session_day on usage_log (session_id, usage_date);
create index ix_usage_day on usage_log (usage_date);

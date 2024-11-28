create table if not exists subscription_product
(
    id                varchar primary key,
    e2_id             varchar,
    name              varchar,
    description       text,
    features          json,
    image_url         varchar,
    type              varchar,
    price_in_cents    bigint,
    creation_datetime timestamp
);

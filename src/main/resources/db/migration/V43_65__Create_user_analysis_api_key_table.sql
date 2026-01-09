CREATE table IF NOT EXISTS "user_analysis_api_key"
(
    id varchar
        CONSTRAINT user_analysis_api_key_pk PRIMARY KEY
        DEFAULT uuid_generate_v4(),
    user_id varchar NOT NULL,
    creation_datetime timestamp without time zone NOT NULL
        DEFAULT current_timestamp,
    expiration_datetime timestamp without time zone,
    api_key varchar,

    CONSTRAINT user_analysis_api_key_user_id_fk
        FOREIGN KEY (user_id)
            REFERENCES "user"(id)
);

CREATE INDEX "user_analysis_api_key_user_id_idx" ON "user_analysis_api_key" (user_id);
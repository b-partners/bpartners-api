create or replace function create_annual_billing_info_table()
    returns void
as
$$BEGIN
    create temporary table annual_billing_info
    (
        account_id varchar,
        name       varchar,
        bic        varchar,
        iban       varchar,
        january    numeric default 0,
        february   numeric default 0,
        march      numeric default 0,
        april      numeric default 0,
        may        numeric default 0,
        june       numeric default 0,
        july       numeric default 0,
        august     numeric default 0,
        september  numeric default 0,
        october    numeric default 0,
        november   numeric default 0,
        december   numeric default 0
    );
end;
$$
    language plpgsql;
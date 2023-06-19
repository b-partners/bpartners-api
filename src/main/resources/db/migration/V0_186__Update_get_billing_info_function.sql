drop function get_billing_info(date_from varchar, date_to varchar);

create or replace function get_billing_info(date_from varchar, date_to varchar)
    returns table
            (
                user_id        varchar,
                first_name     varchar,
                last_name      varchar,
                email varchar,
                amount_value   numeric,
                average        numeric,
                median         double precision,
                minimum_amount numeric,
                maximum_amount numeric,
                start_date     timestamp,
                end_date       timestamp
            )
as
$$
begin
    return query
        select pr.id_user                                                        as user_id,
               u.first_name                                                      as first_name,
               u.last_name                                                       as last_name,
               u.email as email,
               (sum(convert_fraction_to_numeric(pr.amount)))                     as amount_value,
               (avg(convert_fraction_to_numeric(pr.amount)))                     as average,
               percentile_cont(0.5) WITHIN GROUP ( ORDER BY convert_fraction_to_numeric(pr.amount))
                                                                                 as median,
               min(convert_fraction_to_numeric(pr.amount))
                                                                                 as minimum_amount,
               max(convert_fraction_to_numeric(pr.amount))
                                                                                 as maximum_amount,
               to_timestamp(date_from, 'YYYY-MM-DD HH:MI:SS') AT TIME ZONE 'UTC' as start_date,
               to_timestamp(date_to, 'YYYY-MM-DD HH:MI:SS') AT TIME ZONE 'UTC'   as end_date
        from payment_request pr
                 inner join "user" u on pr.id_user = u.id
        where pr.status = 'PAID'
          and pr.created_datetime between to_timestamp(date_from, 'YYYY-MM-DD HH:MI:SS') and to_timestamp(date_to, 'YYYY-MM-DD HH:MI:SS')
        group by pr.id_user, u.first_name, u.last_name, u.email;
end ;
$$
    language plpgsql;
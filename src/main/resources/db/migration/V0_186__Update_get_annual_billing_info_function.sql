drop function get_annual_billing_info(year integer);

create or replace function get_annual_billing_info(year integer)
    returns table
            (
                user_id varchar,
                first_name       varchar,
                last_name        varchar,
                january    numeric,
                february   numeric,
                march      numeric,
                april      numeric,
                may        numeric,
                june       numeric,
                july       numeric,
                august     numeric,
                september  numeric,
                october    numeric,
                november   numeric,
                december   numeric
            )
as
$$
DECLARE
    rec RECORD;
BEGIN
    perform create_annual_billing_info_table();
    perform insert_user_info_into_temp_table();
    FOR rec in select * from get_billing_info_by_year_and_month(year, 1)
        LOOP
            UPDATE annual_billing_info as a
            set january =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.january end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 2)
        LOOP
            UPDATE annual_billing_info as a
            set february =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.february end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 3)
        LOOP
            UPDATE annual_billing_info as a
            set march =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.march end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 4)
        LOOP
            UPDATE annual_billing_info as a
            set april =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.april end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 5)
        LOOP
            UPDATE annual_billing_info as a
            set may =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.may end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 6)
        LOOP
            UPDATE annual_billing_info as a
            set june =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.june end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 7)
        LOOP
            UPDATE annual_billing_info as a
            set july =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.july end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 8)
        LOOP
            UPDATE annual_billing_info as a
            set august =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.august end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 9)
        LOOP
            UPDATE annual_billing_info as a
            set september =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.september end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 10)
        LOOP
            UPDATE annual_billing_info as a
            set october =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.october end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 11)
        LOOP
            UPDATE annual_billing_info as a
            set november =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.november end
            where a.user_id = rec.user_id;
        end loop;
    FOR rec in select * from get_billing_info_by_year_and_month(year, 12)
        LOOP
            UPDATE annual_billing_info as a
            set december =
                    case
                        when rec.user_id is not null then rec.amount_value
                        else a.december end
            where a.user_id = rec.user_id;
        end loop;
    return query select * from annual_billing_info;
    drop table annual_billing_info;
end;
$$
    language plpgsql;

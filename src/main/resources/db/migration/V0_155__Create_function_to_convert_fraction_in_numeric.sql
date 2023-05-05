create or replace function convert_fraction_to_numeric(input varchar)
    returns numeric
as
$$BEGIN
    return (cast(split_part(input, '/', 1) as numeric)
        / cast(split_part(input, '/', 2) as
                numeric)) / 100 ;
END
$$
    language plpgsql;
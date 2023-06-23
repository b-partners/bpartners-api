update "account"
set available_balance = (
                            ((cast(split_part(available_balance, '/', 1) as numeric)
                                / cast(split_part(available_balance, '/', 2) as numeric))
                                * 100)::integer
                            )::varchar || '/1';
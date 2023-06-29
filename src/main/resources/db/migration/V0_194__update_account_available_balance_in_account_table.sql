update "account" set available_balance = (cast(available_balance AS numeric) * 100)::integer || '/1';

alter table if exists temp_subscription_termination
    rename to subscription_termination;

alter index if exists temp_subscription_termination_pkey
    rename to subscription_termination_pkey;

alter index if exists temp_subscription_termination_user_subscription_e2_id_key
    rename to subscription_termination_user_subscription_e2_id_key;

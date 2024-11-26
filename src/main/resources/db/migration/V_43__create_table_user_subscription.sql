create table user_subscription (
                                   id varchar primary key,
                                   e2_id varchar,
                                   active boolean,
                                   payment_methods json,
                                   subscription_product_id varchar references user_subscription_product(id),
                                   free_trial_days bigint,
                                   free_trial_start timestamp,
                                   free_trial_end timestamp,
                                   end_datetime timestamp,
                                   start_datetime timestamp
);

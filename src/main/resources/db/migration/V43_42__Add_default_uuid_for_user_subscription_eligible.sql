DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 'eligibility'
                         AND column_name = 'id'
                         AND column_default LIKE 'uuid_generate_v4()%') THEN
            ALTER TABLE user_subscription_eligible
                ALTER COLUMN id SET DEFAULT uuid_generate_v4();
        END IF;
    END
$$;
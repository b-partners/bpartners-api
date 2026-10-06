DO
$$
BEGIN
UPDATE "user"
SET roles = array_append(roles, 'SUBSCRIPTION_INVOICE_EXPORTER')
WHERE email = 'joe@email.com'
  AND NOT 'SUBSCRIPTION_INVOICE_EXPORTER' = ANY(roles);
END
$$;

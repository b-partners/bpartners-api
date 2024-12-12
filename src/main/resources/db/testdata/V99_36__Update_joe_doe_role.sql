DO
$$
BEGIN
UPDATE "user"
SET roles = array_append(roles, 'ADMIN_ROLE')
WHERE email = 'joe@email.com'
  AND NOT 'ADMIN_ROLE' = ANY(roles);
END
$$;

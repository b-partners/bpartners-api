update "user_whitelisted"
set scopes = '[
  "PROSPECT_EXISTING_MAIL_CREATION_ALLOWED"
]'::jsonb;


update "user_whitelisted"
set scopes = COALESCE(scopes, '[]'::jsonb) || '[
  "API_KEY_NOT_RESTRICTED_BY_TRIAL", "SUBSCRIPTION_VALIDATION_NOT_REQUIRED"
]'::jsonb
where user_id in (select id_user from "user_api_key_full_authorization");

insert into "user_whitelisted" (user_id, scopes)
select id_user, '[
  "API_KEY_NOT_RESTRICTED_BY_TRIAL",
  "SUBSCRIPTION_VALIDATION_NOT_REQUIRED"
]'::jsonb
from "user_api_key_full_authorization" u
where not exists (
    select 1
    from "user_whitelisted" w
    where w.user_id = u.id_user
);

drop table if exists "user_api_key_full_authorization";
create or replace view view_prospect_artisan as
(
select p.*, ah.name as artisan_name
from view_prospect_actual_status p
         join account_holder ah on p.id_account_holder = ah.id);
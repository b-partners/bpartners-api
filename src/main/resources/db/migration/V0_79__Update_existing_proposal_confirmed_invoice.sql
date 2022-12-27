update invoice
set status = 'PROPOSAL_CONFIRMED'
from (select i.id, i.ref
from invoice i,
     (select distinct(ref) as distinct_ref from invoice) as not_null_ref,
     (select id, ref from invoice where status = 'CONFIRMED') conf_invoice
where i.ref = distinct_ref
  and i.ref = conf_invoice.ref
  and i.status = 'PROPOSAL') as proposal_confirmed_invoice
where invoice.id = proposal_confirmed_invoice.id;
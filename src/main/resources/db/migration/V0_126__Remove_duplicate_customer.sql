delete
from customer
where id not in (select min(id) from customer group by first_name, last_name, email);
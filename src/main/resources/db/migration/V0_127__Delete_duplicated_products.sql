delete
from product_template
where id not in (select min(id) from product_template group by (description, unit_price));
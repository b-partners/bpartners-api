alter table prospect
    alter column "name" drop not null;
alter table prospect
    alter column "email" drop not null;
alter table prospect
    alter column "phone" drop not null;
alter table prospect
    alter column "location" drop not null;
alter table prospect
    rename column "location" to "address"
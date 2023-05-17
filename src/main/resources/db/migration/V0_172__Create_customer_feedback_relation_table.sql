create table if not exists customer_feedback
(
    customer_id varchar,
    feedback_id varchar,
    constraint customer_feedback_pk primary key (customer_id, feedback_id),
    constraint customer_fk foreign key (customer_id) references "customer"(id),
    constraint feedback_fk foreign key (feedback_id) references "feedback"(id)
);
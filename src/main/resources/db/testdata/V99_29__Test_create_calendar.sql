insert into "prospect_evaluation_job"
(id, id_account_holder, job_status_message, job_status, "type", started_at, ended_at)
values ('pe_job_id1', 'b33e6eb0-e262-4596-a91f-20c6a7bfd343', null, 'NOT_STARTED', 'CALENDAR_EVENT_CONVERSION',
        '2022-01-01T0:00:00.00Z', null),
       ('pe_job_id2', 'b33e6eb0-e262-4596-a91f-20c6a7bfd343', null, 'IN_PROGRESS', 'CALENDAR_EVENT_CONVERSION',
        '2022-01-02T01:00:00.00Z', null),
       ('pe_job_id3', 'b33e6eb0-e262-4596-a91f-20c6a7bfd343', 'Connection reset', 'FAILED', 'ADDRESS_CONVERSION',
        '2022-01-03T01:00:00.00Z', '2022-01-03T02:00:00.00Z'),
       ('pe_job_id4', 'b33e6eb0-e262-4596-a91f-20c6a7bfd343', 'Successfully converted', 'FINISHED',
        'CALENDAR_EVENT_CONVERSION',
        '2022-01-05T01:00:00.00Z', '2022-01-05T02:00:00.00Z'),
       ('pe_job_id5', 'b33e6eb0-e262-4596-a91f-20c6a7bfd343', 'Successfully converted', 'FINISHED',
        'CALENDAR_EVENT_CONVERSION',
        '2022-01-04T01:00:00.00Z', '2022-01-04T02:00:00.00Z');
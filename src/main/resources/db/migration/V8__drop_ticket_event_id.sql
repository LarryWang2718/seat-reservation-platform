ALTER TABLE ticket
DROP CONSTRAINT fk_ticket_event;

ALTER TABLE ticket
DROP COLUMN event_id;

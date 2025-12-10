ALTER TABLE event
ADD COLUMN sale_start_time TIMESTAMP,
ADD COLUMN sale_end_time TIMESTAMP;

UPDATE event
SET sale_start_time = start_time,
    sale_end_time = end_time
WHERE sale_start_time IS NULL
   OR sale_end_time IS NULL;

ALTER TABLE event
ALTER COLUMN sale_start_time SET NOT NULL;

ALTER TABLE event
ALTER COLUMN sale_end_time SET NOT NULL;

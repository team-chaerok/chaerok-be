ALTER TABLE film_rolls
    ADD COLUMN exited_at TIMESTAMP,
    ADD COLUMN develop_available_at TIMESTAMP;

ALTER TABLE film_rolls
    ADD CONSTRAINT ck_film_rolls_exit_schedule
        CHECK (
            (exited_at IS NULL AND develop_available_at IS NULL)
            OR
            (
                exited_at IS NOT NULL
                AND develop_available_at = exited_at + INTERVAL '1 hour'
            )
        );

CREATE INDEX idx_film_rolls_auto_develop_due
    ON film_rolls(develop_available_at)
    WHERE status IN ('CAPTURING', 'READY')
      AND develop_available_at IS NOT NULL;

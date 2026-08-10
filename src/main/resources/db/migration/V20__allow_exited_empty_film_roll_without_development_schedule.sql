ALTER TABLE film_rolls
    DROP CONSTRAINT ck_film_rolls_exit_schedule;

ALTER TABLE film_rolls
    ADD CONSTRAINT ck_film_rolls_exit_schedule
        CHECK (
            (exited_at IS NULL AND develop_available_at IS NULL)
            OR
            (
                exited_at IS NOT NULL
                AND develop_available_at IS NOT NULL
                AND develop_available_at = exited_at + INTERVAL '1 hour'
            )
            OR
            (
                status = 'EXPIRED'
                AND exited_at IS NOT NULL
                AND develop_available_at IS NULL
            )
        );

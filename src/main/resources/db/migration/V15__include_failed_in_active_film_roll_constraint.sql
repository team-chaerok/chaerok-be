-- FAILED 필름 롤도 같은 FilmRoll로 재시도해야 하는 미완료 상태입니다.
-- 사용자당 미완료 FilmRoll을 최대 하나만 허용하도록 DB 제약을 갱신합니다.
DROP INDEX IF EXISTS uk_film_rolls_user_active;

CREATE UNIQUE INDEX uk_film_rolls_user_active
    ON film_rolls(user_id)
    WHERE status IN (
        'CAPTURING',
        'READY',
        'QUEUED',
        'PROCESSING',
        'FAILED'
    );

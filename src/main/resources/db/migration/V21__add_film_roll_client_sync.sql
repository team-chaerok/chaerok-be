-- FE 로컬 FilmRoll UUID와 서버 FilmRoll을 연결합니다.
-- 기존 서버 FilmRoll은 UUID가 없으므로 nullable로 유지합니다.
ALTER TABLE film_rolls
    ADD COLUMN client_film_roll_id UUID;

ALTER TABLE film_rolls
    ADD CONSTRAINT uk_film_rolls_user_client_film_roll_id
        UNIQUE (user_id, client_film_roll_id);

-- 이전 지역 FilmRoll이 이탈 후 현상 대기/렌더링 중이어도
-- 다음 지역에서 새 촬영을 시작할 수 있어야 합니다.
DROP INDEX IF EXISTS uk_film_rolls_user_active;

CREATE UNIQUE INDEX uk_film_rolls_user_active
    ON film_rolls(user_id)
    WHERE status = 'CAPTURING'
      AND exited_at IS NULL;
-- 앱 삭제/재설치·로컬 DB 유실 등으로 이탈 요청이 영원히 오지 않는
-- CAPTURING FilmRoll을 주기적으로 찾기 위한 부분 인덱스입니다.
CREATE INDEX idx_film_rolls_stale_capturing
    ON film_rolls(updated_at, id)
    WHERE status = 'CAPTURING'
      AND exited_at IS NULL;

-- 기존 Visit 데이터는 사진 연결 정보가 없으므로 photo_id를 nullable로 추가합니다.
-- 신규 Visit은 애플리케이션 계층에서 photoId를 필수로 받아 항상 연결합니다.
ALTER TABLE visits
    ADD COLUMN photo_id BIGINT;

ALTER TABLE visits
    ADD CONSTRAINT fk_visits_photo
        FOREIGN KEY (photo_id)
            REFERENCES photos(id)
            ON DELETE CASCADE;

-- 한 장의 필름 사진을 여러 장소 방문 인증에 재사용하지 못하게 합니다.
-- PostgreSQL UNIQUE는 NULL을 여러 개 허용하므로 기존 Visit 행과 호환됩니다.
ALTER TABLE visits
    ADD CONSTRAINT uk_visits_photo
        UNIQUE (photo_id);
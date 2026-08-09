-- 사용자 한 명에게 동시에 하나의 활성 필름 롤만 허용합니다.
-- 서비스 계층의 사전 검사와 별개로 동시 요청을 DB에서 최종 방어합니다.
CREATE UNIQUE INDEX uk_film_rolls_user_active
    ON film_rolls(user_id)
    WHERE status IN (
        'CAPTURING',
        'READY',
        'QUEUED',
        'PROCESSING'
    );

-- 동일한 S3 원본 객체 키가 여러 Photo 행에 연결되는 것을 방지합니다.
CREATE UNIQUE INDEX uk_photos_original_object_key
    ON photos(original_object_key);

-- Rename regional filter identifiers to short English region names.
-- Preserve compatibility for FilmRoll rows created with the previous IDs.
UPDATE film_rolls
SET filter_id = CASE filter_id
    WHEN 'gongju_baekje_love' THEN 'gongju'
    WHEN 'buyeo_baekje_dream' THEN 'buyeo'
    WHEN 'seosan_warm_sunset' THEN 'seosan'
    WHEN 'yesan_old_memory' THEN 'yesan'
    ELSE filter_id
END
WHERE filter_id IN (
    'gongju_baekje_love',
    'buyeo_baekje_dream',
    'seosan_warm_sunset',
    'yesan_old_memory'
);
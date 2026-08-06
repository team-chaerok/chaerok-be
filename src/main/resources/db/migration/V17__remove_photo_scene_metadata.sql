ALTER TABLE photos
    DROP COLUMN IF EXISTS has_face,
    DROP COLUMN IF EXISTS scene_type;

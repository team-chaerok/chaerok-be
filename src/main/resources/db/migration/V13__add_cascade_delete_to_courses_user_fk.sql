ALTER TABLE courses
DROP CONSTRAINT fk_courses_user;

ALTER TABLE courses
    ADD CONSTRAINT fk_courses_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE;
ALTER TABLE render_jobs
    ADD COLUMN request_message_id VARCHAR(100),
    ADD COLUMN result_message_id VARCHAR(100),
    ADD COLUMN result_bucket VARCHAR(255),
    ADD COLUMN zip_object_key TEXT,
    ADD COLUMN zip_file_size BIGINT,
    ADD COLUMN reel_object_key TEXT,
    ADD COLUMN reel_file_size BIGINT,
    ADD COLUMN manifest_object_key TEXT,
    ADD COLUMN result_occurred_at TIMESTAMP;

ALTER TABLE render_jobs
    ADD CONSTRAINT ck_render_jobs_zip_file_size
        CHECK (zip_file_size IS NULL OR zip_file_size >= 0),
    ADD CONSTRAINT ck_render_jobs_reel_file_size
        CHECK (reel_file_size IS NULL OR reel_file_size >= 0);

CREATE INDEX idx_render_jobs_result_message_id
    ON render_jobs(result_message_id);

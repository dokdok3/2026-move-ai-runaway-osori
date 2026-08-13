ALTER TABLE assignment
    ADD COLUMN completed_at TIMESTAMP;

CREATE INDEX idx_assignment_driver_completed_at
    ON assignment (driver_id, completed_at DESC)
    WHERE completed_at IS NOT NULL;

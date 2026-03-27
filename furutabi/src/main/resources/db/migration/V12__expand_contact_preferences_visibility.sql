ALTER TABLE contact_preferences
    ADD COLUMN profile_visibility VARCHAR(20) NOT NULL DEFAULT 'private';

ALTER TABLE contact_preferences
    ADD COLUMN map_default_visibility VARCHAR(20) NOT NULL DEFAULT 'private';

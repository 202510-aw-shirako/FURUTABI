ALTER TABLE support_notes
    ADD COLUMN related_card_type VARCHAR(30);

ALTER TABLE support_notes
    ADD COLUMN related_card_id BIGINT;

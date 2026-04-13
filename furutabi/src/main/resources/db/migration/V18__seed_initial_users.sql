INSERT INTO users (
    id,
    email,
    password_hash,
    nickname,
    name,
    name_kana,
    birthday,
    gender,
    phone_number,
    address,
    sms_verified,
    additional_verification_status,
    created_at,
    updated_at
) VALUES
    (
        9001,
        'admin@example.com',
        '$2a$10$0N5mBXj8pdodQXemdQeztuc9FwwyfErcy4T5wPF7w3NG5HodZs1AS',
        '管理者',
        '管理者',
        'かんりしゃ',
        DATE '1985-01-01',
        'NO_ANSWER',
        '090-0000-0001',
        'System',
        TRUE,
        'UNREQUESTED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        9002,
        'user1@example.com',
        '$2a$10$0N5mBXj8pdodQXemdQeztuc9FwwyfErcy4T5wPF7w3NG5HodZs1AS',
        '一般ユーザー1',
        '一般ユーザー1',
        'いっぱんゆーざーいち',
        DATE '1990-01-01',
        'NO_ANSWER',
        '090-0000-0002',
        'System',
        FALSE,
        'UNREQUESTED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        9003,
        'user2@example.com',
        '$2a$10$0N5mBXj8pdodQXemdQeztuc9FwwyfErcy4T5wPF7w3NG5HodZs1AS',
        '一般ユーザー2',
        '一般ユーザー2',
        'いっぱんゆーざーに',
        DATE '1992-01-01',
        'NO_ANSWER',
        '090-0000-0003',
        'System',
        FALSE,
        'UNREQUESTED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

INSERT INTO user_roles (user_id, role_name, created_at) VALUES
    (9001, 'ROLE_ADMIN', CURRENT_TIMESTAMP),
    (9001, 'ROLE_USER', CURRENT_TIMESTAMP),
    (9002, 'ROLE_USER', CURRENT_TIMESTAMP),
    (9003, 'ROLE_USER', CURRENT_TIMESTAMP);

INSERT INTO user_profiles (
    user_id,
    bio,
    icon_path,
    region,
    interest_region,
    visit_history,
    care_note,
    with_children,
    food_note,
    relation_note,
    age_range,
    created_at,
    updated_at
) VALUES
    (9001, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9002, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9003, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO contact_preferences (
    user_id,
    receive_operation_notice,
    receive_security_notice,
    receive_bridge_contact,
    receive_local_contact,
    receive_email_notice,
    receive_sms_notice,
    created_at,
    updated_at
) VALUES
    (9001, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9002, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9003, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

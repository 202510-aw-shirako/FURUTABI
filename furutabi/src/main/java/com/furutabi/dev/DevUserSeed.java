package com.furutabi.dev;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevUserSeed implements ApplicationRunner {

    // Development-only fixed credentials for manual verification.
    // These users are not part of the production specification and can be removed later.
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DevUserSeed(
        JdbcTemplate jdbcTemplate,
        PasswordEncoder passwordEncoder,
        @Value("${app.dev-seed.enabled:true}") boolean enabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        seedUser("user@example.com", "USER", "User Seed", "User Seed");
        seedUser("local@example.com", "LOCAL", "Local Seed", "Local Seed");
        seedUser("bridge@example.com", "BRIDGE", "Bridge Seed", "Bridge Seed");
        seedUser("admin@example.com", "ADMIN", "Admin Seed", "Admin Seed");
        seedPilotGateProposal();
        seedAdditionalGateProposals();
        seedPilotOkatteProposal();
        seedAdditionalOkatteProposals();
    }

    private void seedUser(String email, String roleName, String name, String nameKana) {
        List<Long> existingUserIds = jdbcTemplate.query(
            "SELECT id FROM users WHERE email = ?",
            (rs, rowNum) -> rs.getLong("id"),
            email
        );
        Long existingUserId = existingUserIds.isEmpty() ? null : existingUserIds.getFirst();

        long userId;
        if (existingUserId == null) {
            Timestamp now = Timestamp.from(Instant.now());
            jdbcTemplate.update(
                """
                    INSERT INTO users (
                        email, password_hash, nickname, name, name_kana, birthday, gender,
                        phone_number, address, sms_verified, additional_verification_status,
                        created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                email,
                passwordEncoder.encode("password123"),
                roleName.toLowerCase() + "-seed",
                name,
                nameKana,
                Date.valueOf("1990-01-01"),
                "NO_ANSWER",
                "090-0000-0000",
                "Dev Seed",
                Boolean.TRUE,
                "UNREQUESTED",
                now,
                now
            );

            userId = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email = ?",
                    Long.class,
                    email
                ),
                "Seed user ID was not found after insert: " + email
            );
        } else {
            userId = existingUserId;
        }

        if (!hasRole(userId, roleName)) {
            jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_name, created_at) VALUES (?, ?, ?)",
                userId,
                roleName,
                Timestamp.from(Instant.now())
            );
        }
    }

    private boolean hasRole(long userId, String roleName) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_name = ?",
            Long.class,
            userId,
            roleName
        );
        return count != null && count > 0;
    }

    private void seedPilotGateProposal() {
        // Development-only pilot case for manual verification.
        // Remove this once real gate proposal creation or richer dev fixtures are ready.
        long hostUserId = requireUserIdByEmail("local@example.com");
        long bridgeUserId = requireUserIdByEmail("bridge@example.com");
        String title = "【dev確認用】ちいきの入り口 申請確認";

        List<Long> existingProposalIds = jdbcTemplate.query(
            "SELECT id FROM proposals WHERE title = ? AND host_user_id = ? AND deleted_at IS NULL",
            (rs, rowNum) -> rs.getLong("id"),
            title,
            hostUserId
        );

        long proposalId;
        if (existingProposalIds.isEmpty()) {
            Timestamp now = Timestamp.from(Instant.now());
            jdbcTemplate.update(
                """
                    INSERT INTO proposals (
                        proposal_type, bridge_user_id, host_user_id, title, summary, body,
                        duration_minutes, location_name, status, visibility_scope,
                        cover_image_path, created_at, updated_at, deleted_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                "GATE",
                bridgeUserId,
                hostUserId,
                title,
                "dev専用の申請確認用パイロットケースです。",
                "user@example.com で gate 詳細を開き、申請導線を手動確認するための dev 専用データです。",
                90,
                "開発用シード町",
                "published",
                "public",
                null,
                now,
                now,
                null
            );

            proposalId = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM proposals WHERE title = ? AND host_user_id = ? AND deleted_at IS NULL",
                    Long.class,
                    title,
                    hostUserId
                ),
                "Pilot gate proposal ID was not found after insert."
            );
        } else {
            proposalId = existingProposalIds.getFirst();
        }

        seedProposalTag(proposalId, "dev-seed", 0);
        seedProposalTag(proposalId, "gate-pilot", 1);
    }

    private void seedAdditionalGateProposals() {
        seedProposal(
            "GATE",
            "海まで歩いて景色の話を聞く",
            "海を見ながら、この土地の好きな時間をたどる入口です。",
            "短時間で地域の空気に触れられる、やわらかな入口カードです。",
            30,
            "凪咲町の海辺",
            "public",
            List.of("sea", "short-walk")
        );
        seedProposal(
            "GATE",
            "ハウスの前で野菜を見る",
            "育てているものを見ながら、地域の挑戦を少し聞きます。",
            "会話が多すぎない、小さな入口として選びやすいカードです。",
            20,
            "凪咲町の畑",
            "public",
            List.of("farm", "quiet")
        );
        seedProposal(
            "GATE",
            "丘の上から季節の景色を見る",
            "その時期ならではの風景を、無理なく味わいます。",
            "少し歩いて景色を見る、季節の入口カードです。",
            45,
            "凪咲町の高台",
            "public",
            List.of("view", "season")
        );
    }

    private void seedPilotOkatteProposal() {
        // Development-only pilot case for manual verification.
        // Remove this once real okatte proposal creation or richer dev fixtures are ready.
        long hostUserId = requireUserIdByEmail("local@example.com");
        long bridgeUserId = requireUserIdByEmail("bridge@example.com");
        String title = "【dev確認用】ちいきのおかって 候補確認";

        List<Long> existingProposalIds = jdbcTemplate.query(
            "SELECT id FROM proposals WHERE title = ? AND host_user_id = ? AND deleted_at IS NULL",
            (rs, rowNum) -> rs.getLong("id"),
            title,
            hostUserId
        );

        long proposalId;
        if (existingProposalIds.isEmpty()) {
            Timestamp now = Timestamp.from(Instant.now());
            jdbcTemplate.update(
                """
                    INSERT INTO proposals (
                        proposal_type, bridge_user_id, host_user_id, title, summary, body,
                        duration_minutes, location_name, status, visibility_scope,
                        cover_image_path, created_at, updated_at, deleted_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                "OKATTE",
                bridgeUserId,
                hostUserId,
                title,
                "dev確認用のおかって候補パイロットケースです。",
                "user@example.com で候補を見て選び、申請導線まで手動確認するための dev 専用データです。",
                120,
                "開発用おかって会場",
                "published",
                "public",
                null,
                now,
                now,
                null
            );

            proposalId = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM proposals WHERE title = ? AND host_user_id = ? AND deleted_at IS NULL",
                    Long.class,
                    title,
                    hostUserId
                ),
                "Pilot okatte proposal ID was not found after insert."
            );
        } else {
            proposalId = existingProposalIds.getFirst();
        }

        seedProposalTag(proposalId, "dev-seed", 0);
        seedProposalTag(proposalId, "okatte-pilot", 1);
    }

    private void seedAdditionalOkatteProposals() {
        seedProposal(
            "OKATTE",
            "郷土料理をみんなで作る午後",
            "台所を囲みながら、少し長めの時間を一緒に過ごします。",
            "関係ができたあとに開かれる、台所の時間の候補です。",
            120,
            "凪咲町の共同台所",
            "public",
            List.of("kitchen", "shared-time")
        );
        seedProposal(
            "OKATTE",
            "港で牡蠣の仕事を見学する",
            "海の仕事をそばで見ながら、暮らしの話を聞きます。",
            "少し奥の時間として、仕事場の空気に触れる候補です。",
            90,
            "凪咲町の港",
            "public",
            List.of("harbor", "work")
        );
        seedProposal(
            "OKATTE",
            "畑の手入れを一緒にする朝",
            "いつもの作業を少しだけ一緒にしながら、季節の話をします。",
            "背伸びをしない関わり方として開かれる候補です。",
            75,
            "凪咲町の畑",
            "public",
            List.of("field", "morning")
        );
    }

    private void seedProposal(
        String proposalType,
        String title,
        String summary,
        String body,
        int durationMinutes,
        String locationName,
        String visibilityScope,
        List<String> tags
    ) {
        long hostUserId = requireUserIdByEmail("local@example.com");
        long bridgeUserId = requireUserIdByEmail("bridge@example.com");

        List<Long> existingProposalIds = jdbcTemplate.query(
            "SELECT id FROM proposals WHERE title = ? AND host_user_id = ? AND deleted_at IS NULL",
            (rs, rowNum) -> rs.getLong("id"),
            title,
            hostUserId
        );

        long proposalId;
        if (existingProposalIds.isEmpty()) {
            Timestamp now = Timestamp.from(Instant.now());
            jdbcTemplate.update(
                """
                    INSERT INTO proposals (
                        proposal_type, bridge_user_id, host_user_id, title, summary, body,
                        duration_minutes, location_name, status, visibility_scope,
                        cover_image_path, created_at, updated_at, deleted_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                proposalType,
                bridgeUserId,
                hostUserId,
                title,
                summary,
                body,
                durationMinutes,
                locationName,
                "published",
                visibilityScope,
                null,
                now,
                now,
                null
            );

            proposalId = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM proposals WHERE title = ? AND host_user_id = ? AND deleted_at IS NULL",
                    Long.class,
                    title,
                    hostUserId
                ),
                "Seed proposal ID was not found after insert."
            );
        } else {
            proposalId = existingProposalIds.getFirst();
        }

        for (int i = 0; i < tags.size(); i++) {
            seedProposalTag(proposalId, tags.get(i), i);
        }
    }

    private void seedProposalTag(long proposalId, String tagName, int sortOrder) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM proposal_tags WHERE proposal_id = ? AND tag_name = ?",
            Long.class,
            proposalId,
            tagName
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
            "INSERT INTO proposal_tags (proposal_id, tag_name, sort_order, created_at) VALUES (?, ?, ?, ?)",
            proposalId,
            tagName,
            sortOrder,
            Timestamp.from(Instant.now())
        );
    }

    private long requireUserIdByEmail(String email) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                email
            ),
            "Dev seed user ID was not found for email: " + email
        );
    }
}

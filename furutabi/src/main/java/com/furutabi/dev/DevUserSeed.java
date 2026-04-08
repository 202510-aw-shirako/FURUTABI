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
                "dev確認用の申請確認ケースです。",
                "user@example.com で gate 詳細を開き、申請動作を確認するための dev 用データです。",
                90,
                "確認用シード",
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
            GateSeedData.ITO_WALK.title(),
            GateSeedData.ITO_WALK.summary(),
            GateSeedData.ITO_WALK.body(),
            GateSeedData.ITO_WALK.durationMinutes(),
            GateSeedData.ITO_WALK.locationName(),
            "public",
            GateSeedData.ITO_WALK.tags()
        );
        seedProposal(
            "GATE",
            GateSeedData.ITO_FLOWER.title(),
            GateSeedData.ITO_FLOWER.summary(),
            GateSeedData.ITO_FLOWER.body(),
            GateSeedData.ITO_FLOWER.durationMinutes(),
            GateSeedData.ITO_FLOWER.locationName(),
            "public",
            GateSeedData.ITO_FLOWER.tags()
        );
        seedProposal(
            "GATE",
            GateSeedData.HASEGAWA.title(),
            GateSeedData.HASEGAWA.summary(),
            GateSeedData.HASEGAWA.body(),
            GateSeedData.HASEGAWA.durationMinutes(),
            GateSeedData.HASEGAWA.locationName(),
            "public",
            GateSeedData.HASEGAWA.tags()
        );
    }

    private void seedPilotOkatteProposal() {
        long hostUserId = requireUserIdByEmail("local@example.com");
        long bridgeUserId = requireUserIdByEmail("bridge@example.com");
        String title = "【dev確認用】ちいきのおかって 相談確認";

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
                "dev確認用のおかって申請確認ケースです。",
                "user@example.com でおかって詳細を開き、申請動作を確認するための dev 用データです。",
                120,
                "確認用おかって",
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
            "台所を囲みながら、少し長めの時間を一緒に過ごします。",
            120,
            "凪咲町の共同台所",
            "public",
            List.of("kitchen", "shared-time")
        );
        seedProposal(
            "OKATTE",
            "港で牡蠣の仕事を見学する",
            "海の仕事をそばで見ながら、暮らしの話を聞きます。",
            "海の仕事をそばで見ながら、暮らしの話を聞きます。",
            90,
            "凪咲町の港",
            "public",
            List.of("harbor", "work")
        );
        seedProposal(
            "OKATTE",
            "畑の手入れを一緒にする朝",
            "いつもの作業を少しだけ一緒にしながら、季節の話をします。",
            "いつもの作業を少しだけ一緒にしながら、季節の話をします。",
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
            jdbcTemplate.update(
                """
                    UPDATE proposals
                    SET summary = ?,
                        body = ?,
                        duration_minutes = ?,
                        location_name = ?,
                        visibility_scope = ?,
                        status = 'published',
                        updated_at = ?
                    WHERE id = ?
                    """,
                summary,
                body,
                durationMinutes,
                locationName,
                visibilityScope,
                Timestamp.from(Instant.now()),
                proposalId
            );
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
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO proposal_tags (proposal_id, tag_name, sort_order, created_at) VALUES (?, ?, ?, ?)",
                proposalId,
                tagName,
                sortOrder,
                Timestamp.from(Instant.now())
            );
            return;
        }

        jdbcTemplate.update(
            "UPDATE proposal_tags SET sort_order = ? WHERE proposal_id = ? AND tag_name = ?",
            sortOrder,
            proposalId,
            tagName
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

    private record GateSeedData(
        String title,
        String summary,
        String body,
        int durationMinutes,
        String locationName,
        List<String> tags
    ) {
        private static final GateSeedData ITO_WALK = new GateSeedData(
            "海を眺めながら、この街の話を聞く散歩",
            "海を見渡せる場所を伊藤さんとゆっくり歩きながら、この土地のこと、人のこと、ここでの暮らしのことを少しずつ聞いていく時間です。観光案内ではなく、この街に入っていくための最初の入口のような散歩です。",
            "この時間は、名所を次々に案内するような散歩ではありません。海を見渡せる場所を伊藤さんと歩きながら、この土地のこと、人のこと、ここで暮らす時間の流れを少しずつ知っていく入口です。伊藤さんは強く引っ張るタイプではなく、景色を前にしながら、その場所にまつわる話や、この土地で生きてきた感覚を静かに渡してくれます。はじめて来た人でも、無理なくこの街に入っていける時間です。",
            40,
            "海の見える場所",
            List.of("sea", "walk")
        );

        private static final GateSeedData ITO_FLOWER = new GateSeedData(
            "海辺の花を手入れする日",
            "海辺の景色をよくするために、花を手入れしたり、少し場所を整えたりする時間です。ただ眺めるだけでなく、この土地に少し手をかけることで、街との距離が少し近くなります。",
            "伊藤さんがひらくもう一つの入口は、土地の景色に少し手をかける時間です。花を手入れしたり、周りを整えたりしながら、誰かがまた来たくなる風景を一緒につくる。大がかりな作業ではありませんが、ただ見に来るだけではわからない、この土地との関わり方が少し見えてきます。伊藤さんにとって景色は、眺めるものというより、守ったり整えたりしながら次に渡していくものでもあります。その感覚に少し触れられる入口です。",
            60,
            "海辺の花壇",
            List.of("flower", "care")
        );

        private static final GateSeedData HASEGAWA = new GateSeedData(
            "コーヒーを飲みながら、町の見え方が少し変わる",
            "長谷川さんとコーヒーを飲みながら少し話すことで、この町の今の空気や、地域の挑戦の途中にやわらかく触れていく時間です。強い体験ではなく、まずは話しやすさの中から、この土地との距離が少し縮まる入口です。",
            "この時間は、どこかへ連れて行ってもらう体験というより、まずは話してみる入口です。長谷川さんとコーヒーを飲みながら、この土地で今どんなことが起きているのか、どんな人たちが動いているのかを少しずつ聞いていきます。長谷川さんはやわらかく話しやすいので、はじめての人でも身構えすぎずに入っていけます。でも、そのやわらかさの奥には、地域の仕事や未来に対するまっすぐな感覚があります。この町の見え方が、少しだけ内側から変わるような入口です。",
            40,
            "町のコーヒースタンド",
            List.of("coffee", "talk")
        );
    }
}

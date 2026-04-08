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
            "牡蠣小屋で、海のものを囲む時間",
            "海辺の素朴な牡蠣小屋で、牡蠣や海のものを囲みながら、この土地の人のあたたかさや海の恵みに触れる時間です。気取った食事ではなく、海のそばの暮らしの延長にある食卓に少し入れてもらうようなおかってです。",
            "このおかっては、豪華な海鮮体験ではありません。海辺の素朴な牡蠣小屋で、牡蠣や海のものを囲みながら、この土地の食卓のあたたかさに少し入れてもらう時間です。伊藤さんがいることで、外から来た人も無理なくその場に入っていけます。観光向けに整えられた食事ではなく、海のそばの人たちが大事にしてきた食べ方や場の空気が、そのまま少しひらかれている感じが魅力です。海を見て終わるのではなく、最後に食卓までつながる伊藤さんらしいおかってです。",
            120,
            "海辺の牡蠣小屋",
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
            "葡萄を通して、地域の挑戦の途中にふれる",
            "葡萄にふれながら、この土地で進んでいる挑戦の途中を少し見せてもらう時間です。完成したワインや商品を見るだけではなく、その手前の育てる現場や考えていることに触れられるおかってです。",
            "このおかってでは、葡萄を通して、この土地で進んでいる挑戦の途中にふれます。商品として完成したものを見るのではなく、まだ育っている途中のもの、試している途中のもの、考えている途中のものに少し触れられるのが魅力です。長谷川さんは、その途中をうまく隠さずに見せてくれる方です。だから、ただ「いいものですね」で終わらず、「この先どうなるんだろう」と少し応援したくなる。この土地の挑戦に、外から来た人も少しだけ関われる時間です。",
            120,
            "葡萄の畑",
            "public",
            List.of("grape", "challenge")
        );
        seedProposal(
            "OKATTE",
            "菊の仕事場に、少し通してもらう",
            "菊の育つ場や手入れの仕事に少し触れながら、この土地で続いてきた花の仕事の空気を感じる時間です。長谷川さん本人がずっと一緒にいる形ではなく、地域の仕事場に少し通してもらうようなおかってです。",
            "このおかっては、華やかな花体験ではありません。菊がどう育てられ、どう手をかけられているのか、その仕事場の空気に少し通してもらう時間です。長谷川さん自身が前面に出るというより、地域で続いてきた花の仕事へ、外から来た人が少し近づく入口に近いかたちです。見た目の美しさだけではなく、続けるための手間や静かな積み重ねに触れられる。この土地の仕事の厚みを感じられるおかってです。",
            90,
            "菊の仕事場",
            "public",
            List.of("chrysanthemum", "work")
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

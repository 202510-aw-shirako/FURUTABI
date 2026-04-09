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

import com.furutabi.app.ProposalPresentationCatalog;

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
        seedNamedUser(ProposalPresentationCatalog.HOST_USER_ID_ITO, "ito@example.com", "LOCAL", "伊藤 朗士さん", "伊藤 朗士さん", "伊藤 朗士さん");
        seedNamedUser(ProposalPresentationCatalog.HOST_USER_ID_HASEGAWA, "hasegawa@example.com", "LOCAL", "長谷川さん", "長谷川さん", "長谷川さん");
        seedNamedUser(ProposalPresentationCatalog.HOST_USER_ID_HOSYO, "hosyo@example.com", "LOCAL", "宝生さん", "宝生さん", "宝生さん");
        seedNamedUser(ProposalPresentationCatalog.HOST_USER_ID_TODO, "todo@example.com", "LOCAL", "東堂さん", "東堂さん", "東堂さん");
        seedUser("admin@example.com", "ADMIN", "Admin Seed", "Admin Seed");
        seedPilotGateProposal();
        seedAdditionalGateProposals();
        seedPilotOkatteProposal();
        seedAdditionalOkatteProposals();
    }

    private void seedUser(String email, String roleName, String name, String nameKana) {
        seedNamedUser(null, email, roleName, roleName.toLowerCase() + "-seed", name, nameKana);
    }

    private void seedNamedUser(Long preferredUserId, String email, String roleName, String nickname, String name, String nameKana) {
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
                        id, email, password_hash, nickname, name, name_kana, birthday, gender,
                        phone_number, address, sms_verified, additional_verification_status,
                        created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                preferredUserId,
                email,
                passwordEncoder.encode("password123"),
                nickname,
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
            ProposalPresentationCatalog.PROPOSAL_ID_ITO_WALK,
            "ito@example.com",
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
            ProposalPresentationCatalog.PROPOSAL_ID_ITO_FLOWER,
            "ito@example.com",
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
            ProposalPresentationCatalog.PROPOSAL_ID_HASEGAWA_GATE,
            "hasegawa@example.com",
            "GATE",
            GateSeedData.HASEGAWA.title(),
            GateSeedData.HASEGAWA.summary(),
            GateSeedData.HASEGAWA.body(),
            GateSeedData.HASEGAWA.durationMinutes(),
            GateSeedData.HASEGAWA.locationName(),
            "public",
            GateSeedData.HASEGAWA.tags()
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_HOSYO_GATE,
            "hosyo@example.com",
            "GATE",
            GateSeedData.HOSYO.title(),
            GateSeedData.HOSYO.summary(),
            GateSeedData.HOSYO.body(),
            GateSeedData.HOSYO.durationMinutes(),
            GateSeedData.HOSYO.locationName(),
            "public",
            GateSeedData.HOSYO.tags()
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_TODO_WALK,
            "todo@example.com",
            "GATE",
            GateSeedData.TODO_WALK.title(),
            GateSeedData.TODO_WALK.summary(),
            GateSeedData.TODO_WALK.body(),
            GateSeedData.TODO_WALK.durationMinutes(),
            GateSeedData.TODO_WALK.locationName(),
            "public",
            GateSeedData.TODO_WALK.tags()
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_TODO_LEATHER,
            "todo@example.com",
            "GATE",
            GateSeedData.TODO_LEATHER.title(),
            GateSeedData.TODO_LEATHER.summary(),
            GateSeedData.TODO_LEATHER.body(),
            GateSeedData.TODO_LEATHER.durationMinutes(),
            GateSeedData.TODO_LEATHER.locationName(),
            "public",
            GateSeedData.TODO_LEATHER.tags()
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
            ProposalPresentationCatalog.PROPOSAL_ID_ITO_OKATTE,
            "ito@example.com",
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
            ProposalPresentationCatalog.PROPOSAL_ID_HASEGAWA_GRAPE,
            "hasegawa@example.com",
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
            ProposalPresentationCatalog.PROPOSAL_ID_HASEGAWA_CHRYSANTHEMUM,
            "hasegawa@example.com",
            "OKATTE",
            "菊の仕事場に、少し通してもらう",
            "菊の育つ場や手入れの仕事に少し触れながら、この土地で続いてきた花の仕事の空気を感じる時間です。長谷川さん本人がずっと一緒にいる形ではなく、地域の仕事場に少し通してもらうようなおかってです。",
            "このおかっては、華やかな花体験ではありません。菊がどう育てられ、どう手をかけられているのか、その仕事場の空気に少し通してもらう時間です。長谷川さん自身が前面に出るというより、地域で続いてきた花の仕事へ、外から来た人が少し近づく入口に近いかたちです。見た目の美しさだけではなく、続けるための手間や静かな積み重ねに触れられる。この土地の仕事の厚みを感じられるおかってです。",
            90,
            "菊の仕事場",
            "public",
            List.of("chrysanthemum", "work")
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_HOSYO_ANAGO,
            "hosyo@example.com",
            "OKATTE",
            "名物あなご丼を、少し遊びながらつくる昼",
            "この土地の名物として育てているあなご丼を、少し遊び心も交えながら一緒につくって食べる時間です。料理教室というより、名物づくりの途中に少し加わるようなおかってです。",
            "このおかっては、完成した名物をただ食べるだけの時間ではありません。この土地の名物として育てているあなご丼を、一緒に少しつくりながら、その食べ方や遊び心も含めて味わう時間です。料理教室のようにきっちり教わるというより、宝生さんが育ててきた食の空気に少し通してもらう感じが近いです。だから、ただおいしいで終わるのではなく、「この町でこういうふうに育ててきたんだな」と少し見えてきます。旅館の主人として人を迎えてきた宝生さんらしく、食卓ごと町の魅力に通してくれるおかってです。",
            120,
            "旅館の台所",
            "public",
            List.of("anago", "lunch")
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_HOSYO_SCENERY,
            "hosyo@example.com",
            "OKATTE",
            "何度も来ると見えてくる、町の風景の奥をたどる",
            "一度では見えにくいこの町の風景や歴史の奥を、宝生さんと少しずつたどっていく時間です。観光案内ではなく、通うほど見え方が深くなる町の楽しみ方に触れるおかってです。",
            "このおかっては、初めての人向けの町歩きとは少し違います。宝生さんと一緒に、何度か来るうちに見えてくるこの町の風景や歴史の奥をたどっていく時間です。派手な名所より、ふつうに見える道や建物や景色の中に、「ここはこういう場所なんです」と少し奥行きが生まれていく。宝生さんは、町の全部を一気に説明する人ではなく、通うほど少しずつ見せてくれる方です。だからこの時間は、知識をもらうというより、この町との付き合い方が少し深くなるおかってになっています。",
            90,
            "町の路地",
            "public",
            List.of("town", "history")
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_TODO_OKATTE,
            "todo@example.com",
            "OKATTE",
            "工房の奥で、もう少し深い制作に向き合う",
            "その方の能力や希望に応じて、しおり・キーチェーン・名刺入れなどの小さなものから、バッグなどより時間のかかる制作まで、一緒に向き合うものが変わります。軽い体験ではなく、工房の中で本気で手を動かし、仕事の奥へ少し入っていくおかってです。",
            "このおかっては、楽しいワークショップではありません。工房の中で、その人の能力や希望に応じて、しおりやキーチェーン、名刺入れのような小さなものから、より時間のかかる制作まで、本気で向き合っていく時間です。東堂さんは話しすぎず、褒めすぎず、でも本気で向き合っている相手だからこそ本気で見てくれます。奥さんもまた、形や収まり、細部の見え方を同じ仕事の人として見ています。軽い観光体験ではなく、静かな緊張感のある工房で、仕事の厳しさと楽しさの両方に少し触れる。観光客に広くは開きにくい高級品や、本物の仕事の奥に少し入れるのは、このサイトならではの時間です。",
            420,
            "革工房",
            "public",
            List.of("leather", "atelier")
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
        seedProposalForHost("local@example.com", proposalType, title, summary, body, durationMinutes, locationName, visibilityScope, tags);
    }

    private void seedProposal(
        long preferredProposalId,
        String hostEmail,
        String proposalType,
        String title,
        String summary,
        String body,
        int durationMinutes,
        String locationName,
        String visibilityScope,
        List<String> tags
    ) {
        seedProposalForHost(preferredProposalId, hostEmail, proposalType, title, summary, body, durationMinutes, locationName, visibilityScope, tags);
    }

    private void seedProposalForHost(
        String hostEmail,
        String proposalType,
        String title,
        String summary,
        String body,
        int durationMinutes,
        String locationName,
        String visibilityScope,
        List<String> tags
    ) {
        seedProposalForHost(null, hostEmail, proposalType, title, summary, body, durationMinutes, locationName, visibilityScope, tags);
    }

    private void seedProposalForHost(
        Long preferredProposalId,
        String hostEmail,
        String proposalType,
        String title,
        String summary,
        String body,
        int durationMinutes,
        String locationName,
        String visibilityScope,
        List<String> tags
    ) {
        long hostUserId = requireUserIdByEmail(hostEmail);
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
                        id, proposal_type, bridge_user_id, host_user_id, title, summary, body,
                        duration_minutes, location_name, status, visibility_scope,
                        cover_image_path, created_at, updated_at, deleted_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                preferredProposalId,
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

        private static final GateSeedData HOSYO = new GateSeedData(
            "猫の町を歩く",
            "宝生さんと一緒に町を歩きながら、猫のいる風景や、この土地の日常の気配に少しずつ馴染んでいく時間です。観光スポットを回るのではなく、町の空気ごとゆっくり入っていくための入口です。",
            "この時間は、猫の名所を効率よく回るような散歩ではありません。宝生さんと町を歩きながら、猫のいる風景や路地の空気、人の気配に少しずつ馴染んでいく入口です。猫は主役というより、この町の空気の中に自然にいる存在として現れます。だから、歩いているうちに「猫の町」と言いたくなるような感覚が少しずつ育っていく。宝生さんはそれを強く説明しすぎず、この町の歩き方として静かに渡してくれる方です。",
            40,
            "猫のいる町の路地",
            List.of("cat", "walk")
        );

        private static final GateSeedData TODO_WALK = new GateSeedData(
            "町の“いい仕事”を訪ねる散歩",
            "東堂さんと町を歩きながら、普段なら通り過ぎる店や、少し入りにくいけれど町の厚みを支えている仕事の場に少し触れていく時間です。観光名所ではなく、この土地の“ちゃんとした仕事”を手がかりに町の奥行きを知る入口です。",
            "この時間は、名店巡りや観光ガイドではありません。東堂さんと町を歩きながら、呉服店や木工所、小さな工房、割烹、道具屋など、普段なら通り過ぎたり、少し気後れして入りにくかったりする仕事の場に少し近づいていく入口です。東堂さんは強く説明しすぎず、「ここはちゃんとしてる」という目利きの感覚を少しだけ渡してくれます。その流れの中で、後の滞在にも役立つご飯屋さんや喫茶店を、さりげなく教えてもらえることもあります。観光情報を集めるというより、この町でどこが本当にいいかの感覚を少し分けてもらう時間です。",
            40,
            "町の仕事場",
            List.of("craft", "walk")
        );

        private static final GateSeedData TODO_LEATHER = new GateSeedData(
            "この町で身につける、小さな革のものをつくる",
            "東堂さんご夫妻の工房で、革のキーホルダーやストラップのような小さなものをつくる入口です。旅の記念品を買うのではなく、この町で自分の手を通したものを持ち帰ることで、町との距離が少し変わっていきます。",
            "この時間は、深い制作体験そのものが目的ではありません。東堂さんご夫妻の工房で、革のキーホルダーやストラップのような小さなものをつくり、この町で自分の手を通したものを持ち帰る入口です。端材を使った軽い体験ではありますが、雑貨づくりのワークショップにはしません。小さいながらも、素材を見ること、形を考えること、手を動かすことの中に、東堂さんご夫妻の仕事の考え方が少し入っています。工房が少しだけひらかれ、その奥に入る前の静かで確かな入口になっています。",
            60,
            "革工房",
            List.of("leather", "workshop")
        );
    }
}

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
        seedNamedUser(null, "footprint1@example.com", "USER", "ごひいきさんA", "ごひいきさんA", "ごひいきさんA");
        seedNamedUser(null, "footprint2@example.com", "USER", "ごひいきさんB", "ごひいきさんB", "ごひいきさんB");
        seedNamedUser(null, "footprint3@example.com", "USER", "ごひいきさんC", "ごひいきさんC", "ごひいきさんC");
        seedNamedUser(ProposalPresentationCatalog.HOST_USER_ID_RIKYU, "rikyu@example.com", "LOCAL", "利休 縫依乃さん", "利休 縫依乃さん", "利休 縫依乃さん");
        seedNamedUser(ProposalPresentationCatalog.HOST_USER_ID_RURIKAWA, "rurikawa@example.com", "LOCAL", "瑠璃川 乙羽さん", "瑠璃川 乙羽さん", "瑠璃川 乙羽さん");
        seedNamedUser(ProposalPresentationCatalog.HOST_USER_ID_WATANABE, "watanabe@example.com", "LOCAL", "渡辺 楓さん", "渡辺 楓さん", "渡辺 楓さん");
        seedNamedUser(ProposalPresentationCatalog.HOST_USER_ID_WATANABE_COLLAB, "watanabe-collab@example.com", "LOCAL", "渡辺 楓さんと瑠璃川 乙羽さん", "渡辺 楓さんと瑠璃川 乙羽さん", "渡辺 楓さんと瑠璃川 乙羽さん");
        seedNamedUser(ProposalPresentationCatalog.HOST_USER_ID_YOSHINO, "yoshino@example.com", "LOCAL", "吉野 大至さん", "吉野 大至さん", "吉野 大至さん");
        seedUser("admin@example.com", "ADMIN", "Admin Seed", "Admin Seed");
        seedPilotGateProposal();
        seedAdditionalGateProposals();
        seedPilotOkatteProposal();
        seedAdditionalOkatteProposals();
        seedSharedFootprints();
        refreshSharedFootprintPresentation();
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
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_RIKYU_CAFE,
            "rikyu@example.com",
            "GATE",
            GateSeedData.RIKYU_CAFE.title(),
            GateSeedData.RIKYU_CAFE.summary(),
            GateSeedData.RIKYU_CAFE.body(),
            GateSeedData.RIKYU_CAFE.durationMinutes(),
            GateSeedData.RIKYU_CAFE.locationName(),
            "public",
            GateSeedData.RIKYU_CAFE.tags()
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_RIKYU_WALK,
            "rikyu@example.com",
            "GATE",
            GateSeedData.RIKYU_WALK.title(),
            GateSeedData.RIKYU_WALK.summary(),
            GateSeedData.RIKYU_WALK.body(),
            GateSeedData.RIKYU_WALK.durationMinutes(),
            GateSeedData.RIKYU_WALK.locationName(),
            "public",
            GateSeedData.RIKYU_WALK.tags()
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_RURIKAWA_FARM,
            "rurikawa@example.com",
            "GATE",
            GateSeedData.RURIKAWA_FARM.title(),
            GateSeedData.RURIKAWA_FARM.summary(),
            GateSeedData.RURIKAWA_FARM.body(),
            GateSeedData.RURIKAWA_FARM.durationMinutes(),
            GateSeedData.RURIKAWA_FARM.locationName(),
            "public",
            GateSeedData.RURIKAWA_FARM.tags()
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_RURIKAWA_WALK,
            "rurikawa@example.com",
            "GATE",
            GateSeedData.RURIKAWA_WALK.title(),
            GateSeedData.RURIKAWA_WALK.summary(),
            GateSeedData.RURIKAWA_WALK.body(),
            GateSeedData.RURIKAWA_WALK.durationMinutes(),
            GateSeedData.RURIKAWA_WALK.locationName(),
            "public",
            GateSeedData.RURIKAWA_WALK.tags()
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_WATANABE_WATER,
            "watanabe@example.com",
            "GATE",
            GateSeedData.WATANABE_WATER.title(),
            GateSeedData.WATANABE_WATER.summary(),
            GateSeedData.WATANABE_WATER.body(),
            GateSeedData.WATANABE_WATER.durationMinutes(),
            GateSeedData.WATANABE_WATER.locationName(),
            "public",
            GateSeedData.WATANABE_WATER.tags()
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_WATANABE_STREAM,
            "watanabe@example.com",
            "GATE",
            GateSeedData.WATANABE_STREAM.title(),
            GateSeedData.WATANABE_STREAM.summary(),
            GateSeedData.WATANABE_STREAM.body(),
            GateSeedData.WATANABE_STREAM.durationMinutes(),
            GateSeedData.WATANABE_STREAM.locationName(),
            "public",
            GateSeedData.WATANABE_STREAM.tags()
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_YOSHINO_BIKE,
            "yoshino@example.com",
            "GATE",
            GateSeedData.YOSHINO_BIKE.title(),
            GateSeedData.YOSHINO_BIKE.summary(),
            GateSeedData.YOSHINO_BIKE.body(),
            GateSeedData.YOSHINO_BIKE.durationMinutes(),
            GateSeedData.YOSHINO_BIKE.locationName(),
            "public",
            GateSeedData.YOSHINO_BIKE.tags()
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
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_RIKYU_OKATTE,
            "rikyu@example.com",
            "OKATTE",
            "梅の季節を、静かに持ち帰る",
            "梅林で梅の実を採り、梅干しや梅ジュース、梅酒を仕込む時間です。にぎやかな保存食ワークショップではなく、季節を自分の手で受け取り、暮らしに持ち帰る静かな手仕事としてひらかれます。",
            "利休さんのおかっては、にぎやかな体験というより、とても静かに季節を持ち帰る手仕事です。梅の実を手に取り、香りを感じ、拭いて、瓶に入れ、氷砂糖や塩を重ねていく。そうやって、季節が少しずつ瓶の中に入っていく時間です。暮らしの知恵を教わる、というより、季節の手ざわりを一緒に受け取る。持ち帰ったあとも、食卓や晩酌の時間にその季節が続いていくところまで含めて、このおかっての魅力です。",
            150,
            "梅林地区",
            "public",
            List.of("ume", "season")
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_WATANABE_STREAM_OKATTE,
            "watanabe@example.com",
            "OKATTE",
            "やさしい入口の先にある、少し深い渓流釣り",
            "入口より少し深く、希望や経験に応じて踏み込める渓流釣りの時間です。基本はのべ竿で、必要に応じてその先の釣り方にも広げていけます。",
            "入口よりもう少し踏み込んで、ちゃんと釣る時間に入っていくおかってです。基本はのべ竿で、やさしい川釣りをしながら、水辺との距離をもう一歩深めていきます。必要があれば、その人の経験や希望に応じて、延べ竿の先の釣り方にも広げることができます。楓さんは上級者を見せる人ではなく、その人の経験や気分に合わせて、無理のない釣り方を選んでくれる人です。だから、釣りが好きになりかけた人にも、もう少しちゃんと入りたい人にも合う。やさしい入口の先で、水辺との関係を少し深める時間です。",
            180,
            "渓流",
            "public",
            List.of("stream", "fishing")
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_WATANABE_SEA_OKATTE,
            "watanabe@example.com",
            "OKATTE",
            "地域のつながりがあるからこそ開ける、少し特別な海釣り",
            "知り合いの漁師に船を出してもらって行く、小さな海釣りの時間です。観光クルーズではなく、楓さんのつながりがあるからこそ開ける、少し特別な海のおかってです。",
            "この海釣りは、豪華な観光クルーズではありません。知り合いの漁師に船を出してもらうからこそ成り立つ、地域のつながりの上にある海の時間です。海釣りをしたい人にはしっかり釣りの楽しさがあり、釣りにこだわりすぎなくても、海から地域を見るクルージングのような時間として楽しむこともできます。家族連れにも、ごひいきさんにも開きやすく、釣れた魚をその先の店につなげたり、次のツアーの話を育てたりもできる。楓さんらしい、「その場で終わらない海の時間」になっています。",
            180,
            "港",
            "public",
            List.of("sea", "fishing")
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_WATANABE_IMONI,
            "watanabe-collab@example.com",
            "OKATTE",
            "感謝の芋煮会",
            "乙羽さんの畑で採れた芋や野菜、知り合いの漁師から届く海のものを囲んで開かれる、感謝の芋煮会です。楓さんが外から来た人をやわらかくつなぎ、乙羽さんが輪の中へ迎え入れることで、地域の人と同じ鍋を囲むところから、この土地の関係に入っていきます。",
            "感謝の芋煮会は、ただの食イベントではありません。乙羽さんの畑で育ったもの、知り合いの漁師から届いたもの、人の手、季節の巡り、来てくれた人。そうしたものをひとつの鍋と食卓に集める場です。まずは「食べに来てね」と招かれて鍋を囲むところから入ることができます。楓さんは、外から来た人が無理なくその輪に入れるよう整える人です。乙羽さんは、「食べていきなさい」と自然に輪の中へ通す人です。この二人がいることで、交流ではなく、同じ鍋を囲む関係が生まれます。",
            120,
            "乙羽さんの畑",
            "public",
            List.of("imoni", "collab")
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_WATANABE_IMONI_HELP,
            "watanabe-collab@example.com",
            "OKATTE",
            "感謝の芋煮会\nおでってお願いします。",
            "感謝の芋煮会の準備から少し手を貸してもらい、その流れのまま最後にみんなで同じ鍋を囲む参加のしかたです。食べるだけでなく、季節の行事を少し一緒につくった感じが残ります。",
            "「おでってお願いします。」は、感謝の芋煮会の準備から少し手を貸してもらう形です。芋をむく、野菜を運ぶ、鍋の準備をする。そうした食べる前の手仕事に加わったあと、そのままみんなで同じ鍋を囲みます。楓さんが、外から来た人が無理なく入れるよう整え、乙羽さんがその手の動きごと輪の中へ通してくれる。食べるだけでなく、季節の行事を自分も少し一緒につくった感じが残る参加のしかたです。",
            210,
            "乙羽さんの畑",
            "public",
            List.of("imoni", "support")
        );
        seedProposalForHost(
            ProposalPresentationCatalog.PROPOSAL_ID_YOSHINO_FARM,
            "yoshino@example.com",
            "OKATTE",
            "土から始める、実際の農業の入口",
            "本業のハウスとは別の区画で、トマトやコーンなどの夏野菜を育てるために、土づくりから植え付けまでしっかり関わるおかってです。気軽な畑体験ではなく、吉野さんが普段どこを見て、どう考えて土をつくっているのか、その入口を少し見せてもらいます。その後、収穫の時期にまた来てもらい、最後は奥さんの料理で食卓までつながっていきます。",
            "このおかっては、よくある気軽な農業体験ではありません。吉野さんが普段どこを見て土をつくり、どう考えて畝を立てているのか、その入口を少し見せてもらう時間です。土は、ただ作物を支える下地ではなく、命を育てる土台でもあります。肥料を入れ、混ぜ、整えながら、土をつくることの大事さや、土が持っている力を少しずつ知っていきます。本業のハウスとは別の区画で、トマトやコーンなどの夏野菜を育てるために、土づくりから植え付けまで行います。その後、季節が進んだらまた来てもらい、自分たちで関わった畑の収穫をし、最後は奥さんの料理で食卓につながっていきます。仕事の入口を見せてもらいながら、最後は暮らしの実りにも出会える、吉野さん一家らしいおかってです。",
            150,
            "畑",
            "public",
            List.of("farm", "season")
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

        if (preferredProposalId != null) {
            Long existingById = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM proposals WHERE id = ? AND deleted_at IS NULL",
                Long.class,
                preferredProposalId
            );
            if (existingById != null && existingById > 0) {
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
                    preferredProposalId
                );

                for (int i = 0; i < tags.size(); i++) {
                    seedProposalTag(preferredProposalId, tags.get(i), i);
                }
                return;
            }
        }

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

    private void seedSharedFootprints() {
        seedMapRecord(
            1001L,
            "footprint1@example.com",
            "境内の風と、OOさんのおはぎ",
            """
                境内のベンチに座っていると、時々サーッと風が渡っていきます。

                木々がざわめき、きれいな紅葉が町に流れていくようで、秋だなぁと思いました。その日はOOさんで買ったおはぎを持っていって、景色を見ながらゆっくり食べました。

                観光地の見どころとして切り取るというより、この場所の時間に少し混ぜてもらった感じがしました。誰かに強くおすすめしたいというより、こういう時間がこの町にあることを、そっと返しておきたいと思って書いています。
                """,
            "境内のベンチ",
            "public",
            "area",
            "2026-03-12T10:00:00Z"
        );
        seedMapRecord(
            1002L,
            "footprint2@example.com",
            "小さな店先で、ひとこと教わる",
            """
                通りの角にある小さな店先で、季節の話を少しだけ聞かせてもらいました。

                何かを買うことよりも、まずその場にある空気を受け取ることが大事なのだと感じました。店の人の言葉は短かったけれど、暮らしに根ざした重みがありました。

                こういう時間は、強いおすすめ文句にしなくても十分に残ると思います。読み終わったあとに、町を歩く速度が少し変わるような足あとになればと思っています。
                """,
            "通りの角の小さな店先",
            "public",
            "area",
            "2026-07-12T10:00:00Z"
        );
        seedMapRecord(
            1003L,
            "footprint3@example.com",
            "朝の入り口で見つけた、町のやさしさ",
            """
                朝の入口を歩いていると、急いでいない人たちのやりとりが自然に目に入ってきました。

                誰かが何かをしてあげているというより、その場で当たり前に支え合っている感じがありました。旅先として見るより先に、生活の輪郭を受け取った気がしました。

                まだ言葉にしきれないけれど、その優しさをちゃんと返せる旅人でいたいと思いました。
                """,
            "朝の入り口",
            "public",
            "area",
            "2026-11-12T10:00:00Z"
        );
    }

    private void seedMapRecord(
        long preferredMapRecordId,
        String ownerEmail,
        String title,
        String body,
        String locationName,
        String visibility,
        String locationPrecisionLevel,
        String createdAtIso
    ) {
        long ownerUserId = requireUserIdByEmail(ownerEmail);
        Timestamp timestamp = Timestamp.from(Instant.parse(createdAtIso));

        Long existingId = jdbcTemplate.query(
            "SELECT id FROM map_records WHERE id = ? AND deleted_at IS NULL",
            rs -> rs.next() ? rs.getLong("id") : null,
            preferredMapRecordId
        );

        if (existingId == null) {
            jdbcTemplate.update(
                """
                    INSERT INTO map_records (
                        id, user_id, title, body, visibility, location_name, latitude, longitude,
                        location_precision_level, is_draft, created_at, updated_at, visibility_updated_at, deleted_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                preferredMapRecordId,
                ownerUserId,
                title,
                body,
                visibility,
                locationName,
                null,
                null,
                locationPrecisionLevel,
                false,
                timestamp,
                timestamp,
                timestamp,
                null
            );
            return;
        }

        jdbcTemplate.update(
            """
                UPDATE map_records
                SET user_id = ?,
                    title = ?,
                    body = ?,
                    visibility = ?,
                    location_name = ?,
                    location_precision_level = ?,
                    is_draft = FALSE,
                    updated_at = ?,
                    visibility_updated_at = ?,
                    deleted_at = NULL
                WHERE id = ?
                """,
            ownerUserId,
            title,
            body,
            visibility,
            locationName,
            locationPrecisionLevel,
            timestamp,
            timestamp,
            preferredMapRecordId
        );
    }

    private void refreshSharedFootprintPresentation() {
        jdbcTemplate.update(
            """
                UPDATE map_records
                SET title = ?,
                    body = ?,
                    location_name = ?,
                    updated_at = ?,
                    visibility_updated_at = ?,
                    deleted_at = NULL
                WHERE id = ?
                """,
            "境内の風と、たわらやさんのおはぎ",
            """
                こちらの神社の境内のベンチに座っていると、時々サーッと風が渡っていきます。
                木々がざわめき、きれいな紅葉が町に流れていくようで。
                たわらやさんで買ったおはぎを食べながら、ゆっくり秋を感じるのがとても好きです。
                """,
            "こちらの神社の境内",
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now()),
            1001L
        );

        seedMapRecordImage(1001L, "/assets/images/おはぎ.png");
    }

    private void seedMapRecordImage(long mapRecordId, String filePath) {
        Long existingId = jdbcTemplate.query(
            "SELECT id FROM map_record_images WHERE map_record_id = ? AND sort_order = 0",
            rs -> rs.next() ? rs.getLong("id") : null,
            mapRecordId
        );

        if (existingId == null) {
            jdbcTemplate.update(
                """
                    INSERT INTO map_record_images (
                        map_record_id, file_path, sort_order, created_at
                    ) VALUES (?, ?, ?, ?)
                    """,
                mapRecordId,
                filePath,
                0,
                Timestamp.from(Instant.now())
            );
            return;
        }

        jdbcTemplate.update(
            "UPDATE map_record_images SET file_path = ?, sort_order = 0 WHERE id = ?",
            filePath,
            existingId
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
        private static final GateSeedData RIKYU_CAFE = new GateSeedData(
            "純喫茶で、少しずつほどける時間",
            "利休さんの純喫茶で、コーヒーや手作りケーキを前に、少しずつこの町に馴染んでいく時間です。にぎやかに盛り上がるのではなく、「ここにいていい」と思える居場所から、この町へ入っていきます。",
            "利休さんの入り口は、名物喫茶に行く時間というより、この町で安心して自分のままでいられる入口です。コーヒーや手作りケーキを前に、猫の気配や店の空気に触れているうちに、少しずつ緊張がほどけていきます。利休さんは無理に距離を詰めたり、話を引っ張ったりしません。同じものを見ながら、同じ場にいることで、自然に町に馴染んでいける。そんな時間です。",
            40,
            "利休さんの純喫茶",
            List.of("cafe", "quiet")
        );

        private static final GateSeedData RIKYU_WALK = new GateSeedData(
            "木漏れ日の道を、静かに歩く",
            "梅林地区の木漏れ日のきれいな道を、利休さんと静かに歩く時間です。観光ガイドの散策ではなく、喫茶の延長のようなやわらかな歩みの中で、この町に少し馴染んでいきます。",
            "この散歩は、絶景スポットを案内してもらう時間ではありません。利休さんと一緒に、木漏れ日のきれいな林道や小道を、無理に話しすぎず、同じ方向を見ながら歩いていく時間です。人見知りの方でも、ずっと目を合わせたり、距離を詰めすぎたりしなくていい。景色を見ながら同じ方向を向いて歩くことで、気まずくならずに町に少し馴染んでいけます。喫茶の延長にあるような、やわらかな入口です。",
            40,
            "梅林地区",
            List.of("walk", "komorebi")
        );
        private static final GateSeedData RURIKAWA_FARM = new GateSeedData(
            "小さな農園で、おでって",
            "乙羽さんの小さな農園で、外から来た人が少し畑仕事を手伝いながら、育てて分ける暮らしの輪に触れる時間です。本格的な農業体験ではなく、生活の延長にある畑仕事に、少し混ざらせてもらう入口です。",
            "この時間は、観光農園や収穫体験ではありません。乙羽さんの小さな農園で、野菜を収穫したり、雑草を取ったり、苗を整えたりしながら、育てることを少し手伝う入口です。でも、その畑は自分のためだけの畑ではありません。自分たちが食べるため、人にあげるため、漁師に渡すための畑でもあります。だからここでは、作業そのものより、育てて分ける暮らしの輪に少し入る感じが大事です。「これ持ってって」「その野菜はあの人にもあげるの」みたいなやりとりの中で、この土地のつながりが自然に見えてきます。",
            60,
            "小さな農園",
            List.of("farm", "share")
        );

        private static final GateSeedData RURIKAWA_WALK = new GateSeedData(
            "畑のあいだを歩きながら、暮らしに触れる",
            "畑の縁や小さな道を乙羽さんと一緒に歩きながら、人のつながりや季節の気配に少し触れていく時間です。のんびりした時間の流れの中で、ときどき立ち止まり、座って風を感じながら、この土地の見え方を少し分けてもらいます。",
            "この散歩は、景色の名所を案内してもらう時間ではありません。乙羽さんと、畑の縁や小さな農道、家と畑のあいだの道を歩きながら、「この野菜はあの人にもあげるの」「この先に漁師さんがいてね」といった話が自然に出てくるような散歩です。そして乙羽さんらしいのは、歩き続けるだけでなく、よく知っている落ち着く場所でときどき立ち止まり、座って風を感じたり、町の自然や人の気配を眺めたりすることです。のんびりした時間の流れの中で、ご年齢ならでは、乙羽さんならではのものの見方を、ご一緒することで少し垣間見られる。そんな入口です。",
            40,
            "畑沿いの小道",
            List.of("walk", "field")
        );

        private static final GateSeedData WATANABE_WATER = new GateSeedData(
            "水辺の空気を、やわらかくひらく散歩",
            "海辺や水辺を楓さんとゆっくり歩きながら、その日の風や水の色、船や港の気配に少しずつ馴染んでいく時間です。観光ガイドの町歩きではなく、この土地の水辺にやわらかく入っていくための入口です。",
            "この散歩は、観光名所を説明して回る時間ではありません。楓さんと一緒に、海辺や港まわり、堤防や川沿いを歩きながら、その日の潮や風、水の色、光の感じを少しずつ受け取っていく時間です。話しすぎなくても気まずくなく、同じ景色を見ることで、町に少し馴染んでいける。楓さんは強く引っ張るのではなく、「この時間の水辺、いいですよ」と静かに言えそうな距離感で、この土地の入口をひらいてくれます。",
            40,
            "海辺",
            List.of("water", "walk")
        );

        private static final GateSeedData WATANABE_STREAM = new GateSeedData(
            "渓流までの道も楽しい、やさしい釣りの入口",
            "釣りをやってみたいけれどハードルが高い人向けに、楓さんがやさしくひらく渓流釣りの入口です。川に着くまでの道や、水辺を探していく時間も含めて、釣りを少し身近に感じていきます。",
            "渓流まで行く道中も含めて、水辺に入っていく感覚そのものを楽しむ入口です。道具がわからない、どこでやればいいかわからない、子ども連れでできるか不安、そんなハードルを楓さんがやわらかく下げてくれます。家族連れでも参加しやすく、子どもは心配な子やはじめての子ならのべ竿、大人もまずはやさしい形で水辺に入っていけます。釣ることの難しさより、まずは川辺の空気や魚の気配を感じて、「やってみてもいいかも」と思えるようになる。そんな入口です。",
            120,
            "渓流",
            List.of("stream", "entry")
        );

        private static final GateSeedData YOSHINO_BIKE = new GateSeedData(
            "レンタサイクルで走る、吉野さんの景色のコース",
            "観光協会のレンタサイクルを使って、吉野さんと一緒に海や坂のある道を走り、この土地の光や風に触れていく時間です。途中、小高い場所にあるきれいなカフェでひと休みしながら、吉野さんがこの地域に惚れ込んだ理由を少しずつたどります。",
            "吉野さんと一緒に海や坂のある道を走りながら、この土地の風や光の感じに少しずつ馴染んでいく入口です。途中で小高い場所のカフェに立ち寄ることで、景色の中でひと息つく時間もあります。吉野さんは、この地域の朝日や夕日に惚れ込んで移住を決めた人なので、観光の絶景案内というより、「自分はここで決まっちゃったんですよね」と少し照れながら話してくれそうな、その人の目線で景色に触れられるのが魅力です。",
            120,
            "海辺の道",
            List.of("bicycle", "view")
        );
    }
}

(function () {
  // Java移行時メモ:
  // このファイルは公開側 / ログイン後の共通UIを仮描画する層です。
  // 本実装では、header / footer / 導線リンクはサーバー側テンプレートや設定APIで出し分ける形も考えやすそうです。
  // bridge と ログイン後 はワイヤ用補助導線として置いているため、本番では utilityLinks から外す整理もしやすいです。
  // 特に app header から /preview/public/bridge.html へ飛ばす導線は、今は利便性優先の仮置きです。
  // app 文脈から preview へ飛ばすリンクをどこまで許すかは、bridge の本実装時に整理対象として見直します。
  var entryCards = [
    { id: 'gate-1', duration: '30分', title: '海まで歩いて景色の話を聞く', note: '海を見ながら、この土地の好きな時間をたどる入口です。' },
    { id: 'gate-2', duration: '20分', title: 'ハウスの前で野菜を見る', note: '育てているものを見ながら、地域の挑戦を少し聞きます。' },
    { id: 'gate-3', duration: '60分', title: '港のそばを歩いて過ごす', note: '仕事場の近くで、地域の手ざわりを静かに感じます。' },
    { id: 'gate-4', duration: '20分', title: '木漏れ日の道を一緒に歩く', note: '人見知りでも入りやすい、小さな入口です。' },
    { id: 'gate-5', duration: '45分', title: '丘の上から季節の景色を見る', note: 'その時期ならではの風景を、無理なく味わいます。' }
  ];

  var ruleLists = {
    okatte: [
      '無理をして開くのではなく、その時の余力に合わせて考えます。',
      '難しい提案は急がず、見送る・別の形にする判断もできます。',
      'ここで決めた内容は公開されず、必要な範囲で調整していきます。'
    ],
    my_map: [
      '公開範囲を選びながら、自分の記録として残せます。',
      '人に見せる記録と、自分だけの記録を分けて扱えます。',
      '生活に近い場所は出しすぎないよう慎重に扱います。',
      '公開用の記録では、プライベートを守るために詳しく書けないことがあります。そのくらいの余白が、かえってちょうどよかったりします。'
    ],
    footprints: [
      '公開の場には、感謝や好きだったことを残す前提です。',
      '困りごとや改善点は、非公開の連絡導線で受け止めます。',
      '場所や人に負担がかかる書き方は避けてください。',
      '誰かを採点するためではなく、その土地との関わりを残す場です。'
    ],
    gate: [
      '最初から深く入るのではなく、短時間・低負担の入口から始めます。',
      'その日の体調や相性によって、別の入口に変えることもできます。',
      '入口カードは将来 API の一覧データへ置き換える想定です。'
    ],
    gate_public: [
      '公開されるのは、地域とどう関われそうかの入口の空気です。',
      '詳しい事情や個別のやり取りは、ログイン後や相談導線で扱います。',
      'まずは見てみるための入口であり、無理に奥へ進めるページではありません。',
      'あとから関係や相性を見ながら、開き方を調整する前提です。'
    ]
  };

  var notificationItems = [
    {
      notification_id: 'n-1001',
      user_id: 'wire-user-1',
      type: 'proposal-approved',
      kindLabel: '提案・申請',
      title: 'おかっての申請が承認されました',
      body: '郷土料理をみんなで作る午後の申請が承認されました。詳細をご確認ください。',
      related_entity_type: 'proposal',
      related_entity_id: 'okatte-1',
      related_url: '../public/okatte-entry.html?proposal=okatte-1',
      is_read: false,
      created_at: '2026-03-17 09:20',
      read_at: null,
      sender_name: '架け橋さん',
      sender_role: '橋渡し',
      action_label: '提案を見る'
    },
    {
      notification_id: 'n-1002',
      user_id: 'wire-user-1',
      type: 'message-bridge',
      kindLabel: '連絡',
      title: '架け橋さんから連絡が来ました',
      body: '次に合いそうな入口について、短い連絡が届いています。',
      related_entity_type: 'chat',
      related_entity_id: 'chat-bridge-1',
      related_url: '../app/chat.html?thread=thread-bridge-1',
      is_read: false,
      created_at: '2026-03-17 08:10',
      read_at: null,
      sender_name: '長谷川 虹翔さん',
      sender_role: '架け橋さん',
      action_label: '連絡を見る'
    },
    {
      notification_id: 'n-1003',
      user_id: 'wire-user-1',
      type: 'reaction-thanks',
      kindLabel: '反応',
      title: 'ありがとうが届きました',
      body: '海辺の朝の足あとに、ありがとうが届いています。',
      related_entity_type: 'story',
      related_entity_id: 'story-1',
      related_url: '../public/story.html?story=1',
      is_read: false,
      created_at: '2026-03-16 21:32',
      read_at: null,
      sender_name: '地域の方',
      sender_role: '地域',
      action_label: '記事を見る'
    },
    {
      notification_id: 'n-1004',
      user_id: 'wire-user-1',
      type: 'safety-guide',
      kindLabel: '確認',
      title: '公開範囲の見直し案内',
      body: '位置情報の表示粒度について、公開範囲の見直し案内があります。',
      related_entity_type: 'privacy',
      related_entity_id: 'privacy-1',
      related_url: '../app/privacy-settings.html',
      is_read: true,
      created_at: '2026-03-15 18:05',
      read_at: '2026-03-15 19:00',
      action_label: '設定を見る'
    },
    {
      notification_id: 'n-1005',
      user_id: 'wire-user-1',
      type: 'identity-required',
      kindLabel: '重要',
      title: '追加本人確認が必要です',
      body: '安心して関われる場を守るため、追加の本人確認をお願いしています。',
      related_entity_type: 'verify',
      related_entity_id: 'verify-1',
      related_url: '../auth/register-verify.html',
      is_read: false,
      created_at: '2026-03-14 11:48',
      read_at: null,
      severity: 'important',
      action_label: '確認する'
    },
    {
      notification_id: 'n-1006',
      user_id: 'wire-user-1',
      type: 'notice',
      kindLabel: 'お知らせ',
      title: '運営からのお知らせがあります',
      body: '今月の運営方針と、公開の場の扱いについて更新があります。',
      related_entity_type: 'notice',
      related_entity_id: 'notice-1',
      related_url: '../public/notice.html?slug=announcement-spring',
      is_read: true,
      created_at: '2026-03-12 15:15',
      read_at: '2026-03-12 15:30',
      action_label: 'お知らせを見る'
    }
  ];

  var navConfigs = {
    public_main: {
      variant: 'public',
      brandHref: 'index.html',
      mainLinks: [
        { label: 'ちいきの入り口（次段階）', disabled: true },
        { href: '#top-footprints', label: 'ごひいきさんの足あと', currentMatchers: ['index.html', 'story.html'] },
        { label: '私たちの目指すもの（次段階）', disabled: true },
        { label: '地域の方へ（次段階）', disabled: true },
        { label: 'FAQ（次段階）', disabled: true },
        { label: 'お問い合わせ（次段階）', disabled: true }
      ],
      utilityLinks: [
        { href: 'bridge.html', label: 'ブリッジ' }
      ],
      action: [
        { href: '../auth/register.html', label: '新規登録', currentMatchers: ['register.html', 'register-profile.html', 'register-complete.html', 'register-verify.html'] },
        { href: '../auth/login.html', label: 'ログイン', currentMatchers: ['login.html'] }
      ]
    },
    public_secondary: {
      variant: 'public',
      brandHref: 'index.html',
      mainLinks: [
        { label: 'ちいきの入り口（次段階）', disabled: true },
        { href: 'index.html#top-footprints', label: 'ごひいきさんの足あと', currentMatchers: ['index.html', 'story.html'] },
        { label: '私たちの目指すもの（次段階）', disabled: true },
        { label: '地域の方へ（次段階）', disabled: true },
        { label: 'FAQ（次段階）', disabled: true },
        { label: 'お問い合わせ（次段階）', disabled: true }
      ],
      utilityLinks: [
        { href: 'bridge.html', label: 'ブリッジ' }
      ],
      action: [
        { href: '../auth/register.html', label: '新規登録', currentMatchers: ['register.html', 'register-profile.html', 'register-complete.html', 'register-verify.html'] },
        { href: '../auth/login.html', label: 'ログイン', currentMatchers: ['login.html'] }
      ]
    },
    public_gate: {
      variant: 'public',
      brandHref: 'index.html',
      mainLinks: [
        { label: 'ちいきの入り口（次段階）', disabled: true },
        { href: 'index.html#top-footprints', label: 'ごひいきさんの足あと', currentMatchers: ['index.html', 'story.html'] },
        { label: '私たちの目指すもの（次段階）', disabled: true },
        { label: '地域の方へ（次段階）', disabled: true },
        { label: 'FAQ（次段階）', disabled: true },
        { label: 'お問い合わせ（次段階）', disabled: true }
      ],
      utilityLinks: [
        { href: 'bridge.html', label: 'ブリッジ' }
      ],
      action: [
        { href: '../auth/register.html', label: '新規登録', currentMatchers: ['register.html', 'register-profile.html', 'register-complete.html', 'register-verify.html'] },
        { href: '../auth/login.html', label: 'ログイン', currentMatchers: ['login.html'] }
      ]
    },
    auth_login: {
      variant: 'public',
      brandHref: '../public/index.html',
      mainLinks: [
        { href: '../public/gate.html?context=app', label: 'ちいきの入り口', currentMatchers: ['gate.html', 'gate-entry.html'] },
        { href: '../app/home.html?tab=footprints', label: 'ごひいきさんの足あと', currentMatchers: ['home.html', 'index.html', 'story.html'] },
        { href: '../public/about.html', label: '私たちの目指すもの', currentMatchers: ['about.html'] },
        { href: '../public/local.html', label: '地域の方へ', currentMatchers: ['local.html'] },
        { href: '../public/faq.html?context=app', label: 'FAQ', currentMatchers: ['faq.html'] },
        { href: '../public/safety.html?context=app', label: 'お問い合わせ', currentMatchers: ['safety.html', 'safety-complete.html'] }
      ],
      utilityLinks: [
        { href: '../public/bridge.html', label: 'ブリッジ' },
        { href: '../app/home.html', label: 'ログイン後' },
        { href: '../app/local-member-home.html', label: '\u30ed\u30b0\u30a4\u30f3\u5f8c\uff08\u5730\u57df\uff09' }
      ],
      action: [
        { href: '../auth/register.html', label: '新規登録', currentMatchers: ['register.html', 'register-profile.html', 'register-complete.html', 'register-verify.html'] },
        { href: '../auth/login.html', label: 'ログイン', currentMatchers: ['login.html'] }
      ]
    },
    app_home: {
      variant: 'app',
      brandHref: '/app/home',
      mainLinks: [
        { href: '/app/gate', label: 'ちいきの入り口', currentMatchers: ['gate', 'gate.html'] },
        { href: '/app/map-records', label: 'わたしの地図', currentMatchers: ['map-records'] },
        { href: '/app/home', label: 'ごひいきさんの足あと', currentMatchers: ['home', 'home.html'] },
        { href: '/app/okatte', label: 'ちいきのおかって', currentMatchers: ['okatte', 'okatte.html'] },
        // route 実装時は disabled を外し、（次段階）も消して 1 行表示へ戻す。
        { label: '私たちの目指すもの（次段階）', disabled: true },
        { label: 'FAQ（次段階）', disabled: true },
        { label: 'お問い合わせ（次段階）', disabled: true }
      ],
      utilityLinks: [
        { href: '/preview/public/bridge.html', label: 'ブリッジ' }
      ],
      notificationsHref: '/app/mypage',
      action: [
        { href: '/app/chat', label: '連絡', currentMatchers: ['chat', 'chat.html'] },
        { href: '/app/mypage', label: 'マイページ', currentMatchers: ['mypage', 'mypage.html', 'history', 'history.html', 'account', 'account.html', 'profile', 'profile.html', 'privacy-settings', 'privacy-settings.html'] }
      ]
    },
    app_local: {
      variant: 'app',
      brandHref: '/app/local-member-home',
      mainLinks: [
        { href: '/app/gate', label: 'ちいきの入り口', currentMatchers: ['gate', 'gate.html'] },
        { href: '/app/map-records', label: 'わたしの地図', currentMatchers: ['map-records'] },
        { href: '/app/local-member-home', label: 'ごひいきさんの足あと', currentMatchers: ['local-member-home', 'local-member-home.html'] },
        { href: '/app/okatte', label: 'ちいきのおかって', currentMatchers: ['okatte', 'okatte.html'] },
        // route 実装時は disabled を外し、（次段階）も消して 1 行表示へ戻す。
        { label: '私たちの目指すもの（次段階）', disabled: true },
        { label: 'FAQ（次段階）', disabled: true },
        { label: 'お問い合わせ（次段階）', disabled: true }
      ],
      utilityLinks: [
        { href: '/preview/public/bridge.html', label: 'ブリッジ' }
      ],
      notificationsHref: '/app/mypage',
      action: [
        { label: '連絡（次段階）', disabled: true },
        { href: '/app/mypage', label: 'マイページ', currentMatchers: ['mypage', 'mypage.html', 'history', 'history.html', 'account', 'account.html', 'profile', 'profile.html', 'privacy-settings', 'privacy-settings.html'] }
      ]
    },
    app_local_member: {
      variant: 'app',
      brandHref: '/app/local-member-home',
      brandTag: '\u5730\u57df',
      // Java移行時メモ: app_local_member は登録済み地域ユーザー向けヘッダーです。
      // 公開の local.html と混ぜず、LOCAL ロールのログイン後導線として分ける前提です。
      mainLinks: [
        { href: '/app/gate', label: '\u3061\u3044\u304d\u306e\u5165\u308a\u53e3', currentMatchers: ['gate', 'gate.html'] },
        { href: '/app/map-records', label: '\u308f\u305f\u3057\u306e\u5730\u56f3', currentMatchers: ['map-records'] },
        { href: '/app/local-member-home', label: '\u3054\u3072\u3044\u304d\u3055\u3093\u306e\u8db3\u3042\u3068', currentMatchers: ['local-member-home', 'local-member-home.html'] },
        { href: '/app/okatte', label: '\u3061\u3044\u304d\u306e\u304a\u304b\u3063\u3066', currentMatchers: ['okatte', 'okatte.html'] },
        // route 実装時は disabled を外し、（次段階）も消して 1 行表示へ戻す。
        { label: '私たちの目指すもの（次段階）', disabled: true },
        { label: 'FAQ（次段階）', disabled: true },
        { label: '\u304a\u554f\u3044\u5408\u308f\u305b\uff08\u6b21\u6bb5\u968e\uff09', disabled: true }
      ],
      utilityLinks: [
        { href: '/preview/public/bridge.html', label: '\u30d6\u30ea\u30c3\u30b8' }
      ],
      notificationsHref: '/app/mypage',
      action: [
        { href: '/app/chat', label: '\u9023\u7d61', currentMatchers: ['chat', 'chat.html'] },
        { href: '/app/mypage', label: '\u30de\u30a4\u30da\u30fc\u30b8', currentMatchers: ['mypage', 'mypage.html', 'history', 'history.html', 'account', 'account.html', 'profile', 'profile.html', 'privacy-settings', 'privacy-settings.html'] }
      ]
    }
  };

  var footerConfig = {
    about: [
      { href: 'index.html', label: 'FURUTABI' },
      { label: '私たちの目指すもの（次段階）', disabled: true },
      { label: 'FAQ（次段階）', disabled: true },
      { label: 'お問い合わせ（次段階）', disabled: true }
    ],
    usage: [
      { label: 'ちいきの入り口（次段階）', disabled: true },
      { href: 'index.html#top-footprints', label: 'ごひいきさんの足あと' },
      { label: 'わたしの地図（次段階）', disabled: true },
      { label: 'ちいきのおかって（次段階）', disabled: true }
    ],
    legal: [
      { label: '利用規約（次段階）', disabled: true },
      { label: 'プライバシーポリシー（次段階）', disabled: true },
      { href: '../auth/login.html', label: 'ログイン' }
    ]
  };

  function getFooterConfig() {
    var path = window.location.pathname.replace(/\\/g, '/');
    var inApp = /\/app\//.test(path);
    var localApp = /\/app\/local-(member-)?home/.test(path);

    if (!inApp) {
      return footerConfig;
    }

    return {
      about: [
        { href: localApp ? '/app/local-member-home' : '/app/home', label: 'FURUTABI' },
        { label: '私たちの目指すもの（次段階）', disabled: true },
        { label: 'FAQ（次段階）', disabled: true },
        { label: 'お問い合わせ（次段階）', disabled: true }
      ],
      usage: [
        { href: '/app/gate', label: 'ちいきの入り口' },
        { href: localApp ? '/app/local-member-home' : '/app/home', label: 'ごひいきさんの足あと' },
        { href: '/app/map-records', label: 'わたしの地図' },
        { href: '/app/okatte', label: 'ちいきのおかって' }
      ],
      legal: [
        { label: '利用規約（次段階）', disabled: true },
        { label: 'プライバシーポリシー（次段階）', disabled: true },
        { href: '/app/mypage', label: 'マイページ' }
      ]
    };
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function getCurrentPathname() {
    return window.location.pathname.split('/').pop() || 'index.html';
  }

  function getHrefPathname(href) {
    if (!href || href.charAt(0) === '#') {
      return getCurrentPathname();
    }
    return href.split('#')[0].split('?')[0].split('/').pop();
  }

  function appendQueryParam(href, key, value) {
    var hashIndex = href.indexOf('#');
    var hash = '';
    var base = href;

    if (hashIndex >= 0) {
      hash = href.slice(hashIndex);
      base = href.slice(0, hashIndex);
    }

    if (base.indexOf(key + '=') >= 0) {
      return href;
    }

    return base + (base.indexOf('?') >= 0 ? '&' : '?') + encodeURIComponent(key) + '=' + encodeURIComponent(value) + hash;
  }

  function isCurrentLink(link) {
    var current = getCurrentPathname();
    var target = getHrefPathname(link.href);

    if (link.currentMatchers && link.currentMatchers.indexOf(current) !== -1) {
      return true;
    }

    return current === target;
  }

  function renderNavLinks(links, className) {
    return (links || []).map(function (link) {
      if (link.disabled || !link.href) {
        var disabledLabel = escapeHtml(link.label);
        if (disabledLabel.indexOf('（次段階）') >= 0) {
          disabledLabel = disabledLabel.replace('（次段階）', '<br><span class="navSubLabel">（次段階）</span>');
        }
        return '<span class="' + className + ' is-disabled" aria-disabled="true">' + disabledLabel + '</span>';
      }
      var current = isCurrentLink(link);
      return '<a class="' + className + (current ? ' is-current' : '') + '" href="' + escapeHtml(link.href) + '"' + (current ? ' aria-current="page"' : '') + '>' + escapeHtml(link.label) + '</a>';
    }).join('');
  }

  function getUnreadNotificationsCount() {
    return notificationItems.filter(function (item) { return !item.is_read; }).length;
  }

  function renderNotificationBell(config) {
    if (config.variant !== 'app') {
      return '';
    }

    var unreadCount = getUnreadNotificationsCount();
    var badge = '';
    if (unreadCount > 0) {
      badge = '<span class="siteHeaderNoticeBadge" aria-label="未読通知 ' + escapeHtml(unreadCount > 99 ? '99+' : String(unreadCount)) + '件">' + escapeHtml(unreadCount > 99 ? '99+' : String(unreadCount)) + '</span>';
    }

    return '' +
      '<div class="siteHeaderNotice" data-header-notice>' +
        '<!-- 通知は「やり取りの場」ではなく「起きたことに気づく場」 -->' +
        '<!-- ベルアイコンはログイン後ヘッダーに置く -->' +
        '<!-- 未読時はバッジ表示 -->' +
        '<!-- ドロップダウンでは最新数件だけ表示 -->' +
        '<!-- 「すべて見る」で通知一覧ページへ遷移 -->' +
        '<!-- 詳細な会話や調整は通知内で完結させず、提案詳細 / 申請詳細 / チャット / 記事ページ等へ遷移させる -->' +
        '<button class="siteHeaderNoticeButton" type="button" aria-expanded="false" aria-haspopup="dialog" aria-label="通知を開く">' +
          '<span class="siteHeaderNoticeIcon" aria-hidden="true">🔔</span>' +
          badge +
        '</button>' +
        '<div class="siteHeaderNoticeDropdown" hidden data-header-notice-panel>' +
          '<div class="siteHeaderNoticeHead">' +
            '<h2>通知</h2>' +
            (unreadCount > 0 ? '<span class="pill pill--soft">' + escapeHtml(unreadCount > 99 ? '99+' : String(unreadCount)) + '件の未読</span>' : '') +
          '</div>' +
          '<div class="siteHeaderNoticeList" data-header-notice-list></div>' +
          '<div class="siteHeaderNoticeFoot">' +
            '<a href="' + escapeHtml(config.notificationsHref || '../app/notification-center.html') + '">すべて見る</a>' +
          '</div>' +
        '</div>' +
      '</div>';
  }

  function renderImportantNotification(config) {
    if (config.variant !== 'app') {
      return '';
    }

    var importantItem = notificationItems.find(function (item) {
      return item.severity === 'important' && !item.is_read;
    });

    if (!importantItem) {
      return '';
    }

    return '' +
      '<div class="siteHeaderImportantNotice" data-important-notice>' +
        '<div class="siteHeaderImportantNoticeBody">' +
          '<div class="siteHeaderImportantNoticeMeta">' +
            '<span class="pill">大事なお知らせ</span>' +
            '<time>' + escapeHtml(importantItem.created_at) + '</time>' +
          '</div>' +
          '<div class="siteHeaderImportantNoticeTitle">' + escapeHtml(importantItem.title) + '</div>' +
          '<p class="siteHeaderImportantNoticeText">' + escapeHtml(importantItem.body) + '</p>' +
          '<div class="siteHeaderImportantNoticeActions">' +
            '<a href="' + escapeHtml(importantItem.related_url) + '">' + escapeHtml(importantItem.action_label || '確認する') + '</a>' +
          '</div>' +
        '</div>' +
        '<button class="siteHeaderImportantNoticeClose" type="button" aria-label="通知を閉じる">×</button>' +
      '</div>';
  }

  function formatNotificationItem(item, className) {
    return '' +
      '<a class="' + className + (item.is_read ? '' : ' is-unread') + '" href="' + escapeHtml(item.related_url) + '" data-notification-id="' + escapeHtml(item.notification_id) + '">' +
        '<div class="siteHeaderNoticeMeta">' +
          '<span class="pill">' + escapeHtml(item.kindLabel) + '</span>' +
          '<time>' + escapeHtml(item.created_at) + '</time>' +
        '</div>' +
        '<div class="siteHeaderNoticeTitle">' + escapeHtml(item.title) + '</div>' +
        '<p class="siteHeaderNoticeText">' + escapeHtml(item.body) + '</p>' +
      '</a>';
  }

  function hydrateHeaderNotifications(root, config) {
    if (config.variant !== 'app') {
      return;
    }

    var noticeRoot = root.querySelector('[data-header-notice]');
    if (!noticeRoot) {
      return;
    }

    var button = noticeRoot.querySelector('.siteHeaderNoticeButton');
    var panel = noticeRoot.querySelector('[data-header-notice-panel]');
    var list = noticeRoot.querySelector('[data-header-notice-list]');
    if (!button || !panel || !list) {
      return;
    }

    var latestItems = notificationItems.slice(0, 5);
    list.innerHTML = latestItems.length
      ? latestItems.map(function (item) { return formatNotificationItem(item, 'siteHeaderNoticeItem'); }).join('')
      : '<p class="siteHeaderNoticeEmpty">通知がまだありません。新しい提案ややり取りがあると、ここに表示されます。</p>';

    function closePanel() {
      button.setAttribute('aria-expanded', 'false');
      panel.hidden = true;
    }

    function openPanel() {
      button.setAttribute('aria-expanded', 'true');
      panel.hidden = false;
    }

    button.addEventListener('click', function () {
      var expanded = button.getAttribute('aria-expanded') === 'true';
      if (expanded) {
        closePanel();
      } else {
        openPanel();
      }
    });

    document.addEventListener('click', function (event) {
      if (!noticeRoot.contains(event.target)) {
        closePanel();
      }
    });

    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        closePanel();
      }
    });
  }

  function hydrateImportantNotification(root, config) {
    if (config.variant !== 'app') {
      return;
    }

    var notice = root.querySelector('[data-important-notice]');
    if (!notice) {
      return;
    }

    var closeButton = notice.querySelector('.siteHeaderImportantNoticeClose');
    function closeNotice() {
      notice.remove();
    }

    if (closeButton) {
      closeButton.addEventListener('click', closeNotice);
    }
  }

  function renderNav(root) {
    var key = root.getAttribute('data-site-nav');
    var pathname = getCurrentPathname();
    var context = readQueryParam('context');

    // ワイヤでは公開側の詳細ページをログイン後からも共用しているため、
    // context=app のときだけログイン後ナビに読み替える。
    if ((pathname === 'okatte-entry.html'
      || pathname === 'gate-entry.html'
      || pathname === 'gate.html'
      || pathname === 'index.html'
      || pathname === 'about.html'
      || pathname === 'story.html'
      || pathname === 'faq.html'
      || pathname === 'safety.html'
      || pathname === 'safety-complete.html') && context === 'app') {
      key = 'app_home';
    }
    var config = navConfigs[key];

    if (!config) {
      root.innerHTML = '';
      return;
    }

    var brand = root.closest('.inner');
    if (brand) {
      var brandNode = brand.querySelector('.brand');
      if (brandNode && brandNode.tagName !== 'A') {
        brandNode.outerHTML = '<a class="brand" href="' + escapeHtml(config.brandHref) + '">FURUTABI｜郷旅</a>';
      } else if (brandNode) {
        brandNode.setAttribute('href', config.brandHref);
      }
      var activeBrandNode = brand.querySelector('.brand');
      var existingBrandTag = brand.querySelector('.brandTag');
      if (config.brandTag && activeBrandNode) {
        if (!existingBrandTag) {
          activeBrandNode.insertAdjacentHTML('afterend', '<span class="brandTag">' + escapeHtml(config.brandTag) + '</span>');
        } else {
          existingBrandTag.textContent = config.brandTag;
        }
      } else if (existingBrandTag) {
        existingBrandTag.remove();
      }
    }

    root.classList.add('siteHeaderNav');
    root.innerHTML = '' +
      '<!-- 公開側ヘッダーとログイン後ヘッダーは分ける -->' +
        '<!-- ログイン後はロゴからホームへ戻れるため、主ナビにホームは置かない -->' +
      '<!-- 「詳しくは」は使わず「私たちの目指すもの」 -->' +
      '<!-- FAQ と お問い合わせ は独立ページ導線 -->' +
      '<!-- 公開側でも「ごひいきさんの足あと」の正式名称を使う -->' +
      '<button class="siteHeaderToggle" type="button" aria-expanded="false" aria-label="メニューを開く">' +
        '<span></span><span></span><span></span>' +
      '</button>' +
      renderImportantNotification(config) +
      '<div class="siteHeaderMenu">' +
        '<div class="siteHeaderUtility">' + renderNavLinks(config.utilityLinks, 'siteHeaderUtilityLink') + '</div>' +
        '<div class="siteHeaderPrimary">' + renderNavLinks(config.mainLinks, 'siteHeaderLink') + '</div>' +
        '<div class="siteHeaderActionWrap">' + renderNotificationBell(config) + renderNavLinks(config.action, 'siteHeaderAction') + '</div>' +
      '</div>';

    var toggle = root.querySelector('.siteHeaderToggle');
    var menu = root.querySelector('.siteHeaderMenu');
    if (toggle && menu) {
      toggle.addEventListener('click', function () {
        var expanded = toggle.getAttribute('aria-expanded') === 'true';
        toggle.setAttribute('aria-expanded', expanded ? 'false' : 'true');
        menu.classList.toggle('is-open', !expanded);
      });
    }

    hydrateHeaderNotifications(root, config);
    hydrateImportantNotification(root, config);
  }

  function withBasePrefix(href) {
    var path = window.location.pathname.replace(/\\/g, '/');
    var inApp = /\/app\//.test(path);
    var inAuth = /\/auth\//.test(path);
    var inAuthOrApp = inApp || inAuth;
    var appContext = inApp || readQueryParam('context') === 'app';
    var resolvedHref = href;
    var pathname;
    var appContextTargets = ['about.html', 'gate.html', 'gate-entry.html', 'index.html', 'story.html', 'faq.html', 'safety.html', 'safety-complete.html', 'okatte-entry.html'];
    var preferLocalMemberHome = /\/app\/local-(member-)?home(\.html)?/.test(path);

    if (href === '#') {
      return href;
    }

    if (/^(\/|https?:)/.test(resolvedHref)) {
      return resolvedHref;
    }

    if (inAuthOrApp && resolvedHref.indexOf('../') !== 0) {
      resolvedHref = '../public/' + resolvedHref;
    }

    if (inAuth && resolvedHref === '../auth/login.html') {
      return './login.html';
    }

    if (inAuth && resolvedHref === '../auth/register.html') {
      return './register.html';
    }

    if (appContext && (resolvedHref === 'index.html#top-footprints' || resolvedHref === '../public/index.html#top-footprints')) {
      return preferLocalMemberHome ? '../app/local-member-home.html?tab=footprints' : '../app/home.html?tab=footprints';
    }

    pathname = getHrefPathname(resolvedHref);

    if (appContext && appContextTargets.indexOf(pathname) !== -1) {
      resolvedHref = appendQueryParam(resolvedHref, 'context', 'app');
    }

    return resolvedHref;
  }

  function renderFooterLinks(links) {
    return links.map(function (link) {
      if (link.disabled || !link.href) {
        return '<span class="is-disabled" aria-disabled="true">' + escapeHtml(link.label) + '</span>';
      }
      return '<a href="' + escapeHtml(withBasePrefix(link.href)) + '">' + escapeHtml(link.label) + '</a>';
    }).join('');
  }

  function ensureFooter() {
    var activeFooterConfig = getFooterConfig();
    var footer = document.querySelector('[data-site-footer]') || document.querySelector('.siteFooter');
    if (!footer) {
      footer = document.createElement('footer');
      footer.className = 'siteFooter';
      footer.setAttribute('data-site-footer', '');
      var main = document.querySelector('main');
      if (main && main.parentNode) {
        main.parentNode.insertBefore(footer, main.nextSibling);
      } else {
        document.body.appendChild(footer);
      }
    }

    footer.innerHTML = '' +
      '<div class="container siteFooterInner">' +
        '<!-- 地域の方へは独立カラムにしない -->' +
        '<!-- フッターは旅人の方にも分かりやすい構造を優先 -->' +
        '<!-- フッターでは正式名称を使う -->' +
        '<!-- FAQ と お問い合わせ は独立ページ導線 -->' +
        '<!-- 利用規約とプライバシーポリシーは必須 -->' +
        '<div class="siteFooterGrid">' +
          '<section class="siteFooterCol">' +
            '<h2>FURUTABIについて</h2>' +
            '<div class="siteFooterLinks">' + renderFooterLinks(activeFooterConfig.about) + '</div>' +
          '</section>' +
          '<section class="siteFooterCol">' +
            '<h2>使い方</h2>' +
            '<div class="siteFooterLinks">' + renderFooterLinks(activeFooterConfig.usage) + '</div>' +
          '</section>' +
          '<section class="siteFooterCol">' +
            '<h2>ご利用にあたって</h2>' +
            '<div class="siteFooterLinks">' + renderFooterLinks(activeFooterConfig.legal) + '</div>' +
          '</section>' +
        '</div>' +
        '<div class="siteFooterBottom">' +
          '<span>© FURUTABI</span>' +
          '<span>見物から、関係の旅へ。</span>' +
        '</div>' +
      '</div>';
  }

  function getEntryButton(root) {
    var context = root.getAttribute('data-entry-context') || 'public';
    return { context: context, label: '詳しく見る', buttonClass: 'ghost' };
  }

  function buildEntryDetailHref(root, entryId, contextOverride) {
    var context = contextOverride || root.getAttribute('data-entry-context') || 'public';
    var basePath = context === 'app' ? '../public/' : '';

    // TODO: Java 実装時は entry id / slug を API 経由の詳細URLへ置き換える。
    return basePath + 'gate-entry.html?entry=' + encodeURIComponent(entryId) + '&context=' + encodeURIComponent(context);
  }

  function renderEntryCards(root) {
    var button = getEntryButton(root);

    root.innerHTML = entryCards.map(function (card) {
      return '' +
        '<article class="card entryCarouselCard" data-entry-card data-entry-id="' + escapeHtml(card.id) + '">' +
          '<div class="ph ph--thumb" aria-hidden="true"></div>' +
          '<div style="display:flex; justify-content:space-between; gap:12px; align-items:center;">' +
            '<span class="pill">' + escapeHtml(card.duration) + '</span>' +
            '<div class="ph ph--avatar" aria-hidden="true"></div>' +
          '</div>' +
          '<div class="h2">' + escapeHtml(card.title) + '</div>' +
          '<p class="note">' + escapeHtml(card.note) + '</p>' +
          '<a class="btn ' + escapeHtml(button.buttonClass) + '" href="' + escapeHtml(buildEntryDetailHref(root, card.id, button.context)) + '">' + escapeHtml(button.label) + '</a>' +
        '</article>';
    }).join('');
  }

  function renderRuleList(root) {
    var key = root.getAttribute('data-rule-list');
    var items = ruleLists[key] || [];

    root.innerHTML = items.map(function (item) {
      return '<li>・' + escapeHtml(item) + '</li>';
    }).join('');
  }

  function readQueryParam(name) {
    var params = new URLSearchParams(window.location.search);
    return params.get(name);
  }

  function setPageTitle(prefix, title) {
    document.title = prefix + title;
  }

  function renderParagraphs(container, paragraphs) {
    if (!container) {
      return;
    }

    container.innerHTML = (paragraphs || []).map(function (paragraph) {
      return '<p>' + escapeHtml(paragraph) + '</p>';
    }).join('');
  }

  function init() {
    document.querySelectorAll('[data-site-nav]').forEach(renderNav);
    document.querySelectorAll('[data-entry-list]').forEach(renderEntryCards);
    document.querySelectorAll('[data-rule-list]').forEach(renderRuleList);
    ensureFooter();
  }

  window.FURUTABI_SHARED_UI = {
    entryCards: entryCards,
    buildEntryDetailHref: buildEntryDetailHref,
    ruleLists: ruleLists,
    notificationItems: notificationItems,
    navConfigs: navConfigs,
    readQueryParam: readQueryParam,
    setPageTitle: setPageTitle,
    renderParagraphs: renderParagraphs
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

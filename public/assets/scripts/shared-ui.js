(function () {
  // Wire段階の共通UIデータ。
  // Java化ではこのファイルを「固定文言 / 固定ナビ設定」だけに縮小し、
  // 可変データは API かテンプレート側へ移す想定。
  var entryCards = [
    {
      id: 'gate-1',
      duration: '30分',
      title: '生まれ育った店先の話を聞く',
      note: '声をかけ暮らしに触れる'
    },
    {
      id: 'gate-2',
      duration: '10分',
      title: '路地で交わす短い会話',
      note: '短時間ではじめて近づく'
    },
    {
      id: 'gate-3',
      duration: '60分',
      title: '地域の流れを少し教わる',
      note: '肩肘張らずにつながる'
    },
    {
      id: 'gate-4',
      duration: '20分',
      title: '好きな場所で最近をたずねる',
      note: '近況からひとつ深掘り'
    },
    {
      id: 'gate-5',
      duration: '45分',
      title: '町の気配を一緒に歩く',
      note: '暮らしぶりを静かに知る'
    }
  ];

  var ruleLists = {
    okatte: [
      '提案は、すぐに実行するためだけの依頼ではなく、地域の人の暮らしや都合をたずねる入口として扱います。',
      '受け取った側が無理なく返せるよう、返事の強制や即答を求めない前提を置きます。',
      '内容に迷いがあるときは、まず短く相談し、公開の場ではなく非公開の連絡へ回せるようにします。'
    ],
    my_map: [
      '公開前の地図なので、むやみに広げず、自分があとで見返せる記録として使います。',
      '公開するときは、場所や人への配慮が足りているかを見直してから出せる前提にします。',
      '写真や文章に迷いがある場合は、まず下書きで置いておき、あとから整えられるようにします。'
    ],
    footprints: [
      '一方的に切り取るのではなく、「その場所で受け取った時間」を静かに返す書き方を大切にします。',
      '写真枚数や文字数は多すぎなくてかまいません。読んでほしいのは情報量より、その場所で見つけた感じ方です。',
      '終わりごとのポストは公開の場ではなく、公開後に静かに読めるかたちに留めます。',
      '音声や現場での強い呼び込みではなく、気になった人が自分の速度で読める程度の熱量にします。'
    ],
    gate: [
      '登録後すぐに深い関係を求めず、どんな関わり方が合いそうかを選ぶ入口として扱います。',
      '実際に地域の人と会ってから、つなぎ手が登録した場合にだけ通常のログイン後ホームへ進みます。',
      'まだ入口に迷いがある段階では、ここが最初の入口ページです。'
    ],
    gate_public: [
      '入口カードは、地域との関わり方を強く説明するためではなく、最初に空気を感じるために置きます。',
      '表示する内容は短く保ち、関係や人脈の詳しい説明に寄りすぎないようにします。',
      '興味が湧いたときは、実際のカードや登録後の流れへ進める前提にします。',
      'ここで大きく説明しすぎないこと自体が、最初の一歩を上品に見せるための設計です。'
    ]
  };

  var navConfigs = {
    public_main: [
      { href: 'bridge.html', label: 'ブリッジ' },
      { href: '#top-footprints', label: 'ごひいきさんの足あと' },
      { href: 'about.html', label: 'わたしたちの目指すもの' },
      { href: 'safety.html', label: '安心と連絡' },
      { href: 'local.html', label: '地域の方へ' },
      { href: '../auth/login.html', label: 'ログイン' },
      { href: '../app/home.html', label: 'ログイン後' }
    ],
    public_secondary: [
      { href: 'index.html', label: 'トップ' },
      { href: 'about.html', label: 'わたしたちの目指すもの' },
      { href: 'index.html#top-footprints', label: 'ごひいきさんの足あと' },
      { href: '../auth/login.html', label: 'ログイン' }
    ],
    public_gate: [
      { href: 'index.html', label: 'トップ' },
      { href: 'about.html#feature-gate', label: 'ちいきの入り口とは' },
      { href: 'safety.html', label: '安心と連絡' },
      { href: '../auth/login.html', label: 'ログインへ' }
    ],
    auth_login: [
      { href: '../public/bridge.html', label: 'ブリッジ' },
      { href: '../public/index.html', label: '公開トップ' },
      { href: '../public/safety.html', label: '安心と連絡' }
    ],
    app_home: [
      { href: '../public/index.html', label: '公開トップ' },
      { href: '../public/index.html#top-footprints', label: '公開ごひいきさんの足あと' },
      { href: '../public/safety.html', label: '安心と連絡' }
    ]
  };

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function renderNav(root) {
    var key = root.getAttribute('data-site-nav');
    var links = navConfigs[key] || [];

    root.innerHTML = links.map(function (link) {
      return '<a href="' + escapeHtml(link.href) + '">' + escapeHtml(link.label) + '</a>';
    }).join('');
  }

  function getEntryButton(root) {
    var context = root.getAttribute('data-entry-context') || 'public';

    return { context: context, label: '詳しく見る', buttonClass: 'ghost' };
  }

  function buildEntryDetailHref(root, entryId, contextOverride) {
    var context = contextOverride || root.getAttribute('data-entry-context') || 'public';
    var basePath = context === 'app' ? '../public/' : '';

    // Java化では `entry id` を path variable または slug に置き換える想定。
    return basePath + 'gate-entry.html?entry=' + encodeURIComponent(entryId) + '&context=' + encodeURIComponent(context);
  }

  function renderEntryCards(root) {
    // 入口カード自体は共通だが、遷移先だけはページ文脈ごとに変える。
    // Java化では context をサーバー描画か API レスポンスで渡してもよい。
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
    // 運用ルール文言は複数ページで重複しやすいためここに集約している。
    // Java化では CMS 管理にするか、定数テーブル化する候補。
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
    // data-* をフックにしているので、Javaテンプレート化しても
    // HTML構造を大きく変えずに同じ初期化を流用できる。
    document.querySelectorAll('[data-site-nav]').forEach(renderNav);
    document.querySelectorAll('[data-entry-list]').forEach(renderEntryCards);
    document.querySelectorAll('[data-rule-list]').forEach(renderRuleList);
  }

  window.FURUTABI_SHARED_UI = {
    entryCards: entryCards,
    buildEntryDetailHref: buildEntryDetailHref,
    ruleLists: ruleLists,
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

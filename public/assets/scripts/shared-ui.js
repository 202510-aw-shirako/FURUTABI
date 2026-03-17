(function () {
  // Java移行時メモ:
  // このファイルは公開側 / ログイン後の共通UIを仮描画する層です。
  // 本実装では、header / footer / 導線リンクはサーバー側テンプレートや設定APIで出し分ける形も考えやすそうです。
  // bridge と ログイン後 はワイヤ用補助導線として置いているため、本番では utilityLinks から外す整理もしやすいです。
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
      '生活に近い場所は出しすぎないよう慎重に扱います。'
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

  var navConfigs = {
    public_main: {
      variant: 'public',
      brandHref: 'index.html',
      mainLinks: [
        { href: 'gate.html', label: 'ちいきの入り口', currentMatchers: ['gate.html', 'gate-entry.html'] },
        { href: '#top-footprints', label: 'ごひいきさんの足あと', currentMatchers: ['index.html', 'story.html'] },
        { href: 'about.html', label: '私たちの目指すもの', currentMatchers: ['about.html'] },
        { href: 'local.html', label: '地域の方へ', currentMatchers: ['local.html'] },
        { href: 'faq.html', label: 'FAQ', currentMatchers: ['faq.html'] },
        { href: 'safety.html', label: 'お問い合わせ', currentMatchers: ['safety.html', 'safety-complete.html'] }
      ],
      utilityLinks: [
        { href: 'bridge.html', label: 'ブリッジ' },
        { href: '../app/home.html', label: 'ログイン後' }
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
        { href: 'gate.html', label: 'ちいきの入り口', currentMatchers: ['gate.html', 'gate-entry.html'] },
        { href: 'index.html#top-footprints', label: 'ごひいきさんの足あと', currentMatchers: ['index.html', 'story.html'] },
        { href: 'about.html', label: '私たちの目指すもの', currentMatchers: ['about.html'] },
        { href: 'local.html', label: '地域の方へ', currentMatchers: ['local.html'] },
        { href: 'faq.html', label: 'FAQ', currentMatchers: ['faq.html'] },
        { href: 'safety.html', label: 'お問い合わせ', currentMatchers: ['safety.html', 'safety-complete.html'] }
      ],
      utilityLinks: [
        { href: 'bridge.html', label: 'ブリッジ' },
        { href: '../app/home.html', label: 'ログイン後' }
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
        { href: 'gate.html', label: 'ちいきの入り口', currentMatchers: ['gate.html', 'gate-entry.html'] },
        { href: 'index.html#top-footprints', label: 'ごひいきさんの足あと', currentMatchers: ['index.html', 'story.html'] },
        { href: 'about.html', label: '私たちの目指すもの', currentMatchers: ['about.html'] },
        { href: 'local.html', label: '地域の方へ', currentMatchers: ['local.html'] },
        { href: 'faq.html', label: 'FAQ', currentMatchers: ['faq.html'] },
        { href: 'safety.html', label: 'お問い合わせ', currentMatchers: ['safety.html', 'safety-complete.html'] }
      ],
      utilityLinks: [
        { href: 'bridge.html', label: 'ブリッジ' },
        { href: '../app/home.html', label: 'ログイン後' }
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
        { href: '../public/gate.html', label: 'ちいきの入り口', currentMatchers: ['gate.html', 'gate-entry.html'] },
        { href: '../public/index.html#top-footprints', label: 'ごひいきさんの足あと', currentMatchers: ['index.html', 'story.html'] },
        { href: '../public/about.html', label: '私たちの目指すもの', currentMatchers: ['about.html'] },
        { href: '../public/local.html', label: '地域の方へ', currentMatchers: ['local.html'] },
        { href: '../public/faq.html', label: 'FAQ', currentMatchers: ['faq.html'] },
        { href: '../public/safety.html', label: 'お問い合わせ', currentMatchers: ['safety.html', 'safety-complete.html'] }
      ],
      utilityLinks: [
        { href: '../public/bridge.html', label: 'ブリッジ' },
        { href: '../app/home.html', label: 'ログイン後' }
      ],
      action: [
        { href: '../auth/register.html', label: '新規登録', currentMatchers: ['register.html', 'register-profile.html', 'register-complete.html', 'register-verify.html'] },
        { href: '../auth/login.html', label: 'ログイン', currentMatchers: ['login.html'] }
      ]
    },
    app_home: {
      variant: 'app',
      brandHref: '../app/home.html',
      mainLinks: [
        { href: '../public/gate.html', label: 'ちいきの入り口', currentMatchers: ['gate.html', 'gate-entry.html'] },
        { href: '../public/index.html#top-footprints', label: 'ごひいきさんの足あと', currentMatchers: ['index.html', 'story.html'] },
        { href: '../app/home.html#my-map-panel', label: 'わたしの地図', currentMatchers: ['home.html'] },
        { href: '../public/okatte-entry.html?proposal=okatte-1', label: 'ちいきのおかって', currentMatchers: ['okatte-entry.html'] },
        { href: '../public/faq.html', label: 'FAQ', currentMatchers: ['faq.html'] },
        { href: '../public/safety.html', label: 'お問い合わせ', currentMatchers: ['safety.html', 'safety-complete.html'] }
      ],
      utilityLinks: [
        { href: '../public/bridge.html', label: 'ブリッジ' }
      ],
      action: [
        { href: '#', label: 'アカウント' }
      ]
    },
    app_local: {
      variant: 'app',
      brandHref: '../app/local-home.html',
      mainLinks: [
        { href: '../public/gate.html', label: 'ちいきの入り口', currentMatchers: ['gate.html', 'gate-entry.html'] },
        { href: '../public/index.html#top-footprints', label: 'ごひいきさんの足あと', currentMatchers: ['index.html', 'story.html'] },
        { href: '../app/home.html#my-map-panel', label: 'わたしの地図', currentMatchers: ['home.html'] },
        { href: '../public/okatte-entry.html?proposal=okatte-1', label: 'ちいきのおかって', currentMatchers: ['okatte-entry.html'] },
        { href: '../public/faq.html', label: 'FAQ', currentMatchers: ['faq.html'] },
        { href: '../public/safety.html', label: 'お問い合わせ', currentMatchers: ['safety.html', 'safety-complete.html'] }
      ],
      utilityLinks: [
        { href: '../public/bridge.html', label: 'ブリッジ' }
      ],
      action: [
        { href: '#', label: 'アカウント' }
      ]
    }
  };

  var footerConfig = {
    about: [
      { href: 'about.html', label: 'FURUTABI' },
      { href: 'about.html', label: '私たちの目指すもの' },
      { href: 'faq.html', label: 'FAQ' },
      { href: 'safety.html', label: 'お問い合わせ' }
    ],
    usage: [
      { href: 'gate.html', label: 'ちいきの入り口' },
      { href: 'index.html#top-footprints', label: 'ごひいきさんの足あと' },
      { href: 'about.html#feature-map', label: 'わたしの地図' },
      { href: 'okatte-entry.html?proposal=okatte-1', label: 'ちいきのおかって' }
    ],
    legal: [
      { href: 'terms.html', label: '利用規約' },
      { href: 'privacy.html', label: 'プライバシーポリシー' },
      { href: '../auth/login.html', label: 'ログイン' }
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

  function getCurrentPathname() {
    return window.location.pathname.split('/').pop() || 'index.html';
  }

  function getHrefPathname(href) {
    if (!href || href.charAt(0) === '#') {
      return getCurrentPathname();
    }
    return href.split('#')[0].split('?')[0].split('/').pop();
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
      var current = isCurrentLink(link);
      return '<a class="' + className + (current ? ' is-current' : '') + '" href="' + escapeHtml(link.href) + '"' + (current ? ' aria-current="page"' : '') + '>' + escapeHtml(link.label) + '</a>';
    }).join('');
  }

  function renderNav(root) {
    var key = root.getAttribute('data-site-nav');
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
    }

    root.classList.add('siteHeaderNav');
    root.innerHTML = '' +
      '<!-- 公開側ヘッダーとログイン後ヘッダーは分ける -->' +
        '<!-- ログイン後はロゴからホームへ戻れるため、主ナビにホームは置かない -->' +
      '<!-- 「詳しくは」は使わず「私たちの目指すもの」 -->' +
      '<!-- FAQ と お問い合わせ は独立ページ導線 -->' +
      '<!-- 公開側の「足あと」は短縮表示。正式名称は「ごひいきさんの足あと」 -->' +
      '<button class="siteHeaderToggle" type="button" aria-expanded="false" aria-label="メニューを開く">' +
        '<span></span><span></span><span></span>' +
      '</button>' +
      '<div class="siteHeaderMenu">' +
        '<div class="siteHeaderUtility">' + renderNavLinks(config.utilityLinks, 'siteHeaderUtilityLink') + '</div>' +
        '<div class="siteHeaderPrimary">' + renderNavLinks(config.mainLinks, 'siteHeaderLink') + '</div>' +
        '<div class="siteHeaderActionWrap">' + renderNavLinks(config.action, 'siteHeaderAction') + '</div>' +
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
  }

  function withBasePrefix(href) {
    var current = getCurrentPathname();
    var authOrAppPaths = ['home.html', 'local-home.html', 'login.html', 'register.html', 'register-profile.html', 'register-complete.html', 'register-verify.html'];

    if (href === '#') {
      return href;
    }

    if (authOrAppPaths.indexOf(current) !== -1 && href.indexOf('../') !== 0) {
      return '../public/' + href;
    }

    if (href === '../auth/login.html' && authOrAppPaths.indexOf(current) !== -1) {
      return './login.html';
    }

    if (href === '../auth/register.html' && authOrAppPaths.indexOf(current) !== -1) {
      return './register.html';
    }

    return href;
  }

  function renderFooterLinks(links) {
    return links.map(function (link) {
      return '<a href="' + escapeHtml(withBasePrefix(link.href)) + '">' + escapeHtml(link.label) + '</a>';
    }).join('');
  }

  function ensureFooter() {
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
            '<div class="siteFooterLinks">' + renderFooterLinks(footerConfig.about) + '</div>' +
          '</section>' +
          '<section class="siteFooterCol">' +
            '<h2>使い方</h2>' +
            '<div class="siteFooterLinks">' + renderFooterLinks(footerConfig.usage) + '</div>' +
          '</section>' +
          '<section class="siteFooterCol">' +
            '<h2>ご利用にあたって</h2>' +
            '<div class="siteFooterLinks">' + renderFooterLinks(footerConfig.legal) + '</div>' +
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

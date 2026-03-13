(function () {
  var ONE_MONTH_FALLBACK_DAYS = 31;

  // TODO: 本実装時は CMS / API から取得した公開中のお知らせに差し替える。
  var notices = [
    {
      title: '春の受け入れ日程を更新しました',
      summary: '入口カードの公開タイミングと、今月の案内枠の考え方をまとめています。',
      body: [
        '今月は、初めて地域に入る方に向けた入口カードの公開タイミングを少し調整しています。',
        '混雑を避けながら、静かに関われる入口を中心に並べる方針です。',
        '詳細な受け入れ状況は、つなぎ手の案内やログイン後の画面で順次ご確認ください。'
      ],
      category: '運用',
      publishedAt: '2026-03-10T09:00:00+09:00',
      status: 'published',
      slug: 'spring-acceptance-update',
      topicsEnabled: true,
      topicsStartAt: '2026-03-10T09:00:00+09:00',
      topicsEndAt: '',
      createdAt: '2026-03-08T10:00:00+09:00',
      updatedAt: '2026-03-10T09:00:00+09:00'
    },
    {
      title: 'ごひいきさん登録まわりの案内を整えました',
      summary: '登録前に確認したい流れを、公開ページ側でも見つけやすくしています。',
      body: [
        'ごひいきさん登録に進む前に、サイト内で確認してほしい導線を見直しました。',
        'はじめての方でも、登録前に「この場所がどういう空気を守ろうとしているか」を追いやすくしています。',
        '具体的な登録導線は、ログインや案内ページからご確認ください。'
      ],
      category: '案内',
      publishedAt: '2026-03-07T12:00:00+09:00',
      status: 'published',
      slug: 'guide-refresh',
      topicsEnabled: true,
      topicsStartAt: '',
      topicsEndAt: '',
      createdAt: '2026-03-06T15:00:00+09:00',
      updatedAt: '2026-03-07T12:00:00+09:00'
    },
    {
      title: '町はずれの散歩導線を見直しています',
      summary: '季節の変化に合わせて、案内する順路と紹介文の調整を進めています。',
      body: [
        '町はずれの小さな川沿いを歩く導線について、季節に合わせた紹介文の更新を進めています。',
        '静かに過ごしたい方が選びやすいよう、所要時間や雰囲気タグの見せ方も再確認しています。',
        '細かな案内は今後も見直しながら整えていきます。'
      ],
      category: '更新',
      publishedAt: '2026-02-26T10:00:00+09:00',
      status: 'published',
      slug: 'walkway-adjustment',
      topicsEnabled: true,
      topicsStartAt: '2026-02-26T10:00:00+09:00',
      topicsEndAt: '2026-03-31T23:59:59+09:00',
      createdAt: '2026-02-20T10:00:00+09:00',
      updatedAt: '2026-02-26T10:00:00+09:00'
    },
    {
      title: '非公開の相談導線を準備しています',
      summary: '困りごとや改善提案を、公開の空気を壊さず届ける設計を進めています。',
      body: [
        '公開の場ではなく、非公開で相談や改善提案を届けられる導線の整理を進めています。',
        '公開ページ側では、空気を守ることを優先しつつ、必要な連絡先に迷わずたどり着ける構成を目指しています。',
        '運用方針は今後も調整の可能性があります。'
      ],
      category: '準備中',
      publishedAt: '2026-02-18T10:30:00+09:00',
      status: 'published',
      slug: 'private-contact-flow',
      topicsEnabled: true,
      topicsStartAt: '',
      topicsEndAt: '',
      createdAt: '2026-02-17T11:00:00+09:00',
      updatedAt: '2026-02-18T10:30:00+09:00'
    },
    {
      title: '地域の方へ向けた説明ページを見直しました',
      summary: '受け入れ側の考え方を共有する説明を、読み返しやすく整えています。',
      body: [
        '地域の方へ向けた説明ページの構成を見直しました。',
        'どんな方にどんな空気で関わってほしいかが、読み返しやすい形になるよう調整しています。',
        '今後も状況に応じて言葉を磨いていきます。'
      ],
      category: '更新',
      publishedAt: '2026-01-25T09:00:00+09:00',
      status: 'published',
      slug: 'local-page-refresh',
      topicsEnabled: true,
      topicsStartAt: '',
      topicsEndAt: '',
      createdAt: '2026-01-23T10:00:00+09:00',
      updatedAt: '2026-01-25T09:00:00+09:00'
    },
    {
      title: '準備中のお知らせサンプル',
      summary: '公開前のためトピックスには出ません。',
      body: ['これは下書き状態のサンプルです。'],
      category: '下書き',
      publishedAt: '2026-03-12T08:00:00+09:00',
      status: 'draft',
      slug: 'draft-sample',
      topicsEnabled: true,
      topicsStartAt: '',
      topicsEndAt: '',
      createdAt: '2026-03-12T08:00:00+09:00',
      updatedAt: '2026-03-12T08:00:00+09:00'
    }
  ];

  function parseDate(value) {
    var date;
    if (!value) {
      return null;
    }
    date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return null;
    }
    return date;
  }

  function addOneMonth(date) {
    var next;
    if (!date) {
      return null;
    }
    next = new Date(date.getTime());
    next.setMonth(next.getMonth() + 1);
    if (Number.isNaN(next.getTime())) {
      return new Date(date.getTime() + ONE_MONTH_FALLBACK_DAYS * 24 * 60 * 60 * 1000);
    }
    return next;
  }

  function formatDateLabel(value) {
    var date = parseDate(value);
    if (!date) {
      return '';
    }
    return [
      String(date.getFullYear()),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0')
    ].join('.');
  }

  function isPublished(notice) {
    return notice && notice.status === 'published';
  }

  function getEffectiveTopicsEnd(notice) {
    var start = parseDate(notice.topicsStartAt);
    var publishedAt = parseDate(notice.publishedAt);
    var explicitEnd = parseDate(notice.topicsEndAt);

    if (explicitEnd) {
      return explicitEnd;
    }

    // topicsEndAt 未設定時は topicsStartAt -> publishedAt の順に1か月後を採用する。
    return addOneMonth(start || publishedAt);
  }

  function isTopicsVisible(notice, now) {
    var current = now || new Date();
    var start = parseDate(notice.topicsStartAt);
    var end;

    if (!isPublished(notice) || !notice.topicsEnabled) {
      return false;
    }

    if (start && current < start) {
      return false;
    }

    end = getEffectiveTopicsEnd(notice);
    if (end && current > end) {
      return false;
    }

    return true;
  }

  function getSortedPublishedNotices() {
    return notices
      .filter(isPublished)
      .slice()
      .sort(function (a, b) {
        var aPriority = typeof a.priority === 'number' ? a.priority : 0;
        var bPriority = typeof b.priority === 'number' ? b.priority : 0;
        var aTime = parseDate(a.publishedAt);
        var bTime = parseDate(b.publishedAt);

        if (aPriority !== bPriority) {
          return bPriority - aPriority;
        }

        return (bTime ? bTime.getTime() : 0) - (aTime ? aTime.getTime() : 0);
      });
  }

  function getTopicsNotices(options) {
    var current = options && options.now ? options.now : new Date();
    var max = options && options.max ? options.max : 5;

    return getSortedPublishedNotices()
      .filter(function (notice) {
        return isTopicsVisible(notice, current);
      })
      .slice(0, max);
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function buildNoticeHref(basePath, slug) {
    return (basePath || '') + 'notice.html?slug=' + encodeURIComponent(slug);
  }

  function buildNoticeListHref(basePath) {
    return (basePath || '') + 'notices.html';
  }

  function renderTopics(root) {
    var basePath = root.getAttribute('data-notice-base') || '';
    var max = Number(root.getAttribute('data-notice-max') || '5');
    var items = getTopicsNotices({ max: max });
    var html;

    if (!items.length) {
      root.closest('[data-notice-topics]').hidden = true;
      return;
    }

    html = items.map(function (notice) {
      var meta = notice.category || formatDateLabel(notice.publishedAt);
      return '' +
        '<a class="topicItemLink" href="' + escapeHtml(buildNoticeHref(basePath, notice.slug)) + '">' +
          '<span class="topicLine">' +
            (meta ? '<span class="topicMetaText">' + escapeHtml(meta) + '</span>' : '') +
            '<span class="topicTitle">' + escapeHtml(notice.title) + '</span>' +
          '</span>' +
          '<span class="topicCta" aria-hidden="true">›</span>' +
        '</a>';
    }).join('');

    root.innerHTML = html;
  }

  function renderNoticeList(root) {
    var basePath = root.getAttribute('data-notice-base') || '';
    var items = getSortedPublishedNotices();

    root.innerHTML = items.map(function (notice) {
      return '' +
        '<article class="noticeListItem">' +
          '<a class="noticeListLink" href="' + escapeHtml(buildNoticeHref(basePath, notice.slug)) + '">' +
            '<div class="noticeListMeta">' +
              '<span class="pill">' + escapeHtml(notice.category || 'お知らせ') + '</span>' +
              '<span class="noticeListDate">' + escapeHtml(formatDateLabel(notice.publishedAt)) + '</span>' +
            '</div>' +
            '<h2 class="noticeListTitle">' + escapeHtml(notice.title) + '</h2>' +
            '<p class="noticeListSummary">' + escapeHtml(notice.summary || '') + '</p>' +
            '<span class="noticeListCta">詳しく見る</span>' +
          '</a>' +
        '</article>';
    }).join('');
  }

  function renderNoticeDetail(root) {
    var params = new URLSearchParams(window.location.search);
    var slug = params.get('slug');
    var notice = getSortedPublishedNotices().find(function (item) {
      return item.slug === slug;
    }) || getSortedPublishedNotices()[0];
    var bodyHtml;
    var relatedBase = root.getAttribute('data-notice-base') || '';

    if (!notice) {
      root.innerHTML = '<p class="lead">お知らせが見つかりませんでした。</p>';
      return;
    }

    // TODO: 将来は slug または id ごとに CMS / API の詳細データへ差し替える。
    bodyHtml = (notice.body || []).map(function (paragraph) {
      return '<p>' + escapeHtml(paragraph) + '</p>';
    }).join('');

    document.title = 'FURUTABI Wire | ' + notice.title;

    root.innerHTML = '' +
      '<section class="section fv">' +
        '<div class="noticeDetailHead">' +
          '<div class="noticeDetailMeta">' +
            '<span class="pill">' + escapeHtml(notice.category || 'お知らせ') + '</span>' +
            '<span class="noticeDetailDate">' + escapeHtml(formatDateLabel(notice.publishedAt)) + '</span>' +
          '</div>' +
          '<h1 class="h1 noticeDetailTitle">' + escapeHtml(notice.title) + '</h1>' +
          '<p class="lead noticeDetailLead">' + escapeHtml(notice.summary || '') + '</p>' +
        '</div>' +
      '</section>' +
      '<section class="section">' +
        '<div class="noticeArticle card">' + bodyHtml + '</div>' +
      '</section>' +
      '<section class="section">' +
        '<div class="denseBox">' +
          '<div>' +
            '<div class="h2">お知らせ一覧へ戻る</div>' +
            '<p class="note">ほかの更新もこちらから確認できます。</p>' +
          '</div>' +
          '<a class="btn ghost" href="' + escapeHtml(buildNoticeListHref(relatedBase)) + '">一覧を見る</a>' +
        '</div>' +
      '</section>';
  }

  function init() {
    document.querySelectorAll('[data-notice-topics-items]').forEach(renderTopics);
    document.querySelectorAll('[data-notice-list-items]').forEach(renderNoticeList);
    document.querySelectorAll('[data-notice-detail-root]').forEach(renderNoticeDetail);
  }

  window.FurutabiNotices = {
    notices: notices,
    parseDate: parseDate,
    getEffectiveTopicsEnd: getEffectiveTopicsEnd,
    isTopicsVisible: isTopicsVisible,
    getTopicsNotices: getTopicsNotices,
    getSortedPublishedNotices: getSortedPublishedNotices,
    buildNoticeHref: buildNoticeHref,
    buildNoticeListHref: buildNoticeListHref,
    formatDateLabel: formatDateLabel
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

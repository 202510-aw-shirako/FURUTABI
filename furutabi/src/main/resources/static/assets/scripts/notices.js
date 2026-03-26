(function () {
  // Java移行時メモ:
  // notices は仮データです。将来は notices テーブルや CMS API レスポンスへ置き換える形が考えやすそうです。
  // topicsEnabled / topicsStartAt / topicsEndAt / status は、将来の保持項目としてそのまま寄せやすい目です。
  var ONE_MONTH_FALLBACK_DAYS = 31;
  var shared = window.FURUTABI_SHARED_UI;

  // Wire段階のお知らせ共通データ。
  // Java化では notices テーブルや CMS 由来の API レスポンスに置き換える。
  var notices = [
    {
      title: '春の入口公開タイミングを更新しました',
      summary: '入口カードの公開日時と、初回案内の流れをまとめています。',
      body: [
        '春に向けて、地域の人と最初に会う導線を少しだけ整えました。',
        '公開トップで見えるトピックス欄からも、静かに確認できるようにしています。',
        '詳しい入口の扱いや案内の考え方は、ログイン後のページでも追える構成です。'
      ],
      category: '案内',
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
      title: 'ごひいきさん足あとまわりの導線を整えました',
      summary: '足あと記事へ直接進める構成と、記事下の回遊導線を見直しています。',
      body: [
        'ごひいきさんの足あとは、一覧を挟まずに詳細へ進めるようにしました。',
        '記事ページの下にも、地図と別の記事への入口を置いています。',
        '必要以上に強い説明を増やさず、自然に回遊できるよう整えています。'
      ],
      category: '更新',
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
      title: '歩きやすさの調整予定を反映しています',
      summary: '町歩きの見え方にあわせて、表示上の説明や余白を見直しています。',
      body: [
        '歩きやすい見え方になるよう、案内の密度や余白の調整を進めています。',
        '過剰な説明を避けつつ、必要な情報だけ拾える状態を目指しています。',
        '大きな変更は段階的に行い、更新内容はこのお知らせで追えるようにします。'
      ],
      category: '予定',
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
      title: '確認中の下書きサンプル',
      summary: '公開前のためトピックスには出しません。',
      body: ['これは運用確認用のサンプルです。'],
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

    // Java側でも同じ判定を再利用できるよう、ここでは純粋な日付関数に寄せている。
    // topicsEndAt 未設定時は topicsStartAt -> publishedAt の順で1か月後を使う。
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
    // トピックスは「一覧データの一部を切り出す」だけに留める。
    // 将来は API 側で max 件に絞って返してもよい。
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

  function linksEnabled(root) {
    var explicit = root.getAttribute('data-notice-link-enabled');
    if (explicit === 'true') {
      return true;
    }
    if (explicit === 'false') {
      return false;
    }
    return !window.location.pathname.replace(/\\/g, '/').startsWith('/app/');
  }

  function renderTopics(root) {
    // トップ / ログイン後ホームは同じトピックス描画を使う。
    // 差分は basePath と max 件数だけ data-* で渡す。
    var basePath = root.getAttribute('data-notice-base') || '';
    var max = Number(root.getAttribute('data-notice-max') || '5');
    var items = getTopicsNotices({ max: max });
    var section = root.closest('[data-notice-topics]');
    var linkable = linksEnabled(root);

    if (!items.length) {
      if (section) {
        section.hidden = true;
      }
      return;
    }

    root.innerHTML = items.map(function (notice) {
      var meta = notice.category || formatDateLabel(notice.publishedAt);
      if (!linkable) {
        return '' +
          '<div class="topicItemLink is-disabled" aria-disabled="true">' +
            '<span class="topicLine">' +
              (meta ? '<span class="topicMetaText">' + escapeHtml(meta) + '</span>' : '') +
              '<span class="topicTitle">' + escapeHtml(notice.title) + '</span>' +
            '</span>' +
            '<span class="topicCta" aria-hidden="true">次段階</span>' +
          '</div>';
      }
      return '' +
        '<a class="topicItemLink" href="' + escapeHtml(buildNoticeHref(basePath, notice.slug)) + '">' +
          '<span class="topicLine">' +
            (meta ? '<span class="topicMetaText">' + escapeHtml(meta) + '</span>' : '') +
            '<span class="topicTitle">' + escapeHtml(notice.title) + '</span>' +
          '</span>' +
          '<span class="topicCta" aria-hidden="true">→</span>' +
        '</a>';
    }).join('');
  }

  function renderNoticeList(root) {
    // 一覧ページは preview 用の項目だけ描画する。
    // Java化ではこの関数を残し、データ取得だけ API に差し替えやすい。
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
    var slug = shared ? shared.readQueryParam('slug') : new URLSearchParams(window.location.search).get('slug');
    var sorted = getSortedPublishedNotices();
    var notice = sorted.find(function (item) {
      return item.slug === slug;
    }) || sorted[0];
    var relatedBase = root.getAttribute('data-notice-base') || '';

    if (!notice) {
      root.innerHTML = '<p class="lead">お知らせが見つかりませんでした。</p>';
      return;
    }

    // TODO: 本実装時は slug または id をもとに CMS / API の詳細データへ置き換える
    // いまは単一テンプレート + query で詳細切り替えの設計だけ先に固めている。
    if (shared) {
      shared.setPageTitle('FURUTABI Wire | ', notice.title);
    } else {
      document.title = 'FURUTABI Wire | ' + notice.title;
    }

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
        '<div class="noticeArticle card" data-notice-body></div>' +
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

    if (shared) {
      shared.renderParagraphs(root.querySelector('[data-notice-body]'), notice.body || []);
    }
  }

  function init() {
    // 画面側は data-* を置くだけで一覧 / 詳細 / トピックスを差し込める。
    // Javaテンプレート化してもこの接続点を維持しやすい。
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

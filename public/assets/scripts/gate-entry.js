(function () {
  var shared = window.FURUTABI_SHARED_UI;
  var mapImage = './assets/images/凪咲町.svg';

  if (!shared) {
    return;
  }

  // 一覧用データは shared-ui.js、詳細用データはここに分離している。
  // Java化では一覧 API と詳細 API を分けやすいよう、この構造を保つ想定。
  var entryDetails = {
    'gate-1': {
      intro: '店先に立つと、暮らしの温度がいちばん先に伝わってくる。そんな入口です。',
      connector: '商店のつなぎ手',
      connectorNote: '長く町で暮らしてきた人の視点から、まずは店先で町の呼吸を受け取ります。',
      fitFor: [
        'まずは人の空気を知ってから動きたい人',
        '短い会話から地域の輪郭をつかみたい人',
        '静かな入口から町と関わりたい人'
      ],
      flow: [
        '店先でつなぎ手と挨拶を交わす',
        'その日の話題や近況から、無理のない会話を始める',
        '合いそうなら次に向かう人や場所の入口を教えてもらう'
      ],
      body: [
        '観光案内のように情報をまとめて渡すのではなく、ここではまず町で生きてきた人の話し方に触れます。',
        '少し立ち止まり、店先で声を交わすだけでも、その地域がどんな速度で流れているのかが見えてきます。',
        '最初の入口として負荷が低く、それでいて関わり方の相性も見えやすい導線です。'
      ],
      placeName: '商店街の店先',
      pinClass: 'gateEntryPin--a'
    },
    'gate-2': {
      intro: '長く引き留めず、短い会話だけで町との距離をたしかめる入口です。',
      connector: '路地を知るつなぎ手',
      connectorNote: '路地の速度や雰囲気を知る人に、まずひとこと教わる入口です。',
      fitFor: [
        '長い会話にまだ自信がない人',
        '一人旅で静かに入口を探したい人',
        'まず相手の話し方や距離感を見たい人'
      ],
      flow: [
        '決められた時間に短く合流する',
        '路地や周辺の話を少し交わす',
        '続けたい場合だけ次の入口候補を受け取る'
      ],
      body: [
        '路地の途中で交わすひとことには、その町がどんな速度で動いているかがにじみます。',
        'この入口では、深く入り込む前に、まずは軽く声を交わせることを大切にしています。',
        '無理なく終えられるので、最初の一歩として選びやすい入口です。'
      ],
      placeName: '古い路地の角',
      pinClass: 'gateEntryPin--b'
    },
    'gate-3': {
      intro: '場所だけでなく、人や季節の流れも含めて地域を理解したい人向けの入口です。',
      connector: '地域の案内役',
      connectorNote: '町の流れや空気を俯瞰して見られる人に、最初の輪郭を教わります。',
      fitFor: [
        '関係の背景も含めて理解したい人',
        '次の入口や提案につながる土台を作りたい人',
        '少し長めに話しても負担が少ない人'
      ],
      flow: [
        '落ち着いて話せる場所で会う',
        '地域の流れや今の様子を教わる',
        '次に合う人や場所があれば、無理のない範囲でつないでもらう'
      ],
      body: [
        '町には、地図に載る場所とは別に、季節ごとの動きや人のつながりがあります。',
        'この入口では、それを少しだけ言葉にしてもらい、旅人が地域をどう受け取るかを整えます。',
        '時間は少し長めですが、そのぶん次の関わり方まで見えやすくなります。'
      ],
      placeName: '案内役のいる休憩所',
      pinClass: 'gateEntryPin--c'
    },
    'gate-4': {
      intro: '景色や居心地の良い場所をきっかけに、今の地域の気配を聞いていく入口です。',
      connector: '場所に詳しいつなぎ手',
      connectorNote: 'その場所を好きな理由から、町のいまを教えてくれるつなぎ手です。',
      fitFor: [
        '場所から会話を始めたい人',
        '景色や居心地の良さを大切にしたい人',
        '少しだけ深い話にも入ってみたい人'
      ],
      flow: [
        '好きな場所の近くで合流する',
        'その場所の最近の話を聞く',
        '興味が続けば次に会う人や場所の候補を相談する'
      ],
      body: [
        '目の前の景色や居心地の良さをきっかけにすると、地域の話は少し自然に始まります。',
        'この入口では、最近の変化や、その場所にいる人たちの気配を静かに聞き取っていきます。',
        '深すぎず浅すぎず、相性を測るにはちょうどよい入口です。'
      ],
      placeName: '海沿いの高台',
      pinClass: 'gateEntryPin--d'
    },
    'gate-5': {
      intro: '説明を聞くよりも、歩きながら町の気配を身体で受け取りたい人向けの入口です。',
      connector: '歩きのつなぎ手',
      connectorNote: '歩く速度や目線を少し貸してくれる、静かな案内役です。',
      fitFor: [
        '歩きながら雰囲気をつかみたい人',
        '強い説明よりも空気を感じたい人',
        '地域の静かな面に惹かれる人'
      ],
      flow: [
        '歩きやすい場所でつなぎ手と合流する',
        '会話しすぎず、町の気配を一緒に歩く',
        '途中で気になった場所や次の入口を相談する'
      ],
      body: [
        '町の気配は、立ち止まって説明されるより、少し歩きながら受け取る方が自然なことがあります。',
        'この入口では、歩く速度や視線を共有しながら、旅人が町の静かな部分に触れられるようにします。',
        '効率よく回るためではなく、町の手触りを受け取るための入口です。'
      ],
      placeName: '朝の商店街',
      pinClass: 'gateEntryPin--e'
    }
  };

  function getCurrentEntryId() {
    var entryId = shared.readQueryParam('entry') || 'gate-1';
    return entryDetails[entryId] ? entryId : 'gate-1';
  }

  function getContext() {
    return shared.readQueryParam('context') || 'public';
  }

  function buildDetailHref(entryId) {
    var root = document.querySelector('[data-gate-entry-root]');
    return shared.buildEntryDetailHref(root || document.body, entryId, getContext());
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function getEntryAction(context) {
    if (context === 'gate') {
      return {
        href: '../app/home.html',
        label: 'この入口から進む',
        buttonClass: 'primary'
      };
    }
    if (context === 'app') {
      return {
        href: '../app/home.html',
        label: 'ログイン後ホームへ戻る',
        buttonClass: 'ghost'
      };
    }
    return {
      href: '../auth/login.html',
      label: '登録 / ログインして入口を選ぶ',
      buttonClass: 'primary'
    };
  }

  function mergeEntryData(entryId) {
    var base = shared.entryCards.find(function (card) {
      return card.id === entryId;
    }) || shared.entryCards[0];
    var detail = entryDetails[entryId] || entryDetails['gate-1'];
    var merged = {};
    Object.keys(base).forEach(function (key) {
      merged[key] = base[key];
    });
    Object.keys(detail).forEach(function (key) {
      merged[key] = detail[key];
    });
    return merged;
  }

  function buildRelatedCard(entry, isActive) {
    return '' +
      '<article class="card gateEntryRelatedCard' + (isActive ? ' is-active' : '') + '" data-gate-entry-card data-gate-entry-id="' + escapeHtml(entry.id) + '">' +
        '<div class="gateEntryRelatedHead">' +
          '<span class="pill">' + escapeHtml(entry.duration) + '</span>' +
          '<span class="small">' + escapeHtml(entry.placeName) + '</span>' +
        '</div>' +
        '<div class="h2">' + escapeHtml(entry.title) + '</div>' +
        '<p class="note">' + escapeHtml(entry.intro) + '</p>' +
        '<a class="btn ghost" href="' + escapeHtml(buildDetailHref(entry.id)) + '">この入口を見る</a>' +
      '</article>';
  }

  function renderRelatedCards(cardsRoot, activeId) {
    var ordered = shared.entryCards.slice();
    var currentIndex = ordered.findIndex(function (item) {
      return item.id === activeId;
    });

    if (currentIndex > 0) {
      ordered = ordered.slice(currentIndex).concat(ordered.slice(0, currentIndex));
    }

    cardsRoot.innerHTML = ordered.map(function (item) {
      return buildRelatedCard(mergeEntryData(item.id), item.id === activeId);
    }).join('');
  }

  function createMapData() {
    return shared.entryCards.reduce(function (acc, item) {
      var entry = mergeEntryData(item.id);
      acc[item.id] = {
        id: item.id,
        title: entry.title,
        intro: entry.intro,
        duration: entry.duration,
        placeName: entry.placeName,
        link: buildDetailHref(item.id),
        pinClass: entry.pinClass
      };
      return acc;
    }, {});
  }

  function initEntryPreviewMap(root, entryMapData, options) {
    // 足あと地図とは似た操作だが、入口詳細専用の初期化関数。
    // 将来は共通 map controller に寄せるか、API からピン座標を受け取る想定。
    var pins = root ? Array.prototype.slice.call(root.querySelectorAll('[data-gate-entry-pin]')) : [];
    var stage = root ? root.querySelector('[data-gate-entry-stage]') : null;
    var viewport = root ? root.querySelector('[data-gate-entry-viewport]') : null;
    var pinsLayer = root ? root.querySelector('[data-gate-entry-pins-layer]') : null;
    var zoomIn = root ? root.querySelector('[data-gate-entry-zoom-in]') : null;
    var zoomOut = root ? root.querySelector('[data-gate-entry-zoom-out]') : null;
    var selectedId = options.initialEntryId;
    var zoom = 1;
    var panX = 0;
    var panY = 0;
    var dragging = false;
    var dragged = false;
    var dragStartX = 0;
    var dragStartY = 0;
    var panStartX = 0;
    var panStartY = 0;
    var pointerId = null;

    if (!root || !stage || !viewport || !pinsLayer) {
      return null;
    }

    function syncPins() {
      pins.forEach(function (pin) {
        var id = pin.getAttribute('data-gate-entry-pin');
        var item = entryMapData[id];
        pin.classList.toggle('is-active', id === selectedId);
        if (item) {
          pin.setAttribute('href', item.link);
        }
      });
    }

    function syncZoom() {
      viewport.style.transform = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + zoom + ')';
      if (zoomIn) zoomIn.disabled = zoom >= 1.8;
      if (zoomOut) zoomOut.disabled = zoom <= 1;
      stage.classList.toggle('is-draggable', zoom > 1);
    }

    function getClientPoint(event) {
      if (event.touches && event.touches.length) {
        return { x: event.touches[0].clientX, y: event.touches[0].clientY };
      }
      if (event.changedTouches && event.changedTouches.length) {
        return { x: event.changedTouches[0].clientX, y: event.changedTouches[0].clientY };
      }
      return { x: event.clientX, y: event.clientY };
    }

    function startDrag(event) {
      var point = getClientPoint(event);
      if (zoom <= 1) {
        return;
      }
      event.preventDefault();
      dragging = true;
      pointerId = typeof event.pointerId === 'number' ? event.pointerId : null;
      dragStartX = point.x;
      dragStartY = point.y;
      panStartX = panX;
      panStartY = panY;
      dragged = false;
      stage.classList.add('is-dragging');
    }

    function moveDrag(event) {
      var point;
      if (!dragging) {
        return;
      }
      if (pointerId !== null && typeof event.pointerId === 'number' && event.pointerId !== pointerId) {
        return;
      }
      point = getClientPoint(event);
      if (Math.abs(point.x - dragStartX) > 3 || Math.abs(point.y - dragStartY) > 3) {
        dragged = true;
      }
      panX = panStartX + (point.x - dragStartX);
      panY = panStartY + (point.y - dragStartY);
      syncZoom();
    }

    function endDrag() {
      if (!dragging) {
        return;
      }
      dragging = false;
      pointerId = null;
      stage.classList.remove('is-dragging');
    }

    function updateZoom(nextZoom) {
      zoom = Math.max(1, Math.min(1.8, nextZoom));
      if (zoom === 1) {
        panX = 0;
        panY = 0;
      }
      syncZoom();
    }

    pins.forEach(function (pin) {
      pin.addEventListener('click', function (event) {
        var id = pin.getAttribute('data-gate-entry-pin');
        event.preventDefault();
        if (dragged) {
          dragged = false;
          return;
        }
        selectedId = id;
        syncPins();
        if (typeof options.onSelect === 'function') {
          options.onSelect(id);
        }
      });
    });

    stage.addEventListener('pointerdown', function (event) {
      var pinTarget = event.target.closest('[data-gate-entry-pin]');
      if (
        event.target !== stage &&
        event.target !== pinsLayer &&
        !event.target.classList.contains('topMapCanvasImage') &&
        !pinTarget
      ) {
        return;
      }
      startDrag(event);
    });

    stage.addEventListener('wheel', function (event) {
      var delta = event.deltaY < 0 ? 0.1 : -0.1;
      event.preventDefault();
      updateZoom(zoom + delta);
    }, { passive: false });

    if (zoomIn) {
      zoomIn.addEventListener('click', function () {
        updateZoom(zoom + 0.1);
      });
    }

    if (zoomOut) {
      zoomOut.addEventListener('click', function () {
        updateZoom(zoom - 0.1);
      });
    }

    document.addEventListener('pointermove', moveDrag);
    document.addEventListener('pointerup', endDrag);
    document.addEventListener('pointercancel', endDrag);

    syncPins();
    syncZoom();

    return {
      select: function (id) {
        selectedId = id;
        syncPins();
      }
    };
  }

  function renderPage(root) {
    var entryId = getCurrentEntryId();
    var context = getContext();
    var entry = mergeEntryData(entryId);
    var action = getEntryAction(context);
    var entryMapData = createMapData();

    shared.setPageTitle('FURUTABI Wire | ', entry.title);

    root.innerHTML = '' +
      '<section class="section fv gateEntryLeadSection">' +
        '<div class="gateEntryHero">' +
          '<div class="gateEntryHeroCopy">' +
            '<span class="pill">ちいきの入り口</span>' +
            '<h1 class="h1">' + escapeHtml(entry.title) + '</h1>' +
            '<p class="lead">' + escapeHtml(entry.intro) + '</p>' +
          '</div>' +
          '<aside class="card gateEntryConnectorCard">' +
            '<div class="h2">つなぎ手イメージ</div>' +
            '<div class="ph ph--avatar gateEntryConnectorVisual" aria-hidden="true"></div>' +
            '<p class="note"><strong>' + escapeHtml(entry.connector) + '</strong><br>' + escapeHtml(entry.connectorNote) + '</p>' +
          '</aside>' +
        '</div>' +
      '</section>' +
      '<section class="section">' +
        '<div class="grid2 gateEntryBodyGrid">' +
          '<article class="card noticeArticle" data-gate-entry-body></article>' +
          '<div class="gateEntrySideStack">' +
            '<section class="card">' +
              '<div class="h2">合う人</div>' +
              '<ul class="aboutFeatureList" data-gate-entry-fit></ul>' +
            '</section>' +
            '<section class="card">' +
              '<div class="h2">最初の流れ</div>' +
              '<ul class="aboutFeatureList" data-gate-entry-flow></ul>' +
            '</section>' +
          '</div>' +
        '</div>' +
      '</section>' +
      '<section class="section">' +
        '<div class="card">' +
          '<div class="h2">運用ルール</div>' +
          '<ul class="aboutFeatureList" data-rule-list="gate"></ul>' +
        '</div>' +
      '</section>' +
      '<section class="section gateEntryMapSection">' +
        '<div class="sectionTitleRow">' +
          '<h2 class="h2">ちいきの入り口</h2>' +
          '<span class="small">どこで入口を頼むかが分かるようにしています。</span>' +
        '</div>' +
        '<div class="topMapLayout appTopMapLayout gateEntryMapLayout" data-gate-entry-map>' +
          '<div class="myMapMain">' +
            '<div class="topMapStage" aria-label="入口の場所がわかる地図" data-gate-entry-stage>' +
              '<div class="myMapViewport" data-gate-entry-viewport>' +
                '<img class="topMapCanvas topMapCanvasImage" src="' + mapImage + '" alt="入口の場所がわかる地図" />' +
                '<div class="myMapPinsLayer" aria-hidden="false" data-gate-entry-pins-layer>' +
                  Object.keys(entryMapData).map(function (id, index) {
                    var item = entryMapData[id];
                    return '<a class="topMapPin ' + escapeHtml(item.pinClass) + (id === entryId ? ' is-active' : '') + '" href="' + escapeHtml(item.link) + '" data-gate-entry-pin="' + escapeHtml(id) + '" aria-label="' + escapeHtml(item.placeName) + '"><span>' + String(index + 1) + '</span></a>';
                  }).join('') +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>' +
          '<div class="myMapZoomControls" aria-label="地図の拡大縮小">' +
            '<button class="btn ghost myMapZoomBtn" type="button" data-gate-entry-zoom-in>＋</button>' +
            '<button class="btn ghost myMapZoomBtn" type="button" data-gate-entry-zoom-out>−</button>' +
          '</div>' +
        '</div>' +
        '<div class="entryCarousel gateEntryRelatedCarousel" data-gate-entry-carousel>' +
          '<div class="entryCarouselStage">' +
            '<button class="btn ghost entryCarouselArrow entryCarouselArrow--prev" type="button" data-gate-entry-prev aria-label="前の入口"></button>' +
            '<div class="entryCarouselViewport">' +
              '<div class="entryCarouselTrack gateEntryRelatedTrack" data-gate-entry-cards></div>' +
            '</div>' +
            '<button class="btn ghost entryCarouselArrow entryCarouselArrow--next" type="button" data-gate-entry-next aria-label="次の入口"></button>' +
          '</div>' +
        '</div>' +
      '</section>' +
      '<section class="section">' +
        '<div class="denseBox">' +
          '<div>' +
            '<div class="h2">次へ進む</div>' +
            '<p class="note">ワイヤでは、詳細ページの最後に次の導線だけを静かに置いています。</p>' +
          '</div>' +
          '<a class="btn ' + escapeHtml(action.buttonClass) + '" href="' + escapeHtml(action.href) + '">' + escapeHtml(action.label) + '</a>' +
        '</div>' +
      '</section>';

    shared.renderParagraphs(root.querySelector('[data-gate-entry-body]'), entry.body);
    root.querySelectorAll('[data-rule-list]').forEach(function (node) {
      node.innerHTML = shared.ruleLists.gate.map(function (item) {
        return '<li>・' + item + '</li>';
      }).join('');
    });
    root.querySelector('[data-gate-entry-fit]').innerHTML = entry.fitFor.map(function (item) {
      return '<li>・' + item + '</li>';
    }).join('');
    root.querySelector('[data-gate-entry-flow]').innerHTML = entry.flow.map(function (item) {
      return '<li>・' + item + '</li>';
    }).join('');

    renderRelatedCards(root.querySelector('[data-gate-entry-cards]'), entryId);
    var carouselController = initRelatedCarousel(root, entryId, function (id) {
      renderRelatedCards(root.querySelector('[data-gate-entry-cards]'), id);
    });
    var mapController = initEntryPreviewMap(root.querySelector('[data-gate-entry-map]'), entryMapData, {
      initialEntryId: entryId,
      onSelect: function (id) {
        renderRelatedCards(root.querySelector('[data-gate-entry-cards]'), id);
        if (carouselController) {
          carouselController.setCurrent(id);
        }
      }
    });
    carouselController.setSelectHandler(function (id) {
      renderRelatedCards(root.querySelector('[data-gate-entry-cards]'), id);
      if (mapController) {
        mapController.select(id);
      }
    });
  }

  function initRelatedCarousel(root, entryId, onSelect) {
    var cardsRoot = root.querySelector('[data-gate-entry-cards]');
    var prev = root.querySelector('[data-gate-entry-prev]');
    var next = root.querySelector('[data-gate-entry-next]');
    var currentId = entryId;

    function sync(nextId) {
      currentId = nextId;
      onSelect(currentId);
    }

    prev.addEventListener('click', function () {
      var list = shared.entryCards;
      var index = list.findIndex(function (item) { return item.id === currentId; });
      sync(list[(index - 1 + list.length) % list.length].id);
    });

    next.addEventListener('click', function () {
      var list = shared.entryCards;
      var index = list.findIndex(function (item) { return item.id === currentId; });
      sync(list[(index + 1) % list.length].id);
    });

    cardsRoot.addEventListener('click', function (event) {
      var card = event.target.closest('[data-gate-entry-card]');
      if (!card || event.target.closest('a')) {
        return;
      }
      sync(card.getAttribute('data-gate-entry-id'));
    });

    return {
      setCurrent: function (id) {
        currentId = id;
      },
      setSelectHandler: function (handler) {
        onSelect = handler;
      }
    };
  }

  function init() {
    var root = document.querySelector('[data-gate-entry-root]');
    if (!root) {
      return;
    }
    renderPage(root);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

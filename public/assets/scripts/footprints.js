(function () {
  // Java移行時メモ:
  // 足あとデータは、将来一覧用の軽量情報と詳細本文を分けると扱いやすそうです。
  // Top / 詳細 / ログイン後で preview 地図ロジックを共通化しているため、本実装でも UI 挙動を揃える目印になります。
  var defaultLabel = 'ごひいきさんの足あと';
  // Wire段階の足あと共通データ。
  // Java化では `stories` は DB + API へ置き換え、
  // このファイルには preview 地図の UI ロジックだけ残す想定。
  var stories = {
    '1': {
      id: '1',
      title: '境内の風と、OOさんのおはぎ',
      meta: '生まれ育った町を静かに見返す人',
      summary: '大きく説明するのではなく、時間の混ざり方が伝わるような足あとです。',
      recordedAt: '2026-03-12',
      season: 'spring',
      body: [
        '境内のベンチに座っていると、時々サーっと風が渡ってきます。',
        '木々がざわめき、きれいな紅葉が町に流れていくようで、秋だなぁと思いました。その日はOOさんで買ったおはぎを持っていって、景色を見ながらゆっくり食べました。',
        '観光地の見どころとして切り取るというより、この場所の時間に少し混ぜてもらった感じがしました。誰かに強くおすすめしたいというより、こういう時間がこの町にあることを、そっと返しておきたいと思って書いています。'
      ]
    },
    '2': {
      id: '2',
      title: '小さな店先で、ひとこと教わる',
      meta: '暮らしの声をやわらかく受け取る人',
      summary: '声をかけるだけでなく、そこで流れている時間ごと受け取る足あとです。',
      recordedAt: '2026-07-12',
      season: 'summer',
      body: [
        '通りの角にある小さな店先で、季節の話を少しだけ聞かせてもらいました。',
        '何かを買うことよりも、まずその場にある空気を受け取ることが大事なのだと感じました。店の人の言葉は短かったけれど、暮らしに根ざした重みがありました。',
        'こういう時間は、強いおすすめ文句にしなくても十分に残ると思います。読み終わったあとに、町を歩く速度が少し変わるような足あとになればと思っています。'
      ]
    },
    '3': {
      id: '3',
      title: '朝の入り口で見つけた、町のやさしさ',
      meta: '朝の気配を静かに受け取った人',
      summary: '場所だけでなく、そこに流れる関係や気配を持ち帰る足あとです。',
      recordedAt: '2026-11-12',
      season: 'autumn',
      body: [
        '朝の入口を歩いていると、急いでいない人たちのやりとりが自然に目に入ってきました。',
        '誰かが何かをしてあげているというより、その場で当たり前に支え合っている感じがありました。旅先として見るより先に、生活の輪郭を受け取った気がしました。',
        'まだ言葉にしきれないけれど、その優しさをちゃんと返せる旅人でいたいと思いました。'
      ]
    }
  };

  function withBasePath(basePath) {
    // 画面ごとに public / app で相対パスが違うため、リンクだけここで吸収する。
    // Javaルーティング化したら `/stories/{id}` などの絶対パス生成に置き換える。
    return Object.keys(stories).reduce(function (acc, id) {
      var story = stories[id];
      acc[id] = {
        id: story.id,
        title: story.title,
        meta: story.meta,
        summary: story.summary,
        recordedAt: story.recordedAt,
        season: story.season,
        body: story.body.slice(),
        link: (basePath || '') + 'story.html?story=' + story.id,
        aria: defaultLabel + ' ' + story.id
      };
      return acc;
    }, {});
  }

  function renderPreviewBody(container, story) {
    if (!container || !story) {
      return;
    }

    container.innerHTML = '<p class="topMapCommentText topMapCommentText--clamp">' + story.body.join(' ') + '</p>';
  }

  function renderPreviewCard(card, story, label) {
    var commentWrap;
    var labelNode;
    var linkNode;

    if (!card || !story) {
      return;
    }

    // 詳細ページでは本文全文を出すが、地図カードでは同じデータから軽量表示だけを使う。
    card.setAttribute('aria-label', story.aria);
    card.dataset.topMapCurrent = story.id;
    card.dataset.recordedAt = story.recordedAt || '';
    card.dataset.season = story.season || '';

    labelNode = card.querySelector('[data-top-map-label]') || card.querySelector('[data-top-map-label-text]');
    if (labelNode) {
      labelNode.textContent = label || defaultLabel;
    }
    if (card.querySelector('[data-top-map-marker]')) {
      card.querySelector('[data-top-map-marker]').textContent = story.id;
    }
    if (card.querySelector('[data-top-map-title]')) {
      card.querySelector('[data-top-map-title]').textContent = story.title;
    }
    linkNode = card.querySelector('[data-top-map-link]') || card.querySelector('[data-top-map-link-anchor]');
    if (linkNode) {
      linkNode.setAttribute('href', story.link);
    }

    renderPreviewBody(card.querySelector('[data-top-map-comment-body]'), story);
    commentWrap = card.querySelector('.topMapComment');
    if (commentWrap) {
      commentWrap.setAttribute('data-top-map-comment', story.id);
    }
  }

  function syncPreviewPins(pins, selectedId, storyData) {
    pins.forEach(function (pin) {
      var id = pin.dataset ? pin.dataset.topMapPin : pin.getAttribute('data-top-map-pin');
      var story = id ? storyData[id] : null;
      pin.classList.toggle('is-active', id === selectedId);
      if (!story) {
        return;
      }
      pin.setAttribute('href', story.link);
      pin.dataset.recordedAt = story.recordedAt || '';
      pin.dataset.season = story.season || '';
    });
  }

  function initPreviewMap(root, storyData, options) {
    // Top / 記事詳細 / ログイン後ホームの「ごひいきさんの足あと」は
    // すべてこの初期化関数を通す。ページごとの差分は options に閉じ込める。
    var card = root ? root.querySelector('[data-top-map-card]') : null;
    var pins = root ? Array.prototype.slice.call(root.querySelectorAll('[data-top-map-pin]')) : [];
    var stage = root ? root.querySelector('[data-top-map-stage]') : null;
    var viewport = root ? root.querySelector('[data-top-map-viewport]') : null;
    var pinsLayer = root ? root.querySelector('[data-top-map-pins-layer]') : null;
    var zoomIn = root ? root.querySelector('[data-top-map-zoom-in]') : null;
    var zoomOut = root ? root.querySelector('[data-top-map-zoom-out]') : null;
    var selectedId = options && options.initialStoryId ? options.initialStoryId : '1';
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

    if (!root || !card || !storyData) {
      return null;
    }

    function syncCard() {
      var story = storyData[selectedId];
      if (!story) {
        return;
      }

      // Java化後も「一覧用の軽いカード更新」はフロントで維持しやすいよう、
      // 詳細取得と切り分けた単純な同期処理にしている。
      renderPreviewCard(card, story, options && options.label ? options.label : defaultLabel);
      syncPreviewPins(pins, selectedId, storyData);
      if (options && typeof options.onSync === 'function') {
        options.onSync(story, card, pins);
      }
    }

    function syncZoom() {
      if (viewport) {
        viewport.style.transform = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + zoom + ')';
      }
      if (zoomIn) {
        zoomIn.disabled = zoom >= 1.8;
      }
      if (zoomOut) {
        zoomOut.disabled = zoom <= 1;
      }
      if (stage) {
        stage.classList.toggle('is-draggable', zoom > 1);
      }
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
      if (zoom <= 1 || !stage) {
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
      if (stage) {
        stage.classList.remove('is-dragging');
      }
    }

    function updateZoom(nextZoom) {
      zoom = Math.max(1, Math.min(1.8, nextZoom));
      if (zoom === 1) {
        panX = 0;
        panY = 0;
      }
      syncZoom();
    }

    // ピン押下では遷移せず、その場でカードだけ差し替える。
    // 実詳細への遷移はカード側のリンクで行う設計。
    pins.forEach(function (pin) {
      pin.addEventListener('click', function (event) {
        var id = pin.getAttribute('data-top-map-pin');
        event.preventDefault();
        if (dragged) {
          dragged = false;
          return;
        }
        if (!id) {
          return;
        }
        selectedId = id;
        syncCard();
      });
    });

    if (stage) {
      stage.addEventListener('pointerdown', function (event) {
        var pinTarget = event.target.closest('[data-top-map-pin]');
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
    }

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

    syncCard();
    syncZoom();

    return {
      syncCard: syncCard,
      syncZoom: syncZoom
    };
  }

  window.FURUTABI_FOOTPRINTS = {
    stories: stories,
    withBasePath: withBasePath,
    renderPreviewBody: renderPreviewBody,
    renderPreviewCard: renderPreviewCard,
    syncPreviewPins: syncPreviewPins,
    initPreviewMap: initPreviewMap
  };
})();

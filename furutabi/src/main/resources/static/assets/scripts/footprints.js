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
      authorKey: 'author-1',
      pinClass: 'topMapPin--a',
      title: '境内の風と、OOさんのおはぎ',
      meta: '生まれ育った町を静かに見返す人',
      summary: '大きく説明するのではなく、時間の混ざり方が伝わるような足あとです。',
      recordedAt: '2026-03-12',
      season: 'spring',
      year: '2026',
      category: 'food',
      body: [
        '境内のベンチに座っていると、時々サーっと風が渡ってきます。',
        '木々がざわめき、きれいな紅葉が町に流れていくようで、秋だなぁと思いました。その日はOOさんで買ったおはぎを持っていって、景色を見ながらゆっくり食べました。',
        '観光地の見どころとして切り取るというより、この場所の時間に少し混ぜてもらった感じがしました。誰かに強くおすすめしたいというより、こういう時間がこの町にあることを、そっと返しておきたいと思って書いています。'
      ]
    },
    '2': {
      id: '2',
      authorKey: 'author-2',
      pinClass: 'topMapPin--b',
      title: '小さな店先で、ひとこと教わる',
      meta: '暮らしの声をやわらかく受け取る人',
      summary: '声をかけるだけでなく、そこで流れている時間ごと受け取る足あとです。',
      recordedAt: '2026-07-12',
      season: 'summer',
      year: '2026',
      category: 'experience',
      body: [
        '通りの角にある小さな店先で、季節の話を少しだけ聞かせてもらいました。',
        '何かを買うことよりも、まずその場にある空気を受け取ることが大事なのだと感じました。店の人の言葉は短かったけれど、暮らしに根ざした重みがありました。',
        'こういう時間は、強いおすすめ文句にしなくても十分に残ると思います。読み終わったあとに、町を歩く速度が少し変わるような足あとになればと思っています。'
      ]
    },
    '3': {
      id: '3',
      authorKey: 'author-3',
      pinClass: 'topMapPin--c',
      title: '朝の入り口で見つけた、町のやさしさ',
      meta: '朝の気配を静かに受け取った人',
      summary: '場所だけでなく、そこに流れる関係や気配を持ち帰る足あとです。',
      recordedAt: '2026-11-12',
      season: 'autumn',
      year: '2026',
      category: 'people',
      body: [
        '朝の入口を歩いていると、急いでいない人たちのやりとりが自然に目に入ってきました。',
        '誰かが何かをしてあげているというより、その場で当たり前に支え合っている感じがありました。旅先として見るより先に、生活の輪郭を受け取った気がしました。',
        'まだ言葉にしきれないけれど、その優しさをちゃんと返せる旅人でいたいと思いました。'
      ]
    }
  };

  function getSeasonLabel(value) {
    if (value === 'spring') return '春';
    if (value === 'summer') return '夏';
    if (value === 'autumn') return '秋';
    if (value === 'winter') return '冬';
    return '季節をまたぐ';
  }

  function getCategoryLabel(value) {
    if (value === 'food') return '食';
    if (value === 'meal') return '食事';
    if (value === 'scenery') return '景色';
    if (value === 'event') return '行事';
    if (value === 'people') return '人との関わり';
    if (value === 'stop') return '立ち寄り';
    if (value === 'stay') return '宿泊';
    if (value === 'memo') return 'メモ';
    return '体験';
  }

  function getSeasonFromDateString(dateString) {
    var date = new Date(dateString);
    var month;

    if (!dateString || Number.isNaN(date.getTime())) {
      return 'all';
    }

    month = date.getMonth() + 1;

    if (month >= 3 && month <= 5) return 'spring';
    if (month >= 6 && month <= 8) return 'summer';
    if (month >= 9 && month <= 11) return 'autumn';
    return 'winter';
  }

  function getYearFromDateString(dateString) {
    var date = new Date(dateString);

    if (!dateString || Number.isNaN(date.getTime())) {
      return String(new Date().getFullYear());
    }

    return String(date.getFullYear());
  }

  function summarizeMapRecord(pin) {
    var value = pin ? (pin.body || pin.photoNote || '') : '';
    var body = String(value || '').trim();

    if (!body) {
      return 'わたしの地図に残された記録です。';
    }

    return body;
  }

  function buildSharedMapStories(basePath, options) {
    var interactions = window.FURUTABI_INTERACTIONS;
    var visibilityValues = options && options.visibility
      ? (Array.isArray(options.visibility) ? options.visibility : [options.visibility])
      : ['public'];
    var label = options && options.meta ? options.meta : '— わたしの地図';
    var records;

    if (!interactions || typeof interactions.getMapRecords !== 'function') {
      return {};
    }

    records = interactions.getMapRecords().filter(function (pin) {
      return pin && pin.registered && visibilityValues.indexOf(pin.visibility) !== -1;
    });

    return records.reduce(function (acc, pin) {
      var recordedAt = pin.recordedAt || '';
      var id = 'map-' + String(pin.id || '');
      acc[id] = {
        id: id,
        authorKey: pin.authorKey || pin.nickname || (pin.user_id ? String(pin.user_id) : id),
        title: pin.title || 'わたしの地図の記録',
        meta: label,
        summary: summarizeMapRecord(pin),
        recordedAt: recordedAt,
        season: pin.season || getSeasonFromDateString(recordedAt),
        year: pin.recordedYear || getYearFromDateString(recordedAt),
        category: pin.recordType || 'memo',
        body: [summarizeMapRecord(pin)],
        link: basePath || '#',
        aria: 'わたしの地図の記録',
        x: Number(pin.x),
        y: Number(pin.y)
      };
      return acc;
    }, {});
  }

  function mergeStories() {
    var merged = {};

    Array.prototype.slice.call(arguments).forEach(function (source) {
      Object.keys(source || {}).forEach(function (id) {
        merged[id] = source[id];
      });
    });

    return merged;
  }

  function getFacetText(story) {
    var parts = [];

    if (!story) {
      return '';
    }

    if (story.year) {
      parts.push(story.year + '年');
    }
    if (story.season) {
      parts.push(getSeasonLabel(story.season));
    }
    if (story.category) {
      parts.push(getCategoryLabel(story.category));
    }

    return parts.join(' / ');
  }

  function buildFilterOptions(storyData, extractor, formatter) {
    return Object.keys(storyData).map(function (id) {
      return extractor(storyData[id]);
    }).filter(function (value, index, array) {
      return value && array.indexOf(value) === index;
    }).sort().map(function (value) {
      return '<option value="' + value + '">' + formatter(value) + '</option>';
    }).join('');
  }

  function withBasePath(basePath) {
    // 画面ごとに public / app で相対パスが違うため、リンクだけここで吸収する。
    // Javaルーティング化したら `/stories/{id}` などの絶対パス生成に置き換える。
    return Object.keys(stories).reduce(function (acc, id) {
      var story = stories[id];
      acc[id] = {
        id: story.id,
        authorKey: story.authorKey,
        title: story.title,
        meta: story.meta,
        summary: story.summary,
        recordedAt: story.recordedAt,
        season: story.season,
        year: story.year,
        category: story.category,
        pinClass: story.pinClass,
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
    var metaNode;

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
    metaNode = card.querySelector('[data-top-map-meta]');
    if (!metaNode && card.querySelector('[data-top-map-comment]')) {
      metaNode = document.createElement('p');
      metaNode.className = 'topMapMetaLine';
      metaNode.setAttribute('data-top-map-meta', '');
      card.querySelector('[data-top-map-comment]').insertBefore(metaNode, card.querySelector('[data-top-map-comment-body]'));
    }
    if (metaNode) {
      metaNode.textContent = getFacetText(story);
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
    var pins = [];
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
    var filterWrap;
    var filterYear;
    var filterSeason;
    var filterCategory;
    var filterAuthorButton;
    var filterNote;

    if (!root || !card || !storyData) {
      return null;
    }

    function getStoriesInDisplayOrder() {
      return Object.keys(storyData).map(function (id) {
        return storyData[id];
      }).sort(function (left, right) {
        var leftTime = new Date(left.recordedAt || '').getTime();
        var rightTime = new Date(right.recordedAt || '').getTime();

        if (left.pinClass && right.pinClass && left.pinClass !== right.pinClass) {
          return String(left.id).localeCompare(String(right.id), 'ja');
        }
        if (Number.isNaN(leftTime) && Number.isNaN(rightTime)) {
          return String(left.id).localeCompare(String(right.id), 'ja');
        }
        if (Number.isNaN(leftTime)) {
          return 1;
        }
        if (Number.isNaN(rightTime)) {
          return -1;
        }
        return rightTime - leftTime;
      });
    }

    function renderPins() {
      var orderedStories;

      if (!pinsLayer) {
        return;
      }

      orderedStories = getStoriesInDisplayOrder();
      pinsLayer.innerHTML = '';

      orderedStories.forEach(function (story, index) {
        var pin = document.createElement('a');
        var inner = document.createElement('span');

        pin.className = 'topMapPin' + (story.pinClass ? ' ' + story.pinClass : '') + (story.id === selectedId ? ' is-active' : '');
        pin.href = story.link || '#';
        pin.setAttribute('data-top-map-pin', story.id);
        pin.setAttribute('aria-label', (story.aria || defaultLabel) + ' ' + (index + 1) + ' の場所');
        pin.dataset.recordedAt = story.recordedAt || '';
        pin.dataset.season = story.season || '';

        if (Number.isFinite(story.x) && Number.isFinite(story.y)) {
          pin.style.left = story.x + '%';
          pin.style.top = story.y + '%';
        }

        inner.textContent = String(index + 1);
        pin.appendChild(inner);
        pinsLayer.appendChild(pin);
      });

      pins = Array.prototype.slice.call(root.querySelectorAll('[data-top-map-pin]'));
    }

    filterWrap = root.parentNode ? root.parentNode.querySelector('[data-top-map-filters]') : null;
    if (!filterWrap) {
      filterWrap = document.createElement('div');
      filterWrap.className = 'topMapFilters';
      filterWrap.setAttribute('data-top-map-filters', '');
      filterWrap.innerHTML =
        '<label class="topMapFilterField"><span>年</span><select data-top-map-filter-year><option value="">すべて</option>' +
        buildFilterOptions(storyData, function (story) { return story.year || ''; }, function (value) { return value + '年'; }) +
        '</select></label>' +
        '<label class="topMapFilterField"><span>季節</span><select data-top-map-filter-season><option value="">すべて</option><option value="spring">春</option><option value="summer">夏</option><option value="autumn">秋</option><option value="winter">冬</option></select></label>' +
        '<label class="topMapFilterField"><span>記録タイプ</span><select data-top-map-filter-category><option value="">すべて</option>' +
        buildFilterOptions(storyData, function (story) { return story.category || ''; }, getCategoryLabel) +
        '</select></label>';
      root.parentNode.insertBefore(filterWrap, root);
    }

    filterWrap.innerHTML =
      '<label class="topMapFilterField"><span>\u5e74</span><select data-top-map-filter-year><option value="">\u3059\u3079\u3066</option>' +
      buildFilterOptions(storyData, function (story) { return story.year || ''; }, function (value) { return value + '\u5e74'; }) +
      '</select></label>' +
      '<label class="topMapFilterField"><span>\u5b63\u7bc0</span><select data-top-map-filter-season><option value="">\u3059\u3079\u3066</option><option value="spring">\u6625</option><option value="summer">\u590f</option><option value="autumn">\u79cb</option><option value="winter">\u51ac</option></select></label>' +
      '<label class="topMapFilterField"><span>\u8a18\u9332\u30bf\u30a4\u30d7</span><select data-top-map-filter-category><option value="">\u3059\u3079\u3066</option>' +
      buildFilterOptions(storyData, function (story) { return story.category || ''; }, getCategoryLabel) +
      '</select></label>' +
      '<button class="topMapFilterAction" type="button" data-top-map-filter-author aria-pressed="false">\u30ab\u30fc\u30c9\u306e\u65b9\u306e\u30d4\u30f3\u3092\u8868\u793a</button>';

    filterYear = filterWrap.querySelector('[data-top-map-filter-year]');
    filterSeason = filterWrap.querySelector('[data-top-map-filter-season]');
    filterCategory = filterWrap.querySelector('[data-top-map-filter-category]');
    filterAuthorButton = filterWrap.querySelector('[data-top-map-filter-author]');
    filterNote = root.parentNode.querySelector('[data-top-map-filter-note]');
      if (!filterNote) {
        filterNote = document.createElement('p');
        filterNote.className = 'topMapFilterNote';
        filterNote.setAttribute('data-top-map-filter-note', '');
        filterNote.hidden = true;
        root.parentNode.insertBefore(filterNote, root);
      }

      function getImageFrame() {
        var image = root.querySelector('.topMapCanvasImage');
        var stageWidth;
        var stageHeight;
        var naturalWidth;
        var naturalHeight;
        var imageRatio;
        var stageRatio;
        var width;
        var height;
        var left;
        var top;

        if (!stage || !viewport || !pinsLayer || !image) {
          return null;
        }

        stageWidth = stage.clientWidth;
        stageHeight = stage.clientHeight;
        naturalWidth = image.naturalWidth || image.clientWidth || stageWidth;
        naturalHeight = image.naturalHeight || image.clientHeight || stageHeight;

        if (!stageWidth || !stageHeight || !naturalWidth || !naturalHeight) {
          return null;
        }

        imageRatio = naturalWidth / naturalHeight;
        stageRatio = stageWidth / stageHeight;

        if (imageRatio > stageRatio) {
          width = stageWidth;
          height = width / imageRatio;
          left = 0;
          top = (stageHeight - height) / 2;
        } else {
          height = stageHeight;
          width = height * imageRatio;
          top = 0;
          left = (stageWidth - width) / 2;
        }

        return {
          left: left,
          top: top,
          width: width,
          height: height
        };
      }

      function syncPinsLayerFrame() {
        var frame = getImageFrame();

        if (!pinsLayer || !frame) {
          return;
        }

        pinsLayer.style.inset = 'auto';
        pinsLayer.style.left = frame.left + 'px';
        pinsLayer.style.top = frame.top + 'px';
        pinsLayer.style.width = frame.width + 'px';
        pinsLayer.style.height = frame.height + 'px';
      }

    function getFilteredStories() {
      var year = filterYear ? filterYear.value : '';
      var season = filterSeason ? filterSeason.value : '';
      var category = filterCategory ? filterCategory.value : '';
      var selectedStory = storyData[selectedId];
      var authorKey = filterAuthorButton && filterAuthorButton.dataset.authorPinned === 'true' && selectedStory
        ? String(selectedStory.authorKey || '')
        : '';

      return Object.keys(storyData).map(function (id) {
        return storyData[id];
      }).filter(function (story) {
        if (year && String(story.year || '') !== year) {
          return false;
        }
        if (season && String(story.season || '') !== season) {
          return false;
        }
        if (category && String(story.category || '') !== category) {
          return false;
        }
        if (authorKey && String(story.authorKey || '') !== authorKey) {
          return false;
        }
        return true;
      });
    }

    function syncAuthorFilterButton() {
      var selectedStory = storyData[selectedId];
      var isPinned = !!(filterAuthorButton && filterAuthorButton.dataset.authorPinned === 'true');

      if (!filterAuthorButton) {
        return;
      }

      filterAuthorButton.disabled = !(selectedStory && selectedStory.authorKey);
      filterAuthorButton.setAttribute('aria-pressed', isPinned ? 'true' : 'false');
      filterAuthorButton.textContent = isPinned ? '固定解除' : 'カードの方のピンを表示';
    }

    function syncFilteredPins(visibleStories) {
      pins.forEach(function (pin) {
        var id = pin.dataset ? pin.dataset.topMapPin : pin.getAttribute('data-top-map-pin');
        var isMatched = visibleStories.some(function (item) { return item.id === id; });
        pin.hidden = !isMatched;
        pin.style.display = isMatched ? '' : 'none';
        if (!isMatched) {
          pin.classList.remove('is-active');
        }
      });
    }

    function renderEmptyCard() {
      var titleNode = card.querySelector('[data-top-map-title]');
      var bodyNode = card.querySelector('[data-top-map-comment-body]');
      var markerNode = card.querySelector('[data-top-map-marker]');
      var metaNode = card.querySelector('[data-top-map-meta]');
      var linkNode = card.querySelector('[data-top-map-link]') || card.querySelector('[data-top-map-link-anchor]');

      if (titleNode) {
        titleNode.textContent = '条件に合う足あとはまだありません';
      }
      if (bodyNode) {
        bodyNode.innerHTML = '<p class="topMapCommentText">選択した条件に合う足あとがあると、ここに表示されます。</p>';
      }
      if (markerNode) {
        markerNode.textContent = '—';
      }
      if (metaNode) {
        metaNode.textContent = '';
      }
      if (linkNode) {
        linkNode.setAttribute('href', '#');
      }
      pins.forEach(function (pin) {
        pin.hidden = true;
        pin.classList.remove('is-active');
      });
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

    function syncFilteredCard() {
      var visibleStories = getFilteredStories();

      if (!visibleStories.length) {
        syncFilteredPins([]);
        syncAuthorFilterButton();
        if (filterNote) {
          filterNote.hidden = false;
          filterNote.textContent = '条件に合うピンはまだありません。';
        }
        return;
      }

      if (filterNote) {
        filterNote.hidden = true;
        filterNote.textContent = '';
      }

      syncFilteredPins(visibleStories);

      if (visibleStories.some(function (item) { return item.id === selectedId; })) {
        syncCard();
      }
    }

    function getLatestVisibleStory(visibleStories) {
      return visibleStories.slice().sort(function (left, right) {
        var leftTime = new Date(left.recordedAt || '').getTime();
        var rightTime = new Date(right.recordedAt || '').getTime();

        if (Number.isNaN(leftTime) && Number.isNaN(rightTime)) {
          return 0;
        }
        if (Number.isNaN(leftTime)) {
          return 1;
        }
        if (Number.isNaN(rightTime)) {
          return -1;
        }
        return rightTime - leftTime;
      })[0] || null;
    }

    function syncFilteredCard(options) {
      var visibleStories = getFilteredStories();
      var shouldPreferLatest = !!(options && options.preferLatest);
      var latestStory;

      if (!visibleStories.length) {
        syncFilteredPins([]);
        if (filterNote) {
          filterNote.hidden = false;
          filterNote.textContent = '\u6761\u4ef6\u306b\u5408\u3046\u30d4\u30f3\u306f\u307e\u3060\u3042\u308a\u307e\u305b\u3093\u3002';
        }
        return;
      }

      if (filterNote) {
        filterNote.hidden = true;
        filterNote.textContent = '';
      }

      if (
        shouldPreferLatest ||
        visibleStories.every(function (item) { return item.id !== selectedId; })
      ) {
        latestStory = getLatestVisibleStory(visibleStories);
        if (latestStory) {
          selectedId = latestStory.id;
        }
      }

      syncFilteredPins(visibleStories);
      syncAuthorFilterButton();
      syncCard();
    }

    function syncZoom() {
      syncPinsLayerFrame();
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

    // ピンは storyData から都度描画する。
    // 実詳細への遷移はカード側のリンクで行う設計。
    renderPins();

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

      stage.addEventListener('click', function (event) {
        var pinTarget = event.target.closest('[data-top-map-pin]');
        var id;

        if (!pinTarget) {
          return;
        }

        event.preventDefault();
        if (dragged) {
          dragged = false;
          return;
        }

        id = pinTarget.getAttribute('data-top-map-pin');
        if (!id) {
          return;
        }

        selectedId = id;
        syncFilteredCard();
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

    [filterYear, filterSeason, filterCategory].forEach(function (input) {
      if (!input) {
        return;
      }
      input.addEventListener('change', function () {
        syncFilteredCard({ preferLatest: true });
      });
    });

    if (filterAuthorButton) {
      filterAuthorButton.addEventListener('click', function () {
        var isPinned = filterAuthorButton.dataset.authorPinned === 'true';

        filterAuthorButton.dataset.authorPinned = isPinned ? 'false' : 'true';
        syncFilteredCard();
      });
    }

    document.addEventListener('pointermove', moveDrag);
    document.addEventListener('pointerup', endDrag);
    document.addEventListener('pointercancel', endDrag);
    window.addEventListener('resize', syncPinsLayerFrame);
    if (root.querySelector('.topMapCanvasImage')) {
      root.querySelector('.topMapCanvasImage').addEventListener('load', syncPinsLayerFrame);
    }

    syncFilteredCard();
    syncZoom();

    return {
      syncCard: syncCard,
      syncZoom: syncZoom
    };
  }

  window.FURUTABI_FOOTPRINTS = {
    stories: stories,
    getFacetText: getFacetText,
    withBasePath: withBasePath,
    buildSharedMapStories: buildSharedMapStories,
    mergeStories: mergeStories,
    renderPreviewBody: renderPreviewBody,
    renderPreviewCard: renderPreviewCard,
    syncPreviewPins: syncPreviewPins,
    initPreviewMap: initPreviewMap
  };
})();

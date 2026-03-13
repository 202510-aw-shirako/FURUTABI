(function () {
  var defaultLabel = 'ごひいきさんの足あと';
  var stories = {
    '1': {
      id: '1',
      title: '境内の風と、OOさんのおはぎ',
      meta: '— 町はずれの風を好きになった人',
      summary: 'ただ景色を見るだけでなく、少し座って時間を味わいたくなる足あとです。',
      recordedAt: '2026-03-12',
      season: 'spring',
      body: [
        '境内のベンチに座っていると、時々サーっと風が渡ってきます。木々がざわめき、きれいな紅葉が町に流れていくようで、秋だなぁと思います。',
        'その日はOOさんで買ったおはぎを持っていって、景色を見ながらゆっくり食べました。観光地の見どころとして切り取るというより、この場所の時間に少し混ぜてもらった感じがしました。',
        '誰かに強くおすすめしたいというより、こういう時間がこの町にあることを、そっと返しておきたいと思って書いています。'
      ]
    },
    '2': {
      id: '2',
      title: '小さな川沿いを、話しすぎずに歩く',
      meta: '— 川沿いを歩くのが好きな人',
      summary: '静けさの中で地域の空気に触れられる、短い散歩の足あとです。',
      recordedAt: '2026-07-12',
      season: 'summer',
      body: [
        '町はずれの小さな川沿いを歩いていると、観光地のメイン通りでは感じにくい生活の速度が見えてきます。',
        '無理に会話を広げなくても、時々「この先の光がきれいですよ」と教えてもらうくらいで、十分に豊かでした。',
        'この土地では、何か大きな体験をすることだけが旅ではないのだと、歩きながら少しずつわかってきた気がします。'
      ]
    },
    '3': {
      id: '3',
      title: '手を入れている人がいる景色を好きになる',
      meta: '— 季節の手入れに惹かれた人',
      summary: '地域の手入れや気配に触れて、その場所の見え方が変わる足あとです。',
      recordedAt: '2026-11-12',
      season: 'autumn',
      body: [
        '季節の手入れをしている方の話を少し聞いたあとで同じ道を歩くと、見えるものがまったく違いました。',
        'きれいだなと思っていた風景の奥に、誰かが手をかけて守ってきた時間があるとわかると、その景色が少し深くなります。',
        'また来るときは、ただ訪れるだけでなく、その豊かさをちゃんと味わえる旅人でいたいと思いました。'
      ]
    }
  };

  function withBasePath(basePath) {
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

    if (!card || !story) {
      return;
    }

    card.setAttribute('aria-label', story.aria);
    card.dataset.topMapCurrent = story.id;
    card.dataset.recordedAt = story.recordedAt || '';
    card.dataset.season = story.season || '';

    if (card.querySelector('[data-top-map-label]')) {
      card.querySelector('[data-top-map-label]').textContent = label || defaultLabel;
    }
    if (card.querySelector('[data-top-map-marker]')) {
      card.querySelector('[data-top-map-marker]').textContent = story.id;
    }
    if (card.querySelector('[data-top-map-title]')) {
      card.querySelector('[data-top-map-title]').textContent = story.title;
    }
    if (card.querySelector('[data-top-map-link]')) {
      card.querySelector('[data-top-map-link]').setAttribute('href', story.link);
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

    if (!root || !card || !storyData) {
      return null;
    }

    function syncCard() {
      var story = storyData[selectedId];
      if (!story) {
        return;
      }

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

    function startDrag(event) {
      if (zoom <= 1 || !stage) {
        return;
      }
      event.preventDefault();
      dragging = true;
      dragStartX = event.clientX;
      dragStartY = event.clientY;
      panStartX = panX;
      panStartY = panY;
      dragged = false;
      stage.classList.add('is-dragging');
    }

    function moveDrag(event) {
      if (!dragging) {
        return;
      }
      if (Math.abs(event.clientX - dragStartX) > 3 || Math.abs(event.clientY - dragStartY) > 3) {
        dragged = true;
      }
      panX = panStartX + (event.clientX - dragStartX);
      panY = panStartY + (event.clientY - dragStartY);
      syncZoom();
    }

    function endDrag() {
      if (!dragging) {
        return;
      }
      dragging = false;
      if (stage) {
        stage.classList.remove('is-dragging');
      }
    }

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
      stage.addEventListener('mousedown', function (event) {
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
    }

    if (zoomIn) {
      zoomIn.addEventListener('click', function () {
        zoom = Math.min(1.8, zoom + 0.1);
        syncZoom();
      });
    }

    if (zoomOut) {
      zoomOut.addEventListener('click', function () {
        zoom = Math.max(1, zoom - 0.1);
        if (zoom === 1) {
          panX = 0;
          panY = 0;
        }
        syncZoom();
      });
    }

    document.addEventListener('mousemove', moveDrag);
    document.addEventListener('mouseup', endDrag);

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

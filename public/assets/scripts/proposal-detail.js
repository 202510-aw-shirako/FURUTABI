(function () {
  // Java移行時メモ:
  // 詳細ページは単一テンプレート + query string のワイヤ実装です。
  // 本実装では `/gate/{id}` `/okatte/{id}` のようなURLや、詳細APIに置き換える形も考えやすそうです。
  // 関連提案だけ軽量一覧で返すと、ページ全体の負荷を抑えやすそうです。
  var shared = window.FURUTABI_SHARED_UI;
  var proposals = window.FURUTABI_PROPOSALS;
  var mapImage = './assets/images/凪咲町.svg';

  if (!shared || !proposals) {
    return;
  }

  function readProposalId(root, type) {
    var key = type === 'okatte' ? 'proposal' : (shared.readQueryParam('entry') ? 'entry' : 'proposal');
    var id = shared.readQueryParam(key);
    return proposals.getProposal(type, id).id;
  }

  function getContext() {
    return shared.readQueryParam('context') || 'public';
  }

  function buildDetailHref(type, id, basePath) {
    var href = proposals.buildDetailHref(type, id, basePath);
    var context = getContext();
    return href + '&context=' + encodeURIComponent(context);
  }

  function getAction(type, context) {
    if (context === 'app') {
      return {
        href: '../app/home.html',
        label: 'ログイン後ホームへ戻る',
        buttonClass: 'ghost'
      };
    }
    if (context === 'gate' && type === 'gate') {
      return {
        href: '../app/home.html',
        label: 'この入口から進む',
        buttonClass: 'primary'
      };
    }
    return {
      href: '../auth/login.html',
      label: type === 'okatte' ? 'ログインして提案を受け取る' : '登録 / ログインして入口を選ぶ',
      buttonClass: 'primary'
    };
  }

  function renderParagraphs(items) {
    return items.map(function (item) {
      return '<p>' + proposals.escapeHtml(item) + '</p>';
    }).join('');
  }

  function renderTagRow(tags) {
    return '<div class="proposalDetailTags">' + tags.map(function (tag) {
      return '<span class="proposalTag">' + proposals.escapeHtml(tag) + '</span>';
    }).join('') + '</div>';
  }

  function buildPersonSection(item, basePath) {
    var person = proposals.people[item.personId];
    return '' +
      '<section class="card proposalDetailPersonCard">' +
        '<div class="proposalDetailPersonMedia">' +
          '<img src="' + proposals.escapeHtml(proposals.resolveImage(basePath, person.portrait)) + '" alt="' + proposals.escapeHtml(person.name) + '" />' +
        '</div>' +
        '<div class="proposalDetailPersonBody">' +
          '<p class="proposalDetailPersonRole">' + proposals.escapeHtml(person.role) + '</p>' +
          '<h2 class="proposalDetailSectionTitle">この人について</h2>' +
          '<div class="proposalDetailPersonName">' + proposals.escapeHtml(person.name) + '</div>' +
          '<p class="proposalDetailPersonSummary">' + proposals.escapeHtml(person.summary) + '</p>' +
          '<a class="btn ghost" href="#" data-person-modal="' + proposals.escapeHtml(person.id) + '" data-proposal-base="' + proposals.escapeHtml(basePath) + '">この人についてもう少し見る</a>' +
        '</div>' +
      '</section>';
  }

  function buildMapCard(item, type, basePath, context, isActive) {
    return proposals.buildCard(type, item, basePath, { active: isActive, carousel: true, context: context });
  }

  function renderMapCards(track, type, activeId, basePath, context) {
    var ordered = proposals.getCollection(type).slice();
    var index = ordered.findIndex(function (item) { return item.id === activeId; });
    if (index > 0) {
      ordered = ordered.slice(index).concat(ordered.slice(0, index));
    }
    track.innerHTML = ordered.map(function (item) {
      return buildMapCard(item, type, basePath, context, item.id === activeId);
    }).join('');
  }

  function initMap(root, type, activeId, basePath, onSelect) {
    var pins = Array.prototype.slice.call(root.querySelectorAll('[data-proposal-map-pin]'));
    var stage = root.querySelector('[data-proposal-map-stage]');
    var viewport = root.querySelector('[data-proposal-map-viewport]');
    var pinsLayer = root.querySelector('[data-proposal-map-pins-layer]');
    var zoomIn = root.querySelector('[data-proposal-map-zoom-in]');
    var zoomOut = root.querySelector('[data-proposal-map-zoom-out]');
    var selectedId = activeId;
    var zoom = 1;
    var panX = 0;
    var panY = 0;
    var dragging = false;
    var dragged = false;
    var dragStartX = 0;
    var dragStartY = 0;
    var panStartX = 0;
    var panStartY = 0;

    function syncPins() {
      pins.forEach(function (pin) {
        var id = pin.getAttribute('data-proposal-map-pin');
        pin.classList.toggle('is-active', id === selectedId);
        pin.setAttribute('href', buildDetailHref(type, id, basePath));
      });
    }

    function syncZoom() {
      viewport.style.transform = 'translate(' + panX + 'px, ' + panY + 'px) scale(' + zoom + ')';
      if (zoomIn) zoomIn.disabled = zoom >= 1.8;
      if (zoomOut) zoomOut.disabled = zoom <= 1;
      stage.classList.toggle('is-draggable', zoom > 1);
    }

    function updateZoom(nextZoom) {
      zoom = Math.max(1, Math.min(1.8, nextZoom));
      if (zoom === 1) {
        panX = 0;
        panY = 0;
      }
      syncZoom();
    }

    function startDrag(event) {
      if (zoom <= 1) return;
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
      if (!dragging) return;
      if (Math.abs(event.clientX - dragStartX) > 3 || Math.abs(event.clientY - dragStartY) > 3) {
        dragged = true;
      }
      panX = panStartX + (event.clientX - dragStartX);
      panY = panStartY + (event.clientY - dragStartY);
      syncZoom();
    }

    function endDrag() {
      if (!dragging) return;
      dragging = false;
      stage.classList.remove('is-dragging');
    }

    pins.forEach(function (pin) {
      pin.addEventListener('click', function (event) {
        var id = pin.getAttribute('data-proposal-map-pin');
        event.preventDefault();
        if (dragged) {
          dragged = false;
          return;
        }
        selectedId = id;
        syncPins();
        onSelect(id);
      });
    });

    stage.addEventListener('mousedown', function (event) {
      if (
        event.target !== stage &&
        event.target !== pinsLayer &&
        !event.target.classList.contains('topMapCanvasImage') &&
        !event.target.closest('[data-proposal-map-pin]')
      ) {
        return;
      }
      startDrag(event);
    });
    document.addEventListener('mousemove', moveDrag);
    document.addEventListener('mouseup', endDrag);

    stage.addEventListener('wheel', function (event) {
      event.preventDefault();
      updateZoom(zoom + (event.deltaY < 0 ? 0.1 : -0.1));
    }, { passive: false });
    if (zoomIn) zoomIn.addEventListener('click', function () { updateZoom(zoom + 0.1); });
    if (zoomOut) zoomOut.addEventListener('click', function () { updateZoom(zoom - 0.1); });

    syncPins();
    syncZoom();

    return {
      select: function (id) {
        selectedId = id;
        syncPins();
      }
    };
  }

  function initCarousel(root, type, activeId, basePath, context, mapController) {
    var track = root.querySelector('[data-proposal-map-cards]');
    var prev = root.querySelector('[data-proposal-map-prev]');
    var next = root.querySelector('[data-proposal-map-next]');
    var currentId = activeId;
    var list = proposals.getCollection(type);

    function sync(id) {
      currentId = id;
      renderMapCards(track, type, currentId, basePath, context);
      if (mapController) {
        mapController.select(currentId);
      }
    }

    prev.addEventListener('click', function () {
      var index = list.findIndex(function (item) { return item.id === currentId; });
      sync(list[(index - 1 + list.length) % list.length].id);
    });
    next.addEventListener('click', function () {
      var index = list.findIndex(function (item) { return item.id === currentId; });
      sync(list[(index + 1) % list.length].id);
    });
    track.addEventListener('click', function (event) {
      var card = event.target.closest('[data-proposal-id]');
      if (!card || event.target.closest('a')) {
        return;
      }
      sync(card.getAttribute('data-proposal-id'));
    });

    return {
      setCurrent: function (id) {
        currentId = id;
        renderMapCards(track, type, currentId, basePath, context);
      }
    };
  }

  function renderPage(root) {
    var type = root.getAttribute('data-proposal-type');
    var basePath = root.getAttribute('data-proposal-base') || './';
    var context = getContext();
    var item = proposals.getProposal(type, readProposalId(root, type));
    var action = getAction(type, context);
    var pageLabel = type === 'okatte' ? 'ちいきのおかって' : 'ちいきの入り口';

    shared.setPageTitle('FURUTABI Wire | ', item.title);

    root.innerHTML = '' +
      '<section class="section fv proposalDetailLead">' +
        '<div class="proposalDetailHero">' +
          '<figure class="proposalDetailHeroMedia">' +
            '<img src="' + proposals.escapeHtml(proposals.resolveImage(basePath, item.heroImage)) + '" alt="' + proposals.escapeHtml(item.title) + '" />' +
          '</figure>' +
          '<div class="proposalDetailHeroCopy">' +
            '<span class="pill">' + proposals.escapeHtml(pageLabel) + '</span>' +
            '<h1 class="h1 proposalDetailTitle">' + proposals.escapeHtml(item.title) + '</h1>' +
            '<div class="proposalDetailMeta"><span class="proposalDuration">' + proposals.escapeHtml(item.duration) + '</span></div>' +
            renderTagRow(item.tags) +
            '<p class="proposalDetailIntro">' + proposals.escapeHtml(item.intro) + '</p>' +
          '</div>' +
        '</div>' +
      '</section>' +
      '<section class="section proposalDetailTextSection">' +
        '<div class="proposalDetailTwoCol">' +
          '<section class="card proposalDetailPanel">' +
            '<h2 class="proposalDetailSectionTitle">' + proposals.escapeHtml(item.relationshipLead) + '</h2>' +
            '<div class="proposalDetailCopy">' + renderParagraphs(item.relationship) + '</div>' +
          '</section>' +
          '<section class="card proposalDetailPanel">' +
            '<h2 class="proposalDetailSectionTitle">' + proposals.escapeHtml(item.contentLead) + '</h2>' +
            '<div class="proposalDetailCopy">' + renderParagraphs(item.content) + '</div>' +
          '</section>' +
        '</div>' +
      '</section>' +
      '<section class="section proposalDetailPersonSection">' +
        buildPersonSection(item, basePath) +
      '</section>' +
      '<section class="section proposalDetailMapSection">' +
        '<div class="sectionTitleRow">' +
          '<h2 class="h2">地図でもう一度見る</h2>' +
          '<span class="small">この提案の近くにも、別のひらき方があります。</span>' +
        '</div>' +
        '<div class="topMapLayout appTopMapLayout proposalDetailMapLayout" data-proposal-detail-map>' +
          '<div class="myMapMain">' +
            '<div class="topMapStage" aria-label="' + proposals.escapeHtml(pageLabel) + 'の地図" data-proposal-map-stage>' +
              '<div class="myMapViewport" data-proposal-map-viewport>' +
                '<img class="topMapCanvas topMapCanvasImage" src="' + proposals.escapeHtml(mapImage) + '" alt="' + proposals.escapeHtml(pageLabel) + 'の地図" />' +
                '<div class="myMapPinsLayer" aria-hidden="false" data-proposal-map-pins-layer>' +
                  proposals.getCollection(type).map(function (entry, index) {
                    return '<a class="topMapPin ' + proposals.escapeHtml(entry.pinClass) + (entry.id === item.id ? ' is-active' : '') + '" href="' + proposals.escapeHtml(buildDetailHref(type, entry.id, basePath)) + '" data-proposal-map-pin="' + proposals.escapeHtml(entry.id) + '" aria-label="' + proposals.escapeHtml(entry.placeName) + '"><span>' + String(index + 1) + '</span></a>';
                  }).join('') +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>' +
          '<div class="myMapZoomControls" aria-label="地図の拡大縮小">' +
            '<button class="btn ghost myMapZoomBtn" type="button" data-proposal-map-zoom-in>＋</button>' +
            '<button class="btn ghost myMapZoomBtn" type="button" data-proposal-map-zoom-out>−</button>' +
          '</div>' +
        '</div>' +
        '<div class="entryCarousel proposalDetailMapCarousel">' +
          '<div class="entryCarouselStage">' +
            '<button class="btn ghost entryCarouselArrow entryCarouselArrow--prev" type="button" data-proposal-map-prev aria-label="前の提案"></button>' +
            '<div class="entryCarouselViewport">' +
              '<div class="entryCarouselTrack proposalDetailMapTrack" data-proposal-map-cards></div>' +
            '</div>' +
            '<button class="btn ghost entryCarouselArrow entryCarouselArrow--next" type="button" data-proposal-map-next aria-label="次の提案"></button>' +
          '</div>' +
        '</div>' +
      '</section>' +
      '<section class="section">' +
        '<div class="denseBox">' +
          '<div>' +
            '<div class="h2">次へ進む</div>' +
            '<p class="note">詳細を見たあとも、無理なく次の扉に移れるようにしています。</p>' +
          '</div>' +
          '<a class="btn ' + proposals.escapeHtml(action.buttonClass) + '" href="' + proposals.escapeHtml(action.href) + '">' + proposals.escapeHtml(action.label) + '</a>' +
        '</div>' +
      '</section>';

    var mapRoot = root.querySelector('[data-proposal-detail-map]');
    var track = root.querySelector('[data-proposal-map-cards]');
    renderMapCards(track, type, item.id, basePath, context);
    var mapController = initMap(mapRoot, type, item.id, basePath, function (id) {
      carousel.setCurrent(id);
    });
    var carousel = initCarousel(root, type, item.id, basePath, context, mapController);
    carousel.setCurrent(item.id);
  }

  function init() {
    var root = document.querySelector('[data-proposal-detail-root]');
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

(function () {
  var interactions = window.FURUTABI_INTERACTIONS;
  var footprints = window.FURUTABI_FOOTPRINTS;

  if (!interactions) {
    return;
  }

  function resolveTarget(type, id) {
    var stories;
    var mapRecords;
    if (type === 'story_post' && footprints) {
      stories = footprints.withBasePath('../public/');
      if (stories[id]) {
        return {
          title: stories[id].title,
          href: stories[id].link,
          label: 'ごひいきさんの足あと'
        };
      }
    }
    if (type === 'map_record') {
      mapRecords = interactions.getMapRecords();
      return {
        title: (mapRecords.find(function (item) { return item.id === id; }) || {}).title || 'わたしの地図の記録',
        href: './home.html#tab-map',
        label: 'わたしの地図'
      };
    }
    return {
      title: '記録',
      href: './home.html',
      label: '記録'
    };
  }

  function renderRows(items, target) {
    if (!target) {
      return;
    }
    target.innerHTML = items.length
      ? items.map(function (item) {
          if (item.reaction_type) {
            return '' +
              '<article class="historyRow">' +
                '<div class="historyRowMain">' +
                  '<div class="historyRowMeta"><span class="pill pill--soft">' + item.target.label + '</span><span class="small">' + item.created_at + '</span></div>' +
                  '<h3 class="historyRowTitle">' + item.target.title + '</h3>' +
                  '<p class="note">' + (item.reaction_type === 'thank_you' ? 'ありがとう' : 'いいね') + ' を返しました。</p>' +
                '</div>' +
                '<a class="btn ghost" href="' + item.target.href + '">元のページへ</a>' +
              '</article>';
          }
          return '' +
            '<article class="historyRow">' +
              '<div class="historyRowMain">' +
                '<div class="historyRowMeta"><span class="pill pill--soft">' + item.target.label + '</span><span class="small">' + item.created_at + '</span></div>' +
                '<h3 class="historyRowTitle">' + item.target.title + '</h3>' +
                '<p class="note">' + item.body + '</p>' +
              '</div>' +
              '<a class="btn ghost" href="' + item.target.href + '">元のページへ</a>' +
            '</article>';
        }).join('')
      : '<p class="note">まだ履歴はありません。</p>';
  }

  function applyFilter(container, items, type) {
    if (type === 'all') {
      return items;
    }
    return items.filter(function (item) { return item.target_type === type; });
  }

  function initTabs() {
    var tabs = Array.prototype.slice.call(document.querySelectorAll('[data-history-tab]'));
    var panels = Array.prototype.slice.call(document.querySelectorAll('[data-history-panel]'));
    tabs.forEach(function (button) {
      button.addEventListener('click', function () {
        var target = button.getAttribute('data-history-tab');
        tabs.forEach(function (item) { item.classList.toggle('is-on', item === button); });
        panels.forEach(function (panel) {
          panel.hidden = panel.getAttribute('data-history-panel') !== target;
        });
      });
    });
  }

  function initFilter(rootSelector, items, targetSelector) {
    var root = document.querySelector(rootSelector);
    var target = document.querySelector(targetSelector);
    if (!root || !target) {
      return;
    }
    Array.prototype.slice.call(root.querySelectorAll('[data-history-kind]')).forEach(function (button) {
      button.addEventListener('click', function () {
        var kind = button.getAttribute('data-history-kind');
        Array.prototype.slice.call(root.querySelectorAll('[data-history-kind]')).forEach(function (item) {
          item.classList.toggle('is-on', item === button);
        });
        renderRows(applyFilter(root, items, kind), target);
      });
    });
    renderRows(items, target);
  }

  var commentHistory = interactions.getCommentHistory(resolveTarget);
  var reactionHistory = interactions.getReactionHistory(resolveTarget);

  initTabs();
  initFilter('[data-history-filter="comments"]', commentHistory, '[data-history-comments-list]');
  initFilter('[data-history-filter="reactions"]', reactionHistory, '[data-history-reactions-list]');
})();

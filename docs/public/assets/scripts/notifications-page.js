(function () {
  // 目印:
  // 通知は「起きたことに気づく場」のイメージです。
  // 詳しい調整は連絡 / 提案詳細 / 記事ページに渡し、ここでは要約に留めています。

  function getItems() {
    if (!window.FURUTABI_SHARED_UI || !window.FURUTABI_SHARED_UI.notificationItems) {
      return [];
    }
    return window.FURUTABI_SHARED_UI.notificationItems.slice();
  }

  function matchesFilter(item, filter) {
    if (filter === 'all') return true;
    if (filter === 'unread') return !item.is_read;
    if (filter === 'proposal') return item.kindLabel === '提案・申請';
    if (filter === 'contact') return item.kindLabel === '連絡';
    if (filter === 'reaction') return item.kindLabel === '反応';
    if (filter === 'notice') return item.kindLabel === 'お知らせ' || item.kindLabel === '確認' || item.kindLabel === '重要';
    return true;
  }

  function renderItem(item) {
    return '' +
      '<article class="notificationRow' + (item.is_read ? '' : ' is-unread') + '" data-notification-row data-notification-id="' + item.notification_id + '">' +
        '<div class="notificationRowMain">' +
          '<div class="notificationRowMeta">' +
            '<span class="pill">' + item.kindLabel + '</span>' +
            '<time>' + item.created_at + '</time>' +
            '<span class="notificationState">' + (item.is_read ? '既読' : '未読') + '</span>' +
          '</div>' +
          '<h2 class="h2">' + item.title + '</h2>' +
          '<p class="note">' + item.body + '</p>' +
        '</div>' +
        '<div class="notificationRowActions">' +
          '<button class="btn ghost" type="button" data-mark-read>' + (item.is_read ? '既読です' : '既読にする') + '</button>' +
          '<a class="btn ghost" href="' + item.related_url + '">' + (item.action_label || '詳細を見る') + '</a>' +
        '</div>' +
      '</article>';
  }

  function init() {
    var root = document.querySelector('[data-notification-page]');
    if (!root) return;

    // おすすめ:
    // フィルターは一覧を軽くするためのもの。
    // Java 実装時はサーバー側絞り込みでもフロント側絞り込みでも、同じ見え方を保ちやすい構造です。

    var state = {
      items: getItems(),
      filter: 'all'
    };

    var list = root.querySelector('[data-notification-list]');
    var empty = root.querySelector('[data-notification-empty]');
    var tabs = root.querySelectorAll('[data-notification-filter]');
    var markAll = root.querySelector('[data-mark-all-read]');

    function render() {
      var filtered = state.items.filter(function (item) { return matchesFilter(item, state.filter); });
      tabs.forEach(function (tab) {
        var active = tab.getAttribute('data-notification-filter') === state.filter;
        tab.classList.toggle('is-on', active);
        tab.setAttribute('aria-selected', active ? 'true' : 'false');
      });
      list.innerHTML = filtered.map(renderItem).join('');
      empty.hidden = filtered.length > 0;
    }

    tabs.forEach(function (tab) {
      tab.addEventListener('click', function () {
        state.filter = tab.getAttribute('data-notification-filter');
        render();
      });
    });

    list.addEventListener('click', function (event) {
      var button = event.target.closest('[data-mark-read]');
      if (!button) return;
      var row = event.target.closest('[data-notification-row]');
      if (!row) return;
      var id = row.getAttribute('data-notification-id');
      state.items = state.items.map(function (item) {
        if (item.notification_id === id) {
          item.is_read = true;
          item.read_at = item.read_at || new Date().toISOString();
        }
        return item;
      });
      render();
    });

    if (markAll) {
      markAll.addEventListener('click', function () {
        state.items = state.items.map(function (item) {
          item.is_read = true;
          item.read_at = item.read_at || new Date().toISOString();
          return item;
        });
        render();
      });
    }

    render();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

(function () {
  // 目印:
  // ここは「連絡一覧」と「チャット」のワイヤ用データ置き場です。
  // Java 実装時は API / DB に置き換えても、thread 単位で件名や関連先を持つ考え方を保つと整理しやすそうです。

  var threads = [
    {
      thread_id: 'thread-bridge-1',
      user_id: 'wire-user-1',
      counterpart_id: 'bridge-01',
      counterpart_name: '長谷川 虹翔さん',
      counterpart_role: '架け橋さん',
      related_entity_type: 'proposal',
      related_entity_id: 'okatte-1',
      related_label: 'ちいきのおかって',
      title: '郷土料理をみんなで作る午後',
      status: '承認',
      unread_count: 2,
      updated_at: '2026-03-17 08:10',
      last_message_preview: '次に合いそうな入口について、短い連絡をお送りします。',
      related_url: '../public/okatte-entry.html?proposal=okatte-1',
      type_key: 'proposal'
    },
    {
      thread_id: 'thread-local-1',
      user_id: 'wire-user-1',
      counterpart_id: 'local-22',
      counterpart_name: '伊藤 朗士さん',
      counterpart_role: '地域の方',
      related_entity_type: 'application',
      related_entity_id: 'gate-1',
      related_label: 'ちいきの入り口',
      title: '海まで歩いて景色の話を聞く',
      status: '申請中',
      unread_count: 0,
      updated_at: '2026-03-16 16:40',
      last_message_preview: '当日の待ち合わせ場所は、風の様子を見て朝に確定します。',
      related_url: '../public/gate-entry.html?entry=gate-1&context=app',
      type_key: 'proposal'
    },
    {
      thread_id: 'thread-support-1',
      user_id: 'wire-user-1',
      counterpart_id: 'ops-1',
      counterpart_name: '運営',
      counterpart_role: '運営',
      related_entity_type: 'support',
      related_entity_id: 'support-1',
      related_label: 'その他相談',
      title: '公開範囲についての相談',
      status: '相談中',
      unread_count: 1,
      updated_at: '2026-03-15 18:05',
      last_message_preview: '位置情報の表示粒度について、確認のご案内を送りました。',
      related_url: '../app/privacy-settings.html',
      type_key: 'support'
    },
    {
      thread_id: 'thread-verify-1',
      user_id: 'wire-user-1',
      counterpart_id: 'ops-2',
      counterpart_name: '運営',
      counterpart_role: '運営',
      related_entity_type: 'verify',
      related_entity_id: 'verify-1',
      related_label: '本人確認対応',
      title: '追加本人確認のお願い',
      status: '追加確認が必要',
      unread_count: 0,
      updated_at: '2026-03-14 11:48',
      last_message_preview: '本人確認の追加対応が必要なため、確認方法を案内しています。',
      related_url: '../auth/register-verify.html',
      type_key: 'support'
    },
    {
      thread_id: 'thread-story-1',
      user_id: 'wire-user-1',
      counterpart_id: 'local-11',
      counterpart_name: '瑠璃川 乙羽さん',
      counterpart_role: '地域の方',
      related_entity_type: 'story',
      related_entity_id: 'story-1',
      related_label: 'ごひいきさんの足あと',
      title: '海辺の朝の足あとについて',
      status: '終了',
      unread_count: 0,
      updated_at: '2026-03-10 09:00',
      last_message_preview: '気に入ってくれた景色の話を、少し返してもらいました。',
      related_url: '../public/story.html?story=1',
      type_key: 'reaction'
    }
  ];

  var messagesByThread = {
    'thread-bridge-1': [
      { message_id: 'm-1', thread_id: 'thread-bridge-1', sender_id: 'system', sender_role: 'system', body: '申請が送信されました', is_system_message: true, created_at: '2026-03-16 17:10', read_at: '2026-03-16 17:10' },
      { message_id: 'm-2', thread_id: 'thread-bridge-1', sender_id: 'bridge-01', sender_role: '架け橋さん', body: '郷土料理をみんなで作る午後の提案が、今の関わり方に合いそうです。無理のない形で入れるようにしています。', is_system_message: false, created_at: '2026-03-17 07:50', read_at: null },
      { message_id: 'm-3', thread_id: 'thread-bridge-1', sender_id: 'wire-user-1', sender_role: '自分', body: 'ありがとうございます。まずは少人数で落ち着いて参加できるかを確認したいです。', is_system_message: false, created_at: '2026-03-17 08:00', read_at: '2026-03-17 08:00' },
      { message_id: 'm-4', thread_id: 'thread-bridge-1', sender_id: 'system', sender_role: 'system', body: '承認されました', is_system_message: true, created_at: '2026-03-17 08:05', read_at: null },
      { message_id: 'm-5', thread_id: 'thread-bridge-1', sender_id: 'bridge-01', sender_role: '架け橋さん', body: '次に合いそうな入口について、短い連絡をお送りします。詳細はこの件の提案ページからも見返せます。', is_system_message: false, created_at: '2026-03-17 08:10', read_at: null }
    ],
    'thread-local-1': [
      { message_id: 'm-10', thread_id: 'thread-local-1', sender_id: 'system', sender_role: 'system', body: '申請が送信されました', is_system_message: true, created_at: '2026-03-16 15:40', read_at: '2026-03-16 15:40' },
      { message_id: 'm-11', thread_id: 'thread-local-1', sender_id: 'local-22', sender_role: '地域の方', body: '当日の待ち合わせ場所は、風の様子を見て朝に確定します。無理のない時間で考えています。', is_system_message: false, created_at: '2026-03-16 16:40', read_at: '2026-03-16 16:55' }
    ],
    'thread-support-1': [
      { message_id: 'm-20', thread_id: 'thread-support-1', sender_id: 'wire-user-1', sender_role: '自分', body: '公開範囲で迷っているのですが、エリア単位で見せるのが良いでしょうか。', is_system_message: false, created_at: '2026-03-15 17:40', read_at: '2026-03-15 17:40' },
      { message_id: 'm-21', thread_id: 'thread-support-1', sender_id: 'ops-1', sender_role: '運営', body: '位置情報の表示粒度について、確認のご案内を送りました。公開前に迷う場合は、本人のみのまま見直して大丈夫です。', is_system_message: false, created_at: '2026-03-15 18:05', read_at: null }
    ],
    'thread-verify-1': [
      { message_id: 'm-30', thread_id: 'thread-verify-1', sender_id: 'system', sender_role: 'system', body: '追加確認が必要です', is_system_message: true, created_at: '2026-03-14 11:48', read_at: '2026-03-14 11:48' },
      { message_id: 'm-31', thread_id: 'thread-verify-1', sender_id: 'ops-2', sender_role: '運営', body: '安心して関われる場を守るため、追加の本人確認をお願いしています。詳細は本人確認ページから進められます。', is_system_message: false, created_at: '2026-03-14 11:50', read_at: '2026-03-14 12:10' }
    ],
    'thread-story-1': [
      { message_id: 'm-40', thread_id: 'thread-story-1', sender_id: 'local-11', sender_role: '地域の方', body: '気に入ってくれた景色の話を、少し返してもらいました。ありがとう。', is_system_message: false, created_at: '2026-03-10 09:00', read_at: '2026-03-10 09:05' },
      { message_id: 'm-41', thread_id: 'thread-story-1', sender_id: 'system', sender_role: 'system', body: 'このやり取りは終了しました', is_system_message: true, created_at: '2026-03-10 09:10', read_at: '2026-03-10 09:10' }
    ]
  };

  function getQueryParam(name) {
    var params = new URLSearchParams(window.location.search);
    return params.get(name);
  }

  function filterThread(thread, filter, query) {
    var matchesFilter = filter === 'all'
      || (filter === 'unread' && thread.unread_count > 0)
      || (filter === 'proposal' && thread.type_key === 'proposal')
      || (filter === 'bridge' && thread.counterpart_role === '架け橋さん')
      || (filter === 'local' && thread.counterpart_role === '地域の方')
      || (filter === 'ops' && thread.counterpart_role === '運営');

    if (!matchesFilter) return false;
    if (!query) return true;

    var haystack = [thread.counterpart_name, thread.title, thread.last_message_preview].join(' ').toLowerCase();
    return haystack.indexOf(query.toLowerCase()) !== -1;
  }

  function renderThreadRow(thread) {
    return '' +
      '<a class="messageThreadRow' + (thread.unread_count > 0 ? ' is-unread' : '') + '" href="./messages.html?thread=' + encodeURIComponent(thread.thread_id) + '">' +
        '<div class="messageThreadMain">' +
          '<div class="messageThreadMeta">' +
            '<span class="pill">' + thread.counterpart_role + '</span>' +
            '<span class="pill">' + thread.related_label + '</span>' +
            '<span class="pill pill--soft">' + thread.status + '</span>' +
          '</div>' +
          '<div class="messageThreadHead">' +
            '<h2 class="h2">' + thread.counterpart_name + '</h2>' +
            '<time>' + thread.updated_at + '</time>' +
          '</div>' +
          '<div class="messageThreadTitle">' + thread.title + '</div>' +
          '<p class="note">' + thread.last_message_preview + '</p>' +
        '</div>' +
        '<div class="messageThreadSide">' +
          (thread.unread_count > 0 ? '<span class="messageUnreadBadge">' + (thread.unread_count > 99 ? '99+' : thread.unread_count) + '</span>' : '<span class="messageThreadReadState">既読</span>') +
        '</div>' +
      '</a>';
  }

  function applyThreadPreview(root, thread) {
    var preview = root.querySelector('[data-message-thread-preview]');
    var messages = messagesByThread[thread.thread_id] || [];
    var nameNode = root.querySelector('[data-message-thread-counterpart]');
    var roleNode = root.querySelector('[data-message-thread-role]');
    var titleNode = root.querySelector('[data-message-thread-title]');
    var statusNode = root.querySelector('[data-message-thread-status]');
    var detailLink = root.querySelector('[data-message-thread-related-link]');
    var openLink = root.querySelector('[data-message-thread-open-link]');
    var list = root.querySelector('[data-message-thread-messages]');
    var closed = root.querySelector('[data-message-thread-closed]');
    var verify = root.querySelector('[data-message-thread-verify]');
    var composer = root.querySelector('[data-message-thread-composer]');
    var isClosed;
    var requiresVerify;

    if (!preview || !thread) {
      return;
    }

    if (nameNode) nameNode.textContent = thread.counterpart_name;
    if (roleNode) roleNode.textContent = thread.counterpart_role;
    if (titleNode) titleNode.textContent = thread.title;
    if (statusNode) statusNode.textContent = thread.status;
    if (detailLink) detailLink.setAttribute('href', thread.related_url);
    if (openLink) openLink.setAttribute('href', './chat.html?thread=' + encodeURIComponent(thread.thread_id));
    if (list) list.innerHTML = messages.map(renderMessageBubble).join('');

    isClosed = thread.status === '見送り' || thread.status === '終了';
    requiresVerify = thread.status === '追加確認が必要';

    if (closed) closed.hidden = !isClosed;
    if (verify) verify.hidden = !requiresVerify;
    if (composer) composer.hidden = isClosed || requiresVerify;
    preview.hidden = false;
  }

  function initMessageListPage() {
    var root = document.querySelector('[data-message-list-page]');
    if (!root) return;

    // おすすめ:
    // 一覧では「誰と」「何の件で」「今どの状態か」が先に見えると、雑談チャット化しにくくなります。

    var state = {
      filter: 'all',
      query: ''
    };

    var list = root.querySelector('[data-message-list]');
    var empty = root.querySelector('[data-message-empty]');
    var search = root.querySelector('[data-message-search]');
    var filters = root.querySelectorAll('[data-message-filter]');
    var currentThreadId = getQueryParam('thread');
    var currentThread = threads.find(function (thread) {
      return thread.thread_id === currentThreadId;
    }) || null;

    function render() {
      var filtered = threads.filter(function (thread) {
        return filterThread(thread, state.filter, state.query);
      });
      filters.forEach(function (button) {
        var active = button.getAttribute('data-message-filter') === state.filter;
        button.classList.toggle('is-on', active);
        button.setAttribute('aria-selected', active ? 'true' : 'false');
      });
      list.innerHTML = filtered.map(renderThreadRow).join('');
      empty.hidden = filtered.length > 0;
      if (currentThread) {
        var activeLink = list.querySelector('[href="./messages.html?thread=' + currentThread.thread_id + '"]');
        if (activeLink) {
          activeLink.classList.add('is-current');
        }
      }
    }

    filters.forEach(function (button) {
      button.addEventListener('click', function () {
        state.filter = button.getAttribute('data-message-filter');
        render();
      });
    });

    if (search) {
      search.addEventListener('input', function () {
        state.query = search.value || '';
        render();
      });
    }

    render();
    if (currentThread) {
      applyThreadPreview(root, currentThread);
    }
  }

  function renderMessageBubble(message) {
    if (message.is_system_message) {
      return '' +
        '<div class="chatSystemMessage">' +
          '<span class="pill">' + message.sender_role + '</span>' +
          '<p>' + message.body + '</p>' +
          '<time>' + message.created_at + '</time>' +
        '</div>';
    }

    var mine = message.sender_role === '自分';
    return '' +
      '<article class="chatMessage' + (mine ? ' is-mine' : '') + '">' +
        '<div class="chatMessageMeta">' +
          '<span>' + message.sender_role + '</span>' +
          '<time>' + message.created_at + '</time>' +
        '</div>' +
        '<div class="chatMessageBody">' + message.body + '</div>' +
      '</article>';
  }

  function initChatPage() {
    var root = document.querySelector('[data-chat-page]');
    if (!root) return;

    // いざない:
    // チャットは thread_id で提案 / 申請 / 相談にひもづくイメージです。
    // 本実装でも、上部に件名・相手・状態を固定しておくと安心感を保ちやすそうです。

    var threadId = getQueryParam('thread') || 'thread-bridge-1';
    var thread = threads.find(function (item) { return item.thread_id === threadId; }) || threads[0];
    var messages = messagesByThread[thread.thread_id] || [];

    var nameNode = root.querySelector('[data-chat-counterpart]');
    var roleNode = root.querySelector('[data-chat-role]');
    var titleNode = root.querySelector('[data-chat-title]');
    var statusNode = root.querySelector('[data-chat-status]');
    var detailLink = root.querySelector('[data-chat-related-link]');
    var list = root.querySelector('[data-chat-messages]');
    var closed = root.querySelector('[data-chat-closed]');
    var verify = root.querySelector('[data-chat-verify]');
    var composer = root.querySelector('[data-chat-composer]');

    if (nameNode) nameNode.textContent = thread.counterpart_name;
    if (roleNode) roleNode.textContent = thread.counterpart_role;
    if (titleNode) titleNode.textContent = thread.title;
    if (statusNode) statusNode.textContent = thread.status;
    if (detailLink) detailLink.setAttribute('href', thread.related_url);
    if (list) list.innerHTML = messages.map(renderMessageBubble).join('');

    var isClosed = thread.status === '見送り' || thread.status === '終了';
    var requiresVerify = thread.status === '追加確認が必要';

    if (closed) closed.hidden = !isClosed;
    if (verify) verify.hidden = !requiresVerify;
    if (composer) composer.hidden = isClosed || requiresVerify;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      initMessageListPage();
      initChatPage();
    });
  } else {
    initMessageListPage();
    initChatPage();
  }
})();

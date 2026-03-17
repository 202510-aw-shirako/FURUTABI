(function () {
  var STORAGE_KEY = 'furutabi_interactions_state';
  var MAP_RECORDS_KEY = 'furutabi_map_records_state';
  var memoryState = null;
  var memoryMapRecords = null;

  function clone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function readStorage(key, fallback) {
    try {
      var raw = window.localStorage.getItem(key);
      return raw ? JSON.parse(raw) : clone(fallback);
    } catch (error) {
      return clone(fallback);
    }
  }

  function writeStorage(key, value) {
    try {
      window.localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      // Wire fallback: localStorage が使えない環境ではメモリ保持に留める
    }
  }

  function defaultState() {
    return {
      comments: {
        'story_post:1': [
          { comment_id: 'c-story-1', user_id: 'local-1', nickname: '海辺の台所', body: 'おはぎを持っていく景色の返し方が、とてもいいです。', created_at: '2026-03-11 09:30', updated_at: '2026-03-11 09:30', is_hidden: false },
          { comment_id: 'c-story-2', user_id: 'wire-user-1', nickname: '旅の記録', body: '風の話が残っているのが好きでした。', created_at: '2026-03-11 11:12', updated_at: '2026-03-11 11:12', is_hidden: false }
        ],
        'story_post:2': [
          { comment_id: 'c-story-3', user_id: 'wire-user-1', nickname: '旅の記録', body: '静かな朝の感じが伝わってきました。', created_at: '2026-03-09 08:40', updated_at: '2026-03-09 08:40', is_hidden: false }
        ],
        'story_post:3': []
      },
      reactions: {
        'story_post:1': {
          thank_you: { count: 4, users: ['wire-user-1'] },
          like: { count: 12, users: [] }
        },
        'story_post:2': {
          thank_you: { count: 2, users: [] },
          like: { count: 5, users: ['wire-user-1'] }
        },
        'story_post:3': {
          thank_you: { count: 1, users: [] },
          like: { count: 3, users: [] }
        }
      }
    };
  }

  function getState() {
    if (!memoryState) {
      memoryState = readStorage(STORAGE_KEY, defaultState());
    }
    return memoryState;
  }

  function saveState() {
    writeStorage(STORAGE_KEY, getState());
  }

  function getMapRecords() {
    if (!memoryMapRecords) {
      memoryMapRecords = readStorage(MAP_RECORDS_KEY, []);
    }
    return clone(memoryMapRecords);
  }

  function saveMapRecords(records) {
    memoryMapRecords = clone(records || []);
    writeStorage(MAP_RECORDS_KEY, memoryMapRecords);
  }

  function keyFor(targetType, targetId) {
    return targetType + ':' + targetId;
  }

  function ensureTarget(targetType, targetId) {
    var state = getState();
    var key = keyFor(targetType, targetId);

    if (!state.comments[key]) {
      state.comments[key] = [];
    }
    if (!state.reactions[key]) {
      state.reactions[key] = {
        thank_you: { count: 0, users: [] },
        like: { count: 0, users: [] }
      };
    }

    return key;
  }

  function getComments(targetType, targetId) {
    var state = getState();
    var key = ensureTarget(targetType, targetId);
    return clone((state.comments[key] || []).filter(function (item) { return !item.is_hidden; }));
  }

  function addComment(targetType, targetId, body) {
    var state = getState();
    var key = ensureTarget(targetType, targetId);
    var now = new Date();
    var stamp = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + String(now.getDate()).padStart(2, '0') + ' ' + String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
    state.comments[key].push({
      comment_id: 'comment-' + now.getTime(),
      user_id: 'wire-user-1',
      nickname: '旅の記録',
      body: body,
      created_at: stamp,
      updated_at: stamp,
      is_hidden: false
    });
    saveState();
  }

  function removeComment(targetType, targetId, commentId) {
    var state = getState();
    var key = ensureTarget(targetType, targetId);

    state.comments[key] = (state.comments[key] || []).map(function (item) {
      if (item.comment_id === commentId && item.user_id === 'wire-user-1') {
        item.is_hidden = true;
      }
      return item;
    });

    saveState();
  }

  function getReactionSummary(targetType, targetId) {
    var state = getState();
    var key = ensureTarget(targetType, targetId);
    var target = state.reactions[key];
    return {
      thank_you: {
        count: target.thank_you.count,
        isActive: target.thank_you.users.indexOf('wire-user-1') !== -1
      },
      like: {
        count: target.like.count,
        isActive: target.like.users.indexOf('wire-user-1') !== -1
      }
    };
  }

  function toggleReaction(targetType, targetId, reactionType) {
    var state = getState();
    var key = ensureTarget(targetType, targetId);
    var target = state.reactions[key][reactionType];
    var userIndex = target.users.indexOf('wire-user-1');

    if (userIndex === -1) {
      target.users.push('wire-user-1');
      target.count += 1;
    } else {
      target.users.splice(userIndex, 1);
      target.count = Math.max(0, target.count - 1);
    }

    saveState();
  }

  function getCommentHistory(resolveTarget) {
    var state = getState();
    var results = [];

    Object.keys(state.comments).forEach(function (key) {
      state.comments[key].forEach(function (item) {
        var parts;
        if (item.user_id !== 'wire-user-1' || item.is_hidden) {
          return;
        }
        parts = key.split(':');
        results.push({
          target_type: parts[0],
          target_id: parts[1],
          body: item.body,
          created_at: item.created_at,
          target: resolveTarget(parts[0], parts[1])
        });
      });
    });

    return results.sort(function (a, b) {
      return String(b.created_at).localeCompare(String(a.created_at));
    });
  }

  function getReactionHistory(resolveTarget) {
    var state = getState();
    var results = [];

    Object.keys(state.reactions).forEach(function (key) {
      var parts = key.split(':');
      Object.keys(state.reactions[key]).forEach(function (reactionType) {
        var reaction = state.reactions[key][reactionType];
        if (reaction.users.indexOf('wire-user-1') === -1) {
          return;
        }
        results.push({
          target_type: parts[0],
          target_id: parts[1],
          reaction_type: reactionType,
          created_at: '2026-03-17 12:00',
          target: resolveTarget(parts[0], parts[1])
        });
      });
    });

    return results;
  }

  window.FURUTABI_INTERACTIONS = {
    getComments: getComments,
    addComment: addComment,
    removeComment: removeComment,
    getReactionSummary: getReactionSummary,
    toggleReaction: toggleReaction,
    getCommentHistory: getCommentHistory,
    getReactionHistory: getReactionHistory,
    getMapRecords: getMapRecords,
    saveMapRecords: saveMapRecords
  };
})();

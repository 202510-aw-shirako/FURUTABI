(function () {
  // Java移行時メモ:
  // ちいきの入り口 / ちいきのおかって / 人物モーダルの仮データです。
  // 将来は ProposalSummary, ProposalDetail, PersonProfile のように分けて取得する形も考えやすそうです。
  // personId をキーに人物情報を引く形は、本実装でも関係を保ちやすい目印になります。
  var MODAL_ID = 'proposalPersonModal';

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function resolveImage(basePath, filename) {
    return basePath + 'assets/images/' + filename;
  }

  var people = {
    ito: {
      id: 'ito',
      name: '伊藤 朗士さん',
      role: 'この地域で人をつなぐ架け橋さん',
      portrait: '伊藤さん写真①.PNG',
      summary: 'この地域で長く人をつなぎ、外から来る人と地域とのあいだにも橋をかけてきた方です。誰かを迎えることが特別な演出ではなく、ふだんの延長にあるような安心感があります。',
      intro: '海のそばの暮らしも、静かな案内の仕方もわかっている人です。相手を急がせず、その人に合った入口をやわらかく開きます。',
      relationship: [
        '観光の案内だけでなく、暮らしの延長にある場所へ少しずつ人をつないできました。',
        '外から来る人が無理なく混ざれる速度を知っていて、地域の人にも旅人にも負荷の少ない橋をかけます.'
      ],
      gateOpenings: [
        '海沿いの花の場所まで歩く',
        'お店のまわりを少し歩く',
        '高台の風を受けながら話す'
      ],
      okatteOpenings: [
        '港の仕事場をのぞく',
        '海辺の食卓を囲む夕方'
      ]
    },
    hasegawa: {
      id: 'hasegawa',
      name: '長谷川 虹翔さん',
      role: '地域の若い人たちをつなぐ架け橋さん',
      portrait: '長谷川さん写真①.PNG',
      summary: '都会のやり方も地域の空気もわかりながら、地域の中にある面白さや可能性を人につないできた方です。誰かを前に押し出すというより、その人に合う入口を見つけて、自然に橋をかけるのがうまい人です。',
      intro: '若い世代の挑戦や暮らし方を、外から来る人にもわかる言葉でひらける人です。少し新しい関わり方にも、自然に橋をかけます。',
      relationship: [
        '農や食だけでなく、移り住んだ人たちと地域の人が混ざるきっかけを育ててきました。',
        '話しすぎずに場をほどくのが上手で、少し緊張している人にも自然な入口を見つけます。'
      ],
      gateOpenings: [
        'ハウスの前で野菜を見る',
        '朝の商店街を歩く'
      ],
      okatteOpenings: [
        '郷土料理をみんなで作る午後',
        '移住して見えた地域の話をする'
      ]
    }
  };

  var collections = {
    gate: [
      {
        id: 'gate-1',
        title: '海沿いの花の場所まで歩く',
        duration: '20分',
        summary: '海を見ながら少し歩き、この場所のことを聞く小さな入口です。',
        tags: ['はじめて向き', 'ゆっくり', '景色を楽しむ'],
        personId: 'ito',
        image: '伊藤さん写真②.PNG',
        heroImage: '伊藤さん写真③.PNG',
        placeName: '海沿いの花の場所',
        pinClass: 'gateEntryPin--a',
        intro: '海を見ながら少し歩き、この場所のことを聞く小さな入口です。いきなり深く入らなくても、その土地の空気に少し触れられる時間です。',
        relationshipLead: 'この提案でひらく関係',
        relationship: [
          '見物するだけでは見えにくい、その土地との距離の取り方を、少しだけ教えてもらう入口です。',
          '短い時間でも、誰かと一緒に歩くことで、その場所の見え方が少し変わることがあります。',
          'まずは無理のない形で、その地域との相性をたしかめていきます。'
        ],
        contentLead: '提案内容',
        content: [
          '待ち合わせのあと、海沿いの道をゆっくり歩きます。',
          '花が植えられている場所や、景色のよく見えるところで少し立ち止まりながら、その場所のことを聞きます。',
          '長い説明を聞く場ではなく、一緒に歩く中で自然に話せるくらいの時間を想定しています。'
        ]
      },
      {
        id: 'gate-2',
        title: 'ハウスの前で野菜を見る',
        duration: '20分',
        summary: '珍しい野菜を見ながら、この土地で育てる面白さを少し聞きます。',
        tags: ['会話少なめ', '畑のそば', '新しい挑戦'],
        personId: 'hasegawa',
        image: '長谷川さん写真②.PNG',
        heroImage: '長谷川さん写真③.png',
        placeName: 'ハウスの前',
        pinClass: 'gateEntryPin--b',
        intro: '畑のそばに立ちながら、育てているものと、この土地で働く手ざわりを少し聞く入口です。大きな説明より、目の前の景色から入っていきます。',
        relationshipLead: 'この提案でひらく関係',
        relationship: [
          '何を作っているかだけではなく、どんな気配の中で育てているのかが少しずつ見えてきます。',
          '会話が多くなくても、同じものを見ながら立つことで距離がほどけていきます。',
          '地域の新しい挑戦と、そこに関わる人の温度を無理なく受け取れる入口です。'
        ],
        contentLead: '提案内容',
        content: [
          'ハウスの前で待ち合わせをして、いま育てている野菜やその季節の様子を見せてもらいます。',
          '畑の仕事の流れや、珍しい野菜をこの土地で育てる面白さを、短い言葉で少しずつ聞いていきます。',
          '深く掘り下げる場というより、次に関わりたくなるかどうかを確かめる、静かな入口です。'
        ]
      },
      {
        id: 'gate-3',
        title: 'お店のまわりを少し歩く',
        duration: '20分',
        summary: '喫茶店の空気に触れたあと、近くの木漏れ日の道を少し歩きます。',
        tags: ['人見知り向き', '静か', 'ひとりでも入りやすい'],
        personId: 'ito',
        image: '伊藤さん写真①.PNG',
        heroImage: '伊藤さん写真②.PNG',
        placeName: '喫茶店のまわり',
        pinClass: 'gateEntryPin--c',
        intro: 'お店の空気に少し触れてから、その近くを歩く入口です。いきなり深い会話をしなくても、静かな関わり方から始められます。',
        relationshipLead: 'この提案でひらく関係',
        relationship: [
          '誰かに案内されるというより、同じ場所の空気を少し分けてもらうような入口です。',
          '人見知りでも、自分の速度を保ったまま地域との距離を測れます。',
          '最初の数十分で無理をせず、次に進みたいかどうかを見極められる関係の開き方です。'
        ],
        contentLead: '提案内容',
        content: [
          '喫茶店の前や近くの木漏れ日の道を、会話しすぎずに少し歩きます。',
          '途中で気になった店や景色があれば、その場所のことを短く聞きます。',
          'ひとりでも入りやすい場所を軸にしているので、緊張が強い人でも入りやすい提案です。'
        ]
      },
      {
        id: 'gate-4',
        title: '高台の風を受けながら話す',
        duration: '20分',
        summary: '景色のひらけた場所で立ち止まり、今の地域の気配を少し聞く入口です。',
        tags: ['景色重視', '短時間', '安心'],
        personId: 'ito',
        image: '伊藤さん写真③.PNG',
        heroImage: '伊藤さん写真①.PNG',
        placeName: '海沿いの高台',
        pinClass: 'gateEntryPin--d',
        intro: '景色のひらけた場所で、地域の空気を少し受け取る入口です。言葉より先に、その場所の風や明るさが効いてきます。',
        relationshipLead: 'この提案でひらく関係',
        relationship: [
          '何をするかより、どんな場所に立つかで関係が開いていくタイプの入口です。',
          '景色を一緒に見ることで、会話が少なくてもその土地の輪郭が少しつかめます。',
          '安心して最初の一歩を踏み出せる、軽い入口として機能します。'
        ],
        contentLead: '提案内容',
        content: [
          '高台で待ち合わせ、景色の抜ける方向や風の通る場所に少し立ち止まります。',
          '見えている海や町のことを話しながら、この土地の見え方を少し教えてもらいます。',
          '長い滞在ではなく、気持ちよく終われる長さを大切にしています。'
        ]
      },
      {
        id: 'gate-5',
        title: '朝の商店街を歩く',
        duration: '20分',
        summary: 'まだ静かな商店街を歩きながら、暮らしの近くにある地域の気配に触れます。',
        tags: ['朝の時間', '生活の近く', 'やわらかい'],
        personId: 'hasegawa',
        image: '長谷川さん写真①.PNG',
        heroImage: '長谷川さん写真②.PNG',
        placeName: '朝の商店街',
        pinClass: 'gateEntryPin--e',
        intro: '観光の時間ではなく、暮らしの時間に近い場所を歩く入口です。地域の日常の温度を、朝の静けさの中で受け取っていきます。',
        relationshipLead: 'この提案でひらく関係',
        relationship: [
          '人の生活に近い時間を見せてもらうことで、その地域との距離感が変わります。',
          '派手な案内がなくても、商店街の朝の気配から関係が静かにひらいていきます。',
          '日常に寄った入口なので、その先にどんな人や場があるかも想像しやすくなります。'
        ],
        contentLead: '提案内容',
        content: [
          '朝の商店街をゆっくり歩きながら、開きはじめた店先や人の動きを見ていきます。',
          'この時間ならではの空気や、暮らしと観光が重なる場所を少しずつ教えてもらいます。',
          '強いイベント感ではなく、ふだんの延長に混ざるような提案です。'
        ]
      }
    ],
    okatte: [
      {
        id: 'okatte-1',
        title: '郷土料理をみんなで作る午後',
        duration: '60分',
        summary: 'その季節の食材を囲みながら、みんなで一品ずつ手を動かす時間です。',
        tags: ['少し深め', 'みんなで', '食の時間'],
        personId: 'hasegawa',
        image: '長谷川さん写真③.png',
        heroImage: '長谷川さん写真②.PNG',
        placeName: '台所のある集まり場',
        pinClass: 'okattePin--a',
        intro: '少し関係ができたあとにひらかれる、もう一つの入口です。ただ食べるのではなく、一緒に手を動かしながら、その場の空気に混ざっていく時間です。',
        relationshipLead: 'この提案でひらく関係',
        relationship: [
          '地域には、誰にでも一律にはひらかれていないけれど、関係ができたからこそ迎えられる時間があります。',
          'この提案は、その人に合いそうだと感じた先でひらかれる小さなおかってです。',
          '何か特別な体験を消費するのではなく、その場の一員として少し過ごしてみることを大切にしています。'
        ],
        contentLead: '提案内容',
        content: [
          '季節の食材を使いながら、みんなで一品ずつ手を動かします。',
          '料理教室のように整った場というより、ふだんの台所や集まりに少し混ざるような時間です。',
          '会話がずっと続かなくても大丈夫で、手を動かしながらその場のリズムに入っていける提案です。'
        ]
      },
      {
        id: 'okatte-2',
        title: '港の仕事場をのぞく',
        duration: '40分',
        summary: '地域の仕事の手ざわりに少し近づく、顔の見える提案です。',
        tags: ['ごひいきさん向き', '港', '仕事のそば'],
        personId: 'ito',
        image: '伊藤さん写真②.PNG',
        heroImage: '伊藤さん写真③.PNG',
        placeName: '港の仕事場',
        pinClass: 'okattePin--b',
        intro: '少し親しくなったあとに見せてもらえる、仕事の近くの時間です。見学だけでなく、その場の手ざわりを少し受け取るための提案です。',
        relationshipLead: 'この提案でひらく関係',
        relationship: [
          '地域で働く姿のそばに立つことで、その人との関係だけでなく、土地との関係も少し深まります。',
          '外からは見えにくい日々の仕事が、信頼の先でやわらかくひらかれていきます。',
          'その場の流れを壊さずに近づくこと自体が、この提案の大切な部分です。'
        ],
        contentLead: '提案内容',
        content: [
          '港の仕事場で短く合流し、道具や仕事の流れを少し見せてもらいます。',
          '手を止めすぎない範囲で、日々の仕事の話や季節による違いを聞いていきます。',
          '体験メニューではなく、その人の仕事の近くに少し立たせてもらう時間として設計しています。'
        ]
      },
      {
        id: 'okatte-3',
        title: '移住して見えた地域の話をする',
        duration: '40分',
        summary: '景色や暮らしの話をしながら、この地域とのつきあい方を少し深めます。',
        tags: ['外から来た視点', 'ゆっくり', '少し深め'],
        personId: 'hasegawa',
        image: '長谷川さん写真②.PNG',
        heroImage: '長谷川さん写真①.PNG',
        placeName: '景色の見える縁側',
        pinClass: 'okattePin--c',
        intro: '外から来た人だからこそ話せることと、住み続けて見えてきたことの両方を受け取る提案です。少し深いけれど、静かな時間としてひらかれます。',
        relationshipLead: 'この提案でひらく関係',
        relationship: [
          '移住してきた人の視点は、地域の中にありながら少し外にも開いています。',
          'その視点に触れることで、自分がこの地域とどう付き合えそうかを考えやすくなります。',
          '個別性の高い提案なので、その人に合いそうだと感じたあとで自然にひらかれていきます。'
        ],
        contentLead: '提案内容',
        content: [
          '景色の見える場所で座りながら、この地域で暮らして見えてきたことをゆっくり話します。',
          '暮らしや働き方、外から来た人として感じたことなどを、無理のない範囲で言葉にしていきます。',
          '答えをもらう場ではなく、付き合い方の輪郭を少し深めるための時間です。'
        ]
      }
    ]
  };

  function getCollection(type) {
    return collections[type] || [];
  }

  function getProposal(type, id) {
    return getCollection(type).find(function (item) {
      return item.id === id;
    }) || getCollection(type)[0] || null;
  }

  function buildDetailHref(type, id, basePath, context) {
    var file = type === 'okatte' ? 'okatte-entry.html' : 'gate-entry.html';
    var href = basePath + file + '?proposal=' + encodeURIComponent(id);
    if (context) {
      href += '&context=' + encodeURIComponent(context);
    }
    return href;
  }

  function renderTags(tags) {
    return (tags || []).map(function (tag) {
      return '<span class="proposalTag">' + escapeHtml(tag) + '</span>';
    }).join('');
  }

  function buildGateCard(item, basePath, options) {
    var person = people[item.personId];
    var href = buildDetailHref('gate', item.id, basePath, options && options.context);
    var classes = 'card proposalCard proposalCard--gate entryCarouselCard';
    if (options && options.active) {
      classes += ' is-active';
    }

    return '' +
      '<article class="' + classes + '" data-entry-card data-proposal-id="' + escapeHtml(item.id) + '">' +
        '<figure class="proposalPortrait">' +
          '<img src="' + escapeHtml(resolveImage(basePath, item.image)) + '" alt="' + escapeHtml(person.name) + '" />' +
        '</figure>' +
        '<div class="proposalCardMeta">' +
          '<span class="proposalDuration">' + escapeHtml(item.duration) + '</span>' +
          '<span class="proposalPersonMini">' + escapeHtml(person.name) + '</span>' +
        '</div>' +
        '<div class="proposalCardBody">' +
          '<h3 class="proposalTitle">' + escapeHtml(item.title) + '</h3>' +
          '<p class="proposalSummary">' + escapeHtml(item.summary) + '</p>' +
          '<div class="proposalTags">' + renderTags(item.tags) + '</div>' +
        '</div>' +
        '<a class="btn ghost proposalMore" href="' + escapeHtml(href) + '">詳しく見る</a>' +
      '</article>';
  }

  function buildOkatteCard(item, basePath, options) {
    var person = people[item.personId];
    var href = buildDetailHref('okatte', item.id, basePath, options && options.context);
    var classes = 'card proposalCard proposalCard--okatte';
    if (options && options.carousel) {
      classes += ' entryCarouselCard';
    }
    if (options && options.active) {
      classes += ' is-active';
    }

    return '' +
      '<article class="' + classes + '" data-entry-card data-proposal-id="' + escapeHtml(item.id) + '">' +
        '<figure class="proposalVisual">' +
          '<img src="' + escapeHtml(resolveImage(basePath, item.image)) + '" alt="' + escapeHtml(item.title) + '" />' +
          '<span class="proposalFaceChip">' +
            '<img src="' + escapeHtml(resolveImage(basePath, person.portrait)) + '" alt="' + escapeHtml(person.name) + '" />' +
          '</span>' +
        '</figure>' +
        '<div class="proposalCardMeta">' +
          '<span class="proposalDuration">' + escapeHtml(item.duration) + '</span>' +
          '<span class="proposalPersonMini">' + escapeHtml(person.name) + '</span>' +
        '</div>' +
        '<div class="proposalCardBody">' +
          '<h3 class="proposalTitle">' + escapeHtml(item.title) + '</h3>' +
          '<p class="proposalSummary">' + escapeHtml(item.summary) + '</p>' +
          '<div class="proposalTags">' + renderTags(item.tags) + '</div>' +
        '</div>' +
        '<a class="btn ghost proposalMore" href="' + escapeHtml(href) + '">詳しく見る</a>' +
      '</article>';
  }

  function buildCard(type, item, basePath, options) {
    if (type === 'okatte') {
      return buildOkatteCard(item, basePath, options);
    }
    return buildGateCard(item, basePath, options);
  }

  function renderList(root) {
    var type = root.getAttribute('data-proposal-list');
    var basePath = root.getAttribute('data-proposal-base') || './';
    var context = root.getAttribute('data-proposal-context') || '';
    root.innerHTML = getCollection(type).map(function (item) {
      return buildCard(type, item, basePath, { carousel: true, context: context });
    }).join('');
  }

  function renderGrid(root) {
    var type = root.getAttribute('data-proposal-grid');
    var basePath = root.getAttribute('data-proposal-base') || './';
    var context = root.getAttribute('data-proposal-context') || '';
    root.innerHTML = getCollection(type).map(function (item) {
      return buildCard(type, item, basePath, { context: context });
    }).join('');
  }

  function buildPersonModal(personId, basePath) {
    var person = people[personId];
    if (!person) {
      return '';
    }

    return '' +
      '<div class="proposalPersonModalHeader">' +
        '<figure class="proposalPersonModalPhoto">' +
          '<img src="' + escapeHtml(resolveImage(basePath, person.portrait)) + '" alt="' + escapeHtml(person.name) + '" />' +
        '</figure>' +
        '<div class="proposalPersonModalIntro">' +
          '<p class="proposalPersonModalRole">' + escapeHtml(person.role) + '</p>' +
          '<h2 class="proposalPersonModalName">' + escapeHtml(person.name) + '</h2>' +
          '<p class="proposalPersonModalLead">' + escapeHtml(person.summary) + '</p>' +
        '</div>' +
      '</div>' +
      '<div class="proposalPersonModalSection">' +
        '<h3 class="proposalPersonModalHeading">地域との関わり</h3>' +
        '<div class="proposalPersonModalText">' + person.relationship.map(function (item) {
          return '<p>' + escapeHtml(item) + '</p>';
        }).join('') + '</div>' +
      '</div>' +
      '<div class="proposalPersonModalSection">' +
        '<h3 class="proposalPersonModalHeading">この人がひらく入口</h3>' +
        '<ul class="proposalPersonModalList">' + person.gateOpenings.map(function (item) {
          return '<li>' + escapeHtml(item) + '</li>';
        }).join('') + '</ul>' +
      '</div>' +
      '<div class="proposalPersonModalSection">' +
        '<h3 class="proposalPersonModalHeading">この人がひらくおかって</h3>' +
        '<ul class="proposalPersonModalList">' + person.okatteOpenings.map(function (item) {
          return '<li>' + escapeHtml(item) + '</li>';
        }).join('') + '</ul>' +
      '</div>';
  }

  function ensureModalShell() {
    var shell = document.getElementById(MODAL_ID);
    if (shell) {
      return shell;
    }

    shell = document.createElement('div');
    shell.id = MODAL_ID;
    shell.className = 'proposalPersonModal';
    shell.hidden = true;
    shell.innerHTML = '' +
      '<div class="proposalPersonModalBackdrop" data-person-modal-close></div>' +
      '<div class="proposalPersonModalDialog" role="dialog" aria-modal="true" aria-label="この人について">' +
        '<button class="btn ghost proposalPersonModalClose" type="button" data-person-modal-close>閉じる</button>' +
        '<div class="proposalPersonModalBody" data-person-modal-body></div>' +
      '</div>';
    document.body.appendChild(shell);
    return shell;
  }

  function openPersonModal(personId, basePath) {
    var shell = ensureModalShell();
    var body = shell.querySelector('[data-person-modal-body]');
    body.innerHTML = buildPersonModal(personId, basePath || './');
    shell.hidden = false;
    document.body.classList.add('hasProposalModal');
  }

  function closePersonModal() {
    var shell = document.getElementById(MODAL_ID);
    if (!shell) {
      return;
    }
    shell.hidden = true;
    document.body.classList.remove('hasProposalModal');
  }

  function initModalBindings() {
    document.addEventListener('click', function (event) {
      var opener = event.target.closest('[data-person-modal]');
      var closer = event.target.closest('[data-person-modal-close]');
      if (opener) {
        event.preventDefault();
        openPersonModal(opener.getAttribute('data-person-modal'), opener.getAttribute('data-proposal-base') || './');
        return;
      }
      if (closer) {
        event.preventDefault();
        closePersonModal();
      }
    });

    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        closePersonModal();
      }
    });
  }

  function init() {
    document.querySelectorAll('[data-proposal-list]').forEach(renderList);
    document.querySelectorAll('[data-proposal-grid]').forEach(renderGrid);
    ensureModalShell();
    initModalBindings();
  }

  window.FURUTABI_PROPOSALS = {
    collections: collections,
    people: people,
    escapeHtml: escapeHtml,
    resolveImage: resolveImage,
    buildDetailHref: buildDetailHref,
    getProposal: getProposal,
    getCollection: getCollection,
    buildCard: buildCard,
    openPersonModal: openPersonModal,
    closePersonModal: closePersonModal
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

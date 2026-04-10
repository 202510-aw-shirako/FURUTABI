(function () {
  function initProposalMap(root) {
    var section = root.parentNode;
    var pins = Array.prototype.slice.call(root.querySelectorAll('[data-proposal-pin]'));
    var carousel = section ? section.querySelector('.entryCarousel') : null;
    var track = carousel ? carousel.querySelector('[data-entry-track]') : null;
    var items = section ? Array.prototype.slice.call(section.querySelectorAll('[data-proposal-sync-item]')) : [];
    var initialId = root.getAttribute('data-proposal-initial-id');
    var activeId = initialId;

    if (!pins.length || !items.length) {
      return;
    }

    if (!activeId) {
      activeId = items[0].dataset.proposalId;
    }

    function findItem(id) {
      return items.find(function (item) {
        return item.dataset.proposalId === id;
      }) || items[0];
    }

    function bringItemToFront(item) {
      if (!track || !item || track.firstElementChild === item) {
        return;
      }

      track.prepend(item);
      track.style.transition = 'none';
      track.style.transform = 'translateX(0)';
      track.getBoundingClientRect();
    }

    function scrollToCards() {
      var top;
      if (!carousel) {
        return;
      }

      top = window.pageYOffset + carousel.getBoundingClientRect().top - 96;
      window.scrollTo({
        top: Math.max(0, top),
        behavior: 'smooth'
      });
    }

    function syncActiveState(id, options) {
      var item = findItem(id);
      var shouldScroll = options && options.scrollToCards;
      activeId = item.dataset.proposalId;

      pins.forEach(function (pin) {
        pin.classList.toggle('is-active', pin.dataset.proposalId === activeId);
      });

      items.forEach(function (entry) {
        entry.classList.toggle('is-active', entry.dataset.proposalId === activeId);
      });

      bringItemToFront(item);
      items = Array.prototype.slice.call(section.querySelectorAll('[data-proposal-sync-item]'));

      if (shouldScroll) {
        scrollToCards();
      }
    }

    pins.forEach(function (pin) {
      pin.addEventListener('click', function () {
        syncActiveState(pin.dataset.proposalId, { scrollToCards: true });
      });
    });

    items.forEach(function (item) {
      item.addEventListener('click', function (event) {
        if (event.target.closest('a, button')) {
          return;
        }
        syncActiveState(item.dataset.proposalId);
      });
    });

    syncActiveState(activeId);
  }

  function init() {
    Array.prototype.slice.call(document.querySelectorAll('[data-proposal-map]')).forEach(initProposalMap);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

(function () {
  function initCarousel(root) {
    var track = root.querySelector('[data-entry-track]');
    var prev = root.querySelector('[data-entry-prev]');
    var next = root.querySelector('[data-entry-next]');
    var desktopQuery = window.matchMedia('(min-width: 981px)');
    var isAnimating = false;

    if (!track || !track.children.length) {
      return;
    }

    function resetTrackPosition() {
      track.style.transition = 'none';
      track.style.transform = 'translateX(0)';
    }

    function sync() {
      if (!desktopQuery.matches) {
        track.style.transition = '';
        track.style.transform = '';
        return;
      }

      resetTrackPosition();
    }

    function getStepWidth() {
      var firstCard = track.querySelector('[data-entry-card]');
      var styles;

      if (!firstCard) {
        return 0;
      }

      styles = window.getComputedStyle(track);
      return firstCard.getBoundingClientRect().width + parseFloat(styles.columnGap || styles.gap || '0');
    }

    function moveNext() {
      var stepWidth = getStepWidth();
      var first;

      if (!desktopQuery.matches || isAnimating || !stepWidth) {
        return;
      }

      isAnimating = true;
      track.style.transition = 'transform .22s ease';
      track.style.transform = 'translateX(' + (stepWidth * -1) + 'px)';

      window.setTimeout(function () {
        first = track.firstElementChild;
        if (first) {
          track.appendChild(first);
        }
        resetTrackPosition();
        // Force reflow so the next transition starts from the reset position.
        track.getBoundingClientRect();
        isAnimating = false;
      }, 220);
    }

    function movePrev() {
      var stepWidth = getStepWidth();
      var last;

      if (!desktopQuery.matches || isAnimating || !stepWidth) {
        return;
      }

      isAnimating = true;
      last = track.lastElementChild;
      if (last) {
        track.insertBefore(last, track.firstElementChild);
      }
      track.style.transition = 'none';
      track.style.transform = 'translateX(' + (stepWidth * -1) + 'px)';
      track.getBoundingClientRect();
      track.style.transition = 'transform .22s ease';
      track.style.transform = 'translateX(0)';

      window.setTimeout(function () {
        resetTrackPosition();
        track.getBoundingClientRect();
        isAnimating = false;
      }, 220);
    }

    if (prev) {
      prev.addEventListener('click', function () {
        movePrev();
      });
    }

    if (next) {
      next.addEventListener('click', function () {
        moveNext();
      });
    }

    desktopQuery.addEventListener('change', function () {
      if (!desktopQuery.matches) {
        track.style.transition = '';
        track.style.transform = '';
      } else {
        sync();
      }
    });

    window.addEventListener('resize', sync);
    sync();
  }

  function init() {
    Array.prototype.slice.call(document.querySelectorAll('[data-entry-carousel]')).forEach(initCarousel);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

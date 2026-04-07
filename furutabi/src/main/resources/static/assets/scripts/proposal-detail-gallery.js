(function () {
  var galleries = document.querySelectorAll('[data-proposal-gallery]');
  galleries.forEach(function (gallery) {
    var mainImage = gallery.querySelector('[data-proposal-main-image]');
    var thumbs = gallery.querySelectorAll('[data-proposal-image]');
    if (!mainImage || !thumbs.length) {
      return;
    }

    thumbs.forEach(function (thumb) {
      thumb.addEventListener('click', function () {
        var nextSrc = thumb.getAttribute('data-proposal-image');
        var nextAlt = thumb.getAttribute('data-proposal-alt');
        if (!nextSrc) {
          return;
        }

        mainImage.setAttribute('src', nextSrc);
        if (nextAlt) {
          mainImage.setAttribute('alt', nextAlt);
        }

        thumbs.forEach(function (candidate) {
          candidate.classList.remove('is-active');
        });
        thumb.classList.add('is-active');
      });
    });
  });
})();

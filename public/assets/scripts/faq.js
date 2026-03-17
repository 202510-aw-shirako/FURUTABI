(function () {
  function switchTab(nextKey) {
    document.querySelectorAll('[data-faq-tab]').forEach(function (button) {
      var isActive = button.getAttribute('data-faq-tab') === nextKey;
      button.classList.toggle('is-active', isActive);
      button.setAttribute('aria-selected', isActive ? 'true' : 'false');
    });

    document.querySelectorAll('[data-faq-panel]').forEach(function (panel) {
      var isActive = panel.getAttribute('data-faq-panel') === nextKey;
      panel.classList.toggle('is-active', isActive);
      panel.hidden = !isActive;
    });
  }

  function toggleItem(button) {
    var item = button.closest('.faqItem');
    var answer = item ? item.querySelector('.faqAnswer') : null;
    var icon = button.querySelector('.faqIcon');
    var isOpen = button.getAttribute('aria-expanded') === 'true';

    button.setAttribute('aria-expanded', isOpen ? 'false' : 'true');
    if (answer) {
      answer.hidden = isOpen;
    }
    if (icon) {
      icon.textContent = isOpen ? '+' : '-';
    }
    if (item) {
      item.classList.toggle('is-open', !isOpen);
    }
  }

  function initTabs() {
    document.querySelectorAll('[data-faq-tab]').forEach(function (button) {
      button.addEventListener('click', function () {
        switchTab(button.getAttribute('data-faq-tab'));
      });
    });
  }

  function initAccordion() {
    document.querySelectorAll('.faqQuestion').forEach(function (button) {
      button.addEventListener('click', function () {
        toggleItem(button);
      });
    });
  }

  function init() {
    switchTab('traveler');
    initTabs();
    initAccordion();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

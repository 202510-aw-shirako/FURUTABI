(function () {
  var modal = document.querySelector('[data-host-modal]');
  var triggers = document.querySelectorAll('[data-host-modal-trigger]');
  if (!modal || !triggers.length) {
    return;
  }

  var dialog = modal.querySelector('.proposalPersonModalDialog');
  var closeControls = modal.querySelectorAll('[data-host-modal-close]');
  var previousActiveElement = null;

  function getFocusableElements() {
    return Array.prototype.slice.call(
      modal.querySelectorAll(
        'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      )
    ).filter(function (element) {
      return !element.hasAttribute('hidden');
    });
  }

  function closeModal() {
    modal.hidden = true;
    document.body.classList.remove('hasProposalModal');
    document.removeEventListener('keydown', handleKeydown);
    if (previousActiveElement && typeof previousActiveElement.focus === 'function') {
      previousActiveElement.focus();
    }
  }

  function openModal(trigger) {
    previousActiveElement = trigger;
    modal.hidden = false;
    document.body.classList.add('hasProposalModal');
    document.addEventListener('keydown', handleKeydown);
    var focusables = getFocusableElements();
    if (focusables.length) {
      focusables[0].focus();
    } else if (dialog) {
      dialog.focus();
    }
  }

  function handleKeydown(event) {
    if (event.key === 'Escape') {
      event.preventDefault();
      closeModal();
      return;
    }

    if (event.key !== 'Tab') {
      return;
    }

    var focusables = getFocusableElements();
    if (!focusables.length) {
      event.preventDefault();
      return;
    }

    var first = focusables[0];
    var last = focusables[focusables.length - 1];
    var active = document.activeElement;

    if (event.shiftKey && active === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  triggers.forEach(function (trigger) {
    trigger.addEventListener('click', function (event) {
      event.preventDefault();
      openModal(trigger);
    });
  });

  closeControls.forEach(function (control) {
    control.addEventListener('click', function () {
      closeModal();
    });
  });
})();

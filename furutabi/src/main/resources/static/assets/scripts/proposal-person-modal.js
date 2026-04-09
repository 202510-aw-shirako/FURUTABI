(function () {
  var modal = document.querySelector('[data-person-modal]');
  var triggers = document.querySelectorAll('[data-person-modal-open]');
  if (!modal || !triggers.length) {
    return;
  }

  function openModal() {
    if (typeof modal.showModal === 'function') {
      modal.showModal();
    } else {
      modal.setAttribute('open', 'open');
    }
  }

  function closeModal() {
    if (typeof modal.close === 'function') {
      modal.close();
    } else {
      modal.removeAttribute('open');
    }
  }

  triggers.forEach(function (trigger) {
    trigger.addEventListener('click', function (event) {
      event.preventDefault();
      openModal();
    });
  });

  modal.addEventListener('click', function (event) {
    if (event.target === modal) {
      closeModal();
    }
  });
})();

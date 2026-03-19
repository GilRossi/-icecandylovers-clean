document.addEventListener('DOMContentLoaded', () => {
    const logoutForm = document.getElementById('logoutForm');
    if (logoutForm) {
        logoutForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (confirm('Tem certeza que deseja sair do sistema?')) {
                logoutForm.submit();
            }
        });
    }
});
(function() {
    'use strict';
    
    const themeToggle = document.getElementById('theme-toggle');
    const html = document.documentElement;
    
    function initializeTheme() {
        const savedTheme = localStorage.getItem('theme');
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        
        if (savedTheme) {
            applyTheme(savedTheme === 'dark');
        } else {
            applyTheme(prefersDark);
        }
    }
    
    function applyTheme(isDark) {
        if (isDark) {
            html.classList.add('dark');
        } else {
            html.classList.remove('dark');
        }
        updateToggleIcon(isDark);
    }
    
    function updateToggleIcon(isDark) {
        if (!themeToggle) return;
        
        const icon = themeToggle.querySelector('.theme-toggle-icon');
        if (icon) {
            icon.textContent = isDark ? 'light_mode' : 'dark_mode';
        }
    }
    
    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const isDark = html.classList.contains('dark');
            const newTheme = !isDark;
            
            applyTheme(newTheme);
            localStorage.setItem('theme', newTheme ? 'dark' : 'light');
        });
    }
    
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        if (!localStorage.getItem('theme')) {
            applyTheme(e.matches);
        }
    });
    
    initializeTheme();
})();

// Seasonal Analysis JavaScript
console.log('🌦️ Seasonal Analysis JS loading...');

// Wait for DOM to be ready
document.addEventListener('DOMContentLoaded', function() {
    console.log('✅ Seasonal Analysis DOM ready, setting up...');
    setupSeasonalApp();
});

// Get the base URL for API calls, accounting for codespace environments
function getBaseUrl() {
    const hostname = window.location.hostname;
    if (hostname.includes('.app.github.dev') || hostname.includes('.preview.app.github.dev')) {
        return window.location.origin;
    }
    return '';
}

function setupSeasonalApp() {
    console.log('🌦️ Setting up seasonal analysis app...');
    
    // Setup button handlers
    setupSeasonalButtonHandlers();
    
    // Show welcome message
    showSeasonalMessage('✅ Seasonal Analysis ready! Enter your key and explore seasonal patterns.');
}

function setupSeasonalButtonHandlers() {
    console.log('🔘 Setting up seasonal button handlers...');
    
    const buttons = [
        ['showSummerRainyDaysButton', () => showSummerRainyDays()],
        ['showWinterRainyDaysButton', () => showWinterRainyDays()]
    ];
    
    buttons.forEach(([id, handler]) => {
        const button = document.getElementById(id);
        if (button) {
            button.onclick = handler;
            console.log(`✅ Setup seasonal: ${id}`);
        } else {
            console.error(`❌ Missing seasonal: ${id}`);
        }
    });
}

// Utility functions for seasonal analysis
function showSeasonalMessage(text, isError = false) {
    const messageEl = document.getElementById('message');
    if (messageEl) {
        messageEl.textContent = text;
        messageEl.className = isError ? 'alert alert-danger mt-3' : 'alert alert-success mt-3';
        messageEl.classList.remove('d-none');
    }
    console.log(isError ? '❌' : '✅', text);
}

function showSeasonalLoading() {
    const loadingEl = document.getElementById('loading');
    if (loadingEl) loadingEl.classList.remove('d-none');
}

function hideSeasonalLoading() {
    const loadingEl = document.getElementById('loading');
    if (loadingEl) loadingEl.classList.add('d-none');
}

// Summer Rainy Days Analysis
function showSummerRainyDays() {
    console.log('☀️ Showing summer rainy days analysis...');
    const key = document.getElementById('seasonalKey').value;
    
    if (!key) {
        showSeasonalMessage('Please enter your unscramble key first', true);
        return;
    }
    
    showSeasonalLoading();
    showSeasonalMessage('Loading summer rainy days analysis...');
    
    const baseUrl = getBaseUrl();
    fetch(`${baseUrl}/api/statistics/rainy-days/summer/chart?key=${encodeURIComponent(key)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.text();
        })
        .then(html => {
            hideSeasonalLoading();
            const resultsEl = document.getElementById('seasonalResults');
            if (resultsEl) {
                resultsEl.innerHTML = html;
                showSeasonalMessage('✅ Summer rainy days analysis loaded successfully!');
            }
        })
        .catch(error => {
            hideSeasonalLoading();
            showSeasonalMessage(`❌ Error loading summer analysis: ${error.message}`, true);
            console.error('Summer rainy days error:', error);
        });
}

// Winter Rainy Days Analysis
function showWinterRainyDays() {
    console.log('❄️ Showing winter rainy days analysis...');
    const key = document.getElementById('seasonalKey').value;
    
    if (!key) {
        showSeasonalMessage('Please enter your unscramble key first', true);
        return;
    }
    
    showSeasonalLoading();
    showSeasonalMessage('Loading winter rainy days analysis...');
    
    const baseUrl = getBaseUrl();
    fetch(`${baseUrl}/api/statistics/rainy-days/winter/chart?key=${encodeURIComponent(key)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.text();
        })
        .then(html => {
            hideSeasonalLoading();
            const resultsEl = document.getElementById('seasonalResults');
            if (resultsEl) {
                resultsEl.innerHTML = html;
                showSeasonalMessage('✅ Winter rainy days analysis loaded successfully!');
            }
        })
        .catch(error => {
            hideSeasonalLoading();
            showSeasonalMessage(`❌ Error loading winter analysis: ${error.message}`, true);
            console.error('Winter rainy days error:', error);
        });
}

console.log('🌦️ Seasonal Analysis JS loaded completely');

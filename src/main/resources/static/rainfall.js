// Rainfall Analysis JavaScript
console.log('🌧️ Rainfall analysis loading...');

// Wait for DOM to load
document.addEventListener('DOMContentLoaded', function() {
    console.log('✅ DOM ready, setting up rainfall analysis...');
    setupRainfallAnalysis();
});

// Get the base URL for API calls, accounting for codespace environments
function getBaseUrl() {
    const hostname = window.location.hostname;
    if (hostname.includes('.app.github.dev') || hostname.includes('.preview.app.github.dev')) {
        return window.location.origin;
    }
    return '';
}

function setupRainfallAnalysis() {
    console.log('🚀 Setting up rainfall analysis...');
    
    // Setup button handlers
    setupRainfallButtonHandlers();
    
    // Set default key if available
    const keyInput = document.getElementById('rainfallKey');
    if (keyInput) {
        keyInput.value = 'XXK21'; // Default key for testing
    }
    
    showRainfallMessage('✅ Rainfall analysis ready! Enter your key and select an analysis type.', 'success');
}

function setupRainfallButtonHandlers() {
    console.log('🔘 Setting up rainfall button handlers...');
    
    const buttons = [
        ['showTotalRainfallButton', () => showTotalRainfall()],
        ['showSummerRainfallButton', () => showSeasonalRainfall('summer')],
        ['showWinterRainfallButton', () => showSeasonalRainfall('winter')]
    ];
    
    buttons.forEach(([id, handler]) => {
        const button = document.getElementById(id);
        if (button) {
            button.onclick = handler;
            console.log(`✅ Setup rainfall button: ${id}`);
        } else {
            console.error(`❌ Missing rainfall button: ${id}`);
        }
    });
}

// Utility functions
function showRainfallMessage(text, type = 'info') {
    const messageEl = document.getElementById('rainfallMessage');
    if (messageEl) {
        messageEl.textContent = text;
        messageEl.className = `alert alert-${type}`;
        messageEl.classList.remove('d-none');
        setTimeout(() => {
            messageEl.classList.add('d-none');
        }, 5000);
    }
    console.log(type === 'danger' ? '❌' : '✅', text);
}

function showRainfallLoading() {
    const loadingEl = document.getElementById('rainfallLoading');
    if (loadingEl) loadingEl.classList.remove('d-none');
}

function hideRainfallLoading() {
    const loadingEl = document.getElementById('rainfallLoading');
    if (loadingEl) loadingEl.classList.add('d-none');
}

function getRainfallKey() {
    const keyInput = document.getElementById('rainfallKey');
    return keyInput ? keyInput.value.trim() : '';
}

// Main analysis functions
function showTotalRainfall() {
    console.log('💧 Showing total rainfall analysis...');
    const key = getRainfallKey();
    
    if (!key) {
        showRainfallMessage('Please enter your unscramble key first', 'warning');
        return;
    }
    
    showRainfallLoading();
    showRainfallMessage('Loading total rainfall analysis...', 'info');
    
    const baseUrl = getBaseUrl();
    fetch(`${baseUrl}/api/rainfall/total/chart?key=${encodeURIComponent(key)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.text();
        })
        .then(html => {
            hideRainfallLoading();
            const resultsEl = document.getElementById('rainfallResults');
            if (resultsEl) {
                resultsEl.innerHTML = html;
                showRainfallMessage('✅ Total rainfall analysis loaded successfully!', 'success');
            }
        })
        .catch(error => {
            hideRainfallLoading();
            showRainfallMessage(`❌ Error loading total rainfall: ${error.message}`, 'danger');
            console.error('Total rainfall error:', error);
        });
}

function showSeasonalRainfall(season) {
    console.log(`🌧️ Showing ${season} rainfall analysis...`);
    const key = getRainfallKey();
    
    if (!key) {
        showRainfallMessage('Please enter your unscramble key first', 'warning');
        return;
    }
    
    const seasonEmoji = season === 'summer' ? '☀️' : '❄️';
    const seasonName = season.charAt(0).toUpperCase() + season.slice(1);
    
    showRainfallLoading();
    showRainfallMessage(`Loading ${seasonName} rainfall analysis...`, 'info');
    
    const baseUrl = getBaseUrl();
    fetch(`${baseUrl}/api/rainfall/${season}/chart?key=${encodeURIComponent(key)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.text();
        })
        .then(html => {
            hideRainfallLoading();
            const resultsEl = document.getElementById('rainfallResults');
            if (resultsEl) {
                resultsEl.innerHTML = html;
                showRainfallMessage(`✅ ${seasonEmoji} ${seasonName} rainfall analysis loaded successfully!`, 'success');
            }
        })
        .catch(error => {
            hideRainfallLoading();
            showRainfallMessage(`❌ Error loading ${season} rainfall: ${error.message}`, 'danger');
            console.error(`${seasonName} rainfall error:`, error);
        });
}

console.log('🚀 Rainfall analysis JavaScript loaded completely');

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
        ['showWinterRainyDaysButton', () => showWinterRainyDays()],
        ['showAllSeasonsButton', () => showAllSeasons()],
        ['goBackButton', () => window.location.href = 'index.html']
    ];
    
    buttons.forEach(([id, handler]) => {
        const button = document.getElementById(id);
        if (button) {
            button.onclick = handler;
            console.log(`✅ Seasonal Setup: ${id}`);
        } else {
            console.error(`❌ Seasonal Missing: ${id}`);
        }
    });
}

// Utility functions
function showSeasonalMessage(text, isError = false) {
    const messageEl = document.getElementById('seasonalMessage');
    if (messageEl) {
        messageEl.textContent = text;
        messageEl.className = isError ? 'message error' : 'message';
    }
    console.log(isError ? '❌' : '✅', text);
}

function showSeasonalLoading() {
    const loadingEl = document.getElementById('seasonalLoading');
    if (loadingEl) loadingEl.classList.remove('hidden');
}

function hideSeasonalLoading() {
    const loadingEl = document.getElementById('seasonalLoading');
    if (loadingEl) loadingEl.classList.add('hidden');
}

// Seasonal Analysis Functions
function showSummerRainyDays() {
    console.log('☀️ Showing summer rainy days statistics...');
    const key = document.getElementById('seasonalKey').value;
    
    if (!key) {
        showSeasonalMessage('Please enter your unscramble key first', true);
        return;
    }
    
    showSeasonalLoading();
    showSeasonalMessage('Loading summer rainy days statistics...');
    
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
                showSeasonalMessage('✅ Summer rainy days statistics loaded successfully!');
            }
        })
        .catch(error => {
            hideSeasonalLoading();
            showSeasonalMessage(`❌ Error loading summer rainy days: ${error.message}`, true);
            console.error('Summer rainy days error:', error);
        });
}

function showWinterRainyDays() {
    console.log('❄️ Showing winter rainy days statistics...');
    const key = document.getElementById('seasonalKey').value;
    
    if (!key) {
        showSeasonalMessage('Please enter your unscramble key first', true);
        return;
    }
    
    showSeasonalLoading();
    showSeasonalMessage('Loading winter rainy days statistics...');
    
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
                showSeasonalMessage('✅ Winter rainy days statistics loaded successfully!');
            }
        })
        .catch(error => {
            hideSeasonalLoading();
            showSeasonalMessage(`❌ Error loading winter rainy days: ${error.message}`, true);
            console.error('Winter rainy days error:', error);
        });
}

function showAllSeasons() {
    console.log('🌦️ Showing all seasons comparison...');
    const key = document.getElementById('seasonalKey').value;
    
    if (!key) {
        showSeasonalMessage('Please enter your unscramble key first', true);
        return;
    }
    
    showSeasonalLoading();
    showSeasonalMessage('Loading seasonal comparison...');
    
    const baseUrl = getBaseUrl();
    
    // Load both summer and winter charts
    Promise.all([
        fetch(`${baseUrl}/api/statistics/rainy-days/summer/chart?key=${encodeURIComponent(key)}`),
        fetch(`${baseUrl}/api/statistics/rainy-days/winter/chart?key=${encodeURIComponent(key)}`)
    ])
    .then(responses => {
        if (!responses[0].ok || !responses[1].ok) {
            throw new Error('One or more requests failed');
        }
        return Promise.all([responses[0].text(), responses[1].text()]);
    })
    .then(([summerHtml, winterHtml]) => {
        hideSeasonalLoading();
        const resultsEl = document.getElementById('seasonalResults');
        if (resultsEl) {
            resultsEl.innerHTML = `
                <div class="seasonal-comparison">
                    <h2>🌦️ Seasonal Comparison Dashboard</h2>
                    <div class="season-charts">
                        <div class="season-chart">${summerHtml}</div>
                        <div class="season-chart">${winterHtml}</div>
                    </div>
                </div>
            `;
            showSeasonalMessage('✅ Seasonal comparison loaded successfully!');
        }
    })
    .catch(error => {
        hideSeasonalLoading();
        showSeasonalMessage(`❌ Error loading seasonal comparison: ${error.message}`, true);
        console.error('Seasonal comparison error:', error);
    });
}

console.log('🌦️ Seasonal Analysis JS loaded completely');

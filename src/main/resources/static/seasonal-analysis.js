// Seasonal Analysis JavaScript
console.log('🌦️ Seasonal Analysis JS loading...');

// Wait for DOM to be ready
document.addEventListener('DOMContentLoaded', function() {
    console.log('✅ Seasonal Analysis DOM ready, setting up...');
    setupSeasonalAnalysis();
});

// Get the base URL for API calls, accounting for codespace environments
function getBaseUrl() {
    const hostname = window.location.hostname;
    if (hostname.includes('.app.github.dev') || hostname.includes('.preview.app.github.dev')) {
        return window.location.origin;
    }
    return '';
}

// Helper function to get year filter parameters from URL
function getYearFilterParams() {
    const urlParams = new URLSearchParams(window.location.search);
    return {
        startYear: urlParams.get('startYear'),
        endYear: urlParams.get('endYear'),
        excludedYears: urlParams.get('excludedYears')
    };
}

// Helper function to build URL with year filter parameters
function buildSeasonalUrl(baseUrl, key, season) {
    const yearParams = getYearFilterParams();
    let url = `${baseUrl}/api/statistics/rainy-days/${season}/chart?key=${encodeURIComponent(key)}`;
    
    if (yearParams.startYear) {
        url += `&startYear=${encodeURIComponent(yearParams.startYear)}`;
    }
    if (yearParams.endYear) {
        url += `&endYear=${encodeURIComponent(yearParams.endYear)}`;
    }
    if (yearParams.excludedYears) {
        url += `&excludedYears=${encodeURIComponent(yearParams.excludedYears)}`;
    }
    
    return url;
}

function setupSeasonalAnalysis() {
    console.log('🚀 Setting up seasonal analysis...');
    
    // Setup button handlers
    setupSeasonalButtonHandlers();
    
    // Show welcome message
    showSeasonalMessage('✅ Seasonal Analysis ready! Enter your key and select an analysis option.');
    
    // Add visual indicator
    const infoCard = document.querySelector('.info-card');
    if (infoCard) {
        const status = document.createElement('div');
        status.style.cssText = 'background: #d4edda; color: #155724; padding: 8px; border-radius: 4px; margin-top: 10px; font-size: 14px;';
        status.innerHTML = '✅ <strong>Status:</strong> Seasonal analysis module loaded and ready!';
        infoCard.appendChild(status);
    }
}

function setupSeasonalButtonHandlers() {
    console.log('🔘 Setting up seasonal button handlers...');
    
    const buttons = [
        ['showSummerRainyDaysButton', () => showSummerRainyDays()],
        ['showWinterRainyDaysButton', () => showWinterRainyDays()],
        ['showComparativeAnalysisButton', () => showComparativeAnalysis()]
    ];
    
    buttons.forEach(([id, handler]) => {
        const button = document.getElementById(id);
        if (button) {
            button.onclick = handler;
            console.log(`✅ Setup: ${id}`);
        } else {
            console.error(`❌ Missing: ${id}`);
        }
    });
}

// Utility functions for seasonal analysis
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

function getSeasonalKey() {
    const key = document.getElementById('seasonalKey')?.value;
    if (!key) {
        showSeasonalMessage('Please enter your unscramble key first', true);
        return null;
    }
    return key;
}

// Seasonal analysis functions
function showSummerRainyDays() {
    console.log('☀️ Showing summer rainy days statistics...');
    const key = getSeasonalKey();
    if (!key) return;
    
    showSeasonalLoading();
    showSeasonalMessage('Loading summer rainy days statistics...');
    
    const baseUrl = getBaseUrl();
    const url = buildSeasonalUrl(baseUrl, key, 'summer');
    console.log('🌐 Fetching summer data from URL:', url);
    
    fetch(url)
        .then(response => {
            console.log('📊 Summer chart response status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.text();
        })
        .then(html => {
            console.log('✅ Summer chart data received, length:', html.length);
            hideSeasonalLoading();
            const resultsEl = document.getElementById('seasonalResults');
            if (resultsEl) {
                resultsEl.innerHTML = html;
                showSeasonalMessage('✅ Summer rainy days statistics loaded successfully!');
            } else {
                console.error('❌ Seasonal results element not found');
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
    const key = getSeasonalKey();
    if (!key) return;
    
    showSeasonalLoading();
    showSeasonalMessage('Loading winter rainy days statistics...');
    
    const baseUrl = getBaseUrl();
    const url = buildSeasonalUrl(baseUrl, key, 'winter');
    console.log('🌐 Fetching winter data from URL:', url);
    
    fetch(url)
        .then(response => {
            console.log('📊 Winter chart response status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.text();
        })
        .then(html => {
            console.log('✅ Winter chart data received, length:', html.length);
            hideSeasonalLoading();
            const resultsEl = document.getElementById('seasonalResults');
            if (resultsEl) {
                resultsEl.innerHTML = html;
                showSeasonalMessage('✅ Winter rainy days statistics loaded successfully!');
            } else {
                console.error('❌ Seasonal results element not found');
            }
        })
        .catch(error => {
            hideSeasonalLoading();
            showSeasonalMessage(`❌ Error loading winter rainy days: ${error.message}`, true);
            console.error('Winter rainy days error:', error);
        });
}

function showComparativeAnalysis() {
    console.log('📈 Showing comparative seasonal analysis...');
    const key = getSeasonalKey();
    if (!key) return;
    
    showSeasonalLoading();
    showSeasonalMessage('Loading comparative seasonal analysis...');
    
    const baseUrl = getBaseUrl();
    
    // Fetch both summer and winter data with year filtering
    Promise.all([
        fetch(buildSeasonalUrl(baseUrl, key, 'summer')),
        fetch(buildSeasonalUrl(baseUrl, key, 'winter'))
    ])
    .then(responses => {
        console.log('📊 Comparative analysis responses received');
        return Promise.all([
            responses[0].ok ? responses[0].text() : Promise.reject(new Error(`Summer data: HTTP ${responses[0].status}`)),
            responses[1].ok ? responses[1].text() : Promise.reject(new Error(`Winter data: HTTP ${responses[1].status}`))
        ]);
    })
    .then(([summerHtml, winterHtml]) => {
        console.log('✅ Both seasonal datasets received');
        hideSeasonalLoading();
        
        const resultsEl = document.getElementById('seasonalResults');
        if (resultsEl) {
            // Create a side-by-side comparison layout
            resultsEl.innerHTML = `
                <div class="comparative-analysis">
                    <h3>📊 Seasonal Rainfall Comparison</h3>
                    <div class="comparison-container">
                        <div class="season-column">
                            <h4>☀️ Summer Analysis</h4>
                            ${summerHtml}
                        </div>
                        <div class="season-column">
                            <h4>❄️ Winter Analysis</h4>
                            ${winterHtml}
                        </div>
                    </div>
                </div>
            `;
            showSeasonalMessage('✅ Comparative seasonal analysis loaded successfully!');
        }
    })
    .catch(error => {
        hideSeasonalLoading();
        showSeasonalMessage(`❌ Error loading comparative analysis: ${error.message}`, true);
        console.error('Comparative analysis error:', error);
    });
}

console.log('🚀 Seasonal Analysis JS loaded completely');

// Ultra-simple approach - immediate execution
console.log('🚀 App.js loading...');

// Wait for DOM then immediately setup
document.addEventListener('DOMContentLoaded', function() {
    console.log('✅ DOM ready, setting up...');
    setupApp();
});

// Also try immediately if DOM is already ready
if (document.readyState !== 'loading') {
    console.log('✅ DOM already ready, setting up immediately...');
    setupApp();
}

// Get the base URL for API calls, accounting for codespace environments
function getBaseUrl() {
    // Check if we're in a GitHub Codespace environment
    const hostname = window.location.hostname;
    if (hostname.includes('.app.github.dev') || hostname.includes('.preview.app.github.dev')) {
        // We're in a codespace, use the full URL
        return window.location.origin;
    }
    // Local development
    return '';
}

function setupApp() {
    console.log('🚀 Setting up app...');
    
    // Setup year dropdown
    setupYearDropdown();
    
    // Setup all button click handlers
    setupButtonHandlers();
    
    // Show welcome message
    showMessage('✅ App ready! Year dropdown populated. Try the quick test buttons.');
    
    // Add visual indicator
    const infoBox = document.querySelector('.info-box');
    if (infoBox) {
        const status = document.createElement('div');
        status.style.cssText = 'background: #d4edda; color: #155724; padding: 8px; border-radius: 4px; margin-top: 10px; font-size: 14px;';
        status.innerHTML = '✅ <strong>Status:</strong> JavaScript loaded, year dropdown populated, all buttons functional!';
        infoBox.appendChild(status);
    }
}

function setupYearDropdown() {
    const yearSelect = document.getElementById('year');
    if (!yearSelect) {
        console.error('❌ Year select not found');
        return;
    }
    
    console.log('📅 Setting up year dropdown...');
    
    // Add years
    for (let year = 2005; year <= 2025; year++) {
        const option = document.createElement('option');
        option.value = year;
        option.textContent = year;
        yearSelect.appendChild(option);
    }
    
    console.log(`✅ Year dropdown setup complete: ${yearSelect.options.length - 1} years added`);
}

function setupButtonHandlers() {
    console.log('🔘 Setting up button handlers...');
    
    // Simple button setup with existence checks
    const buttons = [
        ['fetchButton', () => fetchApi('/api/landing', 'Landing API')],
        ['healthButton', () => fetchApi('/api/health', 'Health API')],
        ['fetchWeatherButton', () => fetchWeatherData('html')],
        ['fetchWeatherJsonButton', () => fetchWeatherData('json')],
        ['getStatsButton', () => getStats()],
        ['showRainyDaysButton', () => showRainyDays()],
        ['testSep2005', () => runTestWithCurrentKey('2005', '9', 'Sep 2005')],
        ['testJan2010', () => runTestWithCurrentKey('2010', '1', 'Jan 2010')],
        ['testJun2025', () => runTestWithCurrentKey('2025', '6', 'Jun 2025')],
        ['testInvalidKey', () => runTest('2010', '1', 'INVALID_KEY', 'Invalid Key')]
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

// Utility functions
function showMessage(text, isError = false) {
    const messageEl = document.getElementById('message');
    if (messageEl) {
        messageEl.textContent = text;
        messageEl.className = isError ? 'alert alert-danger' : 'alert alert-success';
        messageEl.classList.remove('d-none');
    }
    console.log(isError ? '❌' : '✅', text);
}

function showLoading() {
    const loadingEl = document.getElementById('loading');
    if (loadingEl) loadingEl.classList.remove('d-none');
}

function hideLoading() {
    const loadingEl = document.getElementById('loading');
    if (loadingEl) loadingEl.classList.add('d-none');
}

// API functions
async function fetchApi(url, name) {
    showLoading();
    try {
        const baseUrl = getBaseUrl();
        const response = await fetch(`${baseUrl}${url}`);
        const data = await response.text();
        hideLoading();
        showMessage(`${name}: ${data}`);
    } catch (error) {
        hideLoading();
        showMessage(`${name} Error: ${error.message}`, true);
    }
}

async function fetchWeatherData(format) {
    const year = document.getElementById('year')?.value;
    const month = document.getElementById('month')?.value;
    const key = document.getElementById('key')?.value;
    
    if (!year || !month || !key) {
        showMessage('Please select year, month, and enter key', true);
        return;
    }
    
    showLoading();
    try {
        const baseUrl = getBaseUrl();
        const url = `${baseUrl}/api/weather/data?year=${year}&month=${month}&key=${encodeURIComponent(key)}&format=${format}`;
        const response = await fetch(url);
        const data = await response.text();
        hideLoading();
        
        const weatherDataEl = document.getElementById('weatherData');
        if (weatherDataEl) {
            weatherDataEl.innerHTML = format === 'html' ? data : `<pre>${data}</pre>`;
        }
        showMessage(`Weather data loaded: ${year}-${month} (${format})`);
    } catch (error) {
        hideLoading();
        showMessage(`Weather Error: ${error.message}`, true);
    }
}

async function getStats() {
    const key = document.getElementById('key')?.value;
    if (!key) {
        showMessage('Please enter the unscramble key', true);
        return;
    }
    
    showLoading();
    try {
        const baseUrl = getBaseUrl();
        const response = await fetch(`${baseUrl}/api/weather/stats?key=${encodeURIComponent(key)}`);
        const data = await response.text();
        hideLoading();
        
        const weatherDataEl = document.getElementById('weatherData');
        if (weatherDataEl) weatherDataEl.innerHTML = data;
        showMessage('Weather statistics loaded');
    } catch (error) {
        hideLoading();
        showMessage(`Stats Error: ${error.message}`, true);
    }
}

function runTest(year, month, key, testName) {
    console.log(`🧪 Running test: ${testName}`);
    
    // Set form values
    const yearEl = document.getElementById('year');
    const monthEl = document.getElementById('month');
    const keyEl = document.getElementById('key');
    
    if (yearEl) yearEl.value = year;
    if (monthEl) monthEl.value = month;
    if (keyEl) keyEl.value = key;
    
    showMessage(`🧪 Test: ${testName} - form populated, fetching data...`);
    
    setTimeout(() => fetchWeatherData('html'), 300);
}

function runTestWithCurrentKey(year, month, testName) {
    const key = document.getElementById('key').value;
    if (!key) {
        showMessage('❌ Please enter an unscramble key first', true);
        return;
    }
    runTest(year, month, key, testName);
}

// Rainy Days Statistics Functions
function showRainyDays() {
    console.log('🌧️ Showing rainy days statistics...');
    const key = document.getElementById('key').value;
    
    if (!key) {
        showMessage('Please enter your unscramble key first', true);
        return;
    }
    
    showLoading();
    showMessage('Loading rainy days statistics...');
    
    const baseUrl = getBaseUrl();
    fetch(`${baseUrl}/api/statistics/rainy-days/chart?key=${encodeURIComponent(key)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.text();
        })
        .then(html => {
            hideLoading();
            const resultsEl = document.getElementById('weatherData');
            if (resultsEl) {
                resultsEl.innerHTML = html;
                showMessage('✅ Rainy days statistics loaded successfully!');
            }
        })
        .catch(error => {
            hideLoading();
            showMessage(`❌ Error loading rainy days: ${error.message}`, true);
            console.error('Rainy days error:', error);
        });
}

console.log('🚀 App.js loaded completely');

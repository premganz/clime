// Ultra-simple approach - immediate execution
console.log('� App.js loading...');

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
        messageEl.className = isError ? 'message error' : 'message';
    }
    console.log(isError ? '❌' : '✅', text);
}

function showLoading() {
    const loadingEl = document.getElementById('loading');
    if (loadingEl) loadingEl.classList.remove('hidden');
}

function hideLoading() {
    const loadingEl = document.getElementById('loading');
    if (loadingEl) loadingEl.classList.add('hidden');
}

// API functions
async function fetchApi(url, name) {
    showLoading();
    try {
        const response = await fetch(url);
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
        const url = `/api/weather/data?year=${year}&month=${month}&key=${encodeURIComponent(key)}&format=${format}`;
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
        const response = await fetch(`/api/weather/stats?key=${encodeURIComponent(key)}`);
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
            showMessage('❌ Please enter an unscramble key first', 'error');
            return;
        }
        runTest(year, month, key, testName);
    }

// Debug helpers
window.debugApp = () => {
    console.log('� Debug info:');
    console.log('Year dropdown:', document.getElementById('year')?.options.length);
    console.log('Test button:', document.getElementById('testJan2010'));
    console.log('Message area:', document.getElementById('message'));
};

console.log('� App.js loaded completely');

// Weather data functions
function populateYearDropdown() {
    console.log('📅 Populating year dropdown...');
    const yearSelect = document.getElementById('year');
    
    if (!yearSelect) {
        console.error('❌ Year select not found');
        return false;
    }
    
    // Clear existing options except first
    while (yearSelect.options.length > 1) {
        yearSelect.removeChild(yearSelect.lastChild);
    }
    
    // Add years 2005-2025
    for (let year = 2005; year <= 2025; year++) {
        const option = document.createElement('option');
        option.value = year;
        option.textContent = year;
        yearSelect.appendChild(option);
    }
    
    console.log(`✅ Added ${yearSelect.options.length - 1} years to dropdown`);
    return true;
}

async function fetchWeatherData(format = 'json') {
    const year = document.getElementById('year')?.value;
    const month = document.getElementById('month')?.value;
    const key = document.getElementById('key')?.value;
    
    if (!year || !month || !key) {
        showMessage('Please fill in year, month, and key', true);
        return;
    }
    
    showLoading();
    try {
        const url = `/api/weather/data?year=${year}&month=${month}&key=${encodeURIComponent(key)}&format=${format}`;
        const response = await fetch(url);
        const data = await response.text();
        hideLoading();
        
        const weatherDataEl = document.getElementById('weatherData');
        if (weatherDataEl) {
            if (format === 'html') {
                weatherDataEl.innerHTML = data;
            } else {
                weatherDataEl.innerHTML = `<pre>${data}</pre>`;
            }
        }
        showMessage(`Weather data loaded for ${year}-${month}`);
    } catch (error) {
        hideLoading();
        showMessage(`Error fetching weather data: ${error.message}`, true);
    }
}

async function getWeatherStats() {
    const key = document.getElementById('key')?.value;
    
    if (!key) {
        showMessage('Please enter the unscramble key', true);
        return;
    }
    
    showLoading();
    try {
        const url = `/api/weather/stats?key=${encodeURIComponent(key)}`;
        const response = await fetch(url);
        const data = await response.text();
        hideLoading();
        
        const weatherDataEl = document.getElementById('weatherData');
        if (weatherDataEl) {
            weatherDataEl.innerHTML = data;
        }
        showMessage('Weather statistics loaded');
    } catch (error) {
        hideLoading();
        showMessage(`Error fetching stats: ${error.message}`, true);
    }
}

function quickTest(year, month, key) {
    console.log(`🧪 Quick test: ${year}-${month} with key: ${key}`);
    
    // Set form values
    const yearEl = document.getElementById('year');
    const monthEl = document.getElementById('month');
    const keyEl = document.getElementById('key');
    
    if (yearEl) yearEl.value = year;
    if (monthEl) monthEl.value = month;
    if (keyEl) keyEl.value = key;
    
    let testName = key === 'INVALID_KEY' ? 'Invalid Key Test' : `${year}-${month}`;
    showMessage(`🧪 Running test: ${testName}`);
    
    setTimeout(() => fetchWeatherData('html'), 500);
}

function copyKeyToClipboard() {
    const keyInput = document.getElementById('key');
    if (!keyInput) return;
    
    try {
        keyInput.select();
        keyInput.setSelectionRange(0, 99999);
        navigator.clipboard.writeText(keyInput.value).then(() => {
            showMessage('🔑 Key copied to clipboard!');
        }).catch(() => {
            document.execCommand('copy');
            showMessage('🔑 Key copied to clipboard!');
        });
    } catch (err) {
        showMessage('Failed to copy key', true);
    }
}

// Event binding and initialization
function initializeApp() {
    console.log('🚀 Initializing app...');
    
    // Get all button elements
    const buttons = {
        fetchButton: document.getElementById('fetchButton'),
        healthButton: document.getElementById('healthButton'),
        fetchWeatherButton: document.getElementById('fetchWeatherButton'),
        fetchWeatherJsonButton: document.getElementById('fetchWeatherJsonButton'),
        getStatsButton: document.getElementById('getStatsButton'),
        copyKeyButton: document.getElementById('copyKeyButton'),
        testSep2005: document.getElementById('testSep2005'),
        testJan2010: document.getElementById('testJan2010'),
        testJun2025: document.getElementById('testJun2025'),
        testInvalidKey: document.getElementById('testInvalidKey')
    };
    
    // Check for missing elements
    const missing = Object.entries(buttons).filter(([name, el]) => !el).map(([name]) => name);
    if (missing.length > 0) {
        console.error('❌ Missing elements:', missing);
        return false;
    }
    
    console.log('✅ All elements found, binding events...');
    
    // Bind events
    buttons.fetchButton.onclick = () => fetchLanding();
    buttons.healthButton.onclick = () => fetchHealth();
    buttons.fetchWeatherButton.onclick = () => fetchWeatherData('html');
    buttons.fetchWeatherJsonButton.onclick = () => fetchWeatherData('json');
    buttons.getStatsButton.onclick = () => getWeatherStats();
    buttons.copyKeyButton.onclick = () => copyKeyToClipboard();
    
    // Quick test buttons
    buttons.testSep2005.onclick = () => quickTestWithCurrentKey('2005', '9');
    buttons.testJan2010.onclick = () => quickTestWithCurrentKey('2010', '1');
    buttons.testJun2025.onclick = () => quickTestWithCurrentKey('2025', '6');
    buttons.testInvalidKey.onclick = () => quickTest('2010', '1', 'INVALID_KEY');
    
    // Populate year dropdown
    populateYearDropdown();
    
    // Welcome message
    showMessage('Welcome! Click a button to interact with the Spring Boot API.');
    
    console.log('✅ App initialized successfully!');
    return true;
}

// Main Application
class App {
    constructor() {
        this.apiClient = new ApiClient();
        this.ui = new UIManager();
        this.initEventListeners();
        this.displayWelcomeMessage();
    }

    initEventListeners() {
        // Get all elements first
        const elements = {
            fetchButton: document.getElementById('fetchButton'),
            healthButton: document.getElementById('healthButton'),
            fetchWeatherButton: document.getElementById('fetchWeatherButton'),
            fetchWeatherJsonButton: document.getElementById('fetchWeatherJsonButton'),
            getStatsButton: document.getElementById('getStatsButton'),
            copyKeyButton: document.getElementById('copyKeyButton'),
            testSep2005: document.getElementById('testSep2005'),
            testJan2010: document.getElementById('testJan2010'),
            testJun2025: document.getElementById('testJun2025'),
            testInvalidKey: document.getElementById('testInvalidKey')
        };

        // Check for missing elements
        const missingElements = Object.entries(elements)
            .filter(([name, element]) => !element)
            .map(([name]) => name);

        if (missingElements.length > 0) {
            console.error('❌ Missing DOM elements:', missingElements);
            console.error('This may prevent the application from working properly');
            return;
        }

        console.log('✅ All DOM elements found, binding event listeners...');

        // Bind event listeners with error handling
        try {
            elements.fetchButton.addEventListener('click', () => this.fetchLanding());
            elements.healthButton.addEventListener('click', () => this.fetchHealth());
            elements.fetchWeatherButton.addEventListener('click', () => this.fetchWeatherData('html'));
            elements.fetchWeatherJsonButton.addEventListener('click', () => this.fetchWeatherData('json'));
            elements.getStatsButton.addEventListener('click', () => this.getWeatherStats());
            elements.copyKeyButton.addEventListener('click', () => this.copyKeyToClipboard());
            
            // Quick test button listeners
            elements.testSep2005.addEventListener('click', () => this.quickTestWithCurrentKey('2005', '9'));
            elements.testJan2010.addEventListener('click', () => this.quickTestWithCurrentKey('2010', '1'));
            elements.testJun2025.addEventListener('click', () => this.quickTestWithCurrentKey('2025', '6'));
            elements.testInvalidKey.addEventListener('click', () => this.quickTest('2010', '1', 'INVALID_KEY'));
            
            console.log('✅ All event listeners bound successfully');
        } catch (error) {
            console.error('❌ Error binding event listeners:', error);
        }
        fetchWeatherJsonButton.addEventListener('click', () => this.fetchWeatherData('json'));
        getStatsButton.addEventListener('click', () => this.getWeatherStats());
        copyKeyButton.addEventListener('click', () => this.copyKeyToClipboard());
        
        // Quick test button listeners
        testSep2005.addEventListener('click', () => this.quickTestWithCurrentKey('2005', '9'));
        testJan2010.addEventListener('click', () => this.quickTestWithCurrentKey('2010', '1'));
        testJun2025.addEventListener('click', () => this.quickTestWithCurrentKey('2025', '6'));
        testInvalidKey.addEventListener('click', () => this.quickTest('2010', '1', 'INVALID_KEY'));
        
        // Populate year dropdown - retry mechanism for better reliability
        console.log('🔄 Initializing year dropdown...');
        this.initializeYearDropdown();

        // Add keyboard support
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && e.ctrlKey) {
                this.fetchLanding();
            }
        });
        
        console.log('🚀 App initialization complete!');
    }

    initializeYearDropdown() {
        let attempts = 0;
        const maxAttempts = 10;
        
        const tryPopulate = () => {
            attempts++;
            const yearSelect = document.getElementById('year');
            
            if (yearSelect && yearSelect.offsetParent !== null) {
                // Element exists and is visible
                this.populateYearDropdown();
                console.log('Year dropdown initialized successfully on attempt', attempts);
                return true;
            } else if (attempts < maxAttempts) {
                console.warn(`Year dropdown not ready, attempt ${attempts}/${maxAttempts}`);
                setTimeout(tryPopulate, 300 * attempts); // Increasing delay
                return false;
            } else {
                console.error('Failed to initialize year dropdown after maximum attempts');
                // Last resort - try anyway
                if (yearSelect) {
                    this.populateYearDropdown();
                }
                return false;
            }
        };
        
        return tryPopulate();
    }

    displayWelcomeMessage() {
        this.ui.displayMessage('Welcome! Click a button to interact with the Spring Boot API.');
    }

    async fetchLanding() {
        this.ui.showLoading();
        
        const result = await this.apiClient.fetchData('/landing');
        
        if (result.success) {
            this.ui.displayMessage(`🎉 ${result.data}`);
        } else {
            this.ui.displayError(result.error);
        }
    }

    async fetchHealth() {
        this.ui.showLoading();
        
        const result = await this.apiClient.fetchData('/health');
        
        if (result.success) {
            this.ui.displayMessage(`💚 ${result.data}`);
        } else {
            this.ui.displayError(result.error);
        }
    }

    populateYearDropdown() {
        console.log('populateYearDropdown called');
        const yearSelect = document.getElementById('year');
        
        if (!yearSelect) {
            console.error('Year select element not found!');
            return;
        }
        
        console.log('Year select found, current options:', yearSelect.options.length);
        
        // Clear existing options except the first one (placeholder)
        const options = Array.from(yearSelect.options);
        for (let i = options.length - 1; i > 0; i--) {
            yearSelect.removeChild(options[i]);
        }
        
        console.log('Options cleared, remaining:', yearSelect.options.length);
        
        // Add years from 2005 to 2025
        for (let year = 2005; year <= 2025; year++) {
            const option = document.createElement('option');
            option.value = year;
            option.textContent = year;
            yearSelect.appendChild(option);
        }
        
        console.log(`Year dropdown populated with ${yearSelect.options.length - 1} years (2005-2025)`);
        console.log('Final options count:', yearSelect.options.length);
        
        // Force a refresh of the select element
        yearSelect.style.display = 'none';
        yearSelect.offsetHeight; // Trigger reflow
        yearSelect.style.display = '';
    }

    async fetchWeatherData(format = 'json') {
        const year = document.getElementById('year').value;
        const month = document.getElementById('month').value;
        const key = document.getElementById('key').value;
        const weatherDataEl = document.getElementById('weatherData');

        if (!year || !month || !key) {
            this.ui.displayError('Please select year, month, and enter the unscramble key');
            return;
        }

        this.ui.showLoading();
        weatherDataEl.innerHTML = '';

        try {
            const response = await fetch(`/api/weather/data?year=${year}&month=${month}&key=${encodeURIComponent(key)}&format=${format}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }

            if (format === 'html') {
                const htmlData = await response.text();
                weatherDataEl.innerHTML = htmlData;
                this.ui.displayMessage(`✅ Weather data loaded for ${year}/${month}`);
            } else {
                const jsonData = await response.json();
                weatherDataEl.innerHTML = `<div class="json-display">${JSON.stringify(jsonData, null, 2)}</div>`;
                this.ui.displayMessage(`✅ Weather data loaded: ${jsonData.recordCount} records for ${year}/${month}`);
            }

        } catch (error) {
            this.ui.displayError(error.message);
            weatherDataEl.innerHTML = '';
        }
    }

    async getWeatherStats() {
        const key = document.getElementById('key').value;
        const weatherDataEl = document.getElementById('weatherData');

        if (!key) {
            this.ui.displayError('Please enter the unscramble key');
            return;
        }

        this.ui.showLoading();
        weatherDataEl.innerHTML = '';

        try {
            const response = await fetch(`/api/weather/stats?key=${encodeURIComponent(key)}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }

            const stats = await response.json();
            
            weatherDataEl.innerHTML = `
                <div class="stats-display">
                    <div class="stat-card">
                        <h4>${stats.totalRecords}</h4>
                        <p>Total Records</p>
                    </div>
                    <div class="stat-card">
                        <h4>${stats.cleanRecords}</h4>
                        <p>Clean Records</p>
                    </div>
                    <div class="stat-card">
                        <h4>${stats.flaggedRecords}</h4>
                        <p>Flagged Records</p>
                    </div>
                    <div class="stat-card">
                        <h4>${stats.anomalyPercentage.toFixed(2)}%</h4>
                        <p>Anomaly Rate</p>
                    </div>
                </div>
            `;
            
            this.ui.displayMessage(`📊 Statistics loaded successfully`);

        } catch (error) {
            this.ui.displayError(error.message);
            weatherDataEl.innerHTML = '';
        }
    }

    async copyKeyToClipboard() {
        const keyInput = document.getElementById('key');
        try {
            await navigator.clipboard.writeText(keyInput.value);
            this.ui.displayMessage('🔑 Key copied to clipboard!');
        } catch (err) {
            // Fallback for older browsers
            keyInput.select();
            document.execCommand('copy');
            this.ui.displayMessage('🔑 Key copied to clipboard!');
        }
    }

    quickTest(year, month, key) {
        // Set the form values
        document.getElementById('year').value = year;
        document.getElementById('month').value = month;
        document.getElementById('key').value = key;
        
        // Show which test is being run
        let testName = '';
        if (key === 'INVALID_KEY') {
            testName = 'Invalid Key Test';
        } else {
            const monthNames = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 
                               'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            testName = `${monthNames[parseInt(month)]} ${year}`;
        }
        
        this.ui.displayMessage(`🧪 Running quick test: ${testName}`);
        
        // Fetch the data
        setTimeout(() => {
            this.fetchWeatherData('html');
        }, 500);
    }

    quickTestWithCurrentKey(year, month) {
        const key = document.getElementById('key').value;
        if (!key) {
            this.showStatus('❌ Please enter an unscramble key first', 'error');
            return;
        }
        this.quickTest(year, month, key);
    }
}

// Initialize the application when DOM is ready
function initializeApp() {
    console.log('🚀 Initializing Clime Application...');
    try {
        const app = new App();
        window.climeApp = app;
        console.log('✅ App initialized successfully');
        
        // Additional attempt after a short delay
        setTimeout(() => {
            console.log('🔄 Secondary initialization attempt...');
            app.initializeYearDropdown();
        }, 1000);
        
    } catch (error) {
        console.error('❌ Failed to initialize app:', error);
    }
}

// Multiple initialization strategies
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeApp);
} else if (document.readyState === 'interactive' || document.readyState === 'complete') {
    // DOM is already ready
    console.log('📱 DOM already ready, initializing immediately...');
    initializeApp();
}

// Backup initialization on window load
window.addEventListener('load', () => {
    console.log('🔄 Window loaded - backup initialization...');
    if (!window.climeApp) {
        initializeApp();
    } else {
        // Just try to fix the dropdown
        window.climeApp.initializeYearDropdown();
    }
});

// Add some fun console messages for developers
console.log(`
🚀 Clime Application Started!
📡 Using modern Fetch API instead of XMLHttpRequest (AJAX)
🎨 Modern CSS with CSS Grid and Flexbox
⚡ ES6+ JavaScript with Classes and Async/Await
🌦️ Weather Data API with Scrambling & Anomaly Detection
🔑 Unscramble Key: [Hidden for security]

Try Ctrl+Enter to trigger the landing API call!
Use the quick test buttons to test weather data functionality!

🔧 Debugging commands:
- climeApp.populateYearDropdown() - Manually populate year dropdown
- climeApp.initializeYearDropdown() - Re-initialize year dropdown with retries
`);

// Global helper functions for debugging
window.fixYearDropdown = () => {
    if (window.climeApp) {
        console.log('🔧 Manually fixing year dropdown...');
        window.climeApp.populateYearDropdown();
    } else {
        console.error('❌ App not initialized yet');
    }
};

window.debugDropdown = () => {
    const yearSelect = document.getElementById('year');
    console.log('Year select element:', yearSelect);
    console.log('Year select options:', yearSelect ? yearSelect.options.length : 'N/A');
    console.log('Year select visible:', yearSelect ? yearSelect.offsetParent !== null : 'N/A');
    if (yearSelect) {
        console.log('Current options:', Array.from(yearSelect.options).map(opt => opt.value));
    }
};

// Service Worker registration (optional - for PWA features)
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        // We could register a service worker here for offline functionality
        console.log('💡 Service Worker support detected. Ready for PWA features!');
    });
}

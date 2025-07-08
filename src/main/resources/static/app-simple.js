// Simple debugging app
console.log('App.js loading...');

// Add visible status to page
function addStatus(message, color) {
    color = color || 'blue';
    var statusDiv = document.getElementById('debug-status');
    if (!statusDiv) {
        statusDiv = document.createElement('div');
        statusDiv.id = 'debug-status';
        statusDiv.style.cssText = 'background: #e9ecef; border-left: 4px solid #007bff; padding: 10px; margin: 10px; font-family: monospace; font-size: 12px;';
        document.body.appendChild(statusDiv);
    }
    var time = new Date().toLocaleTimeString();
    statusDiv.innerHTML += '<div style="color: ' + color + ';">[' + time + '] ' + message + '</div>';
    console.log(message);
}

function showMessage(text, isError) {
    var messageEl = document.getElementById('message');
    if (messageEl) {
        messageEl.textContent = text;
        messageEl.className = isError ? 'message error' : 'message';
    }
}

function setupApp() {
    addStatus('Setting up app...');
    
    // Check year dropdown
    var yearSelect = document.getElementById('year');
    if (yearSelect) {
        addStatus('Year dropdown found', 'green');
        
        // Add years
        for (var year = 2005; year <= 2025; year++) {
            var option = document.createElement('option');
            option.value = year;
            option.textContent = year;
            yearSelect.appendChild(option);
        }
        addStatus('Added ' + (yearSelect.options.length - 1) + ' years to dropdown', 'green');
    } else {
        addStatus('Year dropdown NOT FOUND', 'red');
    }
    
    // Check test button
    var testBtn = document.getElementById('testJan2010');
    if (testBtn) {
        addStatus('Test Jan 2010 button found', 'green');
        testBtn.onclick = function() {
            addStatus('Test button clicked!', 'green');
            alert('Test button works!');
            
            // Fill form
            if (yearSelect) yearSelect.value = '2010';
            var monthSelect = document.getElementById('month');
            if (monthSelect) monthSelect.value = '1';
            var keyInput = document.getElementById('key');
            if (keyInput) keyInput.value = '';
            
            addStatus('Form auto-filled: 2010, January, key set', 'green');
        };
    } else {
        addStatus('Test Jan 2010 button NOT FOUND', 'red');
    }
    
    // Check other buttons
    var fetchBtn = document.getElementById('fetchButton');
    if (fetchBtn) {
        addStatus('Fetch button found', 'green');
        fetchBtn.onclick = function() {
            addStatus('Fetch button clicked', 'green');
            fetch('/api/landing')
                .then(function(response) { return response.text(); })
                .then(function(data) {
                    addStatus('API response: ' + data, 'green');
                    showMessage('API Response: ' + data);
                })
                .catch(function(error) {
                    addStatus('API error: ' + error.message, 'red');
                });
        };
    } else {
        addStatus('Fetch button NOT FOUND', 'red');
    }
    
    addStatus('App setup complete!', 'green');
    showMessage('App loaded - check debug info for status');
}

// Initialize
addStatus('App.js loaded, waiting for DOM...');

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupApp);
} else {
    setupApp();
}

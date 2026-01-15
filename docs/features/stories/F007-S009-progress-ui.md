# F007-S009: Add Execution Progress UI (Spinner, Timer)

## User Story

**As a** user
**I want** to see execution progress
**So that** I know the query is running

## Acceptance Criteria

- [ ] Given query submitted, then loading spinner shown
- [ ] Given execution in progress, then elapsed timer displayed
- [ ] Given execution complete, then spinner hidden
- [ ] Given execution complete, then final time shown
- [ ] Given form, then disabled during execution

## Technical Notes

### Files to Modify
- `src/main/resources/templates/queries/execute.html`
- `src/main/resources/static/js/execution.js`

### Execution Page Template
```html
<div id="execution-status" class="hidden mb-6">
    <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center gap-4">
        <div id="spinner" class="animate-spin h-6 w-6 border-4 border-blue-500 border-t-transparent rounded-full"></div>
        <div>
            <p class="font-medium text-blue-800">Executing query...</p>
            <p class="text-sm text-blue-600">Elapsed: <span id="elapsed-time">0.0</span>s</p>
        </div>
    </div>
</div>

<div id="results-container" class="hidden">
    <!-- Results will be loaded here -->
</div>
```

### JavaScript
```javascript
let startTime;
let timerInterval;

function executeQuery(form) {
    const statusDiv = document.getElementById('execution-status');
    const resultsDiv = document.getElementById('results-container');
    const submitBtn = form.querySelector('button[type="submit"]');

    // Show loading state
    statusDiv.classList.remove('hidden');
    resultsDiv.classList.add('hidden');
    submitBtn.disabled = true;

    // Start timer
    startTime = Date.now();
    timerInterval = setInterval(() => {
        const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
        document.getElementById('elapsed-time').textContent = elapsed;
    }, 100);

    // Submit form via AJAX
    const formData = new FormData(form);
    fetch(form.action, {
        method: 'POST',
        body: formData
    })
    .then(response => response.text())
    .then(html => {
        clearInterval(timerInterval);
        statusDiv.classList.add('hidden');
        resultsDiv.innerHTML = html;
        resultsDiv.classList.remove('hidden');
        submitBtn.disabled = false;
    })
    .catch(error => {
        clearInterval(timerInterval);
        statusDiv.innerHTML = `<div class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-800">
            Error: ${error.message}
        </div>`;
        submitBtn.disabled = false;
    });

    return false; // Prevent form submission
}
```

### Form Modification
```html
<form th:action="@{/queries/{id}/execute(id=${query.id})}"
      method="post"
      onsubmit="return executeQuery(this)">
    <!-- Parameter inputs -->
    <button type="submit" class="bg-rbc-blue text-white px-6 py-2 rounded-lg">
        Execute Query
    </button>
</form>
```

## Test Plan

- [ ] Visual test: Spinner shows during execution
- [ ] Visual test: Timer updates in real-time
- [ ] Integration test: Form disabled during execution

## Parent Feature

Relates to F007-select-execution

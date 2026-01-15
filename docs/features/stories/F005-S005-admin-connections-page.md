# F005-S005: Create Admin Connections List Page

## User Story

**As an** administrator
**I want** to view all configured database connections
**So that** I can monitor and test them

## Acceptance Criteria

- [ ] Given connections page, then all configured connections listed
- [ ] Given connection card, then name, type, host displayed
- [ ] Given connection card, then status indicator (connected/disconnected)
- [ ] Given connection card, then Test Connection button available
- [ ] Given Test Connection click, then async test performed
- [ ] Given admin page, then only ADMIN role can access

## Technical Notes

### Files to Create/Modify
- `src/main/java/com/ivamare/controller/AdminConnectionController.java`
- `src/main/resources/templates/admin/connections.html`

### Controller Method
```java
@GetMapping
public String listConnections(Model model) {
    model.addAttribute("pageTitle", "Database Connections");
    model.addAttribute("connections", connectionRegistry.listConnections());
    return "admin/connections";
}
```

### Template
```html
<html th:replace="~{layout/base :: layout(~{::content})}">
<div th:fragment="content">
    <h1 class="text-2xl font-bold text-rbc-blue mb-6">Database Connections</h1>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div th:each="conn : ${connections}" class="bg-white rounded-lg shadow p-6">
            <div class="flex justify-between items-start mb-4">
                <h2 th:text="${conn.name}" class="font-semibold text-lg">Connection Name</h2>
                <span th:class="${conn.connected ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}"
                      class="px-2 py-1 rounded text-xs">
                    <span th:text="${conn.connected ? 'Connected' : 'Unknown'}">Status</span>
                </span>
            </div>

            <div class="text-sm text-gray-600 space-y-1 mb-4">
                <p><strong>Type:</strong> <span th:text="${conn.type}">SQLSERVER</span></p>
                <p><strong>Host:</strong> <span th:text="${conn.host}">server.com</span></p>
                <p><strong>Database:</strong> <span th:text="${conn.database}">dbname</span></p>
            </div>

            <button onclick="testConnection(this)"
                    th:data-id="${conn.id}"
                    class="w-full bg-rbc-blue text-white py-2 rounded hover:bg-rbc-blue-light transition">
                Test Connection
            </button>
        </div>
    </div>
</div>
</html>
```

### JavaScript for Test
```javascript
async function testConnection(button) {
    const connectionId = button.dataset.id;
    button.disabled = true;
    button.textContent = 'Testing...';

    try {
        const response = await fetch(`/admin/connections/${connectionId}/test`, { method: 'POST' });
        const result = await response.json();
        button.textContent = result.success ? 'Success!' : 'Failed';
        button.className = result.success ? 'bg-green-600 ...' : 'bg-red-600 ...';
    } catch (e) {
        button.textContent = 'Error';
    }
}
```

## Test Plan

- [ ] Integration test: Connections page loads
- [ ] Integration test: All connections displayed
- [ ] Integration test: Test button triggers connection test

## Parent Feature

Relates to F005-database-connections

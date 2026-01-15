# F005-S006: Add Connection Dropdown to Query Editor

## User Story

**As an** administrator
**I want** to select a connection when creating queries
**So that** queries are associated with the correct database

## Acceptance Criteria

- [ ] Given query create/edit form, then connection dropdown present
- [ ] Given dropdown, then all configured connections listed
- [ ] Given connection selection, then saved with query
- [ ] Given query view, then connection name displayed
- [ ] Given connection dropdown, then required field

## Technical Notes

### Files to Modify
- `src/main/java/com/ivamare/controller/QueryController.java`
- `src/main/resources/templates/queries/form.html`

### Controller Model Attribute
```java
@ModelAttribute("availableConnections")
public List<ConnectionInfo> availableConnections() {
    return connectionRegistry.listConnections();
}
```

### Form Dropdown
```html
<div class="mb-4">
    <label for="connectionName" class="block text-sm font-medium text-gray-700 mb-1">
        Database Connection *
    </label>
    <select id="connectionName"
            name="connectionName"
            required
            th:field="*{connectionName}"
            class="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-rbc-blue">
        <option value="">Select a connection...</option>
        <option th:each="conn : ${availableConnections}"
                th:value="${conn.id}"
                th:text="${conn.name + ' (' + conn.type + ')'}">
            Connection Name
        </option>
    </select>
</div>
```

### Query Entity Field
```java
@Entity
public class Query {
    // ...

    @Column(name = "connection_name", nullable = false, length = 100)
    private String connectionName;
}
```

## Test Plan

- [ ] Unit test: Connections populated in model
- [ ] Integration test: Dropdown displays connections
- [ ] Integration test: Selected connection saved with query

## Parent Feature

Relates to F005-database-connections

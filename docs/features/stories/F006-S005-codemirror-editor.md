# F006-S005: Integrate CodeMirror YAML Editor

## User Story

**As an** administrator
**I want** a YAML editor with syntax highlighting
**So that** I can easily write query configurations

## Acceptance Criteria

- [ ] Given query form, then CodeMirror editor displayed
- [ ] Given YAML content, then syntax highlighting applied
- [ ] Given editor, then line numbers visible
- [ ] Given invalid YAML, then error indicators shown
- [ ] Given editor, then auto-indent on new line

## Technical Notes

### Files to Create
- `src/main/resources/templates/fragments/codemirror.html`

### CodeMirror 6 Setup
```html
<!-- Include in head -->
<script type="importmap">
{
  "imports": {
    "codemirror": "https://cdn.jsdelivr.net/npm/codemirror@6.x/dist/index.js",
    "@codemirror/lang-yaml": "https://cdn.jsdelivr.net/npm/@codemirror/lang-yaml@6.x/dist/index.js"
  }
}
</script>

<!-- Editor container -->
<div id="yaml-editor" class="border rounded-lg overflow-hidden"></div>
<textarea id="configYaml" name="configYaml" th:field="*{configYaml}" class="hidden"></textarea>

<script type="module">
import { EditorView, basicSetup } from 'codemirror';
import { yaml } from '@codemirror/lang-yaml';

const textarea = document.getElementById('configYaml');
const container = document.getElementById('yaml-editor');

const editor = new EditorView({
    doc: textarea.value,
    extensions: [
        basicSetup,
        yaml(),
        EditorView.updateListener.of(update => {
            if (update.docChanged) {
                textarea.value = update.state.doc.toString();
            }
        })
    ],
    parent: container
});
</script>
```

### Editor Styling
```css
.cm-editor {
    height: 400px;
    font-size: 14px;
}
.cm-editor .cm-scroller {
    overflow: auto;
}
```

### Initial YAML Template
```yaml
# Query Configuration
sql: |
  SELECT *
  FROM table_name
  WHERE column = :param1

parameters:
  - name: param1
    type: STRING
    label: "Parameter Label"
    required: true

# For UPDATE_WORKFLOW type:
# selectSql: |
#   SELECT id, column1, column2 FROM table WHERE condition = :param
# updateSql: |
#   UPDATE table SET column1 = :newValue WHERE id = :id
# rollbackColumns: [column1]
```

## Test Plan

- [ ] Visual test: YAML syntax highlighted
- [ ] Visual test: Line numbers visible
- [ ] Integration test: Content synced to hidden textarea

## Parent Feature

Relates to F006-query-management

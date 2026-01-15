# F004-S001: Configure Tailwind CSS with Custom Colors

## User Story

**As a** developer
**I want** Tailwind CSS configured with RBC brand colors
**So that** the UI has a consistent, professional appearance

## Acceptance Criteria

- [ ] Given Tailwind CSS, then available via CDN initially
- [ ] Given custom colors, then RBC Royal Blue (#003168) defined
- [ ] Given custom colors, then RBC Yellow (#FFD200) defined
- [ ] Given color utilities, then bg-rbc-blue, text-rbc-blue available
- [ ] Given color utilities, then bg-rbc-yellow, text-rbc-yellow available

## Technical Notes

### Files to Create
- `src/main/resources/static/css/tailwind-config.js` (inline config)

### CDN Setup (Initial)
```html
<script src="https://cdn.tailwindcss.com"></script>
<script>
tailwind.config = {
  theme: {
    extend: {
      colors: {
        'rbc-blue': '#003168',
        'rbc-blue-light': '#004d99',
        'rbc-blue-dark': '#002244',
        'rbc-yellow': '#FFD200',
        'rbc-yellow-light': '#FFE34D',
        'rbc-yellow-dark': '#CCB300',
      }
    }
  }
}
</script>
```

### Color Usage Guidelines
- **Primary**: rbc-blue for headers, navigation, buttons
- **Accent**: rbc-yellow for CTAs, highlights, active states
- **Text**: rbc-blue for headings, gray-700 for body
- **Background**: white for content, gray-100 for page

## Test Plan

- [ ] Visual test: Custom colors render correctly
- [ ] Visual test: Color contrast meets accessibility standards

## Parent Feature

Relates to F004-ui-layout

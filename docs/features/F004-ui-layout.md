# F004: UI Layout & Navigation

## Description

Create the base UI layout using Thymeleaf and Tailwind CSS with RBC-inspired color scheme. Implement the main layout template, navigation sidebar, header, and footer components.

## Acceptance Criteria

- [ ] Tailwind CSS integrated and configured
- [ ] RBC color scheme applied (Royal Blue #003168, Yellow #FFD200)
- [ ] Fixed header with logo, navigation, user info
- [ ] Collapsible sidebar navigation
- [ ] Main content area with proper spacing
- [ ] Footer with version info
- [ ] Responsive design (desktop-first)
- [ ] Navigation items: Dashboard, Queries, History, Admin (role-based)
- [ ] User dropdown with logout option

## User Stories

- [ ] F004-S001: Configure Tailwind CSS with custom colors
- [ ] F004-S002: Create base Thymeleaf layout template
- [ ] F004-S003: Build header component with user info
- [ ] F004-S004: Build sidebar navigation component
- [ ] F004-S005: Build footer component
- [ ] F004-S006: Create dashboard page (placeholder)
- [ ] F004-S007: Add role-based navigation visibility

## Technical Notes

- Use Thymeleaf fragments for reusable components
- Tailwind via CDN initially, then build process
- Color variables in tailwind.config.js
- Icons: Heroicons or similar

## Dependencies

- F003 (authentication for user info display)

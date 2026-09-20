# Login and dashboard redesign plan

## Current status
- The login screen now uses the school background image behind the full app layout instead of placing the image inside the card.
- The dashboard includes a new school-year creation option beside the dropdown.
- The next pass is focused on deleting school years and polishing the dashboard chart behavior.

## Working improvements
1. Add a delete action for school years from the dashboard filter.
2. Ensure the chart remains readable and consistent when filters change.
3. Update the login form copy so it asks for a name instead of an email address.
4. Keep the visual style aligned with the dashboard palette and the school branding.

## Next steps
- Implement a confirmation flow before deleting a selected school year.
- Remove matching records from the in-memory data when a school year is deleted.
- Improve chart axis labels, month ordering, and data zero-filling so it reads better.
- Recheck the login flow to keep naming and password labeling consistent.

# Agenda QPetiscos — Development guidelines

## Workflow and release safety
- When the owner instructs “EFETUAR COMMIT” (case-insensitive), execute the approved work and commit it to develop in the same turn. Do not respond with plans, promises or repeated approval requests instead of execution. Report the actual commit SHA, changed files, validation and any concrete blocker. Never commit to main or bypass mandatory safety checks.
- Work on develop. Never write to, commit, push, merge, deploy or release to main. Main is exclusively controlled by the owner, even after approval of a stable version.
- Read current repository files before changing them; preserve unrelated code and user changes. Prefer small, coherent commits with descriptive messages.
- Review diffs, run relevant tests and report actual results, limitations and migration/deployment steps. Do not claim tests were run when they were not.
- Beta Web and Android must be validated by the owner before stable release. Maintain existing GitHub Pages root (stable) and /beta/ (develop) separation.
- Update version metadata consistently for feature releases, including Android versionCode/versionName and visible version/changelog where applicable. Documentation-only commits need no version bump.
- No secrets, tokens, service-account keys, signing material or personal restaurant data in commits or logs. Use existing secrets and configuration.
- Do not create, edit or delete real reservations, takeaways, notes or tasks for testing. Beta uses isolated TEST Supabase and Firebase; never connect TEST clients to production. Use mocks or isolated test data/environments.
- Database migrations must be additive/backward-compatible where possible, reviewed separately, and supplied with validation and rollback guidance. Never execute production SQL or modify Edge Functions without explicit authorization. Preserve existing stable clients and notification behavior.

## Architecture
- Inspect existing structure before implementation: index.html, core/, shared/, mobile/, desktop/, assets/, android-app/, .github/. Reuse existing helpers and components rather than duplicating them.
- Android packages the web interface locally; ensure new scripts/assets are included by the native loader/build. Web updates alone do not update installed APKs.
- Preserve shared-account multi-device identity, per-device settings, Supabase synchronization and Firebase push infrastructure. Preserve autonomous widget sessions, token refresh, cached data and network failure handling.
- Keep changes minimal; avoid unrelated refactors and obsolete SQL/README artifacts. Include only necessary current migration/validation scripts.
- The menu is informational and local/offline: no product management, availability tracking or ordering. Preserve verified original names, prices, variants and descriptions.

## Design and accessibility
- Use Portuguese (Portugal), existing logo and consistent outline icons. No emoji substitutes for interface icons.
- Maintain four app themes: Diurno (#FFFFFF background, #F3EFE9 cards, #402817 text, #966039 accent); Noturno (#000000, #1C1C1C, #FFFFFF, #C49A73); Castanho claro (#966039, #80502E, #FFFFFF, #F5D5A9); Castanho escuro (#402817, #5A3D29, #FFFFFF, #E5A363).
- Preserve bottom navigation Agenda / Calendário / Novo / Eventos / Mais; logo returns to today's Agenda. Respect existing font sizing and responsive behavior.
- Cards compact, rounded, with integrated colored left stripe where appropriate; avoid overflow, clipped labels and oversized icons. Dialogs centered, dismissible outside, with accessible controls. Completed events remain in app without strikethrough, in muted style; widgets hide completed items.
- Preserve approved menu: mobile two-column category grid, web category sidebar, search and inline descriptions/price variants.
- For visual changes, provide actual rendered previews where feasible, explicitly distinguishing prototypes from implementation. Validate narrow mobile and desktop layouts and all themes.

## Approved notes/tasks roadmap
- v0.8.0-beta.3: notes notifications and task reminders. v0.8.0-beta.4: notes widget. RC then stable only after owner validation.
- Notes and tasks support importance, pinning, optional assignment and deadline, comments/replies, read confirmation, history and recycling. Preserve existing behavior.
- Android notification designs: new note, important note, new task, reminder, comment, completed task; use existing personalized notification style, expand/collapse, and deep link to relevant item.
- In-app notification center: Todas / Eventos / Notas, unread indicators, mark read, deletion of notification only, expansion and item navigation.
- Per-device preferences: new notes, important notes, new tasks, tasks assigned to me, edits, comments, completed/reopened tasks, task reminders. Preserve existing event preferences.
- Reminder options: at deadline, 5/15/30/60/120/1440 minutes before; recipients all devices, assigned device or current device. Cancel obsolete reminders on completion/deletion and reconcile edits/reopening; avoid duplicate deliveries.
- Notes widget: opaque Branco / Castanho claro / Castanho escuro themes; compact cards, Todas / Notas / Tarefas filters and counts; pinned items prioritized, pending tasks only, overdue tasks highlighted red, no comment counts or delete button. Expand descriptions, open item details, complete task, confirm important note reading, refresh and create via app. Preserve cache on connection failure. Reopened tasks reappear; confirmed notes remain visible.
- Native Android notification and widget behavior must be verified on-device; browser validation is not a substitute.

## Validation
- Check syntax/build and relevant automated tests, if available. Review responsive UI, navigation, permissions, offline behavior and regressions.
- For releases verify GitHub Actions results, Beta Web, Android APK and owner acceptance. Report exact commit and changed files.
- Do not assume production migrations or deployments succeeded. Explicitly communicate outstanding checks and blockers.

- Every delivery requesting compilation or testing must include an actionable interactive validation checklist, with pass/fail/not applicable, regression cases, exact version/commit and a way to report results. Do not mark unexecuted tests as passed.

- Each interactive validation checklist must provide an 'Aprovar todos' control for each test section, preserving individual pass/fail/not applicable controls and accurate progress counts.

## Mandatory visual acceptance gate
- Approved wireframes are binding implementation references. Before any UI feature commit, compare the actual rendered interface against the approved design: hierarchy, titles (no duplicates), all icons, labels, spacing, alignment, colors, controls, empty states and responsive layout. Do not silently substitute or omit elements.
- Use the existing Lucide icon pipeline. Every new icon key must be mapped in tools/icon-map.json and verified against the generated icon bundle; prohibit Unicode/emoji substitutes and silent fallback icons in new features.
- Service reference: tablet 12.4-inch landscape 2800×1752 (also test logical viewport 1400×876), sidebar Service directly below Agenda with concierge-bell icon, single title, compact KPI row (at most two rows), Reservations left and Takeaways right, independent scrolling, visible notes, state/action icons, collapsed completed sections, upper-right takeaway alert overlay. Service is exclusive to wide Web (viewport at least 768 CSS pixels), never native Android or narrow Web. Preserve all four themes.
- Before declaring a UI delivery ready, capture actual rendered screenshots at the target viewport, mobile and all themes, compare with the approved reference, and fix discrepancies. If rendering/build tools are unavailable, explicitly flag visual QA as blocked, do not claim visual acceptance or release readiness. Static checks do not replace screenshots. Include exact commit title, description, SHA and outstanding tests in delivery.

- Mobile navigation is strictly Agenda / Calendário / Novo / Eventos / Mais. Serviço and Relatórios are wide Web only; never add them to mobile navigation or swipe routes without explicit approval.

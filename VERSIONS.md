# LibreLog v2 Version History

## 2.0.0-SNAPSHOT (in progress)

Phase 0-6 initial cut on the `mvp` branch.

- Phase 0: Single Maven module skeleton, Postgres + Liquibase, Docker + Traefik deploy.
- Phase 1: Login, station CRUD, users CRUD (admin only), LibreTime connection screen with Test button.
- Phase 2: Read-only LibreTime library, show instances, and templates browse.
- Phase 3: Customers, orders, and spots CRUD.
- Phase 4: Day Builder UI with show instances, slot editor, spot pool, library search;
  per-day editor lock with 15-minute TTL and admin force-unlock; optimistic version on save.
- Phase 5: Push schedule day to LibreTime; status DRAFT -> PUSHED; admin reopen.
- Phase 6: Pull playback history from LibreTime; reconcile by file id within +/- 5 minute
  match window; per-order reconciliation view.

## v1 (archived)

See [v1-final](https://github.com/bilyfoster/librelog/releases/tag/v1-final) on the
[`archive/v1`](https://github.com/bilyfoster/librelog/tree/archive/v1) branch and the
[ARCHIVE.md](https://github.com/bilyfoster/librelog/blob/archive/v1/ARCHIVE.md) on that
branch.

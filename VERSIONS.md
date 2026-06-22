# LibreLog v2 Version History

## 2.1.0

End-to-end sales→air→reconcile hardening.

- Fix: push now persists each row's computed air time (`scheduledAt`) so playback
  reconciliation matches clock-built and cart/spot-resolved rows, not just preloaded ones.
- Fix: the LibreTime client follows DRF pagination (`next`) instead of reading only the
  first page — library, show instances, and playout history are no longer truncated.
- Spot production lifecycle: DRAFT → PRODUCED → APPROVED → TRAFFICKED. Only approved spots
  air; pushing an approved spot marks it TRAFFICKED; removing a spot's audio resets it to
  DRAFT. New `POST /api/spots/{id}/status` endpoint and status column in the spots table.
- Order-backed commercial carts re-sync automatically when an order's spots change.
- Cart freshness: per-cart `selectionStrategy` (ROTATION | NEWEST_FIRST) and optional
  `maxAgeHours`, backed by `CartMember.freshnessAt`. Use NEWEST_FIRST + a max age for news
  and voicetracks so stale audio never airs.
- Spots flagged SPECIFIC_SHOW now only air inside their target show (previously ignored).
- First unit tests (`TimeWindowUtil`, cart resolver) and a "How it works" starter guide plus
  refreshed guided-tour/help copy.
- DB migration `v2-010` adds the new columns and backfills existing data (existing spots →
  APPROVED, existing members' freshness → created_at).

## 2.0.0-SNAPSHOT (MVP baseline)

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

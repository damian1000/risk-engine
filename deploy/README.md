# Deploy

The live risk view runs as a systemd JVM service behind Caddy, alongside the order book on the
same box. The pipeline (`.github/workflows/deploy.yml`) builds once via `installDist`, ships the
tested artifact over SSH against a pinned host key, unpacks it into `~/releases/risk-engine/<commit>`
and moves `~/risk-engine` onto it with a symlink rename, syncs the systemd unit only on change,
restarts, and gates success on a `/readyz` 200. A release that does not come up is rolled back to
its predecessor by the same remote script; three releases are retained.

## Service

`deploy/risk-engine.service` runs the `installDist` launcher on `PORT=8081` with `-Xmx160m` — the
process is light (no Kafka, no ring buffer), sized to fit beside the order book on a 1 GB box. Port
8081 is bound to localhost; only Caddy is public.

## GitHub Actions secrets (repo settings → Secrets → Actions)

Same box as the order book, so the same three values:

- `DEPLOY_SSH_KEY` — the deploy private key
- `DEPLOY_HOST` — the box IP
- `DEPLOY_USER` — the login user (`ubuntu`)

## Host setup

A Cloudflare A record for `risk.damianhoward.com` points at the box, **DNS only / grey cloud** —
proxying it breaks Caddy's ACME challenge. The Caddy route itself needs no manual step: the host's
configuration is version-controlled and installed by a deploy, validated and backed up first. The
service adds no publicly reachable port.

## Rollback

Automatic, and decided on the box rather than by the runner. A release that does not answer
`/readyz` has the `~/risk-engine` symlink moved back onto the previous release directory and the
service restarted, so a runner that dies mid-deploy cannot leave a broken release serving. Three
releases are retained, which is what makes the previous one still there to point at.

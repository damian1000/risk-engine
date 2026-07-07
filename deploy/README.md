# Deploy

The live risk view runs as a systemd JVM service behind Caddy, alongside the order book on the
same box. The pipeline (`.github/workflows/deploy.yml`) builds once via `installDist`, ships the
tested artifact over SSH, keeps the previous release for rollback, syncs the systemd unit only on
change, restarts, and gates success on a `/healthz` 200.

## Service

`deploy/risk-engine.service` runs the `installDist` launcher on `PORT=8081` with `-Xmx160m` — the
process is light (no Kafka, no ring buffer), sized to fit beside the order book on a 1 GB box. Port
8081 is bound to localhost; only Caddy is public.

## GitHub Actions secrets (repo settings → Secrets → Actions)

Same box as the order book, so the same three values:

- `DEPLOY_SSH_KEY` — the deploy private key
- `DEPLOY_HOST` — the box IP
- `DEPLOY_USER` — the login user (`ubuntu`)

## One-time steps to go live

1. **DNS (Cloudflare).** Add an A record `risk.damianhoward.com` → `145.241.193.169`, set **DNS
   only / grey cloud** so Caddy can complete the ACME challenge (proxied/orange breaks it).

2. **Caddy route.** Append to `/etc/caddy/Caddyfile` on the box, then `sudo systemctl reload caddy`:

   ```
   risk.damianhoward.com {
       reverse_proxy localhost:8081
   }
   ```

   Ports 80/443 are already open (VCN security list + iptables, shared with the order book); no new
   port is exposed — 8081 stays localhost-only.

3. **First deploy.** Actions → **Deploy** → **Run workflow** (`workflow_dispatch`). It installs and
   enables the service, and the health check confirms `:8081/healthz`.

4. **Deploy on merge.** Once the manual run is green, add a push trigger to
   `.github/workflows/deploy.yml` so merges to `main` deploy automatically:

   ```yaml
   on:
     push:
       branches: [main]
     workflow_dispatch:
   ```

## Rollback

The previous release is kept at `~/risk-engine-prev`; restore its `bin`/`lib` and
`sudo systemctl restart risk-engine`.

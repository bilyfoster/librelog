# Tailscale Setup for log-dev.gayphx.com

## Network Configuration

Since you're using Tailscale:
- **Tailscale IP**: 100.85.223.27
- **Domain**: log-dev.gayphx.com
- **Same as**: dev-studio.gayphx.com (also uses 100.85.223.27)

## How It Works

1. **Tailscale DNS** or **hosts file** resolves `log-dev.gayphx.com` → `100.85.223.27`
2. **Traefik** (listening on port 80/443) receives the request with `Host: log-dev.gayphx.com`
3. **Traefik** routes to LibreLog containers based on Host header
4. **SSL** certificate is requested automatically via Let's Encrypt

## Setup Options

### Option 1: Tailscale MagicDNS (Recommended)
If Tailscale MagicDNS is enabled:
- Add `log-dev.gayphx.com` to Tailscale DNS settings
- Point it to: `100.85.223.27`
- All devices on Tailscale network will resolve it automatically

### Option 2: Hosts File (Same as dev-studio)
Add to your local `/etc/hosts`:
```
100.85.223.27    log-dev.gayphx.com
```

### Option 3: Check if Wildcard Works
If `*.gayphx.com` resolves to `100.85.223.27` via Tailscale, it might work automatically.

## Current Status

✅ Traefik configured for log-dev.gayphx.com
✅ Containers connected to traefik network  
✅ Routing works (tested with Host header)
⏳ Need DNS/hosts entry: log-dev.gayphx.com → 100.85.223.27

## Test After Setup

Once `log-dev.gayphx.com` resolves to `100.85.223.27`:
- https://log-dev.gayphx.com/login
- https://log-dev.gayphx.com/setup
- https://log-dev.gayphx.com/api/docs

## Verify Traefik Routing

Traefik is already configured and responding:
```bash
curl -H "Host: log-dev.gayphx.com" http://100.85.223.27
# Should redirect to https://log-dev.gayphx.com
```



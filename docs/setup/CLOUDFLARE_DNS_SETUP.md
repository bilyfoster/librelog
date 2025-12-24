# Cloudflare DNS Setup for log-dev.gayphx.com

## Current Status
- DNS is managed by Cloudflare (newt.ns.cloudflare.com)
- `dev-studio.gayphx.com` is already configured and working
- `log-dev.gayphx.com` needs to be added the same way

## Server Information
- **Public IP**: 71.36.178.185
- **Domain**: log-dev.gayphx.com

## Steps to Add log-dev.gayphx.com in Cloudflare

1. **Log into Cloudflare Dashboard**
   - Go to https://dash.cloudflare.com
   - Select the `gayphx.com` domain

2. **Add DNS Record** (same way dev-studio was added)
   - Go to **DNS** → **Records**
   - Click **Add record**
   - **Type**: A
   - **Name**: `log-dev`
   - **IPv4 address**: `71.36.178.185`
   - **Proxy status**: Same as dev-studio (probably "Proxied" if using Cloudflare CDN, or "DNS only" if direct)
   - **TTL**: Auto
   - Click **Save**

3. **Verify DNS Propagation**
   ```bash
   dig log-dev.gayphx.com +short
   # Should return: 71.36.178.185
   ```

4. **Traefik will automatically:**
   - Detect the new DNS record
   - Request Let's Encrypt SSL certificate
   - Route traffic to LibreLog containers

## Current Traefik Configuration
✅ Traefik is already configured and waiting for DNS:
- Frontend route: `Host(log-dev.gayphx.com)` → port 3000
- API route: `Host(log-dev.gayphx.com) && PathPrefix(/api)` → port 8000
- SSL: Let's Encrypt (automatic once DNS is configured)

## Testing After DNS is Added

Once DNS propagates (usually 1-5 minutes):
1. **Check DNS**: `nslookup log-dev.gayphx.com`
2. **Access**: https://log-dev.gayphx.com/login
3. **Login**: admin / admin123

## Alternative: Test Locally

If you want to test before DNS is configured, add to `/etc/hosts`:
```
71.36.178.185    log-dev.gayphx.com
```

Then access: https://log-dev.gayphx.com/login



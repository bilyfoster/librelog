# Hosts File Setup for log-dev.gayphx.com

## How dev-studio.gayphx.com Works

`dev-studio.gayphx.com` is configured via `/etc/hosts` file (on your local machine, not the server). This allows you to access it without adding a DNS record in Cloudflare.

## Add log-dev.gayphx.com to Your Local Hosts File

### On Linux/Mac:
```bash
sudo bash -c 'echo "71.36.178.185    log-dev.gayphx.com" >> /etc/hosts'
```

### On Windows:
1. Open Notepad as Administrator
2. Open file: `C:\Windows\System32\drivers\etc\hosts`
3. Add line: `71.36.178.185    log-dev.gayphx.com`
4. Save

### Verify:
```bash
ping log-dev.gayphx.com
# Should ping 71.36.178.185
```

## Access After Adding Hosts Entry

Once added to your hosts file:
- **Frontend**: https://log-dev.gayphx.com
- **Login**: https://log-dev.gayphx.com/login
- **API Docs**: https://log-dev.gayphx.com/api/docs

## Current Status

✅ Traefik configured for log-dev.gayphx.com
✅ Containers connected to traefik network
✅ SSL certificate will be requested automatically
⏳ Waiting for hosts file entry on your local machine

## Notes

- The hosts file entry is on YOUR local machine (where you browse from), not the server
- Traefik will automatically request SSL certificate once you access the domain
- This works the same way as dev-studio.gayphx.com



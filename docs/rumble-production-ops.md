# Rumble to Jazz Production Ops

## Jazz PostgreSQL Read Access

Append the Jazz server listener address in `/etc/postgresql/[version]/main/postgresql.conf`:

```text
listen_addresses = 'localhost,[Jazz_Server_IP]'
```

Allow the Rumble host in `/etc/postgresql/[version]/main/pg_hba.conf`:

```text
# TYPE  DATABASE        USER              ADDRESS                 METHOD
host    libretime       libretime_user    [Rumble_Server_IP]/32   md5
```

Restart PostgreSQL:

```bash
sudo systemctl restart postgresql
```

## Dedicated SFTP Drop Zone

```bash
sudo useradd -m rumble_bridge -s /bin/false
sudo mkdir -p /srv/libretime/rumble_import
sudo chown -R rumble_bridge:libretime /srv/libretime/rumble_import
sudo chmod 775 /srv/libretime/rumble_import
```

Install Rumble's public SSH key in `/home/rumble_bridge/.ssh/authorized_keys`.

> Prerequisite on the Rumble side: generate the keypair on the Rumble host
> (`ssh-keygen -t ed25519 -f rumble_jazz_key`) and mount the **private** key into the
> api container (volume or orchestrator secret) — never bake it into the image.

## Rumble-side Configuration

Set these env vars on the api container (see `docker-compose.yml`):

| Env var | Default | Notes |
|---|---|---|
| `JAZZ_SFTP_HOST` | _(empty)_ | Jazz host; empty disables the handoff (uploads stay local, status `UPLOADED`) |
| `JAZZ_SFTP_USER` | `rumble_bridge` | |
| `JAZZ_SFTP_PORT` | `22` | |
| `JAZZ_SFTP_KEY_PATH` | _(empty)_ | Path to the mounted private key inside the container |
| `JAZZ_IMPORT_PATH` | `/srv/libretime/rumble_import` | Drop zone from above |
| `JAZZ_IMPORT_TRIGGER` | `/usr/local/bin/rumble_import_trigger.sh` | Script installed above |

After an SFTP upload completes, Rumble executes the trigger over the same SSH
connection:

```bash
ssh -i $JAZZ_SFTP_KEY_PATH $JAZZ_SFTP_USER@$JAZZ_SFTP_HOST $JAZZ_IMPORT_TRIGGER $JAZZ_IMPORT_PATH/<file>.mp3
```

The runtime image includes ffmpeg (loudnorm → MP3 44.1kHz 192k CBR stereo); transcoded
files land in `/var/librelog/transcode` (compose volume `transcode_data`).

## Import Trigger

Deploy `ops/jazz/rumble_import_trigger.sh` to Jazz:

```bash
sudo install -o root -g root -m 0755 ops/jazz/rumble_import_trigger.sh /usr/local/bin/rumble_import_trigger.sh
```

Rumble should execute it remotely after upload:

```bash
ssh rumble_bridge@[Jazz_Server_IP] /usr/local/bin/rumble_import_trigger.sh /srv/libretime/rumble_import/example.mp3
```

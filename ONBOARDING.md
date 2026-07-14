# Twilio SMS Client — Onboarding Guide

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Frontend (Svelte 5)               │
│  ┌──────────┐ ┌──────────┐ ┌───────────────────┐   │
│  │Customer  │ │Admin     │ │Internal Chat      │   │
│  │Dashboard │ │Console   │ │(WebSocket + REST) │   │
│  └────┬─────┘ └────┬─────┘ └────────┬──────────┘   │
│       │            │                 │              │
└───────┼────────────┼─────────────────┼──────────────┘
        │            │                 │
        ▼            ▼                 ▼
┌─────────────────────────────────────────────────────┐
│              Servlets (Jakarta EE)                   │
│  /dashboard  /admin/*  /send-sms  /ws/chat          │
│  /profile    /logout   /delete-sms  /api/chat/*     │
│  /register   /webhook/sms  /admin/smpp-logs         │
└─────────┬───────────────────────────────────┬────────┘
          │                                   │
          ▼                                   ▼
┌─────────────────┐           ┌─────────────────────────┐
│  SmsRouter      │           │  UserRepository         │
│  ↓ dispatch     │           │  (JDBC → NeonDB)        │
│  TwilioProvider │           │  Flyway migrations      │
│  SmppProvider   │           └─────────────────────────┘
│  → SMSC/smscsim │
└─────────────────┘
```

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 21+ | Server runtime |
| Maven | 3.9+ | Build |
| Node | 20+ | Frontend build |
| Podman / Docker | 4+ | smscsim container |
| PostgreSQL (NeonDB) | 17 | Shared database |

## Quick Start

```bash
# 1. Clone & enter
git clone <repo>
cd Twilio-SMS-Client

# 2. Configure environment
cp .env.example .env   # edit as needed
# APP_PROFILE=local → uses LOCAL_SMPP_* vars
# APP_PROFILE=docker → uses DOCKER_SMPP_* vars

# 3. Start SMSC simulator
podman-compose up -d smscsim
# or: docker compose up -d smscsim
# Verify: curl http://localhost:12775/ → web UI

# 4. Build frontend
cd frontend && npm install && npm run build && cd ..

# 5. Start Jetty
mvn jetty:run

# 6. Open http://localhost:8080
# Login: admin / 123456
```

## Database Schema (NeonDB)

Managed via Flyway (V1–V5). All migrations are additive — never drops or alters existing columns without `IF NOT EXISTS`.

### Key Tables

**`users`** — Core user accounts
| Column | Type | Notes |
|--------|------|-------|
| id | SERIAL PK | |
| username | VARCHAR(50) UNIQUE | |
| password_hash | VARCHAR(255) | BCrypt |
| role | user_role ENUM | 'customer' or 'administrator' |
| msisdn | VARCHAR(20) | Phone number, used for MO routing |
| sms_provider | VARCHAR(10) | TWILIO / SMPP / AUTO |
| smpp_host / smpp_port / smpp_system_id / smpp_password / smpp_address_range | Various | Per-user SMPP override |
| twilio_account_sid / twilio_auth_token / twilio_sender_id | VARCHAR | Per-user Twilio override |

**`sms_history`** — All SMS records
| Column | Type | Notes |
|--------|------|-------|
| id | SERIAL PK | |
| user_id | INT FK→users | Owner |
| direction | sms_direction ENUM | 'outbound' or 'inbound' |
| from_phone / to_phone | VARCHAR | |
| message | TEXT | |
| status | message_status ENUM | 'delivered', 'failed', 'pending' |
| provider_ref_id | VARCHAR | SMPP message_id for DLR matching |
| sent_at | TIMESTAMP | Auto-set |

**`internal_messages`** — Real-time chat between users
**`system_messages`** / **`system_message_reads`** — Broadcast system

### Flyway Files

| Migration | Purpose |
|-----------|---------|
| V1__database.sql | Initial schema (users, sms_history, message_status) |
| V2__user_role.sql | Add user_role enum |
| V3__sms_provider.sql | Add sms_provider + SMPP columns to users |
| V4__internal_messages.sql | Internal chat table |
| V5__system_message_reads.sql | Broadcast read tracking |

All unsafe migrations (destructive) are banned. Schema evolves by `ADD COLUMN IF NOT EXISTS`.

## SMS Flows

### Outbound (MT)

```
SendSmsServlet.doPost()
  ↓
SmsRouter.send(recipient, message, userId)
  ↓ reads user's sms_provider from DB
  ├── TWILIO → TwilioSmsProvider.send() → Twilio API
  ├── SMPP   → SmppSmsProvider.send() → SmppSessionManager.submit() → smscsim
  └── AUTO   → try SMPP first, fallback to Twilio
  ↓
UserRepository.recordSms() ← saves to sms_history
  ↓
DLR (~2s via smscsim) → onAcceptDeliverSm(esmClass=4)
  → handleDeliveryReceipt() → UserRepository.updateSmsStatusByProviderRefId()
```

### Inbound (MO) via SMPP

```
User sends SMS to SMSC number
  ↓
smscsim receives → creates DELIVER_SM PDU
  ↓ sent to bound SMPPSession (system_id=smppclient)
onAcceptDeliverSm(esmClass=0)
  → handleInboundMessage()
  → UserRepository.findUserIdByPhone(to) ← matches users.msisdn
  → UserRepository.saveInboundSms()
```

### Inbound via Twilio Webhook

```
Twilio → POST /webhook/sms (application/x-www-form-urlencoded)
  ↓
TwilioWebhookServlet.doPost()
  → validates X-Twilio-Signature (if TWILIO_AUTH_TOKEN set)
  → findUserIdBySenderId(to) ← matches users.twilio_sender_id OR users.msisdn
  → saveInboundSms()
```

## SMS Providers

### Per-User Routing

Each user has an `sms_provider` column:
- `TWILIO` — always uses Twilio API (requires valid credentials)
- `SMPP` — always uses SMPP via smscsim (or real SMSC in production)
- `AUTO` — tries SMPP first; if it fails, falls back to Twilio

Provider config is resolved in order:
1. User-specific DB columns (`smpp_host`, `smpp_port`, etc.)
2. Environment variables (`LOCAL_SMPP_*` or `DOCKER_SMPP_*` based on `APP_PROFILE`)
3. Defaults (localhost:2776)

### smscsim (Local SMSC Simulator)

**Image**: `localhost/smscsim-fixed` (custom build, not upstream)
**Base**: ukarim/smscsim with one fix — empty `service_type` in DELIVER_SM PDU (upstream sets "smscsim" which exceeds SMPP 5-char limit, causing jsmpp to reject it).

**Build locally** (if upstream changes):
```bash
git clone https://github.com/ukarim/smscsim.git /tmp/smscsim
cd /tmp/smscsim
# Edit smsc.go: set service_type to "" in deliverSmPDU
CGO_ENABLED=0 go build -o smscsim .
podman build -t localhost/smscsim-fixed .
```

**Ports**:
| Port | Use |
|------|-----|
| 2776 | SMPP (host) → 2775 (container) |
| 12775 | Web UI for MO injection |

**Inject MO via web UI**:
```bash
curl -X POST http://localhost:12775/ \
  -d "sender=+15551234567" \
  -d "recipient=+201090702972" \
  -d "message=test" \
  -d "system_id=smppclient"
```

**DLR**: Automatically sent ~2s after SUBMIT_SM when `registered_delivery` flag is set.

## Admin Features

### Broadcast
- `POST /admin/broadcast` with JSON `{content, sendSms}`
- Sends internal chat message to all users via WebSocket
- Optionally sends as real SMS via each user's configured provider

### SMPP Logs (Admin Debug Panel)
- `GET /admin/smpp-logs` — returns last 500 SMPP events
- Events: BIND, SUBMIT, DLR, MO, ERROR
- Auto-refreshes every 3s in admin dashboard
- Click "SMPP Logs" button in header to open

### Customer CRUD
- View all customers, SMS counts
- Create / Edit / Delete customer accounts
- View per-customer SMS history (inbound + outbound)

## Internal Chat

- WebSocket endpoint: `/ws/chat` (authenticated via HTTP session)
- REST backup: `/api/chat/*`
- Real-time messaging between any two users
- System broadcasts appear in chat as system messages

## Wireshark — SMPP Packet Capture

### Installation

```bash
# CachyOS / Arch
sudo pacman -S wireshark-qt

# Add user to wireshark group for non-root capture
sudo usermod -aG wireshark $USER
# Log out and back in, or run: newgrp wireshark
```

### Capture SMPP Traffic

Since smscsim runs on localhost:2776, capture on the loopback interface:

```bash
# Option A: Start Wireshark GUI (recommended)
sudo wireshark &

# Option B: Capture to file then open
sudo tcpdump -i lo -w /tmp/smpp.pcap port 2776
# ...send some SMSes, then Ctrl+C
wireshark /tmp/smpp.pcap
```

### Filter Packets

Once in Wireshark:

| Filter | What it shows |
|--------|---------------|
| `tcp.port == 2776` | All SMPP traffic (TCP-level) |
| `smpp` | SMPP protocol packets only (requires SMPP dissector) |
| `smpp.command == 0x00000004` | Only SUBMIT_SM (outbound) |
| `smpp.command == 0x80000004` | Only SUBMIT_SM_RESP (ack) |
| `smpp.command == 0x00000005` | Only DELIVER_SM (inbound) |
| `smpp.command == 0x80000005` | Only DELIVER_SM_RESP |
| `smpp.command == 0x00000009` | Only BIND_TRX |
| `smpp.command == 0x80000009` | Only BIND_TRX_RESP |

### Steps for a Full Trace

1. Start Wireshark capture on `lo`, filter `port 2776`
2. Send an SMS from the app (`send-sms` POST)
3. Wait for DLR (~2s)
4. Inject MO via smscsim web UI
5. Stop capture
6. In Wireshark: right-click a packet → Follow → TCP Stream

You'll see the raw SMPP PDU hex for every exchange:
- `BIND_TRX` → `BIND_TRX_RESP` (session establishment)
- `SUBMIT_SM` → `SUBMIT_SM_RESP` (outbound SMS + message_id)
- `DELIVER_SM` → `DELIVER_SM_RESP` (DLR or MO)
- `ENQUIRE_LINK` → `ENQUIRE_LINK_RESP` (keepalive every 30s)

### Troubleshooting

**No SMPP dissector?** Wireshark auto-detects SMPP on port 2776. If not, force it: right-click a TCP packet → Decode As → Transport → SMPP.

**Permission denied?** Ensure user is in `wireshark` group or run as sudo.

**Only TCP packets, no SMPP?** The SMPP session must be established (bound) before any SMPP PDUs flow. If capture starts mid-session, rebind by sending an SMS.

## CLI / Podman / IntelliJ — 3 Dev Workflows

### 1. CLI (Fastest)

```bash
# Terminal 1: SMSC
podman-compose up -d smscsim

# Terminal 2: Build + run
cd frontend && npm run build && cd ..
mvn jetty:run
```

### 2. Podman (All-in-one)

```bash
podman-compose --env-file .env up -d
# Set APP_PROFILE=docker in .env for container networking
```

### 3. IntelliJ IDEA

1. Open project root
2. Run smscsim via Services panel (Docker connection, `docker-compose.yml`)
3. Run `mvn jetty:run` in terminal
4. Open built-in browser at localhost:8080

## Testing Guide

```bash
# Register (no real Twilio creds → fails gracefully)
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test","fullName":"Tester","msisdn":"+999","email":"t@t.com"}'
# Response: {"status":"error","message":"Gateway execution error"}

# Login as admin
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' -c cookies.txt

# View admin dashboard
curl -b cookies.txt http://localhost:8080/admin/dashboard

# View SMPP logs
curl -b cookies.txt http://localhost:8080/admin/smpp-logs

# Login as user (AUTO provider → SMPP)
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zkhattab","password":"kh007"}' -c cookies2.txt

# Send SMS via SMPP
curl -X POST http://localhost:8080/send-sms \
  -H "Content-Type: application/json" \
  -b cookies2.txt \
  -d '{"recipient":"+15550000001","message":"hello"}'

# Inject MO
curl -X POST http://localhost:12775/ \
  -d "sender=+15551111111" \
  -d "recipient=+201090702972" \
  -d "message=MO test" \
  -d "system_id=smppclient"

# Verify inbound in DB
PGPASSWORD=<pass> psql -h <host> -U <user> -d <db> \
  -c "SELECT direction, from_phone, message FROM sms_history ORDER BY id DESC LIMIT 3;"
```

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| SLF4J NOP logging | Jetty classloader isolates webapp from log binding | Use `slf4j-simple` (not logback-classic) |
| MO not appearing in DB | User's `msisdn` is NULL in `users` table | `UPDATE users SET msisdn='...' WHERE id=?` |
| Webhook returns 404 | User has no `twilio_sender_id` or matching `msisdn` | Set one of them in user profile |
| Cannot bind to SMSC | smscsim container not running | `podman restart smscsim-fixed` |
| "Failed to load class StaticLoggerBinder" | Jetty Maven Plugin classpath | Switch to `slf4j-simple` in pom.xml |
| SMS sends but DLR never arrives | smscsim not running or wrong port | Check `podman ps`, verify port 2776 |
| Profile update wipes fields | Old code unconditionally SET all columns | Fixed: now uses `containsKey` check |

## File Map

```
src/main/java/com/twilio/twilio_project/
├── SmppSessionManager.java    — SMPP session pool, bind/send/DLR/MO
├── SmpEventLogger.java        — Ring buffer for SMPP debug logs
├── SmsRouter.java             — Per-user provider dispatch
├── TwilioSmsProvider.java     — Twilio API wrapper
├── SmppSmsProvider.java       — SMPP provider wrapper
├── TwilioWebhookServlet.java  — Inbound SMS via Twilio callback
├── SendSmsServlet.java        — POST /send-sms
├── AdminLogServlet.java       — GET /admin/smpp-logs
├── AdminDashboardServlet.java — GET /admin/dashboard
├── BroadcastServlet.java      — POST /admin/broadcast
├── ChatWebSocket.java         — WS /ws/chat
├── UserRepository.java        — All JDBC queries
├── SpaFilter.java             — SPA routing fallback
├── AuthFilter.java            — Session auth guard
├── LogoutServlet.java         — GET /logout
└── PhoneUtil.java             — Phone normalization

frontend/src/lib/
├── AdminDashboard.svelte      — Admin console (customers, broadcast, SMPP logs)
├── CustomerDashboard.svelte   — User dashboard (SMS, internal chat, system)
├── AdminCustomerView.svelte   — Customer create/edit modal
├── InternalChat.svelte        — Real-time user-to-user chat
└── SystemConversation.svelte  — Broadcast system messages
```

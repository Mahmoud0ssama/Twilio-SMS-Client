#!/usr/bin/env bash
# Test SMPP via Restcomm from command line.
# Uses Python to connect to Restcomm as a 2nd SMPP client and send an inbound
# SMS back to our app (simulates a customer reply).
#
# Prerequisites: podman-compose up -d  (or docker-compose)
# Usage: ./scripts/test-sms.sh

set -euo pipefail

RESTCOMM_HOST="${1:-127.0.0.1}"
RESTCOMM_PORT="${2:-2776}"
SYSTEM_ID="${3:-smppclient}"
PASSWORD="${4:-password}"
DEST_PHONE="${5:-+201090702972}"   # must match a user's msisdn in the app
FROM_PHONE="${6:-+15551234567}"

echo "=== SMPP Test: Send inbound SMS via Restcomm ==="
echo "Host: $RESTCOMM_HOST:$RESTCOMM_PORT"
echo "To:   $DEST_PHONE"
echo "From: $FROM_PHONE"
echo ""

python3 << EOF
import sys, struct, time, socket

def send_pdu(sock, data):
    sock.sendall(struct.pack('>I', len(data)) + data)

def recv_pdu(sock):
    hdr = sock.recv(4)
    if len(hdr) < 4:
        return None, None
    length = struct.unpack('>I', hdr)[0]
    body = b''
    while len(body) < length - 4:
        chunk = sock.recv(length - 4 - len(body))
        if not chunk:
            return None, None
        body += chunk
    return length, body

def make_pdu(command_id, seq, body=b''):
    hdr = struct.pack('>IIII', 16 + len(body), 0x80000000 | command_id, 0, seq)
    return hdr + body

def parse_pdu(body):
    cmd = struct.unpack('>I', body[4:8])[0] & 0x7fffffff
    status = struct.unpack('>I', body[8:12])[0]
    seq = struct.unpack('>I', body[12:16])[0]
    return cmd, status, seq, body[16:]

seq = 1

# 1. Connect to Restcomm
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.settimeout(10)
sock.connect(('$RESTCOMM_HOST', $RESTCOMM_PORT))
print("1. Connected to Restcomm")

# 2. Bind as transmitter (BIND_TRX = 0x00000009)
bind_body = b'$SYSTEM_ID\x00' + b'$PASSWORD\x00' + b'\x00' * 4 + struct.pack('BB', 0x00, 0x00) + b'\x00'
pdu = make_pdu(0x00000009, seq, bind_body)
send_pdu(sock, pdu)
_, body = recv_pdu(sock)
cmd, status, _, _ = parse_pdu(body)
if status != 0:
    print(f"FAIL: Bind failed with status {status}")
    sys.exit(1)
seq += 1
print("2. Bound as transmitter (BIND_TRX)")

# 3. Send DELIVER_SM (simulate inbound SMS from phone)
# DELIVER_SM command_id = 0x00000005
msg_bytes = b'Hello from SMPP test!'
deliver_body = (
    b'\x00' * 1 +                # service_type
    b'\x01' +                    # source_addr_ton = INTERNATIONAL
    b'\x01' +                    # source_addr_npi = ISDN
    b'$FROM_PHONE\x00' +         # source_addr
    b'\x01' +                    # dest_addr_ton  
    b'\x01' +                    # dest_addr_npi
    b'$DEST_PHONE\x00' +         # dest_addr
    b'\x00' +                    # esm_class
    b'\x00' +                    # protocol_id
    b'\x00' +                    # priority_flag
    b'\x00\x00' +                # schedule_delivery_time
    b'\x00\x00' +                # validity_period
    b'\x00' +                    # registered_delivery
    b'\x00' +                    # replace_if_present_flag
    b'\x00' +                    # data_coding
    b'\x00' +                    # sm_default_msg_id
    struct.pack('B', len(msg_bytes)) +
    msg_bytes
)
pdu = make_pdu(0x00000005, seq, deliver_body)
send_pdu(sock, pdu)
_, body = recv_pdu(sock)
cmd, status, seq_recv, _ = parse_pdu(body)
if status == 0:
    print(f"3. DELIVER_SM sent OK (seq={seq_recv})")
else:
    print(f"3. DELIVER_SM failed with status {status}")
    sys.exit(1)

# 4. Unbind
seq += 1
pdu = make_pdu(0x00000006, seq)
send_pdu(sock, pdu)
_, body = recv_pdu(sock)
print("4. Unbound")

sock.close()
print("\n=== Done ===")
EOF
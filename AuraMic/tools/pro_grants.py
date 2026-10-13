#!/usr/bin/env python3
"""AuraMic free-Pro grants - admin tool (runs on YOUR computer, never in the app).

A grant code is a small payload signed with your private key (ECDSA P-256). The app only contains the public key, so
nobody can make codes from the app. Codes carry an expiry date and can be bound to one phone (its "request code",
shown in the app under Pro -> Redeem a code). Revocation: `revoke` adds the grant id to the list compiled into the
next app update (the app has no internet access, so revocation takes effect with that update; keep expiries short).

  python pro_grants.py genkey  --key ~/.auramic-grants/private_key.pem        # once; prints the public key for the app
  python pro_grants.py issue   --key ... --days 90 --note "Mom"               # any phone, 90 days
  python pro_grants.py issue   --key ... --days 365 --bind K3D7QX2M --note "Me" # only the phone showing that request code
  python pro_grants.py revoke  --id 123456789                                 # rewrites RevokedGrants.kt
  python pro_grants.py list                                                   # the ledger of issued codes

Keep private_key.pem secret and backed up (losing it only means you cannot issue new codes with this app version).
"""
import argparse, base64, csv, datetime, hashlib, os, pathlib, secrets, struct, sys
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.asymmetric.utils import decode_dss_signature, encode_dss_signature

DOMAIN = b"AURAMIC-PRO-GRANT"
HOME = pathlib.Path(os.environ.get("AURAMIC_GRANTS_HOME", pathlib.Path.home() / ".auramic-grants"))
REVOKED_KT = pathlib.Path(__file__).resolve().parent.parent / "android/app/src/main/java/com/kaafisir/auramic/data/pro/RevokedGrants.kt"


def b32(b: bytes) -> str:
    return base64.b32encode(b).decode().rstrip("=")


def bind_hash(request_code: str) -> int:
    rc = "".join(c for c in request_code.upper() if c.isalnum())
    h = struct.unpack(">I", hashlib.sha256(b"auramic-install|" + rc.encode()).digest()[:4])[0]
    return h or 1


def payload(grant_id: int, expiry_day: int, bind: int) -> bytes:
    return bytes([1]) + struct.pack(">IHI", grant_id, expiry_day, bind)


def make_code(key, grant_id: int, expiry_day: int, bind: int) -> str:
    p = payload(grant_id, expiry_day, bind)
    r, s = decode_dss_signature(key.sign(DOMAIN + p, ec.ECDSA(hashes.SHA256())))
    raw = b32(p + r.to_bytes(32, "big") + s.to_bytes(32, "big"))
    return "AURA-" + "-".join(raw[i:i + 5] for i in range(0, len(raw), 5))


def load_key(path):
    return serialization.load_pem_private_key(pathlib.Path(path).expanduser().read_bytes(), password=None)


def public_b64(key) -> str:
    return base64.b64encode(key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)).decode()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    g = sub.add_parser("genkey"); g.add_argument("--key", required=True)
    i = sub.add_parser("issue"); i.add_argument("--key", required=True); i.add_argument("--days", type=int, required=True)
    i.add_argument("--bind", default=""); i.add_argument("--note", default="")
    r = sub.add_parser("revoke"); r.add_argument("--id", type=int, required=True)
    sub.add_parser("list")
    a = ap.parse_args()
    HOME.mkdir(parents=True, exist_ok=True)
    ledger = HOME / "ledger.csv"

    if a.cmd == "genkey":
        path = pathlib.Path(a.key).expanduser()
        if path.exists(): sys.exit(f"{path} exists - refusing to overwrite a private key")
        path.parent.mkdir(parents=True, exist_ok=True)
        key = ec.generate_private_key(ec.SECP256R1())
        path.write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
        os.chmod(path, 0o600)
        print("private key:", path)
        print("PUBLIC_KEY (put in ProGrants.kt):", public_b64(key))
    elif a.cmd == "issue":
        if not 1 <= a.days <= 3650: sys.exit("--days must be 1..3650")
        key = load_key(a.key)
        grant_id = secrets.randbelow(2**32 - 1) + 1
        expiry = datetime.date.today() + datetime.timedelta(days=a.days)
        expiry_day = (expiry - datetime.date(1970, 1, 1)).days
        bind = bind_hash(a.bind) if a.bind else 0
        code = make_code(key, grant_id, expiry_day, bind)
        new = not ledger.exists()
        with ledger.open("a", newline="") as f:
            w = csv.writer(f)
            if new: w.writerow(["issued", "grant_id", "expires", "bound_request_code", "note"])
            w.writerow([datetime.date.today().isoformat(), grant_id, expiry.isoformat(), a.bind, a.note])
        print(f"grant id {grant_id}, valid until {expiry.isoformat()} (end of day, UTC), {'bound to ' + a.bind if a.bind else 'any phone'}")
        print(code)
    elif a.cmd == "revoke":
        ids = set()
        if REVOKED_KT.exists():
            import re
            ids = {int(x) for x in re.findall(r"(\d+)L", REVOKED_KT.read_text())}
        ids.add(a.id)
        body = ", ".join(f"{x}L" for x in sorted(ids))
        REVOKED_KT.write_text(
            "package com.kaafisir.auramic.data.pro\n\n"
            "/** Grant ids revoked by the admin tool (tools/pro_grants.py revoke). Takes effect with the next app update. */\n"
            f"object RevokedGrants {{\n    val IDS: Set<Long> = setOf({body})\n}}\n")
        print(f"revoked {a.id}; rebuild and publish the app for it to take effect")
    elif a.cmd == "list":
        print(ledger.read_text() if ledger.exists() else "no codes issued yet")


if __name__ == "__main__":
    main()

"""Deterministic signing key for qwgram Next.

The key is derived from the TG_APP_ID/TG_APP_HASH repository secrets, so every CI build is signed
with the same key (updates install over each other) while nothing secret is stored in the repo.
Writes next/keystore.p12 (password: qwgram, alias: qwgram).
"""
import hashlib, os, random, subprocess, sys, datetime
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa

app_id = os.environ.get("TG_APP_ID", "")
app_hash = os.environ.get("TG_APP_HASH", "")
if not app_hash:
    sys.exit("TG_APP_HASH is empty")
seed = int.from_bytes(hashlib.sha256(f"qwgram-next-sign:{app_id}:{app_hash}".encode()).digest(), "big")
rng = random.Random(seed)

def is_probable_prime(n, k=40):
    if n < 2:
        return False
    for p in (2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37):
        if n % p == 0:
            return n == p
    d, s = n - 1, 0
    while d % 2 == 0:
        d //= 2
        s += 1
    for _ in range(k):
        a = rng.randrange(2, n - 1)
        x = pow(a, d, n)
        if x in (1, n - 1):
            continue
        for _ in range(s - 1):
            x = pow(x, 2, n)
            if x == n - 1:
                break
        else:
            return False
    return True

def prime(bits):
    while True:
        c = rng.getrandbits(bits) | (1 << (bits - 1)) | (1 << (bits - 2)) | 1
        if c % 65537 != 1 and is_probable_prime(c):
            return c

e = 65537
p = prime(1024)
q = prime(1024)
while q == p:
    q = prime(1024)
n = p * q
phi = (p - 1) * (q - 1)
d = pow(e, -1, phi)
priv = rsa.RSAPrivateNumbers(p, q, d, d % (p - 1), d % (q - 1), pow(q, -1, p), rsa.RSAPublicNumbers(e, n)).private_key()
name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "qwgram"), x509.NameAttribute(NameOID.ORGANIZATION_NAME, "qwgram")])
cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(priv.public_key())
        .serial_number(seed % (1 << 63) or 1)
        .not_valid_before(datetime.datetime(2026, 1, 1))
        .not_valid_after(datetime.datetime(2076, 1, 1))
        .sign(priv, hashes.SHA256()))
out = os.path.join(os.path.dirname(__file__), "..")
key_pem = os.path.join(out, "sign-key.pem")
cert_pem = os.path.join(out, "sign-cert.pem")
with open(key_pem, "wb") as f:
    f.write(priv.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
with open(cert_pem, "wb") as f:
    f.write(cert.public_bytes(serialization.Encoding.PEM))
subprocess.check_call(["openssl", "pkcs12", "-export", "-inkey", key_pem, "-in", cert_pem, "-name", "qwgram",
                       "-passout", "pass:qwgram", "-out", os.path.join(out, "keystore.p12")])
os.remove(key_pem)
print("keystore ready, cert sha256:", cert.fingerprint(hashes.SHA256()).hex()[:16])

* To test WebTransport locally with self-signed certificates (for example, using Chrome's --ignore-certificate-errors-spki-list flag), you must generate an ECDSA certificate valid for 14 days or less—browsers strictly reject WebTransport self-signed certificates that use RSA or exceed 14 days.

* Generate ECDSA Certificate & Private Key (Valid for 14 Days)

Run this single OpenSSL command in the Linux terminal:

```bash
# 1. Generate a 10-day ECDSA certificate for localhost
openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 \
  -keyout key.pem -out cert.pem -days 10 -nodes \
  -subj "/CN=localhost" -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

# 2. Print the Base64 SHA-256 hash to pass into your TypeScript client
openssl x509 -in cert.pem -outform der | openssl dgst -sha256 -binary | base64
```

. Get the SPKI Hash (Required for Browser Testing)

For Chrome/Chromium without a trusted CA, compute the base64 SHA-256 hash of the certificate's public key:

```bash
# 1. Get the SPKI fingerprint
SPKI=$(openssl x509 -in cert.pem -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64)

# 2. Launch Chrome ignoring certificate errors for that specific key
google-chrome --origin-to-force-quic-on=localhost:8080 --ignore-certificate-errors-spki-list=$SPKI```
```

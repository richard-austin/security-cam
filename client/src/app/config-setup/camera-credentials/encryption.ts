export class Encryption {
  readonly publicKey: Uint8Array;
  constructor(publicKey: Uint8Array) {
    this.publicKey = publicKey;
  }
  async encrypt(strToEncrypt: string) {
    //import key to encrypt with RSA-OAEP
    const key: CryptoKey = await crypto.subtle.importKey(
      "spki",
      this.publicKey,
      {name: "RSA-OAEP", hash: {name: "SHA-256"}},
      false,
      ["encrypt"]);
    return await this.encryptMessage(key, strToEncrypt)
  }

  private async encryptMessage(publicKey: CryptoKey, strToEncrypt: string) {
    const enc = new TextEncoder();
    const encoded = enc.encode(strToEncrypt);
    const result = await window.crypto.subtle.encrypt(
      {
        name: "RSA-OAEP",
      },
      publicKey,
      encoded
    );

    let binary = '';
    const bytes = new Uint8Array(result);
    let len = bytes.byteLength;
    for (let i = 0; i < len; i++)
      binary += String.fromCharCode(bytes[i]);
    return window.btoa(binary);
  }
}

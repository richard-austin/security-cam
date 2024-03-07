export class Encryption {
  async encrypt(strToEncrypt: string) {
    const publicKeyB64 = `
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnMpUfwvCtT1lQUchQJL1pN1AYXCYjEtO
aOdEK662ch7thOvMiiysLDA3I66sLfCgzfCSUROIafY12m4ROSPLobicmHz9e5eravGrMlJZW2lv
vgj1lvS/4cBUYSr6OHi+PAbzH6r7xxPosMHNNzeJKWnl2ane01xkmqDqF9F79mSuVuQrNSNwQ3ej
JLWnLb/USm4zsNTB8QLZPKL7KIviC3h+oOFenrzWjMY8emcSz/BBVcGGyxVL+vCKrC91CYGMStoS
TBACb7aC3drsDik9XpoX5ZZaePAzJO13KKEjl7yOFjSenRuwQ3aWsdEeCQI1nPA+52zq7GT38Mpp
KT5t7QIDAQAB`;
    //Convert the public key in base 64 (DER encoded) to array buffer
    const publicKeyAB = this.str2ab(atob(publicKeyB64.replace('/\s/g', '')));

    //import key to encrypt with RSA-OAEP
    const key: CryptoKey = await crypto.subtle.importKey(
      "spki",
      publicKeyAB,
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

  private str2ab(str: string) {
    const arrBuff = new ArrayBuffer(str.length);
    const bytes = new Uint8Array(arrBuff);
    for (let i = 0; i < str.length; i++) {
      bytes[i] = str.charCodeAt(i);
    }
    return bytes;
  }
}

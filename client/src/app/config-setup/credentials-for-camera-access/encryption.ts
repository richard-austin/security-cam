export class Encryption {
  async encrypt(strToEncrypt: string) {
    const publicKeyB64 = `
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnQvG/Rdc8IVkr1ys6/xOwm8V6os50IWh
+q+BHEc2ZN43twtqITLk/kk09KUDdpxwSd44i2YjC7NW4sWTO+mF8Cbecu+FX7Fd6BdvP8bOHuq0
Z9xisfPcjeYFQ1QpDj0xM/of7q9tAeDH5GiQOV+Y8mV5l/bzeqfUEolAI+usudhUUmB0vZQ7JE83
gG9Nb661FRNvRZHP3MCalgWu4IBgqemPtE17KeV+JN5yLH7cl/reSoQSzkfkrzmWoTpJhfAm9GfG
Wg0619xsqy8KcE+LH+faY3B9CioVd9vdUVOz/dsKkA7Z5s1uagMSqG3fimfFSjvpA0b5ctlJToYv
ueVCnQIDAQAB`;
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

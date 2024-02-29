export class Encryption {
  async encrypt(strToEncrypt: string) {
    const publicKeyB64 = `MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAk9jVbHCjk0/dfSHgJ1MKZEMwTQASHgFf/11DrBBGb8xinUazwCefee2vy1SuMchVTpp1HitX5onJPejBBnbgYQKuXMzzY8HK16Ou9sdIIfiiPCZF/h+qWMRRWck2c3my84HpiL80fnr1U/5I5r0CLp+eblhwufpp+u0bFgTmJdhDggHzg2tBPBWkdpeTgBVg9mfwgs1IOQWmpuZvBw9l+aVyyJFz8ZFqAlejG17BvsKHv1IU/Akp9eOcXEnBJL8ZdDfvNXVfFhbdWhkOl1sJI4BEp3f18uC0jepOLiou1gVE3DIiErRBIhX5hMYRKEHnEJ/xk4yeDcqWt3dgvgjj68saMj51TBk4BwuBEVKTVwBaEwSQGI4FUih1gFPUfgNKeVGfoWX8MLHNqJhtCQ/JD+L/v/vW9K3Xxc5tmxAsrWo0GV+b02wpyCyNRLSNfk6UuPShrO28GaDXIQYgIJvFgTwD55u/PKKhdnXBTQsa/Id6K2AigsIskMiyUDvqO31ftP9cqD2HwEmHkMpTB/B6I6f32OYXDKfdJ4hznzmBDt03FlXXQCPKjXwrtg4jGT/vCBP1uM3xdX6I7mS18lyRJNUjJUhBkSRma6815PAvQXTf2GAPNiLyUcTZuqJVeYQNLQeMOu0eWRQt9R/UcR/bQ45dhSv1UEHERd9tlp0QqRcCAwEAAQ==`;
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

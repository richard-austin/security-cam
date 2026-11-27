export class UniStreamWebTransportClient {
  private transport: WebTransport | null = null;

  constructor(
    private url: string,
    private onMessage: (data: Uint8Array) => void
  ) {}

  async connect(certHashBase64?: string): Promise<void> {
    const options: WebTransportOptions = {};
    if (certHashBase64) {
      const binaryHash = Uint8Array.from(atob(certHashBase64), (c) => c.charCodeAt(0));
      options.serverCertificateHashes = [{ algorithm: 'sha-256', value: binaryHash }];
    }

    this.transport = new WebTransport(this.url, options);
    await this.transport.ready;
    this.readIncomingUniStreams();
  }

  /**
   * Matches Go's AcceptUniStream + io.ReadAll:
   * Opens a unidirectional stream, writes raw binary, and closes to signal message end.
   */
  async sendMessage(payload: Uint8Array): Promise<void> {
    if (!this.transport) throw new Error('Not connected');
    const stream = await this.transport.createUnidirectionalStream();
    const writer = stream.getWriter();
    await writer.write(payload);
    await writer.close(); // Triggers io.ReadAll completion on Go server
  }

  /**
   * Matches Go's sendPayloadOverUniStream:
   * Reads each incoming unidirectional stream until QUIC FIN, then emits the full frame.
   */
  private async readIncomingUniStreams(): Promise<void> {
    if (!this.transport) return;
    const reader = this.transport.incomingUnidirectionalStreams.getReader();

    try {
      while (true) {
        const { value: stream, done } = await reader.read();
        if (done) break;
        this.consumeUniStream(stream).catch(console.error);
      }
    } finally {
      reader.releaseLock();
    }
  }

  private async consumeUniStream(stream: ReadableStream<Uint8Array>): Promise<void> {
    const reader = stream.getReader();
    const chunks: Uint8Array[] = [];
    let totalLength = 0;

    try {
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        chunks.push(value);
        totalLength += value.byteLength;
      }

      const fullMessage = new Uint8Array(totalLength);
      let offset = 0;
      for (const chunk of chunks) {
        fullMessage.set(chunk, offset);
        offset += chunk.byteLength;
      }
      this.onMessage(fullMessage);
    } finally {
      reader.releaseLock();
    }
  }

  close(): void {
    this.transport?.close();
  }
}

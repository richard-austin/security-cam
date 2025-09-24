export class AudioSettings {
  level: number;
  mute: boolean;
  audioLatencyLimiting: boolean;

  constructor(level: number, mute: boolean, audioLatencyLimiting: boolean) {
    this.level = level;
    this.mute = mute;
    this.audioLatencyLimiting = audioLatencyLimiting;
  }
}

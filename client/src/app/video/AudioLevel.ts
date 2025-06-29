export class AudioLevel {
  level: number;
  mute: boolean;

  constructor(level: number, mute: boolean) {
    this.level = level;
    this.mute = mute;
  }
}

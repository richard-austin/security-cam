import {
  AfterViewInit,
  Component,
  model, ModelSignal,
  output,
  OutputEmitterRef
} from '@angular/core';
import {MatCard} from "@angular/material/card";
import {MatSlider, MatSliderThumb} from "@angular/material/slider";
import {FormsModule} from "@angular/forms";
import {MatIcon} from "@angular/material/icon";
import {MatIconButton} from "@angular/material/button";
import {MatCheckbox, MatCheckboxChange} from "@angular/material/checkbox";
import {NgStyle} from "@angular/common";
import {MatTooltip} from "@angular/material/tooltip";
import {UtilsService} from "../../shared/utils.service";

@Component({
  selector: 'app-audio-control',
  imports: [
    MatCard,
    MatIcon,
    MatSlider,
    FormsModule,
    MatSliderThumb,
    MatIconButton,
    MatCheckbox,
    NgStyle,
    MatTooltip
  ],
  templateUrl: './audio-control.component.html',
  styleUrl: './audio-control.component.scss'
})
class AudioControlComponent implements AfterViewInit {
  muteAudio: OutputEmitterRef<boolean> = output<boolean>()
  setLevel = output<number>();
  setAudioLatencyLimiting: OutputEmitterRef<boolean> =output<boolean>();
  mute: ModelSignal<boolean> = model<boolean>(false);
  level: ModelSignal<number> = model.required<number>();
  audioLatencyLimiting: ModelSignal<boolean> = model<boolean>(false);
  lastLevel!: number;

  toggleMuteAudio() {
    this.mute.set(!this.mute());
    if (this.mute()) {
      this.lastLevel = this.level();
      this.level.set(0);
    } else
      this.level.set(this.lastLevel);
    this.muteAudio.emit(this.mute());
  }

  setVolume() {
    this.setLevel.emit(this.level());
  }

  ngAfterViewInit() {
    this.lastLevel = this.level();
  }

  zeroTo100(value: number) {
    return `${Math.round(value * 100)}`;
  }

  setLatencyLimiting($event: MatCheckboxChange) {
    this.audioLatencyLimiting.set($event.checked);
    this.setAudioLatencyLimiting.emit($event.checked);
  }

  protected readonly UtilsService = UtilsService;
}

export default AudioControlComponent

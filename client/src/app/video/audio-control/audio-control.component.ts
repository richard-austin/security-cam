import {
  AfterViewInit,
  Component, input, InputSignal, OnInit,
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
class AudioControlComponent implements OnInit, AfterViewInit {
  muteAudio: OutputEmitterRef<boolean> = output<boolean>()
  setLevel = output<number>();
  setAudioLatencyLimiting: OutputEmitterRef<boolean> =output<boolean>();
  initialMuteState: InputSignal<boolean> = input<boolean>(false);
  initialLevel: InputSignal<number> = input.required<number>();
  audioLatencyLimiting: InputSignal<boolean | undefined> = input<boolean | undefined>(undefined);
  lastLevel!: number;

  mute!: boolean;
  level!: number;

  toggleMuteAudio() {
    this.mute = !this.mute;
    if (this.mute) {
      this.lastLevel = this.level;
      this.level = 0;
    } else
      this.level = this.lastLevel;
    this.muteAudio.emit(this.mute);
  }

  setVolume() {
    this.setLevel.emit(this.level);
  }

  zeroTo100(value: number) {
    return `${Math.round(value * 100)}`;
  }

  setLatencyLimiting($event: MatCheckboxChange) {
    this.setAudioLatencyLimiting.emit($event.checked);
  }

  protected readonly UtilsService = UtilsService;

  ngOnInit(): void {
    this.mute = this.initialMuteState();
    this.lastLevel = this.initialLevel();
    this.level = this.mute ? 0 : this.lastLevel;
  }

  ngAfterViewInit() {
  }
}

export default AudioControlComponent

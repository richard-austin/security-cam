import {AfterViewInit, Component, EventEmitter, Input, Output} from '@angular/core';
import {MatCard} from "@angular/material/card";
import {MatSlider, MatSliderThumb} from "@angular/material/slider";
import {FormsModule} from "@angular/forms";
import {MatIcon} from "@angular/material/icon";
import {MatIconButton} from "@angular/material/button";
import {MatCheckbox, MatCheckboxChange} from "@angular/material/checkbox";
import {NgStyle} from "@angular/common";
import {MatTooltip} from "@angular/material/tooltip";

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
  @Output() muteAudio = new EventEmitter<boolean>();
  @Output() setLevel = new EventEmitter<number>();
  @Output() setAudioLatencyLimiting = new EventEmitter<boolean>();
  @Input() mute: boolean = false;
  @Input() level!: number;
  @Input() audioLatencyLimiting!: boolean;
  lastLevel!: number;

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

  ngAfterViewInit() {
    this.lastLevel = this.level;
  }

  zeroTo100(value: number) {
    return `${Math.round(value * 100)}`;
  }

  setLatencyLimiting($event: MatCheckboxChange) {
    this.audioLatencyLimiting = $event.checked;
    this.setAudioLatencyLimiting.emit($event.checked);
  }
}

export default AudioControlComponent

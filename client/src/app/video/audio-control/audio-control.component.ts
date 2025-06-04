import {AfterViewInit, Component, EventEmitter, Input, Output} from '@angular/core';
import {MatCard} from "@angular/material/card";
import {MatSlider, MatSliderThumb} from "@angular/material/slider";
import {FormsModule} from "@angular/forms";
import {MatIcon} from "@angular/material/icon";

@Component({
  selector: 'app-audio-control',
  imports: [
    MatCard,
    MatIcon,
    MatSlider,
    FormsModule,
    MatSliderThumb
  ],
  templateUrl: './audio-control.component.html',
  styleUrl: './audio-control.component.scss'
})
export class AudioControlComponent implements AfterViewInit {
  @Output() muteAudio = new EventEmitter<boolean>();
  @Output() setLevel= new EventEmitter<number>();
  @Input() mute: boolean = false;
  @Input() level!: number;
  lastLevel!: number;

  toggleMuteAudio() {
    this.mute = !this.mute;
    if (this.mute) {
      this.lastLevel = this.level;
      this.level = 0;
    }
    else
      this.level = this.lastLevel;
    this.muteAudio.emit(this.mute);
  }

  setVolume($event: Event) {
    this.level = ($event.target as HTMLInputElement).valueAsNumber;
    this.setLevel.emit(this.level);
  }

  ngAfterViewInit() {
    this.lastLevel = this.level;
  }

  zeroTo100(value: number) {
    return `${Math.round(value * 100)}`;
  }
}

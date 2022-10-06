import { Input } from '@angular/core';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-preset-button',
  templateUrl: './preset-button.component.html',
  styleUrls: ['./preset-button.component.scss']
})
export class PresetButtonComponent implements OnInit {
  @Input() presetId!: string;
  constructor() { }

  ngOnInit(): void {
  }

  presetPressed() {

  }
}

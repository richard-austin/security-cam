import {Component, Input, OnInit} from '@angular/core';

export enum eMoveDirections {tiltUp, tiltDown, panLeft, panRight}

@Component({
  selector: 'app-ptzbutton',
  templateUrl: './ptzbutton.component.html',
  styleUrls: ['./ptzbutton.component.scss']
})
export class PTZButtonComponent implements OnInit {
  @Input() matIcon!: string;
  @Input() ptzBtnMatTooltip: string = "";
  @Input() moveDirection!: eMoveDirections;
  constructor() { }

  ngOnInit(): void {
  }

  move() {
  }

  stop() {
  }
}

import { Component, OnInit } from '@angular/core';
import { eMoveDirections } from './ptzbutton/ptzbutton.component';

@Component({
  selector: 'app-ptzcontrols',
  templateUrl: './ptzcontrols.component.html',
  styleUrls: ['./ptzcontrols.component.scss']
})
export class PTZControlsComponent implements OnInit {
  eMoveDirections: any = eMoveDirections;
  constructor() { }

  ngOnInit(): void {
  }
}

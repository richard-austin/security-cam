import {Component, Input, OnInit} from '@angular/core';
import {Camera} from 'src/app/cameras/Camera';
import {ReportingComponent} from 'src/app/reporting/reporting.component';
import {PTZMove, PTZService, PTZStop} from '../../ptz.service';

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
  @Input() camera!: Camera | null;
  @Input() reporting!: ReportingComponent;

  constructor(private ptz: PTZService) {
  }

  move() {
    let ptz: PTZMove = new PTZMove(this.moveDirection, this.camera?.onvifHost as string);
    this.ptz.move(ptz).subscribe(() => {
      },
      reason => {
        this.reporting.errorMessage = reason;
      });
  }

  stop() {
    let ptz: PTZStop = new PTZStop(this.camera?.onvifHost as string)
    this.ptz.stop(ptz).subscribe(() => {
      },
      reason => {
        this.reporting.errorMessage = reason;
      })
  }

  ngOnInit(): void {
  }
}

import {Component, Input, OnInit} from '@angular/core';
import {Camera} from 'src/app/cameras/Camera';
import {ReportingComponent} from 'src/app/reporting/reporting.component';
import { UtilsService } from 'src/app/shared/utils.service';
import {PTZMove, PTZService, PTZStop} from '../../ptz.service';

export enum eMoveDirections {tiltUp, tiltDown, panLeft, panRight, zoomIn, zoomOut}

@Component({
    selector: 'app-ptzbutton',
    templateUrl: './ptzbutton.component.html',
    styleUrls: ['./ptzbutton.component.scss'],
    standalone: false
})
export class PTZButtonComponent implements OnInit {
  @Input() matIcon!: string;
  @Input() ptzBtnMatTooltip: string = "";
  @Input() moveDirection!: eMoveDirections;
  @Input() camera!: Camera;
  @Input() reporting!: ReportingComponent;
  @Input() scale: number = 2;
  isGuest: boolean = true;

  constructor(private ptz: PTZService, private utils: UtilsService) {
  }

  move() {
    let ptz: PTZMove = new PTZMove(this.moveDirection, this.camera);
    this.ptz.move(ptz).subscribe(() => {
      },
      reason => {
        this.reporting.errorMessage = reason;
      });
  }

  stop() {
    let ptz: PTZStop = new PTZStop(this.camera)
    this.ptz.stop(ptz).subscribe(() => {
      },
      reason => {
        this.reporting.errorMessage = reason;
      })
  }

  ngOnInit(): void {
    this.isGuest = this.utils.isGuestAccount;
  }
}

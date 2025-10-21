import {Component, Input, OnInit} from '@angular/core';
import {Camera} from 'src/app/cameras/Camera';
import {ReportingComponent} from 'src/app/reporting/reporting.component';
import { UtilsService } from 'src/app/shared/utils.service';
import {PTZMove, PTZService, PTZStop} from '../../ptz.service';
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {MatIconButton} from "@angular/material/button";

export enum eMoveDirections {tiltUp, tiltDown, panLeft, panRight, zoomIn, zoomOut}

@Component({
  selector: 'app-ptzbutton',
  templateUrl: './ptzbutton.component.html',
  imports: [
    MatTooltip,
    MatIcon,
    MatIconButton
  ],
  styleUrls: ['./ptzbutton.component.scss']
})
export class PTZButtonComponent implements OnInit {
  @Input() matIcon!: string;
  @Input() ptzBtnMatTooltip: string = "";
  @Input() moveDirection!: eMoveDirections;
  @Input() camera!: Camera;
  @Input() reporting!: ReportingComponent;
  @Input() scale: number = 2;
  isGuest: boolean = true;
  mouseDown: boolean = false;
  constructor(private ptz: PTZService, private utils: UtilsService) {
  }

  move($event: MouseEvent | TouchEvent) {
    if($event.type === 'touchstart') {
      $event.preventDefault();
    }
    if(($event.type === "mousedown" && (($event as MouseEvent).buttons & 1) === 1) || $event.type === "touchstart") {
      let ptz: PTZMove = new PTZMove(this.moveDirection, this.camera);
      this.mouseDown = true;
      this.ptz.move(ptz).subscribe(() => {
        },
        reason => {
          this.reporting.errorMessage = reason;
        });
    }
  }

  stop($event?: MouseEvent | TouchEvent) {
    if($event === undefined || ($event.type === "mouseup" && (($event as MouseEvent).buttons & 1) === 0) || $event.type === "touchend") {
      this.mouseDown = false;
      let ptz: PTZStop = new PTZStop(this.camera)
      this.ptz.stop(ptz).subscribe(() => {
        },
        reason => {
          this.reporting.errorMessage = reason;
        })
    }
  }

  // Stop the movement if the mouse is slid outside the button with the mouse button still down
  stopIfMouseDown() {
    if(this.mouseDown) {
      this.stop();
    }
  }

  ngOnInit(): void {
    this.isGuest = this.utils.isGuestAccount;
  }

  protected readonly UtilsService = UtilsService;
}

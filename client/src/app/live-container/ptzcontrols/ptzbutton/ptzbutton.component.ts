import {Component, input, InputSignal, OnInit} from '@angular/core';
import {Camera} from 'src/app/cameras/Camera';
import {ReportingComponent} from 'src/app/reporting/reporting.component';
import {UtilsService} from 'src/app/shared/utils.service';
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
  matIcon: InputSignal<string> = input.required<string>();
  ptzBtnMatTooltip: InputSignal<string> = input<string>("");
  moveDirection: InputSignal<eMoveDirections> = input.required<eMoveDirections>();
  camera: InputSignal<Camera> = input.required<Camera>();
  reporting: InputSignal<ReportingComponent> = input.required<ReportingComponent>();
  scale: InputSignal<number> = input<number>(2);

  isGuest: boolean = true;
  mouseDown: boolean = false;

  constructor(private ptz: PTZService, private utils: UtilsService) {
  }

  move($event: MouseEvent | TouchEvent) {
    if ($event.type === 'touchstart') {
      $event.preventDefault();
    }
    if (($event.type === "mousedown" && (($event as MouseEvent).buttons & 1) === 1) || $event.type === "touchstart") {
      let ptz: PTZMove = new PTZMove(this.moveDirection(), this.camera());
      this.mouseDown = true;
      this.ptz.move(ptz).subscribe({
        next: () => {
        },
        error: reason => {
          this.reporting().errorMessage = reason;
        }
      });
    }
  }

  stop($event?: MouseEvent | TouchEvent) {
    if ($event === undefined || ($event.type === "mouseup" && (($event as MouseEvent).buttons & 1) === 0) || $event.type === "touchend") {
      this.mouseDown = false;
      let ptz: PTZStop = new PTZStop(this.camera())
      this.ptz.stop(ptz).subscribe({
        next: () => {
        },
        error: reason => {
          this.reporting().errorMessage = reason;
        }
      });
    }
  }

  // Stop the movement if the mouse is slid outside the button with the mouse button still down
  stopIfMouseDown() {
    if (this.mouseDown) {
      this.stop();
    }
  }

  ngOnInit(): void {
    this.isGuest = this.utils.isGuestAccount;
  }

  protected readonly UtilsService = UtilsService;
}

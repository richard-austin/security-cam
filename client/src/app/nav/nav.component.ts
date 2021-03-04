import {AfterViewInit, Component, OnInit} from '@angular/core';
import {faVideo} from '@fortawesome/free-solid-svg-icons';
import {CameraService} from "../cameras/camera.service";
import {Camera} from "../cameras/Camera";

@Component({
  selector: 'app-nav',
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss']
})
export class NavComponent implements OnInit, AfterViewInit {

  // Font awesome icons
  faCamera = faVideo;
  cameras: Camera[] = [];

  constructor(private cameraSvc: CameraService) {
  }

  setVideoStream(cam: Camera):void {
      this.cameraSvc.setActiveLive([cam]);
      window.location.href = '#/live';
  }

  showRecording(cam:Camera):void
  {
    this.cameraSvc.setActiveLive([cam]);
    window.location.href = '#/recording';
  }

  multiCamView() {
    window.location.href = '#/multicam';
  }

  ngOnInit(): void {
    this.cameras = this.cameraSvc.getCameras();
  }

  ngAfterViewInit(): void {
  }

}

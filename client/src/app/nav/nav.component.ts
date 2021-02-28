import {AfterViewInit, Component, OnInit} from '@angular/core';
import {faVideo} from '@fortawesome/free-solid-svg-icons';
import {CameraService} from "../cameras/camera.service";
import {Camera, Uri} from "../cameras/Camera";

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

  setVideoStream(uri: Uri):void {
      // let c:Camera[] = this.cameraSvc.getCameras();
      // let urls:Uri[] = [];
      // urls.push(c[0].uris[0]);
      // urls.push(c[1].uris[0]);
      // urls.push(c[0].uris[1]);
  //    urls.push(c[1].uris[1]);

//    this.cameraSvc.setActiveLive(urls);
      this.cameraSvc.setActiveLive([uri]);
      window.location.href = '#/live';
  }

  ngOnInit(): void {
    this.cameras = this.cameraSvc.getCameras();
  }

  ngAfterViewInit(): void {
  }

}

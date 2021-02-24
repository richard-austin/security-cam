import {AfterViewInit, Component, OnInit} from '@angular/core';
import {faCamera} from '@fortawesome/free-solid-svg-icons';
import {CameraService} from "../cameras/camera.service";
import {Camera} from "../cameras/Camera";
import {timer} from "rxjs";

@Component({
  selector: 'app-nav',
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss']
})
export class NavComponent implements OnInit, AfterViewInit {

  // Font awesome icons
  faCamera = faCamera;
  cameras: Camera[] = [];

  constructor(private cameraSvc: CameraService) {
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    timer(1000).subscribe(()=> {
      this.cameras = this.cameraSvc.getCameras();

      let x = this.cameras[0].uris[0].type;
      let y = x;
    });
  }
}

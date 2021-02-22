import { Component, OnInit } from '@angular/core';
import {CameraService} from "./camera.service";

@Component({
  selector: 'app-cameras',
  templateUrl: './cameras.component.html',
  styleUrls: ['./cameras.component.scss']
})
export class CamerasComponent implements OnInit {
  camera: any;

  constructor(private camService:CameraService) { }

  ngOnInit(): void {
  }

  getCameras() {
      this.camService.getCameras().subscribe(
        cameras => {
          this.camera= cameras[1].camera.uris[0].uri.type;
          console.log(cameras)
        }
      )
  }
}

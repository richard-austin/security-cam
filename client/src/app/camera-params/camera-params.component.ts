import { Component, OnInit } from '@angular/core';
import {UtilsService} from "../shared/utils.service";

@Component({
  selector: 'app-camera-params',
  templateUrl: './camera-params.component.html',
  styleUrls: ['./camera-params.component.scss']
})
export class CameraParamsComponent implements OnInit {

  constructor(private utils:UtilsService) { }

  ngOnInit(): void {
    this.utils.cameraParams("192.168.0.34", "web/cgi-bin/hi3510/param.cgi", "cmd=getinfrared&cmd=getserverinfo").subscribe(
      result =>
      {
        let x = result;
      }

    )
  }
}

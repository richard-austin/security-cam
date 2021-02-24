import { Component, OnInit } from '@angular/core';
import {CameraService} from "./camera.service";

@Component({
  selector: 'app-cameras',
  templateUrl: './cameras.component.html',
  styleUrls: ['./cameras.component.scss']
})
export class CamerasComponent implements OnInit {

  constructor(private camService:CameraService) { }

  ngOnInit(): void {
  }
}

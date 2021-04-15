import { Component, OnInit } from '@angular/core';
import {MyIp, UtilsService} from "../shared/utils.service";

@Component({
  selector: 'app-set-ip',
  templateUrl: './set-ip.component.html',
  styleUrls: ['./set-ip.component.scss']
})
export class SetIpComponent implements OnInit {
  myIp: string = "";
  constructor(private utilsService:UtilsService) { }

  ngOnInit(): void {
    this.utilsService.setIp().subscribe((ip:MyIp) =>{
      this.myIp = ip.myIp;
    })
  }
}

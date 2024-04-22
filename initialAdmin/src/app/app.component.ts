import {AfterViewInit, Component} from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements AfterViewInit{
  title = 'initialAdmin';
  amqOff: boolean = false;

  constructor() {
    const queryString = window.location.search;
    const urlParams = new URLSearchParams(queryString);
    this.amqOff = urlParams.get('amqOff') == 'true';
  }

  registerLocalAccount() {
    window.location.href="#/registerLocalAccount"
  }

  setupSMTPClient () {
    window.location.href = "#/setupSMTPClient"
  }

  registerActiveMQAccount() {
    window.location.href = "#/registerActiveMQAccount"
  }

  ngAfterViewInit(): void {
    // window.location.href = '/#/registerLocalAccount';
  }
}

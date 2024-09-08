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
    if(typeof(window) !== 'undefined') {
      const queryString = window.location.search;
      const urlParams = new URLSearchParams(queryString);
      this.amqOff = urlParams.get('amqOff') != null;
    }
  }

  registerLocalAccount() {
    if(typeof(window) !== 'undefined')
      window.location.href="#/registerLocalAccount"
  }

  setupSMTPClient () {
    if(typeof(window) !== 'undefined')
      window.location.href = "#/setupSMTPClient"
  }

  registerActiveMQAccount() {
    if(typeof(window) !== 'undefined')
      window.location.href = "#/registerActiveMQAccount"
  }

  ngAfterViewInit(): void {
    // window.location.href = '/#/registerLocalAccount';
  }
}

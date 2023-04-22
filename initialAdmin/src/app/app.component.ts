import {AfterViewInit, Component} from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements AfterViewInit{
  title = 'initialAdmin';

  registerLocalAccount() {
    window.location.href="#/registerLocalAccount"
  }

  setupSMTPClient () {
    window.location.href = "#/setupSMTPClient"
  }

  ngAfterViewInit(): void {
    // window.location.href = '/#/registerLocalAccount';
  }
}

import {AfterViewInit, Component} from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements AfterViewInit{
  title = 'initialAdmin';

  setUpSMTPClient() {
    window.location.href = '#/'
  }

  registerLocalAccount() {
    window.location.href="#/registerLocalAccount"
  }
  ngAfterViewInit(): void {
    // window.location.href = '/#/registerLocalAccount';
  }
}

import {AfterViewInit, Component} from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements AfterViewInit{
  title = 'createUserAccount';

  ngAfterViewInit(): void {
    // window.location.href = '/#/registerLocalAccount';
  }
}

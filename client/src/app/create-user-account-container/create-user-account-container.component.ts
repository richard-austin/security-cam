import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {fromEvent, merge, Subscription} from 'rxjs';

@Component({
  selector: 'app-create-user-account-container',
  templateUrl: './create-user-account-container.component.html',
  styleUrls: ['./create-user-account-container.component.scss']
})
export class CreateUserAccountContainerComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('cuaframe') cuaframeEl!: ElementRef<HTMLIFrameElement>;
  cuaframe!: HTMLIFrameElement;

  height!: number;
  iframeEventsHandle!: Subscription;

  constructor() { }


  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.cuaframe = this.cuaframeEl.nativeElement;
    this.height = window.outerHeight - 70;

    // Subscribe to mousemove, mousedown and keydown events on the create user account iframe
    //  and dispatch them to this application to keep the idle timer from timeing out
    //  when there is user activity.
    this.iframeEventsHandle =
      merge(fromEvent(this.cuaframe.contentWindow as Window, 'mousedown'),
        fromEvent(this.cuaframe.contentWindow as Window, 'mousemove'),
        fromEvent(this.cuaframe.contentWindow as Window, 'keydown')).subscribe(() => {
        window.dispatchEvent(new Event('mousemove'));
      });
  }
  ngOnDestroy(): void {
    this.iframeEventsHandle.unsubscribe();
  }
}

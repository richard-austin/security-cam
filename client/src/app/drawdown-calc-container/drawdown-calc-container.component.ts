import {AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {fromEvent, merge, Subscription} from "rxjs";

@Component({
    selector: 'app-drawdown-calc-container',
    templateUrl: './drawdown-calc-container.component.html',
    styleUrls: ['./drawdown-calc-container.component.scss'],
    standalone: false
})
export class DrawdownCalcContainerComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('dciframe') dciframeEl!: ElementRef<HTMLIFrameElement>;
  dciframe!: HTMLIFrameElement;

  height!: number;
  iframeEventsHandle!: Subscription;

  constructor() {
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.dciframe = this.dciframeEl.nativeElement;
    this.height = window.outerHeight - 70;

    // Subscribe to mousemove, mousedown and keydown events on the drawdown calculator
    //  and dispatch them to this application to keep the idle timer from timeing out
    //  when there is user activity.
    this.iframeEventsHandle =
      merge(fromEvent(this.dciframe.contentWindow as Window, 'mousedown'),
        fromEvent(this.dciframe.contentWindow as Window, 'mousemove'),
        fromEvent(this.dciframe.contentWindow as Window, 'keydown')).subscribe(() => {
        window.dispatchEvent(new Event('mousemove'));
      });
  }

  ngOnDestroy(): void {
    this.iframeEventsHandle?.unsubscribe();
  }
}


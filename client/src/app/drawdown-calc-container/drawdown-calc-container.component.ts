import {AfterViewInit, Component, ElementRef, OnInit, ViewChild} from '@angular/core';

@Component({
  selector: 'app-drawdown-calc-container',
  templateUrl: './drawdown-calc-container.component.html',
  styleUrls: ['./drawdown-calc-container.component.scss']
})
export class DrawdownCalcContainerComponent implements OnInit, AfterViewInit {
  @ViewChild('dciframe') dciframeEl!: ElementRef<HTMLIFrameElement>;
  dciframe!: HTMLIFrameElement;

  height!: number;

  constructor() { }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    this.dciframe = this.dciframeEl.nativeElement;
    this.height= window.outerHeight - 70;
  }
}

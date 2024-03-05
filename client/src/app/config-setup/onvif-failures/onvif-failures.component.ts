import {AfterViewInit, Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-onvif-failures',
  templateUrl: './onvif-failures.component.html',
  styleUrls: ['./onvif-failures.component.scss']
})
export class OnvifFailuresComponent implements OnInit, AfterViewInit {
  @Input() failures!: Map<string, string>;
  displayedColumns: string[] = ['onvifUrl', 'error', 'onvifUser', 'onvifPassword', 'discover'];
  constructor() { }

  ngAfterViewInit(): void {
      let x = this.failures;
    }

  ngOnInit(): void {
  }

  protected readonly Symbol = Symbol;
}

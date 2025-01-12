import {Component, Input, OnInit} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

export enum styles {success, warning, danger}
@Component({
    selector: 'app-reporting',
    templateUrl: './reporting.component.html',
    styleUrls: ['./reporting.component.scss'],
    standalone: false
})
export class ReportingComponent implements OnInit {
  styles: typeof styles = styles;
  e: HttpErrorResponse | undefined | null;
  message: string | undefined;
  style!: styles;
  validationErrors!: string[];
  showMessageInError: boolean = true;
  showMessageFromMessage: boolean = false;
  @Input() embedded: boolean = false;
  isHTML: boolean = false;

  constructor() {
  }

  set errorMessage(e: HttpErrorResponse) {
    this.clearMessage();
    this.message = undefined;
    this.e = e;
    this.style = styles.danger;
    this.validationErrors = [];

    if (e.status === 400) {
      this.style = styles.warning;
      for (const key of Object.keys(e.error)) {
          this.validationErrors.push(key + ': ' + e.error[key]);
      }
    } else if (typeof (e.error) !== 'string') {
      this.showMessageInError = false;
      if(e.error !== undefined && e.error.text !== undefined) {
        let text: string = e.error.text;
        this.isHTML = text.startsWith("<!doctype html>");
      }

      if (typeof (e.message) === 'string') {
          this.showMessageFromMessage = true;
        }
    }
  }

  set successMessage(success: string) {
    this.clearMessage();
    this.message = success;
    this.style = styles.success;
  }

  set warningMessage(warning: string) {
    this.clearMessage();
    this.message = warning;
    this.style = styles.warning;
  }

  dismiss() {
    this.clearMessage();
  }

  private clearMessage() {
    this.e = undefined;
    this.message = undefined;
    this.isHTML = false;
  }

  ngOnInit(): void {
  }
}

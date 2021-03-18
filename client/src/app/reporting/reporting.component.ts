import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";

@Component({
  selector: 'app-reporting',
  templateUrl: './reporting.component.html',
  styleUrls: ['./reporting.component.scss']
})
export class ReportingComponent implements OnInit {
  error: HttpErrorResponse | undefined;
  success: string | undefined;
  validationErrors!:string[];

  constructor()
  {
  }

  set errorMessage(error: HttpErrorResponse)
  {
    this.success = undefined;
    this.error = error;
    this.validationErrors = [];

      if(error.status === 400)
      {
        for(const key of Object.keys(error.error))
          this.validationErrors.push(key + ': ' + error.error[key]);
      }
  }

  set successMessage(success: string)
  {
    this.success = success;
  }

  dismiss() {
    this.clearMessage();
  }

  private clearMessage() {
    // @ts-ignore
    this.error = undefined;
    this.success = undefined;
  }

  ngOnInit(): void {
  }
}

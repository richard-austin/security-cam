import {Component, OnInit} from '@angular/core';
import {HttpErrorResponse} from "@angular/common/http";

@Component({
  selector: 'app-reporting',
  templateUrl: './reporting.component.html',
  styleUrls: ['./reporting.component.scss']
})
export class ReportingComponent implements OnInit {
  error: HttpErrorResponse | undefined;
  success: string | undefined;
  validationErrors!:string[];
  showMessageInError:boolean = true;

  constructor()
  {
  }

  set errorMessage(error: HttpErrorResponse)
  {
    this.clearMessage();
    this.success = undefined;
    this.error = error;
    this.validationErrors = [];

      if(error.status === 400)
      {
        for(const key of Object.keys(error.error))
          this.validationErrors.push(key + ': ' + error.error[key]);
      }
      else if(typeof (error.error) !== 'string')
      {
        this.showMessageInError=false;
      }
  }

  set successMessage(success: string)
  {
    this.clearMessage();
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

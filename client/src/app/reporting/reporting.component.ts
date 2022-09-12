import {Component, Input, OnInit} from '@angular/core';
import {HttpErrorResponse} from "@angular/common/http";

@Component({
  selector: 'app-reporting',
  templateUrl: './reporting.component.html',
  styleUrls: ['./reporting.component.scss']
})
export class ReportingComponent implements OnInit {
  error: HttpErrorResponse | undefined;
  success: string | undefined;
  warning: string | undefined;
  validationErrors!:string[];
  showMessageInError:boolean = true;
  @Input() embedded: boolean = false;
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

  set warningMessage(warning: string)
  {
    this.clearMessage();
    this.warning = warning;
  }

  dismiss() {
    this.clearMessage();
  }

  private clearMessage() {
    this.error = undefined;
    this.success = undefined;
    this.warning = undefined;
  }

  ngOnInit(): void {
  }
}

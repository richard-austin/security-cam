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

  /**
   * isHTML: Returns true if string is html
   * @param str
   */
  isHTML(str:string): boolean {
    let doc = new DOMParser().parseFromString(str, "text/html");
    return Array.from(doc.body.childNodes).some(node => node.nodeType === 1);
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
      else if(typeof (error.error) !== 'string')
      {
        this.showMessageInError=false;
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

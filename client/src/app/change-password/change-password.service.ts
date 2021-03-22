import { Injectable } from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";
import {Camera} from "../cameras/Camera";
import {Observable, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class ChangePasswordService {
  readonly httpJSONOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })
  };

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) { }

  changePassword(oldPassword:string, newPassword:string, confirmNewPassword:string):Observable<void>
  {
    let passwordChange:{oldPassword: string, newPassword:string, confirmNewPassword: string} = {oldPassword: oldPassword, newPassword: newPassword, confirmNewPassword: confirmNewPassword};
    return this.http.post<void>(this._baseUrl.getLink("user", "changePassword"), JSON.stringify(passwordChange), this.httpJSONOptions).pipe(
      tap(),
      catchError((err:HttpErrorResponse) => throwError(err))
    );
  }
}

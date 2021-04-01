import { Injectable } from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "./BaseUrl/BaseUrl";
import {Observable, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";

export class Temperature
{
   temp: string = "";
}

@Injectable({
  providedIn: 'root'
})
export class UtilsService {
  readonly httpJSONOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })
  };

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) { }

  getTemperature():Observable<Temperature>
  {
    return this.http.post<Temperature>(this._baseUrl.getLink("utils", "getTemperature"), '', this.httpJSONOptions).pipe(
      tap(),
      catchError((err:HttpErrorResponse) => throwError(err))
    );
  }
}

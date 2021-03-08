import { Injectable } from '@angular/core';
import {Observable, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";

@Injectable({
  providedIn: 'root'
})
export class MotionService {
  readonly httpJSONOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })
  };


  constructor(private http: HttpClient, private _baseUrl: BaseUrl) { }

  getTimeOffsetForEpoch(epoch: number, motionName: string): Observable<any> {
    let params: {epoch: string, motionName: string} = {epoch: epoch.toString(), motionName: motionName};
    return this.http.post<number>(this._baseUrl.getLink("motion", "getTimeOffsetForEpoch"), JSON.stringify(params), this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }
}

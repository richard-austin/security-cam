import {Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {BaseUrl} from '../shared/BaseUrl/BaseUrl';
import {catchError} from 'rxjs/operators';
import {Observable, throwError} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CloudProxyService {
  readonly httpJSONOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })
  };

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {
  }

  getStatus(): Observable<boolean> {
    return this.http.post<boolean>(this._baseUrl.getLink('cloudProxy', 'status'), '', this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  start(): Observable<void> {
    return this.http.post<void>(this._baseUrl.getLink('cloudProxy', 'start'), '', this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  stop(): Observable<void> {
    return this.http.post<void>(this._baseUrl.getLink('cloudProxy', 'stop'), '', this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err)));
  }
}

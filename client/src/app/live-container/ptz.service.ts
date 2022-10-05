import { Injectable } from '@angular/core';
import {Observable, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {eMoveDirections} from "./ptzcontrols/ptzbutton/ptzbutton.component";
import { BaseUrl } from '../shared/BaseUrl/BaseUrl';

export class PTZMove {
  constructor(moveDirection: eMoveDirections, onvifBaseAddress: string) {
    this.moveDirection = moveDirection;
    this.onvifBaseAddress = onvifBaseAddress;
  }
  moveDirection!: eMoveDirections;
  onvifBaseAddress!: string;
}

export class PTZStop {
  constructor(onvifBaseAddress: string) {
    this.onvifBaseAddress = onvifBaseAddress;
  }
  onvifBaseAddress!: string;
}

@Injectable({
  providedIn: 'root'
})
export class PTZService {
  readonly httpJSONOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })}

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) { }

  move(ptz: PTZMove): Observable<void> {
    return this.http.post<void>(this._baseUrl.getLink("ptz", "move"), JSON.stringify(ptz), this.httpJSONOptions).pipe(tap(), catchError((err: HttpErrorResponse) => throwError(err)));
  }

  stop(ptz: PTZStop): Observable<void> {
    return this.http.post<void>(this._baseUrl.getLink("ptz", "stop"), JSON.stringify(ptz), this.httpJSONOptions).pipe(tap(), catchError((err: HttpErrorResponse) => throwError(err)));
  }
}

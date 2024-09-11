import {Injectable} from '@angular/core';
import {Observable, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";
import { HttpClient, HttpErrorResponse, HttpHeaders } from "@angular/common/http";
import {eMoveDirections} from "./ptzcontrols/ptzbutton/ptzbutton.component";
import {BaseUrl} from '../shared/BaseUrl/BaseUrl';
import {ePresetOperations} from "./ptzcontrols/preset-button/preset-button.component";
import { Camera } from '../cameras/Camera';

class PtzOperations {
  onvifBaseAddress: string;
  creds: string;
  constructor(cam: Camera) {
    this.onvifBaseAddress = cam.onvifHost
    this.creds = cam.cred
  }
}
export class PTZMove extends PtzOperations {
  constructor(moveDirection: eMoveDirections, cam:Camera) {
    super(cam);
    this.moveDirection = moveDirection;
  }

  moveDirection: eMoveDirections;
}

export class PTZStop extends PtzOperations{
  constructor(cam: Camera) {
    super(cam);
  }
}

export class PTZPresetCommand extends PtzOperations{
  constructor(operation: ePresetOperations, cam: Camera, preset: string) {
    super(cam);
    this.operation = operation;
    this.preset = preset;
  }
  operation: ePresetOperations;
  preset: string;
}

export class PTZPresetsInfoCommand extends PtzOperations{
  constructor(cam: Camera) {
    super(cam);
  }
}

export class Preset {
  name!: string;
  token!: string;
}

export class PTZPresetsInfoResponse {
  maxPresets!: number;
  presets!: Preset[];
}

@Injectable({
  providedIn: 'root'
})
export class PTZService {
  readonly httpJSONOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })
  }

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {
  }

  move(ptz: PTZMove): Observable<void> {
    return this.http.post<void>(this._baseUrl.getLink("ptz", "move"), JSON.stringify(ptz), this.httpJSONOptions).pipe(tap(), catchError((err: HttpErrorResponse) => throwError(err)));
  }

  stop(ptz: PTZStop): Observable<void> {
    return this.http.post<void>(this._baseUrl.getLink("ptz", "stop"), JSON.stringify(ptz), this.httpJSONOptions).pipe(tap(), catchError((err: HttpErrorResponse) => throwError(err)));
  }

  preset(presetOp: PTZPresetCommand): Observable<void> {
    return this.http.post<void>(this._baseUrl.getLink("ptz", "preset"), JSON.stringify(presetOp), this.httpJSONOptions).pipe(tap(), catchError((err: HttpErrorResponse) => throwError(err)));
  }

  ptzPresetsInfo(presetOp: PTZPresetsInfoCommand): Observable<PTZPresetsInfoResponse> {
    return this.http.post<PTZPresetsInfoResponse>(this._baseUrl.getLink("ptz", "ptzPresetsInfo"), JSON.stringify(presetOp), this.httpJSONOptions).pipe(tap(), catchError((err: HttpErrorResponse) => throwError(err)));
  }
}

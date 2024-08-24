import {Injectable} from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from "@angular/common/http";
import {BaseUrl} from "./BaseUrl/BaseUrl";
import {Observable, Subject, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";
import {CameraParams, Camera} from "../cameras/Camera";
import {environment} from "../../environments/environment";
import {cameraType} from '../cameras/camera.service';
import {IMessage} from "@stomp/stompjs";


export class Temperature {
  temp: string = "";
}

export class Version {
  version: string = "";
}

export class MyIp {
  myIp: string = "";
}

export enum messageType {idleTimeoutStatus, logoff}

export abstract class Message {
  protected constructor(messageType: messageType) {

    this.messageType = messageType;
  }

  messageType!: messageType;
}

export class IdleTimeoutStatusMessage extends Message {
  constructor(active: boolean) {
    super(messageType.idleTimeoutStatus);
    this.active = active;
  }

  active: boolean = true;
}

export class LogoffMessage extends Message {
  constructor() {
    super(messageType.logoff);
  }
}

export class GuestStatus {
  guestAccount: boolean = true;

  constructor(guestAccount: boolean) {
    this.guestAccount = guestAccount;
  }
}

export class GuestAccountStatus {
  enabled: boolean = false;
}

export class SetCameraParams {
  constructor(cameraTypes: cameraType, address: string, uri: string, infraredstat: string, cameraName: string, reboot: boolean = false, wdr?: string, lamp_mode?: string) {
    this.cameraType = cameraTypes;
    this.address = address;
    this.uri = uri;
    this.infraredstat = infraredstat;
    this.cameraName = cameraName;
    this.wdr = wdr;
    this.lamp_mode = lamp_mode;
    this.reboot = reboot;
  }

  cameraType: cameraType;
  address: string;
  uri: string;
  infraredstat: string;
  cameraName: string;
  wdr: string | undefined;
  lamp_mode: string | undefined;
  reboot: boolean;
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

  readonly httpTextOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'text/plain',
      'Authorization': 'my-auth-token'
    }),
    responseType: 'texty'
  };
  private _messaging: Subject<any> = new Subject<any>();
  private _isGuestAccount: boolean = true;
  speakActive: boolean = true;
  private _activeMQTransportActive: boolean = false;
  private _cloudProxyRunning: boolean = false;

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {
    // Initialise the speakActive state
    this.audioInUse().subscribe();
  }

  getTemperature(): Observable<Temperature> {
    return this.http.post<Temperature>(this._baseUrl.getLink("utils", "getTemperature"), '', this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  getVersion(): Observable<Version> {
    return this.http.post<Version>(this._baseUrl.getLink("utils", "getVersion"), '', this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  getOpenSourceInfo(): Observable<string> {
    return this.http.post(this._baseUrl.getLink("utils", "getOpenSourceInfo"), '', {responseType: 'text'}).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  setIp(): Observable<MyIp> {
    return this.http.post<MyIp>(this._baseUrl.getLink("utils", "setIP"), '', this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  cameraParams(address: string, uri: string, params: string): Observable<CameraParams> {
    let cameraParams: { address: string, uri: string, params: string } = {address: address, uri: uri, params: params};
    return this.http.post<CameraParams>(this._baseUrl.getLink("utils", "cameraParams"), JSON.stringify(cameraParams), this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  setCameraParams(cameraParams: SetCameraParams): Observable<CameraParams> {
    return this.http.post<CameraParams>(this._baseUrl.getLink("utils", "setCameraParams"), JSON.stringify(cameraParams), this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  changePassword(oldPassword: string, newPassword: string, confirmNewPassword: string): Observable<void> {
    let passwordChange: { oldPassword: string, newPassword: string, confirmNewPassword: string } = {
      oldPassword: oldPassword,
      newPassword: newPassword,
      confirmNewPassword: confirmNewPassword
    };
    return this.http.post<void>(this._baseUrl.getLink("user", "changePassword"), JSON.stringify(passwordChange), this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  getEmail(): Observable<{ email: string }> {
    return this.http.post<{ email: string }>(this._baseUrl.getLink("user", "getEmail"), '', this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  changeEmail(password: string, newEmail: string, confirmNewEmail: string) {
    let passwordChange: { password: string, newEmail: string, confirmNewEmail: string } = {
      password: password,
      newEmail: newEmail,
      confirmNewEmail: confirmNewEmail
    };
    return this.http.post<void>(this._baseUrl.getLink("user", "changeEmail"), JSON.stringify(passwordChange), this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  setupGuestAccount(enabled: boolean, password: string, confirmPassword: string) {
    let params: { enabled: boolean, password: string, confirmPassword: string } = {
      enabled: enabled,
      password: password,
      confirmPassword: confirmPassword
    }

    return this.http.post(this._baseUrl.getLink("user", "setupGuestAccount"), JSON.stringify(params), this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError((err)))
    )
  }

  /**
   * isGuest: check if logged in as guest.
   *          This is called from the nav component ngOnInit
   */
  async isGuest(): Promise<GuestStatus> {
    let retVal: GuestStatus = environment.production ? await this.http.post<GuestStatus>(this._baseUrl.getLink("user", "isGuest"), '', this.httpJSONOptions).toPromise() : new GuestStatus(false)
    this.isGuestAccount = retVal.guestAccount;
    return retVal;
  }

  /**
   * isGuestAccount: Set true if guest account, else false.
   * @param value
   */
  set isGuestAccount(value: boolean) {
    this._isGuestAccount = value;
  }

  /**
   * isGuestAccount: Returns true if logged in as guest. The value is set by a preceding call to isGuest from the nav component.
   */
  get isGuestAccount() {
    return this._isGuestAccount;
  }

  async guestAccountEnabled(): Promise<GuestAccountStatus> {
    return await this.http.post<GuestAccountStatus>(this._baseUrl.getLink("user", "guestAccountEnabled"), '', this.httpJSONOptions).toPromise()
  }

  sendMessage(message: Message) {
    this._messaging.next(message);
  }

  getMessages(): Observable<Message> {
    return this._messaging.asObservable();
  }

  getScrollableContentStyle(scrollableContent: HTMLElement | null | undefined, setMaxHeight: boolean = false): string {
    // Calculated scrollbar height, don't use or we Expression changed after it was checked error will occur
    //   scrollableContent?.offsetHeight - scrollableContent?.clientHeight;
    const scrollbarHeight = 20; //Should be the same as height in ::-webkit-scrollbar
    const extraBit = 13;  // To make browser window vertical scrollbar disappear

    if (scrollableContent !== null && scrollableContent !== undefined) {
      const boundingRect = scrollableContent.getBoundingClientRect()
      return (setMaxHeight ? 'max-' : '') + `height: calc(100dvh - ${boundingRect.top + scrollbarHeight + extraBit}px);`
    }
    else return ""
  }


  startAudioOut(cam: Camera, netcam_uri: string) {
    return this.http.post<void>(this._baseUrl.getLink("utils", "startAudioOut"), JSON.stringify({
      cam: cam,
      netcam_uri: netcam_uri
    }), this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  stopAudioOut() {
    return this.http.post<void>(this._baseUrl.getLink("utils", "stopAudioOut"), "", this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  audioInUse() {
    return this.http.post<{
      audioInUse: boolean
    }>(this._baseUrl.getLink("utils", "audioInUse"), "", this.httpJSONOptions).pipe(
      tap((result) => {
        this.speakActive = result.audioInUse;
      }),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  /**
   * talkOff: Called on receipt of the talkOff websocket message. This disables audio out to any camera while the channel is in use and
   *          re-enables it when that usage has finished.
   * @param message
   */
  talkOff(message: IMessage) {
    if (message.body) {
      let msgObj = JSON.parse(message.body);
      if (msgObj.message === 'talkOff') {
        this.speakActive = msgObj.instruction == "on";
      }
    }
  }

  setTransportStatus(message: IMessage) {
    let strMsg: string;
    if (message.isBinaryBody)
      strMsg = new TextDecoder().decode(message.binaryBody);
    else
      strMsg = message.body;
    let status: { transportActive: boolean } = JSON.parse(strMsg)
    this._activeMQTransportActive = status.transportActive;
  }

  get activeMQTransportActive(): boolean {
    return this._activeMQTransportActive
  }

  set activeMQTransportActive(value: boolean) {
    this._activeMQTransportActive = value;
  }
  get cloudProxyRunning(): boolean {
    return this._cloudProxyRunning;
  }

  set cloudProxyRunning(value: boolean) {
    this._cloudProxyRunning = value;
  }
}

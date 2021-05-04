import { Injectable } from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "./BaseUrl/BaseUrl";
import {Observable, Subject, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";

export class Temperature
{
   temp: string = "";
}

export class Version
{
  version: string = "";
}

export class MyIp
{
  myIp: string = "";
}

export enum messageType {idleTimeoutStatus}

export abstract class Message
{
  protected constructor(messageType:messageType) {

    this.messageType=messageType;
  }
  messageType!: messageType;
}

export class IdleTimeoutStatusMessage extends Message
{
  constructor(active: boolean) {
    super(messageType.idleTimeoutStatus);
    this.active=active;
  }

  active: boolean = true;
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

  private _messaging:Subject<any> = new Subject<any>();

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) { }

  getTemperature():Observable<Temperature>
  {
    return this.http.post<Temperature>(this._baseUrl.getLink("utils", "getTemperature"), '', this.httpJSONOptions).pipe(
      tap(),
      catchError((err:HttpErrorResponse) => throwError(err))
    );
  }

  getVersion():Observable<Version>
  {
    return this.http.post<Version>(this._baseUrl.getLink("utils", "getVersion"), '', this.httpJSONOptions).pipe(
      tap(),
      catchError((err:HttpErrorResponse) => throwError(err))
    );
  }

  setIp():Observable<MyIp>
  {
    return this.http.post<MyIp>(this._baseUrl.getLink("utils", "setIP"), '', this.httpJSONOptions).pipe(
      tap(),
      catchError((err:HttpErrorResponse) => throwError(err))
    );
  }

  cameraParams(address:string, uri:string, params:string):Observable<{}>
  {
    let cameraParams:{address:string, uri:string, params:string} = {address:address, uri:uri, params:params};
    return this.http.post<{}>(this._baseUrl.getLink("utils", "cameraParams"), JSON.stringify(cameraParams), this.httpJSONOptions).pipe(
      tap(),
      catchError((err:HttpErrorResponse) => throwError(err))
    );
  }

  sendMessage(message:Message)
  {
    this._messaging.next(message);
  }

  getMessages(): Observable<Message> {
    return this._messaging.asObservable();
  }
}

import {Injectable} from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from "@angular/common/http";
import {BaseUrl} from "./BaseUrl/BaseUrl";
import {Observable, Subject, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";
import {SMTPData} from "../setup-smtpclient/setup-smtpclient.component";

export class Temperature {
  temp: string = "";
  isAdmin: boolean = false;
}

export class Version {
  version: string = "";
}

export class MyIp {
  myIp: string = "";
}

export enum messageType {idleTimeoutStatus, loggedIn, loggedOut}

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

export class LoggedInMessage extends Message {
  role: string;

  constructor(role: string) {
    super(messageType.loggedIn);
    this.role = role;
  }
}

export class LoggedOutMessage extends Message {
  constructor() {
    super(messageType.loggedOut);
  }
}

export class Account {
  productId!: string;
  accountCreated!: boolean;
  accountEnabled!: boolean;
  userName!: string;
  email!: string;
  nvrConnected!: boolean;
  usersConnected!: number;
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

  readonly httpUrlEncoded = {
    headers: new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'my-auth-token'
    })
  }

  private _messaging: Subject<any> = new Subject<any>();
  private _loggedIn: boolean = false;
  private _isAdmin: boolean = false;
  private _hasLocalAccount: boolean = false;
  private _hasActiveMQCreds!: boolean;
  public readonly passwordRegex: RegExp = new RegExp(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/);
  public readonly activeMQPasswordRegex: RegExp = new RegExp(/^$|^[A-Za-z0-9]{20}$/);
  public readonly hostNameRegex =  /^[a-zA-Z0-9][a-zA-Z0-9._-]*$/
  public readonly ipV4RegEx = /^([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))$/
  public readonly ipV6RegEx = /^s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:)))(%.+)?s*(\/([0-9]|[1-9][0-9]|1[0-1][0-9]|12[0-8]))?$/

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {
  }


  get loggedIn(): boolean {
    return this._loggedIn;
  }

  get isAdmin() {
    return this._isAdmin;
  }

  get hasLocalAccount() : boolean
  {
    return this._hasLocalAccount;
  }

  get hasActiveMQCreds(): boolean {
    return this._hasActiveMQCreds;
  }

  createOrUpdateLocalNVRAccount(username: string, password: string, confirmPassword: string, email: string, confirmEmail: string, updateExisting: boolean = false) : Observable<void> {
    let details: { username: string, password: string, confirmPassword: string, email: string, confirmEmail: string, updateExisting: boolean} =
      {
        username: username,
        password: password,
        confirmPassword: confirmPassword,
        email: email,
        confirmEmail: confirmEmail,
        updateExisting: updateExisting
      };
    return this.http.post<void>(this._baseUrl.getLink("user", "createOrUpdateAccountLocally"), details, this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  addOrUpdateActiveMQCreds(username: string, password: string, confirmPassword: string, mqHost: string, updateExisting: boolean = false) : Observable<void> {
    let details: { username: string, password: string, confirmPassword: string, mqHost: string, updateExisting: boolean} =
      {
        username: username,
        password: password,
        confirmPassword: confirmPassword,
        mqHost: mqHost,
        updateExisting: updateExisting
      };
    return this.http.post<void>(this._baseUrl.getLink("user", "addOrUpdateActiveMQCreds"), details, this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  checkForAccountLocally() : Observable<boolean> {
    return this.http.post<boolean>(this._baseUrl.getLink("user", "checkForAccountLocally"), "", this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  setupSMTPClientLocally(smtpData: SMTPData) {
    return this.http.post<boolean>(this._baseUrl.getLink("utils", "setupSMTPClientLocally"), JSON.stringify(smtpData), this.httpJSONOptions);
  }

  checkForActiveMQCreds(): Observable<{hasActiveMQCreds: boolean, mqHost: string}> {
    this._hasActiveMQCreds = false;
    return this.http.post<{hasActiveMQCreds: boolean, mqHost: string}>(this._baseUrl.getLink('user', 'checkForActiveMQCreds'), '', this.httpJSONOptions).pipe(
      tap((result) => {
        this._hasActiveMQCreds = result.hasActiveMQCreds;
      }),
      catchError((err: HttpErrorResponse) => throwError(err))
    )
  }

  getSMTPClientParamsLocally() : Observable<SMTPData> {
    return this.http.post<SMTPData>(this._baseUrl.getLink("utils", "getSMTPClientParamsLocally"), "", this.httpJSONOptions);
  }

  getUserAuthorities(): Observable<{ authority: string }[]> {
    return this.http.post<{ authority: string }[]>(this._baseUrl.getLink('cloud', 'getUserAuthorities'), '', this.httpJSONOptions).pipe(
      tap((auth) => {
        let strAuth: string = auth[0]?.authority;
        switch (strAuth) {
          case 'ROLE_CLIENT':
            this._isAdmin = false;
            this._loggedIn = true;
            this.getHasLocalAccount();
            break;
          case 'ROLE_ADMIN':
            this._isAdmin = true;
            this._loggedIn = true;
            break;
          case 'ROLE_ANONYMOUS':
            this._isAdmin = this._loggedIn = false;
            this.sendMessage(new LoggedOutMessage());  // Tell nav component we are logged out
            break;
          default:
            this._isAdmin = this._loggedIn = false;
            this.sendMessage(new LoggedOutMessage());  // Tell nav component we are logged out
        }
      }),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  getHasLocalAccount() : void
  {
    this._hasLocalAccount = false;
    this.http.post<boolean>(this._baseUrl.getLink('user', 'checkForAccountLocally'), '', this.httpJSONOptions).pipe(
      tap((result) => {
        this._hasLocalAccount = result;
      }),
      catchError((err: HttpErrorResponse) => throwError(err))
    ).subscribe()
  }

  sendMessage(message: Message) {
    this._messaging.next(message);
  }

  getMessages(): Observable<Message> {
    return this._messaging.asObservable();
  }
}

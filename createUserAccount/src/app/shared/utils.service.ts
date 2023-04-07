import {Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "./BaseUrl/BaseUrl";
import {Observable, Subject, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";

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
  public readonly passwordRegex: RegExp = new RegExp(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,64}$/);

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

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {
  }

  login(username: string, password: string, rememberMe: boolean): Observable<[{ authority: string }]> {
    let creds: string = "username=" + username + "&password=" + password + (rememberMe ? "&remember-me=on" : '');
    return this.http.post<[{ authority: string }]>(this._baseUrl.getLink("login", "authenticate"), creds, this.httpUrlEncoded).pipe(
      tap((result) => {
          if (result[0].authority === 'ROLE_ADMIN') {
            this._isAdmin = this._loggedIn = true;
          }
          else {
            this._loggedIn = true;
            this.getHasLocalAccount();
          }
        },
        () => {
          this._isAdmin = this._loggedIn = false;
        }),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  logoff(): void {
    this._hasLocalAccount = this._loggedIn = false;
    window.location.href = 'logoff';
  }

  getTemperature(): Observable<Temperature> {
    return this.http.post<Temperature>(this._baseUrl.getLink("cloud", "getTemperature"), '', this.httpJSONOptions).pipe(
       catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  register(userName: string, productId: string, password: string, confirmPassword: string, email: string, confirmEmail: string): Observable<any> {
    let details: { username: string, productId: string, password: string, confirmPassword: string, email: string, confirmEmail: string } =
      {
        username: userName,
        productId: productId,
        password: password,
        confirmPassword: confirmPassword,
        email: email,
        confirmEmail: confirmEmail
      };
    return this.http.post<any>(this._baseUrl.getLink("cloud", "register"), details, this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  registerLocalNVRAccount(username: string, password: string, confirmPassword: string, email: string, confirmEmail: string) {
    let details: { username: string, password: string, confirmPassword: string, email: string, confirmEmail: string } =
      {
        username: username,
        password: password,
        confirmPassword: confirmPassword,
        email: email,
        confirmEmail: confirmEmail
      };
    return this.http.post<any>(this._baseUrl.getLink("user", "createAccount"), details, this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  removeLocalNVRAccount(): Observable<{username: string}>
  {
    return this.http.post<{username: string}>(this._baseUrl.getLink("user", "removeAccount"), '', this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  getVersion(isLocal: boolean): Observable<Version> {
    return this.http.post<Version>(this._baseUrl.getLink(isLocal ? "cloud" : "utils", "getVersion"), '', this.httpJSONOptions).pipe(
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

  sendResetPasswordLink(email: string, clientUri: string): Observable<void> {
    let em: { email: string, clientUri: string } = {email: email, clientUri: clientUri};
    return this.http.post<void>(this._baseUrl.getLink('cloud', 'sendResetPasswordLink'), JSON.stringify(em), this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
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
    this.http.post<boolean>(this._baseUrl.getLink('user', 'hasLocalAccount'), '', this.httpJSONOptions).pipe(
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
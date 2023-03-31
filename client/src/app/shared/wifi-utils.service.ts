import {Injectable} from '@angular/core';
import {Observable, throwError} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {BaseUrl} from './BaseUrl/BaseUrl';
import { IPDetails } from './IPDetails';
import {WifiDetails} from './wifi-details';
import { WifiStatus } from './wifi-status';
import { EthernetConnectionStatus } from './ethernet-connection-status';
import { CurrentWifiConnection } from './current-wifi-connection';

@Injectable({
  providedIn: 'root'
})
export class WifiUtilsService {
  readonly httpJSONOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })
  };

  // readonly httpTextOptions = {
  //   headers: new HttpHeaders({
  //     'Content-Type': 'text/plain',
  //     'Authorization': 'my-auth-token',
  //   }),
  //   responseType: 'text'
  // };
  //
  readonly httpTextHeaders = new HttpHeaders({
    'Content-Type': 'text/plain',
    'Authorization': 'my-auth-token',
  });


  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {
  }

  getActiveIPAddresses(): Observable<IPDetails[]> {
    return this.http.post<any>(this._baseUrl.getLink('wifiUtils', 'getActiveIPAddresses'), '', this.httpJSONOptions).pipe(
      map((ocr) => (ocr.responseObject as IPDetails[])),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  getLocalWifiDetails(): Observable<WifiDetails[]> {
    return this.http.post<WifiDetails[]>(this._baseUrl.getLink('wifiUtils', 'scanWifi'), '', this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  checkWifiStatus(): Observable<WifiStatus> {
     return this.http.post<WifiStatus>(this._baseUrl.getLink('wifiUtils', 'checkWifiStatus'), '', this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  checkConnectedThroughEthernet(): Observable<EthernetConnectionStatus>
  {
    return this.http.post<WifiStatus>(this._baseUrl.getLink('wifiUtils', 'checkConnectedThroughEthernet'), '', this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  setWifiStatus(status: string): Observable<WifiStatus>
  {
    let param:{status: string} = {status: status};
    return this.http.post<WifiStatus>(this._baseUrl.getLink('wifiUtils', 'setWifiStatus'), JSON.stringify(param), this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  setUpWifi(ssid: string, password?: string) : Observable<{response: string}>{
    let param:{ssid: string, password: string | undefined} = {ssid: ssid, password: undefined};
    if(password !== undefined && password !== "")
      param.password = password;

    return this.http.post<{response:string}>(this._baseUrl.getLink('wifiUtils', 'setUpWifi'), JSON.stringify(param), this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }

  getCurrentWifiConnection() : Observable<CurrentWifiConnection>
  {
    return this.http.post<CurrentWifiConnection>(this._baseUrl.getLink('wifiUtils', 'getCurrentWifiConnection'), '', this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }
}

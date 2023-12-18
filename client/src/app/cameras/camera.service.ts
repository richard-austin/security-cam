import {EventEmitter, Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";
import {Observable, Subject, throwError} from "rxjs";
import {catchError, map, tap} from "rxjs/operators";
import {AudioEncoding, Camera, CameraParamSpec, Stream} from "./Camera";
import {CameraAdminCredentials} from "../credentials-for-camera-access/credentials-for-camera-access.component";
import {NativeDateAdapter} from '@angular/material/core';


/**
 * MotionEvents as received from the server
 */
export class MotionEvents {
  events: string[] = [];
}

export class LocalMotionEvent {
  manifest!: string;
  epoch!: number;
  dateTime!: string;
}

/**
 * LocalMotionEvents: Motion events as delivered to the recordings page
 */
export class LocalMotionEvents {
  events: LocalMotionEvent[] = [];
}

export class DateSlot {
  date!: Date;
  lme: LocalMotionEvents = new LocalMotionEvents();
}

/**
 * CustomDateAdapter: For formatting the date n the datepicker on the recording page
 */
export class CustomDateAdapter extends NativeDateAdapter {
  readonly months: string[] = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  format(date: Date, displayFormat: any): string {
    const days = date.getDate();
    const months = date.getMonth() + 1;
    const year = date.getFullYear();
    return ("00" + days).slice(-2) + '-' + this.months[months - 1] + '-' + year;
  }
}

export enum cameraType {none, sv3c, zxtechMCW5B10X}

@Injectable({
  providedIn: 'root'
})
export class CameraService {
  readonly httpJSONOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })
  };

  readonly httpUploadOptions = {
    headers: new HttpHeaders({
      'Authorization': 'my-auth-token'
    })
  };

  private cameras: Map<string, Camera> = new Map();

  errorEmitter: EventEmitter<HttpErrorResponse> = new EventEmitter<HttpErrorResponse>();

  public readonly _cameraParamSpecs: CameraParamSpec[] =
    [new CameraParamSpec(
      cameraType.none,
      "",
      '',
      "Not Listed"),
      new CameraParamSpec(cameraType.sv3c,
        "cmd=getinfrared&cmd=getserverinfo&cmd=getoverlayattr&-region=0&cmd=getserverinfo&cmd=getoverlayattr&-region=1",
        'web/cgi-bin/hi3510/param.cgi',
        "SV3C (General)"),
      new CameraParamSpec(
        cameraType.zxtechMCW5B10X,
        "cmd=getvideoattr&cmd=getlampattrex&cmd=getimageattr&cmd=getinfrared&cmd=getserverinfo&cmd=getoverlayattr&-region=0&cmd=getserverinfo&cmd=getoverlayattr&-region=1",
        'web/cgi-bin/hi3510/param.cgi',
        "ZTech MCW5B10X")]

  private readonly _audioEncodings: AudioEncoding[] = [
    new AudioEncoding('None', 'None'),  // No audio in stream
    new AudioEncoding('Not Listed', 'Not Listed'),  // Audio type not listed, transcode to AAC
    new AudioEncoding('G711', 'G711'),  // Transcode to AAC
    new AudioEncoding('G726', 'G726'),  // Transcode to AAC
    new AudioEncoding('AAC', 'AAC'),    // No transcoding required

  ];

  private readonly _ftpRetriggerWindows: { name: string, value: number }[] = [
    {name: "10", value: 10},
    {name: "20", value: 20},
    {name: "30", value: 30},
    {name: "40", value: 40},
    {name: "50", value: 50},
    {name: "60", value: 60},
    {name: "70", value: 70},
    {name: "80", value: 80},
    {name: "90", value: 90},
    {name: "100", value: 100}
  ];

  get cameraParamSpecs() {
    return this._cameraParamSpecs;
  };

  get audioEncodings() {
    return this._audioEncodings;
  }

  get ftpRetriggerWindows() {
    return this._ftpRetriggerWindows;
  }

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {
    this.loadCameras().subscribe((cams) => {
      this.cameras = cams;

    })
  }

  /**
   * getCameras: Returns an array of cameras
   */
  public getCameras(): Map<string, Camera> {
    return this.cameras;
  }

  private static convertCamsObjectToMap(cams: Object): Map<string, Camera> {
    let cameras: Map<string, Camera> = new Map<string, Camera>();

    for (let key in cams) {
      // @ts-ignore
      let cam: Camera = cams[key];
      let streams: Map<string, Stream> = new Map<string, Stream>();
      for (let j in cam.streams) {
        // @ts-ignore
        let stream: Stream = cam.streams[j] as Stream;
        stream.selected = stream.defaultOnMultiDisplay;
        streams.set(j, stream);
      }
      cam.streams = streams;  //Make the streams object into a map
      cameras.set(key, cam);
    }
    return cameras;
  }

  /**
   * loadCameras: Get camera set up details from the server
   * @private
   */
  loadCameras(): Observable<Map<string, Camera>> {
    return this.http.post<Map<string, Camera>>(this._baseUrl.getLink("cam", "getCameras"), '', this.httpJSONOptions).pipe(
      map((cams: Object) => {
          return CameraService.convertCamsObjectToMap(cams);
        }
      ),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  haveCameraCredentials(): Observable<string> {
    return this.http.post(this._baseUrl.getLink("cam", "haveCameraCredentials"), '', {responseType: 'text'}).pipe(
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  updateCameras(camerasJON: string):
    Observable<Map<string, Camera>> {
    let cameras = {camerasJSON: camerasJON};
    return this.http.post<any>(this._baseUrl.getLink("cam", "updateCameras"), JSON.stringify(cameras), this.httpJSONOptions).pipe(
      tap((cams) => {
        this.cameras = new Map();

        for (const key in cams)
          this.cameras.set(key, cams[key]);
      }),
      map(cams => {
        return CameraService.convertCamsObjectToMap(cams);
      })
    );
  }

  discover(): Observable<Map<string, Camera>> {
    return this.http.post<any>(this._baseUrl.getLink("onvif", "discover"), '', this.httpJSONOptions).pipe(
      map(cams => {
        return CameraService.convertCamsObjectToMap(cams);
      })
    );
  }

  discoverCameraDetails(onvifUrl: string): Observable<Camera> {
    const formData: FormData = new FormData();
    formData.append('onvifUrl', onvifUrl)
    return this.http.post<any>(this._baseUrl.getLink("onvif", "discoverCameraDetails"), formData, this.httpUploadOptions).pipe(
      map(cams => {
        let map: Map<string, Camera> = CameraService.convertCamsObjectToMap(cams);
        if (map.size == 1)
          return map.entries().next().value[1];
      })
    );
  }

  uploadMaskFile(uploadFile: any): Observable<any> {
    const formData: FormData = new FormData();
    formData.append('maskFile', uploadFile);
    return this.http.post<any>(this._baseUrl.getLink("cam", "uploadMaskFile"), formData, this.httpUploadOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  getSnapshot(url: string): Observable<Array<any>> {
    const formData: FormData = new FormData();
    formData.append('url', url);
    return this.http.post<Array<any>>(this._baseUrl.getLink("onvif", "getSnapshot"), formData, this.httpUploadOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  setCameraAdminCredentials(creds: CameraAdminCredentials): Observable<any> {
    return this.http.post<any>(this._baseUrl.getLink("cam", "setAccessCredentials"), creds, this.httpUploadOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  getAccessToken(cameraHost: string, port: number): Observable<{ accessToken: string }> {
    let params: {} = {host: cameraHost, port: port}
    return this.http.post<{
      accessToken: string
    }>(this._baseUrl.getLink("cam", "getAccessToken"), params, this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  resetTimer(accessToken: string): Observable<void> {
    let params: {} = {accessToken: accessToken}
    return this.http.post<void>(this._baseUrl.getLink("cam", "resetTimer"), params, this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  closeClients(accessToken: string): Observable<void> {
    let params: {} = {accessToken: accessToken}
    return this.http.post<void>(this._baseUrl.getLink("cam", "closeClients"), params, this.httpJSONOptions).pipe(
      catchError((err: HttpErrorResponse) => throwError(err)));
  }
}

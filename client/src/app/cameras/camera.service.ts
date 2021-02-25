import { Injectable } from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";
import {Observable, Subject, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";
import {Camera, Uri} from "./Camera";

@Injectable({
  providedIn: 'root'
})
export class CameraService {
  readonly httpJSONOptions ={
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })
  };

  private activeLiveUpdates: Subject<any> = new Subject<any>();

  private cameras:Camera[] =[];

  // List of live views currently active
  private activeLive:Uri[] = [];

  // Currently active recording
  private activeRecording!:Camera;

  constructor(private http:HttpClient, private _baseUrl:BaseUrl) {
    this.getCamerasConfig().subscribe(cameras => {
      // Build up a cameras array which excludes the addition guff which comes from
      // having the cameras set up configured in application.yml
      for (const i in cameras) {
        const c = cameras[i];
        this.cameras.push(c);
      }
    });

  }

  /**
   * Get details of all cameras to be shown live
   */
  getActiveLive():Uri[]
  {
    return this.activeLive;
  }

  /**
   * getActiveRecording: Get details of the active recording
   */
  getActiveRecording():Camera{
    return this.activeRecording;
  }

  /**
   * setActiveLive; Set the list of cameras to be shown for viewing
   * @param cameras: The set of cameras to be viewed live
   */
  setVideoStreams(uris:Uri[]):void
  {
    this.activeLive = uris;
    this.activeRecording = new Camera();

    this.activeLiveUpdates.next(uris);
  }

  getActiveLiveUpdates():Observable<any>
  {
      return this.activeLiveUpdates.asObservable();
  }
  /**
   * setActiveRecording: Set a camera to show recordings from
   * @param camera: The camera whose recordings are to be shown
   */
  setActiveRecording(camera:Camera):void
  {
    this.activeRecording = camera;
    this.activeLive = [];
  }

  /**
   * getCameras: Get details for all cameras
   */
  public getCameras():Camera[]
  {
      return this.cameras;
  }

  /**
   * getCamerasConfig: Get camera set up details from the server
   * @private
   */
  private getCamerasConfig():Observable<any> {
    return this.http.post<{}>(this._baseUrl.getLink("cam", "getCameras"), '', this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }
}

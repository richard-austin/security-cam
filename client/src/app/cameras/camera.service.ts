import { Injectable } from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";
import {Observable, throwError} from "rxjs";
import {catchError, tap} from "rxjs/operators";
import {Camera} from "./Camera";

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

  private cameras:Camera[] =[];

  // List of live views currently active
  private activeLive:Camera[] = [];

  // Currently active recording
  private activeRecording!:Camera;

  constructor(private http:HttpClient, private _baseUrl:BaseUrl) {
    this.getCamerasConfig().subscribe(cameras => {
      // Build up a cameras array which excludes the addition guff which comes from
      // having the cameras set up configured in application.yml
      for (const i in cameras) {
        const c = cameras[i];
        // let camera: Camera = new Camera();
        // camera.name = c.name;
        // c.uris.forEach((uri: any) =>
        //   camera.uris.push(uri)
        // );
        // c.recordings.forEach((recording: any) =>
        //   camera.recordings.push(recording)
        // );
        //
        // camera.recordings = c.recordings;
        this.cameras.push(c);
      }
    });

  }

  /**
   * Get details of all cameras to be shown live
   */
  getActiveLive():Camera[]
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
  setActiveLive(cameras:Camera[]):void
  {
    this.activeLive = cameras;
    this.activeRecording = new Camera();
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

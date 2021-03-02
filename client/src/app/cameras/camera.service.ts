import {Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";
import {Observable, Subject, throwError} from "rxjs";
import {catchError, filter, map, tap} from "rxjs/operators";
import {Camera, Uri} from "./Camera";

declare let moment:any;

/**
 * MotionEvents as received from the server
 */
export class MotionEvents
{
    events: string[] = [];
}

/**
 * LocalMotionEvents: Motion events as delivered to the recordings page
 */
export class LocalMotionEvents
{
  events: {epoch: number, dateTime: string}[] = [];
}

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

  private activeLiveUpdates: Subject<any> = new Subject<any>();

  private cameras: Camera[] = [];

  // List of live views currently active
  private activeLive: Uri[] = [];

  // Currently active recording
  private activeRecording!: Camera;

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {
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
  getActiveLive(): Uri[] {
    return this.activeLive;
  }

  /**
   * getActiveRecording: Get details of the active recording
   */
  getActiveRecording(): Camera {
    return this.activeRecording;
  }

  /**
   * setActiveLive; Set the list of cameras to be shown for viewing
   * @param cameras: The set of cameras to be viewed live
   */
  setActiveLive(uris: Uri[]): void {
    this.activeLive = uris;
    this.activeRecording = new Camera();

    this.activeLiveUpdates.next(uris);
  }

  getActiveLiveUpdates(): Observable<any> {
    return this.activeLiveUpdates.asObservable();
  }

  /**
   * setActiveRecording: Set a camera to show recordings from
   * @param camera: The camera whose recordings are to be shown
   */
  setActiveRecording(camera: Camera): void {
    this.activeRecording = camera;
    this.activeLive = [];
  }

  /**
   * getCameras: Get details for all cameras
   */
  public getCameras(): Camera[] {
    return this.cameras;
  }

  /**
   * cameraForUri: Get the camera having the given uri
   * @param uri
   */
  cameraForUri(uri: Uri): Camera | undefined {
    let cameras: Camera[] = this.getCameras();
    let retVal: Camera | undefined = undefined;

    for (let i = 0; i < cameras.length; ++i) {
      let camera: Camera = cameras[i];
      for (let j = 0; j < camera.uris.length; ++j) {
        let thisuri: Uri = camera.uris[j];

        if (thisuri.uri === uri.uri) {
          retVal = camera;
          break;
        }
      }
    }

    // If not a live uri, try the recording uris
    if (retVal === undefined) {
      for (let i = 0; i < cameras.length; ++i) {
        let camera: Camera = cameras[i];
        for (let j = 0; j < camera.recordings.length; ++j) {
          let thisuri: Uri = camera.recordings[j];

          if (thisuri.uri === uri.uri) {
            retVal = camera;
            break;
          }
        }
      }
    }
    return retVal;
  }

  /**
   * getCamerasConfig: Get camera set up details from the server
   * @private
   */
  getCamerasConfig(): Observable<Camera[]> {
    return this.http.post<Camera[]>(this._baseUrl.getLink("cam", "getCameras"), '', this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  getMotionEvents(motionName: string): Observable<LocalMotionEvents>
  {
    let toDateTime:(epoch:number) => string = (epoch:number):string => {
      let a = new Date(epoch * 1000);
      let months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
      let year = a.getFullYear();
      let month = months[a.getMonth()];
      let date = a.getDate();
      let hour = a.getHours();
      let min = a.getMinutes();
      let sec = a.getSeconds();


      let time = date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec ;

      return time;
    }
    let searchString: string = 'moved-at-';
    let retVal = new LocalMotionEvents();

    let name:{cameraName: string} = {cameraName: motionName};
    return this.http.post<MotionEvents>(this._baseUrl.getLink("motion", "getMotionEvents"), JSON.stringify(name), this.httpJSONOptions).pipe(
      map((value:MotionEvents) => {
        value.events.forEach((event:string) =>{
            let epochTime:number = parseInt(event.substr(event.indexOf(searchString)+searchString.length));
            let formattedDate: string = moment(new Date(epochTime * 1000)).format('DD-MMM-YYYY HH:mm:ss');
            retVal.events.push({epoch: epochTime, dateTime: formattedDate});
        });
        retVal.events.sort((a,b) => a.epoch - b.epoch);
        return retVal;
      }),
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }
}

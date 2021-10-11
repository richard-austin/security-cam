import {EventEmitter, Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";
import {Observable, pipe, Subject, throwError} from "rxjs";
import {catchError, map, tap} from "rxjs/operators";
import {Camera, CameraStream, Stream} from "./Camera";


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

  private cameraStreams: CameraStream[] = [];

  // List of live views currently active
  private activeLive: CameraStream[] = [];

  // Currently active recording
  private activeRecording!: Camera;
  errorEmitter: EventEmitter<HttpErrorResponse> = new EventEmitter<HttpErrorResponse>();

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {
    this.loadCameraStreams().subscribe(cameraStreams => {
        // Build up a cameraStreams array which excludes the addition guff which comes from
        // having the cameraStreams set up configured in application.yml
        for (const i in cameraStreams) {
          const c = cameraStreams[i];
          this.cameraStreams.push(c);
        }
      },
      // Error messages would be shown by the nav component
      reason => this.errorEmitter.emit(reason)
    );
  }

  /**
   * Get details of all cameraStreams to be shown live
   */
  getActiveLive(): CameraStream[] {
    return this.activeLive;
  }

  /**
   * getActiveRecording: Get details of the active recording
   */
  getActiveRecording(): Camera {
    return this.activeRecording;
  }

  /**
   * setActiveLive; Set the list of cameraStreams to be shown for viewing
   * @param cam: The set of cameraStreams to be viewed live
   * @param sendNotification: Send notification of to subscribed processes (such as recording page) if true.
   *                          (defaulted to true)
   */
  setActiveLive(cam: CameraStream[], sendNotification: boolean = true): void {
    this.activeLive = cam;
    this.activeRecording = new Camera();

    if (sendNotification)
      this.activeLiveUpdates.next(cam);
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
   * getCameraStreams: Get details for all cameraStreams
   */
  public getCameraStreams(): CameraStream[] {
    return this.cameraStreams;
  }

  /**
   * loadCameras: Get camera set up details from the server
   * @private
   */
  loadCameras(): Observable<Camera[]> {
    return this.http.post<Camera[]>(this._baseUrl.getLink("cam", "getCameras"), '', this.httpJSONOptions).pipe(
      map((cams: Camera[]) => {
        let cameras: Camera[] = [];

        for (let i in cams) {
          let cam: Camera = cams[i];
          for (const j in cam.streams) {
            // @ts-ignore
            cam.streams[j].selected = cam.streams[j].defaultOnMultiDisplay;
          }
          cameras.push(cam); // Make into a normal array so ngFor can work
        }
        return cameras;
      }),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  /**
   * loadCameraStreams: Get camera streams from the server
   */
  loadCameraStreams(): Observable<CameraStream[]> {
    return this.http.post<Camera[]>(this._baseUrl.getLink("cam", "getCameras"), '', this.httpJSONOptions).pipe(
      map((cams: Camera[]) => {
        let cameraStreams: CameraStream[] = [];

        for (const i in cams) {
          let cam: Camera = cams[i];

          for (const j in cam.streams) {
            let cs = new CameraStream();
            cs.camera = cam;

            // @ts-ignore   // Ignore "Element implicitly has an 'any' type because type 'Map ' has no index signature"
            cs.stream = cam.streams[j] as Stream;
            cs.stream.selected = cs.stream.defaultOnMultiDisplay;
            cameraStreams.push(cs);
          }
        }
        return cameraStreams;
      }),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }
}

import {EventEmitter, Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders} from "@angular/common/http";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";
import {Observable, Subject, throwError} from "rxjs";
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

  readonly httpUploadOptions = {
    headers: new HttpHeaders({
      'Authorization': 'my-auth-token'
    })
  };

  private activeLiveUpdates: Subject<any> = new Subject<any>();
  private configUpdates: Subject<void> = new Subject();

  private cameraStreams: CameraStream[] = [];
  private cameras: Camera[] = [];

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

          if (!this.cameras.find((cs: Camera) => {
            return cs.name === c.camera.name
          }))
            this.cameras.push(c.camera);
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
   * configUpdated: Send message to the nav component when the camera configuration has changed.
   */
  configUpdated(): void {
    this.configUpdates.next()
  }

  /**
   * getConfigUpdates: nave component subscribes to this to be notified of camera configuration changes.
   */
  getConfigUpdates(): Observable<any> {
    return this.configUpdates.asObservable();
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
   * getCameras: Returns an array of cameras
   */
  public getCameras(): Camera[] {
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

  private static createCameraStreams(cams: Object): CameraStream[] {
    let cameraStreams: CameraStream[] = [];

    for (let i in cams) {
      // @ts-ignore
      let cam: Camera = cams[i] as Camera;

      if (cam) {
        let streams: Map<string, Stream> = new Map<string, Stream>();
        for (const j in cam.streams) {
          let cs = new CameraStream();
          cs.camera = cam;

          // @ts-ignore   // Ignore "Element implicitly has an 'any' type because type 'Map ' has no index signature"
          cs.stream = cam.streams[j];
          streams.set(j, cs.stream);
          cs.stream.selected = cs.stream.defaultOnMultiDisplay;
          cameraStreams.push(cs);
        }
        // cam.streams = streams;  // Make the streams object into a map
      }
    }
    return cameraStreams;
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

  /**
   * loadCameraStreams: Get camera streams from the server
   */
  loadCameraStreams(): Observable<CameraStream[]> {
    return this.http.post<Map<string, Camera>>(this._baseUrl.getLink("cam", "getCameras"), '', this.httpJSONOptions).pipe(
      map((cams: any) => {
        return CameraService.createCameraStreams(cams);
      }),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  updateCameras(camerasJON: string):
    Observable<Map<string, Camera>> {
    let cameras = {camerasJSON: camerasJON};
    return this.http.post<any>(this._baseUrl.getLink("cam", "updateCameras"), JSON.stringify(cameras), this.httpJSONOptions).pipe(
      tap((cams) => {
        this.cameras = [];

        for (const key in cams)
          this.cameras.push(cams[key] as Camera);

        this.cameraStreams = CameraService.createCameraStreams(cams);
      }),
      map(cams => {
        return CameraService.convertCamsObjectToMap(cams);
      })
    );
  }

  discover():Observable<Map<string, Camera>> {
    return this.http.post<any>(this._baseUrl.getLink("onvif", "discover"), '', this.httpJSONOptions).pipe(
      tap((cams) => {
        this.cameras = [];

        for (const key in cams)
          this.cameras.push(cams[key] as Camera);

        this.cameraStreams = CameraService.createCameraStreams(cams);
      }),
      map(cams => {
        return CameraService.convertCamsObjectToMap(cams);
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

  getSnapshot(url:string): Observable<Array<any>>{
    const formData: FormData = new FormData();
    formData.append('url', url);
    return this.http.post<Array<any>>(this._baseUrl.getLink("onvif", "getSnapshot"), formData, this.httpUploadOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }
}

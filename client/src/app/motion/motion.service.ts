import {Injectable} from '@angular/core';
import {Observable, throwError} from "rxjs";
import {catchError, map, tap} from "rxjs/operators";
import { HttpClient, HttpErrorResponse, HttpHeaders } from "@angular/common/http";
import {BaseUrl} from "../shared/BaseUrl/BaseUrl";
import {Camera, Stream} from "../cameras/Camera";
import {LocalMotionEvents, MotionEvents} from "../cameras/camera.service";

declare let moment: any;

@Injectable({
  providedIn: 'root'
})
export class MotionService {
  readonly httpJSONOptions = {
    headers: new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'my-auth-token'
    })
  };

  constructor(private http: HttpClient, private _baseUrl: BaseUrl) {

  }


  /**
   * getMotionEvents: Get the list of .m3u8 manifest files of the recordings for this cameraStream. Convert these
   *                  to an array of manifest file name, epoch time and formatted date/time
   * @param cam: The camera
   * @param stream: The stream on cam
   */
  getMotionEvents(cam: Camera, stream: Stream): Observable<LocalMotionEvents> {
    let epochStartDelim: string = '-';
    let epochEndDelim: string = '_';
    let retVal = new LocalMotionEvents();

    let name: { cam: Camera, stream: Stream } = {cam: cam, stream: stream};
    return this.http.post<MotionEvents>(this._baseUrl.getLink("motion", "getMotionEvents"), JSON.stringify(name), this.httpJSONOptions).pipe(
      map((value: MotionEvents) => {
        value.events.forEach((event: string) => {
          let startIndex: number = event.lastIndexOf(epochStartDelim);
          let endIndex: number = event.lastIndexOf(epochEndDelim);
          let epochTime: number = parseInt(event.substr(startIndex + 1, endIndex - startIndex));
          let formattedDate: string = moment(new Date(epochTime * 1000)).format('DD-MMM-YYYY HH:mm:ss');
          retVal.events.push({manifest: event, epoch: epochTime, dateTime: formattedDate});
        });
        retVal.events.sort((a, b) => a.epoch - b.epoch);
        return retVal;
      }),
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err)));
  }

  /**
   * downloadRecording: Download a .mp4 file for the recording whose manifest file name is provided
   * @param stream: The stream that the recordings are from.
   * @param manifest: The manifest file for the recording from which the .mp4 file will be created and downloaded
   */
  async downloadRecording(stream: Stream, manifest: string) {
    let recording: { stream: Stream, manifest: string } = {stream: stream, manifest: manifest};
    return this.http.post(this._baseUrl.getLink("motion", "downloadRecording"), JSON.stringify(recording),
      {
        headers: new HttpHeaders({
          'Content-Type': 'application/json',
          'Authorization': 'my-auth-token',
          'Response-Type': 'Blob'
        }),
        responseType: 'blob'
      }).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    ).toPromise();
  }

  /**
   * deleteRecording: Delete all the files for the recording of which fileNme is one of the files
   * @param stream: Camera stream
   * @param fileName: Name of the file to be deleted for the camera stream
   */
  deleteRecording(stream: Stream, fileName: string): Observable<void> {
    let recording: { stream: Stream, fileName: string } = {stream: stream, fileName: fileName};
    return this.http.post<void>(this._baseUrl.getLink("motion", "deleteRecording"), JSON.stringify(recording), this.httpJSONOptions).pipe(
      tap(),
      catchError((err: HttpErrorResponse) => throwError(err))
    );
  }
}

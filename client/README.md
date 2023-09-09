<h2 style="text-align: center">NVR for CCTV Access Via Web Browser</h2>

### Introduction
This is a Network Video Recorder accessed through ta web browser. designed to run on a Raspberry pi.
Access can be either direct or through a Cloud service. There is no live implementation
of the Cloud Service, but the source code is freely available at
https://github.com/richard-austin/cloud-server.

#### NVR features
* Secure authenticated web access.
* Live, low latency (approx 1 second) video from network cameras with RTSP source.
* View individual or all cameras on one page.
* Recordings of motion events, selectable by date and time.
* Recordings triggered by Motion service (https://github.com/Motion-Project/motion), or by FTP of an image from camera. Many cameras can ftp an image when they detect motion.
* Quick setup of certain camera parameters for SV3C type cameras.
* Hosting of camera admin page, This allows secure access to camera web admin outside the LAN.
* Configuration editor supporting Onvif camera discovery.
* email notification if public IP address changes (when using port forwarding).
* Initial set up of user account from LAN only. Subsequent changes can be done when logged in through existing account.
* Get NVR LAN IP addresses.
* Get Local Wi-Fi details.
* Set/unset NVR Wi-Fi access.
* Enable/Disable access through Cloud server.
* 
# Client

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 11.1.2.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.

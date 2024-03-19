## Development
Pre built .deb files for deployment on a Raspberry pi 4 are available in the Releases section.
The requirements to build the project yourself are detailed below: -
#### Platform for Development
* Ubuntu 23.10 (Mantic Minotaur) on PC (Windows WSL2 or direct boot)

#### The project is verified to build with the following:-
* go version go1.20.1
* Angular CLI: 15.2.0 or greater
* Node: 18.17.1
* npm: 9.9.7
* Package Manager: npm 9.6.7
* Grails Version: 5.3.2
* openjdk version "19.0.2" 2023-01-17
* Gradle 7.6
* Python 3.11.4

Using other versions may cause build issues in some cases.

### Set up build environment
```
git clone git@github.com:richard-austin/security-cam.git
cd security-cam
```
### Build for deployment to Raspberry pi
The Raspberry pi should be running Ubuntu 23.10 (Mantic Minotaur) OS.
```
./gradlew buildDebFile 
```
This will create a deb file with a name of the form security-cam_*VERSION*-*nn*-*ID-dirty*_arm64.deb
Where:-
* *VERSION* is the most recent git repo tag
* *nn* Is the number of commits since the last git tag (not present if no commits since last tag.)
* *ID* The last git commit ID (not present if no commits since last tag.)
* *dirty* "dirty" is included in the name if there were uncommitted changes to the source code when built.

When the build completes navigate to where the .deb file was created:-
```
cd xtrn-scripts-and-config/deb-file-creation
```
scp the .deb file to the Raspberry pi
## Installation on the Raspberry pi
```
sudo apt update
sudo apt upgrade 
```
(restart if advised to after upgrade)

Navigate to where the .deb file is located
<pre>
sudo apt install ./<i>deb_file_name</i>.deb
</pre>
* Wait for installation to complete.
* The Tomcat web server will take 1 - 2 minutes to start
  the application.
* <i>If this is the first installation on the Raspberry pi..</i>
    * Make a note of the product key (a few lines up).
      This will be required if you use the Cloud Service to connect
      to the NVR, otherwise it is not required.
    * <i>Generate the site certificate..</i>
      ```
      cd /etc/security-cam
      sudo ./install-cert.sh
      ```
      Fill in the details it requests (don't put in any information you are not happy with being publicly visible, for
      example you may want to put in a fake email address etc.)
    * nginx will not have started in the absence of the site certificate, so restart nginx.
      ```
      sudo systemctl restart nginx
      ```

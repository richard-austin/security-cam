## Development
Pre built .deb files for deployment on a Raspberry pi 4 are available in the Releases section.
The requirements to build the project yourself are detailed below: -
#### Platform for Development
* Ubuntu 24.04 (Noble Numbat) on PC (Windows WSL2 or direct boot)

#### The project uses the following SDK's/package managers:-
* go version go1.20.1
* Angular CLI: 18.2.3 or greater
* Node: 18.20.4
* Package Manager: npm 10.7.0
* openjdk version "21.0.5" 2024-01-16
* Gradle 8.10.2 
* Python 3.12.3

*To perform a Gradle build, only openjdk and git need to be installed as the
other tools are specified as plugins for the gradle build. The project structure
should be a git repository, as the version number generation uses information from
git about the repo.
To do further development work on the project, you may
need to install some or all of these tools.*

Using other versions may cause build issues in some cases.

### Set up build environment
The project should build on most linux machines with a bash shell. 

*openjdk 17 must be installed on the build machine.*
```
git clone git@github.com:richard-austin/security-cam.git
cd security-cam
```
### Build for deployment to Raspberry pi

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
The Raspberry pi should be running Ubuntu 24.04 (Noble Numbat) OS.
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
      ```
        Example: -
        Product key: U9iO-H45E-1IIU-J743
      ```
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

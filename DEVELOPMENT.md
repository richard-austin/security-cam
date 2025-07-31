## Development
Pre built .deb files for deployment on a Raspberry pi 4 are available in the Releases section.
The requirements to build the project yourself are detailed below: -
#### Platform for Development
* Ubuntu 24.04 (Noble Numbat), or Debian with dpkg version 1.22.06 or later version.
* Open JDK version 21
* git 2.43.0 or later version.

#### The project uses the following SDK's/package managers:-
* go version go1.20.1
* Angular CLI: 18.2.3 or greater
* Node: 18.20.4
* Package Manager: npm 10.7.0
* openjdk version "21.0.5" 2024-01-16
* Gradle 8.11.1 
* Python 3.12.3

*To perform a Gradle build, only openjdk and git need to be installed as the
other tools are specified as plugins in Gradle. The project structure
should be a git repository, as the version number generation uses information from
git about the repo.
To do further development work on the project, you may
need to install some or all of these tools.*

Using other versions may cause build issues in some cases.

### Set up build environment
The project should build on most linux machines with a bash shell, dpkg, openjdk 21 and git installed. 

*openjdk 21 must be installed on the build machine.*
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
* **Only do this if you want to replace the default site certificate with a personalised (but  still untrusted) one!**
     * <i>Generate the site certificate.</i>
        ```
        cd /etc/security-cam
        sudo ./install-cert.sh
        ```
        Fill in the details it requests (don't put in any information you are not happy with being publicly visible, for
        example you may want to put in a fake email address etc.)
        <br><br>
        Restart nginx to use the new certificate.
        ```
        sudo systemctl restart nginx
        ```
## Java ONVIF (Open Network Video Interface Forum)
* Onvif modules

  There are two modules in the security project which provide the onvif functionality (camera discovery and PTZ functions).
  These are onvif-ws-client and onvif-java. They are not separate programs but together form a dependency of the server module.
  When the server project is built, these will be built beforehand if their generated code is not up to date.


ONVIF is a community to standardize communication between IP-based security products (like cameras).

This project aims to improve https://github.com/milg0/onvif-java-lib.<br>
I've tried to convince its author to use to my code but it seems we have different objectives: my goal is to create a project that focus on the funny part of the development of an ONVIF application, **keeping the interaction with the WS as simple as possible** and delege that annoying part to Apache CXF in order to not waste the developer time in writing (and MAINTAINING) code that interacts with ONVIF web services.<br>
My wish is to help other developers willing to contribute to an enterprise-level Java library for ONVIF devices.

### Rebuilding WS stubs


If you need to change the list of managed WSDLs (in onvif/onvif-ws-tests/src/main/resources/wsdl) and thus you need to regenerate the WS Java stubs using the [Apache CXF codegen maven plugin](http://cxf.apache.org/docs/maven-cxf-codegen-plugin-wsdl-to-java.html), you need to go through the following steps:
1. **Download Onvif WSDLs** to onvif/onvif-ws-tests/src/main/resources/wsdl appending the version before the .wsdl suffix.
   For example, from main dir (onvif) use you can run the following shell command:<br>
   ```wget http://www.onvif.org/onvif/ver10/device/wsdl/devicemgmt.wsdl onvif-ws-client/src/main/resources/wsdl/devicemgmt_2.5.wsdl ```
1. **Add required url-rewriting rules (if needed)** to onvif/onvif-ws-tests/src/main/resources/wsdl/jax-ws-catalog.xml
1. **Run the class generation command**: ```./gradlew onvif-java:clean onvif-java:build```
1. To see how to properly add a new ONVIF service to OnvifDevice look into OnvifDevice.init()


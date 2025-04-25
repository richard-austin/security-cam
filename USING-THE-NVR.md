## Using the NVR

### The Menus

The NVR has a menu bar at the top of the page. On a PC screen
this menu bar will normally show the menu names, though on a mobile
device a <img src="README.images/hamburger-2-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> icon must be tapped to reveal them.

Before any option is selected, the
page below the menu bar will be blank.

#### Select Camera
This menu allows selection of live video/audio camera streams. The names
are listed in the form <i>Camera Name(Stream Description)</i>
so there can be more than one stream per camera.

> On selecting the live stream, the video will be shown with a
<img src="README.images/stopwatch-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>
icon and a selector just below it. The selection option is the maximum
> latency in seconds. You can set this to the lowest setting where the video remains stable.
> The lowest usable setting is dependent on various factors and won't
> necessarily be the same for all your cameras.
>
> If two-way audio is enabled, a <img src="README.images/microphone-off-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>
> button and a device selector will also be below the video. Select the required audio input
> device and click the <img src="README.images/microphone-off-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button to
> speak. While audio output is active, the button changes to <img src="README.images/microphone-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>,
> click it again to turn audio output off.
>
> You can zoom in on the video by using the mouse wheel on a PC, or pinch zoom on a mobile. Mouse down and drag will pan the zoomed video while
> touch and move does the same on mobile. This is distinct from PTZ operations which can be done on suitable cameras.

##### Multi Camera View
The last option on the Select Camera menu is Multi Camera View. This shows one stream
from each camera in the configuration. The default stream shown
for a camera will be the one selected as Default On Multi Display.
The camera streams can be switched by clicking the
<img src="README.images/settings-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img>
button at the top left of the page and changing the selection on the displayed pop-up form.
The number of display columns can also be selected on this form.
###### Number of columns
The Multi Camera View shows the cameras in a grid view which fills the width of the
viewport. The number of columns is selectable from one to four using radio buttons at the top 
of the pop-up form. Note that the number of displayed columns are reduced if the 
viewport width is too narrow, so if you have four columns selected, it will
actually show four then three then two then one as you reduce the browser width. 
> Each video on multi camera view will have the latency chasing setter below it. Any
> cameras with two-way audio enabled will also have the <img src="README.images/microphone-off-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> button and device selector.
>
> Mouse wheel zoom and mouse down drag pan functions, and their mobile counterparts can be done on each of the videos.
#### Select Recording
This menu allows selection of recordings made on camera streams.
The names
are listed in the form <i>Camera Name(Stream Description)</i>.
On selection, the latest recording on that stream will be shown.
Earlier recordings can be selected from the date control and Motion
Events selector in the top left of the page. A warning box will appear if there are no recordings for the selected stream.

#### Camera Settings
* **Quick Camera Setup**

  <i>Quick Camera Setup is available only for SV3C and ZXTech cameras (as set in the
  configuration for Camera Type)</i>

  This provides a convenient means of setting the night lighting mode, and Camera Name.
  The camera can also be rebooted if required.
* **Camera Admin**

  Note that for this function to be accessible outside the LAN, port forwarding must be set up for port 446.

  Provides access to camera web admin page through the NVR. Access is
  protected by the NVR authentication system (Spring Security) as well as the NVR's access token system. SV3C
  and ZXTech camera credentials are provided by the NVR if they
  were set up in the camera configuration page, otherwise they will need to be entered after selecting the camera.
  Any other camera types will need their credentials entered after the camera is selected.

#### General
* **Configure Camera Setup**

  Set up the NVR for your set of cameras (which must be on the same LAN as the NVR).

  This is described in more detail under the section "Set Up Cameras" above.
* **Log Off**

  Log off from the NVR, a dialogue box allows confirmation or cancelling this operation.
* **Change Password**

  User must first enter the current password, then enter and confirm a new one.
* **Change Account Email**

  The email address is where password reset links will be sent when requesting from the login page. It is also where warning messages are sent if the public IP address has changed (for use with external access via port forwarding).

  User must first enter the password, then enter and confirm the new email address.
* **Set Up Guest Account**

  The guest account allows viewing the live streams and recordings, but does not allow any sort of admin operation which could alter the configuration.

  Enter and confirm a password for the guest account. This password
  is used with the username "guest" to log in. The checkbox allows the guest account to be enabled and disabled, while the password remains unaltered.

* **Save current IP**

  Save the current public IP that the NVR appears at (with port forwarding set up).

  If the public IP subsequently changes from this, a warning email will be sent to the registered
  email address, giving the new public IP address.

  This function is only used after setting up a new NVR or after the public IP has changed.
* **Get Active Local IP Addresses**

  Get the LAN addresses of the NVR. There are IP addresses for the Wi-Fi and Ethernet interfaces.
* **Wifi Admin**
    * **Local Wifi Details**

      Lists the Wi-Fi access points in the area along with msgQueue strength and other information.
    * **Wifi Settings**

      Setup or enable/disable the NVR Wi-Fi connection.

      To use this the NVR must have an Ethernet connection, and the browser must be connected to the NVR through the Ethernet IP address.
* **Set CloudProxy Status**

**Note, An experimental Cloud Service has been developed (see <a href="https://github.com/richard-austin/cloud-server">here</a>). This works in conjunction with <a href="https://github.com/richard-austin/activemq-for-cloud-service">ActiveMQ</a>**
For information on accessing the NVR through the Cloud Service, see README_CLOUD.md
* **Admin Functions**

  The initial setup functions used to set credentials for a direct access account.
  This can be accessed on the LAN without authentication at http://nvr_lan_ip_addr:8080/cua/.

  Click on the <img src="README.images/hamburger-2-svgrepo-com.svg" width="20"  style="position: relative; top: 5px"></img> icon at the top left for the menu options·
    * **Create or Update User Account**

      When accessed via the General -> Admin Functions menu, a user account will already exist.
      In this case you can use this to modify the username and/or password and/or email address for the account.
      In any case, all those fields must be entered before clicking Update Account.
    * **Set Up SMTP Client**

      The SMTP Client must be set up for the NVR to send emails (reset password and changed public IP notification).

      Use this if you need to set up or change the settings for the NVR's SMTP client.
      All fields must be entered before clicking Confirm. Any existing values will be
      shown in their fields.
* **About**

  Version information for the NVR and open source software used in the project..

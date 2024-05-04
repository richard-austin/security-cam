## Accessing the NVR through the Cloud Service

### Introduction

The Cloud Service gives an alternative method of access to NVRs. The NVR makes a client
connection to the Clouds ActiveMQ server, which obviates the requirement
for port forwarding when the NVR is behind NAT, when the Cloud Server is on a public IP.
The Cloud Service can host one or more NVRs, each accessible separately
using the appropriate account credentials. These credentials are set up on the Cloud
Service and are not necessarily the same as the NVRs direct access
credentials. It is possible to access NVRs which have no direct
access credentials on the Cloud server. To set up a cloud account for an NVR
you must have the product ID for the NVR. This ID is generated on the initial
installation of the NVR, and is shown near the end of the text that comes
up on the terminal during installation. It is not changed on subsequent upgrades.

There is no live implementation of the Cloud Service, the source code
is <a href="https://github.com/richard-austin/cloud-server">here</a>.

The Cloud Service and <a href="https://github.com/richard-austin/activemq-for-cloud-service">ActiveMQ</a> should be set
up
(with necessary configuration adjustments) on (preferably) a public server to enable the
use of NVRs through this means.
## Setting up the NVR for the Cloud Service
### <div id="set-amq-params">Set ActiveMQ username, password and host</div>
To connect to ActiveMQ, the correct credentials and ActiveMQ host have to be entered. 
* If a local NVR account has been set up: -
  * Open the General menu and select **Admin Functions**
  * Select **Set ActiveMQ Credentials**
* If there is no local NVR account
  * From within the same LAN as the NVR, open a browser at ***NVR address:8080/cua***
  * Select **Set ActiveMQ Credentials**
* Enter the username (usually cloud) given on initial installation of **activemq-for-cloud-service**
* Enter the password given on initial installation of **activemq-for-cloud-service** (this will be a 20 character
  string consisting of upper and lower case letters and numbers).
* Enter the password again in the **Confirm Password** field
* Enter the ActiveMQ host (this can be an IP address or hostname).
* Click the **Update Creds** button, the new settings will be applied.
#### Changing the ActiveMQ username and password
* Open the General menu and select "Set ActiveMQ Credentials"
* The credentials can be changed by the admin user at any time by repeating the above procedure, the current ActiveMQ
  host will appear as the default in the ActiveMQ Host field.
* Click the **Update Creds** button, the new settings will be applied.
#### <div id="amq-host">Changing the ActiveMQ Host</div>
* Open the General menu and select "Set ActiveMQ Credentials"
* To change the host without changing the username and password, leave the username and password fields blank
  and update the ActiveMQ host field only.
* Click the **Update Creds** button, the new settings will be applied.
* If there is a local account for direct access on the NVR, you should ensure that
  the link to the Cloud Service is enabled: -
    * **General** menu
        * **Set CloudProxy Status**
            * Ensure the checkbox is checked.
  * NVR connection to Cloud ActiveMQ
      * If the connection is successful, you should see a green dialogue box
  with the message "**Success: Connected to the Cloud successfully**". 
  You should then be able to use the Cloud Service. 
      * If the connection to ActiveMQ fails, the message <span style="color: red; font-weight: bold">Not Connected To
        Transport</span> will be shown.
         * This can mean that the ActiveMQ host, username or password are not set correctly (See Updating the ActiveMQ username and password), 
           or that ActiveMQ is not running correctly.
      * The error message <span style="color: red">Product key was not accepted by the Cloud server</span>
        indicates that the NVR connected successfully to ActiveMQ, but it was not authenticated on the Cloud Server. Check that the Cloud Server is running, and it is configured correctly to connect to ActiveMQ.

* If there is no local NVR account, connection to the Cloud Service will be automatically enabled. 
In this case the NVR cannot indicate if it has failed to connect to Active MQ.
You can check the cloudproxy.log at /var/log/security-cam
<div style="margin-left: 2rem">

* If connection was successful, the log should have a line like: -
<div style="color: lightgreen; margin-left: 2rem">2024-02-22 16:44:20.341 INFO  CLOUDPROXY [pool-248-thread-1] - loginToCloud:191 - Connected successfully to the Cloud</div>
   
* If the NVR failed to connect to ActiveMQ, the log will have a line similar to: - 

<div style="color: red; margin-left: 2rem">2024-02-22 16:42:00.747 ERROR CLOUDPROXY [pool-230-thread-1] - showExceptionDetails:539 - javax.jms.JMSException exception in Clo
     udAMQProxy.start: Could not connect to broker URL: ssl://192.168.1.82:61617?socket.verifyHostName=false. Reason: java.net.NoRoute
     ToHostException: No route to host</div>
<div style="margin-left: 2rem; margin-top: 0.25rem">This can mean that the <a href="#amq-host">ActiveMQ host</a> is not set correctly or that 
  ActiveMQ is not running.</div>

* If the log has the following line: -
<div style="color: red; margin-left: 2rem">2024-02-22 16:43:57.623 ERROR CLOUDPROXY [pool-245-thread-1] - loginToCloud:200 - Product key was not accepted by the Cloud server</div>
<div style="margin-left: 2rem; margin-top: 0.25rem">This indicates that the NVR connected successfully to ActiveMQ, but it was not authenticated on the Cloud Server. 
Check that the Cloud Server is running, and it is configured correctly to connect to ActiveMQ.</div>
</div>

* **Hosting of camera admin page**, This feature is not supported through the Cloud Service.

When the NVR is accessed through the Cloud service, port forwarding is not required
as all communication is through a client connection that the NVR makes to the
Cloud service. Camera web admin pages are not accessible through the Cloud Service.

### Cloudproxy parameters in application.yml

#### These are under the cloudProxy section: -

| *Parameter*                | Description                                                                                                                                                                                                                                      |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| enabled                    | CloudProxy will run if true (and CloudProxy status is enabled on the NVR General menu, or the NVR has no local user account)                                                                                                                     |
| productKeyPath             | Path to the file containing the NVR Product key                                                                                                                                                                                                  |
| cloudActiveMQUrl           | Url to the ActiveMQ service that the NVRs and the Cloud server connect to. This should begin with failover://ssl: The host part will be replaced by the host entered in the **<a href="#set-amq-params">Set ActiveMQ Credentials</a>** dialogue. |
| activeMQInitQueue          | The name of the queue in ActiveMQ through which connections are initiated. This must be the same on all NVRs and the Cloud server.                                                                                                               |
| webServerForCloudProxyHost | The host name for the NVRs cloud web server (normally localhost)                                                                                                                                                                                 |
| webServerForCloudProxyPort | The port for the NVRs web server (Normally 8088) This service is set up on nginx to provide special access for Cloud connections.                                                                                                                | 
| logLevel                   | The log level for cloudproxy.log (normally located at /var/log/security-cam)                                                                                                                                                                     |

Please see the README.md for the <a href="https://github.com/richard-austin/cloud-server">Cloud Service</a> and <a href="https://github.com/richard-austin/activemq-for-cloud-service">ActiveMQ for Cloud Service</a> for details on setting up a cloud account and using the Cloud service.


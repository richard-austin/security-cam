package org.onvif.client;

import de.onvif.soap.OnvifDevice;
import de.onvif.soap.ProcessedPullMessagesResponse;
import de.onvif.soap.PullMessagesCallbacks;
import de.onvif.soap.PullPointSubscriptionHandler;
import jakarta.xml.soap.SOAPException;
import org.onvif.ver10.events.wsdl.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PullPointTest implements PullMessagesCallbacks {
    private PullPointTest() {

    }
    static private PullPointTest theInstance =null;

    public static void main(String[] args) throws IOException {
        if(theInstance == null)
            theInstance = new PullPointTest();

        theInstance.run(args);
    }

    public void run(String[] args) throws IOException {
        OnvifCredentials creds = GetTestDevice.getOnvifCredentials(args);
        System.out.println("Connect to camera, please wait ...");

        OnvifDevice cam = null;
        try {
            cam = new OnvifDevice(creds.getHost(), creds.getUser(), creds.getPassword());
        } catch (ConnectException | SOAPException e1) {
            System.err.println("No connection to device with ip " + creds + ", please try again.");
            System.exit(0);
        } catch (URISyntaxException e) {
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        System.out.println("Connected to device " + cam.getDeviceInfo());

        try {
            PullPointSubscriptionHandler ppsh = new PullPointSubscriptionHandler(cam, this);
            ppsh.createPullPointSubscription(("tns1://."));
            ppsh.subcribe();
         } catch (Exception e) {
            System.out.println(e.getClass().getName() + " " + e.getMessage());
        }
    }

    Map<Date, ProcessedPullMessagesResponse> responses = new HashMap<>();

    @Override
    public void onPullMessagesReceived(PullMessagesResponse pullMessages) {
        ProcessedPullMessagesResponse ppmr = new ProcessedPullMessagesResponse(pullMessages);
        ppmr.responseData.forEach((x) ->
                x.Data.forEach((data) ->
                        System.out.println(x.created + " " + x.topic + " " + data.Name + " " + data.Value)));

        responses.put(new Date(), ppmr);
    }
}

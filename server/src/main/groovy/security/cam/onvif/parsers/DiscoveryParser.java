package security.cam.onvif.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import security.cam.onvif.OnvifUtils;
import security.cam.onvif.DiscoveryMode;
import security.cam.onvif.models.Device;
import security.cam.onvif.models.DiscoveryType;
import security.cam.onvif.models.OnvifDevice;
import security.cam.onvif.models.UPnPDevice;
import security.cam.onvif.responses.OnvifResponse;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tomas Verhelst on 04/09/2018.
 * Copyright (c) 2018 TELETASK BVBA. All rights reserved.
 */
public class DiscoveryParser extends OnvifParser<List<Device>> {

    //Constants
    public static final String TAG = DiscoveryParser.class.getSimpleName();
    private static final String LINE_END = "\r\n";
    private static String KEY_UPNP_LOCATION = "LOCATION: ";
    private static String KEY_UPNP_SERVER = "SERVER: ";
    private static String KEY_UPNP_USN = "USN: ";
    private static String KEY_UPNP_ST = "ST: ";

    //Attributes
    private DiscoveryMode mode;
    private String hostName;

    //Constructors
    public DiscoveryParser(DiscoveryMode mode) {
        this.mode = mode;
        hostName = "";
    }

    //Methods

    @Override
    public List<Device> parse(OnvifResponse response) {
        List<Device> devices = new ArrayList<>();

        switch (mode) {
            case ONVIF:
                devices.addAll(parseOnvif(response));
                break;
            case UPNP:
                devices.add(parseUPnP(response));
                break;
        }

        return devices;
    }

    private List<Device> parseOnvif(OnvifResponse response) {
        ArrayList<Device> devices = new ArrayList<>();
        try {
            getXpp().setInput(new StringReader(response.getXml()));
            eventType = getXpp().getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG && getXpp().getName().equals("Types")) {
                    getXpp().next();
                    String type = getXpp().getText();

                    if (mode.equals(DiscoveryMode.ONVIF) && type.contains(DiscoveryType.NETWORK_VIDEO_TRANSMITTER.type)) {
                        String uri = OnvifUtils.retrieveXAddrs(getXpp());
                        devices.addAll(parseDevicesFromUri(uri));
                    }
                }

                eventType = getXpp().next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return devices;
    }

    private Device parseUPnP(OnvifResponse response) {
        String header = response.getXml();
        String location = parseUPnPHeader(header, KEY_UPNP_LOCATION);
        String server = parseUPnPHeader(header, KEY_UPNP_SERVER);
        String usn = parseUPnPHeader(header, KEY_UPNP_USN);
        String st = parseUPnPHeader(header, KEY_UPNP_ST);
        return new UPnPDevice(getHostName(), header, location, server, usn, st);
    }

    //Properties

    private String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    private List<OnvifDevice> parseDevicesFromUri(String uri) {
        List<OnvifDevice> devices = new ArrayList<>();
        String[] uris = uri.split("\\s+");
        for (String address : uris) {
            OnvifDevice device = new OnvifDevice(getHostName());
            device.addAddress(address);
            devices.add(device);
        }

        return devices;
    }

    private String parseUPnPHeader(String header, String whatSearch) {
        String result = "";
        int searchLinePos = header.indexOf(whatSearch);
        if (searchLinePos != -1) {
            searchLinePos += whatSearch.length();
            int locColon = header.indexOf(LINE_END, searchLinePos);
            result = header.substring(searchLinePos, locColon);
        }
        return result;
    }


}

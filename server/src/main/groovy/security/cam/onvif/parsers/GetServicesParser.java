package security.cam.onvif.parsers;

import security.cam.onvif.OnvifUtils;
import security.cam.onvif.models.OnvifServices;
import security.cam.onvif.models.OnvifType;
import security.cam.onvif.responses.OnvifResponse;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by Tomas Verhelst on 04/09/2018.
 * Copyright (c) 2018 TELETASK BVBA. All rights reserved.
 */
public class GetServicesParser extends OnvifParser<OnvifServices> {

    @Override
    public OnvifServices parse(OnvifResponse response) {
        OnvifServices path = new OnvifServices();

        try {
            getXpp().setInput(new StringReader(response.getXml()));
            eventType = getXpp().getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG && getXpp().getName().equals("Namespace")) {
                    getXpp().next();
                    String currentNamespace = getXpp().getText();

                    if (currentNamespace.equals(OnvifType.GET_DEVICE_INFORMATION.namespace)) {
                        String uri = OnvifUtils.retrieveXAddr(getXpp());
                        path.setDeviceInformationPath(OnvifUtils.getPathFromURL(uri));
                    } else if (currentNamespace.equals(OnvifType.GET_MEDIA_PROFILES.namespace)
                            || currentNamespace.equals(OnvifType.GET_STREAM_URI.namespace)) {
                        String uri = OnvifUtils.retrieveXAddr(getXpp());
                        path.setProfilesPath(OnvifUtils.getPathFromURL(uri));
                        path.setStreamURIPath(OnvifUtils.getPathFromURL(uri));
                    }
                }

                eventType = getXpp().next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return path;
    }

}

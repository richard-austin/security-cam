package security.cam.onvif.listeners;

import security.cam.onvif.models.OnvifDevice;
import security.cam.onvif.responses.OnvifResponse;


/**
 * Created by Tomas Verhelst on 03/09/2018.
 * Copyright (c) 2018 TELETASK BVBA. All rights reserved.
 */
public interface OnvifResponseListener {

    void onResponse(OnvifDevice onvifDevice, OnvifResponse response);

    void onError(OnvifDevice onvifDevice, int errorCode, String errorMessage);
}

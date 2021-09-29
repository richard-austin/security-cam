package security.cam.onvif.requests;

import security.cam.onvif.models.OnvifType;

/**
 * Created by Tomas Verhelst on 03/09/2018.
 * Copyright (c) 2018 TELETASK BVBA. All rights reserved.
 */
public interface OnvifRequest {

    String getXml();

    OnvifType getType();

}

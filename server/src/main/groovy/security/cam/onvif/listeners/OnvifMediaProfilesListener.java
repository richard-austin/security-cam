package security.cam.onvif.listeners;

import security.cam.onvif.models.OnvifDevice;
import security.cam.onvif.models.OnvifMediaProfile;


import java.util.List;

/**
 * Created by Tomas Verhelst on 03/09/2018.
 * Copyright (c) 2018 TELETASK BVBA. All rights reserved.
 */
public interface OnvifMediaProfilesListener {

    void onMediaProfilesReceived(OnvifDevice device, List<OnvifMediaProfile> mediaProfiles);

}

package security.cam.onvif.listeners;

import java.util.List;

import security.cam.onvif.models.Device;

/**
 * Created by Tomas Verhelst on 04/09/2018.
 * Copyright (c) 2018 TELETASK BVBA. All rights reserved.
 */
public interface DiscoveryCallback {

    void onDiscoveryStarted();

    void onDevicesFound(List<Device> devices);

    void onDiscoveryFinished();

}

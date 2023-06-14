package security.cam.interfaceobjects

import security.cam.audiobackchannel.BackchannelClientHandler

interface OnReady {
    void ready(BackchannelClientHandler handler)
}

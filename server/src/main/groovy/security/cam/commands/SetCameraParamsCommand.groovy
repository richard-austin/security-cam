package security.cam.commands

import server.cameraType

class SetCameraParamsCommand extends CameraParamsCommand {
    Integer cameraType
    String infraredstat
    String lamp_mode
    String wdr
    String cameraName

    static constraints = {
        cameraType(nullable: false, inList:[1, 2])
        infraredstat(nullable: true, inList: [null, 'auto', 'open', 'close'],
        validator:{infraredstat, cmd ->
            if(cmd.getCameraType() == cameraType.sv3c.ordinal()) {
                // Not validating, just setting up params in the base class from the command values
                cmd.params = 'cmd=setinfrared'
                cmd.params += "&-infraredstat=${cmd.infraredstat}"
                cmd.params += "&cmd=setoverlayattr&-region=0&cmd=setoverlayattr&-region=1"
                cmd.params += "&-name=${cmd.cameraName}"
            }
            return
        })
        wdr(nullable: true, inList:["on", "off"])
        lamp_mode(nullable: true, inList:["0", "1", "2"],
        validator: {lamp_mode, cmd ->
            // Not validating, just setting up params in the base class from the command values
            if(cmd.getCameraType() == cameraType.zxtechMCW5B10X.ordinal()) {
                cmd.params = 'cmd=setlampattrex'
                cmd.params += "&-lamp_mode=${cmd.lamp_mode}"
                cmd.params += "cmd=setimageattr"
                cmd.params += "&-wdr=${cmd.wdr}"
                cmd.params += "&cmd=setoverlayattr&-region=0&cmd=setoverlayattr&-region=1"
                cmd.params += "&-name=${cmd.cameraName}"
            }
            return
        })
    }
}

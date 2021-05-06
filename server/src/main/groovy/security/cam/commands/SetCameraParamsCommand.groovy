package security.cam.commands

class SetCameraParamsCommand extends CameraParamsCommand {
    String infraredstat

    static constraints = {
        infraredstat(nullable: false, inList: ['auto', 'open', 'close'],
        validator:{infraredstat, cmd ->
            // Not validating, just setting up params in the base class from the command values
            cmd.params='cmd=setinfrared'
            cmd.params+="&-infraredstat=${cmd.infraredstat}"
            return
        })
    }
}

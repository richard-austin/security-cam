package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.commands.GetMotionEventsCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

import java.nio.file.Path
import java.nio.file.Paths

@Transactional
class MotionService {
    GrailsApplication grailsApplication
    LogService logService

     /**
     * Get the names of the motion event files
     * @param cameraName
     * @return
     */
    def getMotionEvents(GetMotionEventsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            def baseDir = grailsApplication.config.camerasHomeDirectory
            Path motionEventsDirectory = Paths.get(baseDir as String, cmd.camera.recording.location as String)
            File f = new File(motionEventsDirectory.toString())

            // Keep only the entries for the given cameraName, or return all if it's null
            result.responseObject = f.list(new FilenameFilter() {
                @Override
                boolean accept(File file, String s) {
                    return s.endsWith('.m3u8')
                }
            })

            if (result.responseObject == null) {
                result.status = PassFail.FAIL
                result.error = "Cannot access motion events"
                logService.cam.error("Error in getMotionEvents: "+result.error)
            }
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getMotionEvents: "+ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

    private Map<String, TreeMap<Long, Double>> epochToOffsetMaps = new HashMap<String, Map<Long, Double>>()

    /**
     * epochToOffsetHasEntryFor: Check if the epoch to offset map contains the given key
     * @param motionName: The motionName key
     * @return true if key is in map else false
     */
    ObjectCommandResponse epochToOffsetHasEntryFor(String motionName)
    {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            result.responseObject = epochToOffsetMaps.containsKey(motionName)
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in epochToOffsetHasEntryFor: "+ex.getMessage())
            result.status = PassFail.FAIL
            result.error = "Exception in epochToOffsetHasEntryFor: " + ex.getMessage()
        }
        return result
    }

    /**
     * saveEpochToOffsetMap: Save the TreeMap of epoch times vs offset time into recording generated from the
     *                       master manifest file. Note that as the recording plays, the oldest .ts and manifest files are
     *                       deleted and  new master manifest is generated (every 2 minutes at the time of writing, so this map must
     *                       be updated just before it is to be used each time.
     * @param map: The map to be saved
     * @param motionName: The name of the motion events (containing epoch times) that will be used against this map
     * @return The map
     */
    private ObjectCommandResponse saveEpochToOffsetMap(Map<Long, Double> map, String motionName)
    {
        ObjectCommandResponse retVal = new ObjectCommandResponse()
        try {
            epochToOffsetMaps[motionName] = map as TreeMap
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in saveEpochToOffsetMap: "+ex.getMessage())
            retVal.status = PassFail.FAIL
            retVal.error = ex.getMessage()
        }
        return retVal  // Nothing returned in responseObject
    }

    /**
     * getOffsetForEpoch: Using the (just updated) epoch to offset times map, get the nearest offset time below the one which
     *                              will take you to the epoch time in the recording
     * @param epoch: Tje epoch time for which you want the corresponding time offset
     * @param motionName: The name of the motion events (containing epoch times) that will be used against this map
     * @return: The offset time
     */
    ObjectCommandResponse getOffsetForEpoch(Long epoch, String motionName)
    {
        ObjectCommandResponse result = new ObjectCommandResponse()
        logService.cam.debug("start getOffsetForEpoch: (timing check)")
        try {
            Map.Entry<Long, Double> floorEntry, ceilEntry
            Double retVal = -1
            TreeMap<Long, Double> map = epochToOffsetMaps[motionName]
            if (map) {
                floorEntry = map.floorEntry(epoch)
                if (floorEntry && floorEntry.key == epoch) {
                    logService.cam.debug("getOffsetForEpoch: using floor entry ("+epoch.toString()+":"+floorEntry.value.toString())
                    retVal = floorEntry.value
                }
                else {
                    ceilEntry = map.ceilingEntry(epoch)
                    if (ceilEntry && ceilEntry.key == epoch) {
                        logService.cam.debug("getOffsetForEpoch: using ceiling entry ("+epoch.toString()+":"+ceilEntry.value.toString())
                        retVal = ceilEntry.value
                    }
                    else if (floorEntry && ceilEntry) {
                        // Epoch is between the floor and ceiling key values, so do interpolation
                        Double o1 = floorEntry.value, o2 = ceilEntry.value
                        Long e1 = floorEntry.key, e2 = ceilEntry.key
                        retVal = (epoch - e1) * (o2 - o1) / (e2 - e1) + o1
                        logService.cam.debug("getOffsetForEpoch: epoch is between floor and ceiling values (epoch="+epoch.toString()+":\n"+
                                "floor="+floorEntry.value+"\n"+
                                "ceiling="+ceilEntry.value.toString()+"\n"+
                                "interpolated="+retVal)
                    }
                }
            }
            result.responseObject = retVal
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in getOffsetForEpoch: "+ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        logService.cam.debug("finished getOffsetForEpoch: (timing check)")
        return result
    }
}

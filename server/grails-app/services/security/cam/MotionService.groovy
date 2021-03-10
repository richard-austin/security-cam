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
     * createTimeVsOffsetMap: Create the lookup table which maps epoch times to offset times into the recording.
     *                        The epoch times which form part of the motion event file names can then be used
     *                        to seek the events in the recording
     * @param cmd: Command object containing the camera details
     * @return: The complete map
     */
    private ObjectCommandResponse createTimeVsOffsetMap(GetMotionEventsCommand cmd)
    {
        logService.cam.debug("start createTimeVsOffsetMap: (timing check)")

        ObjectCommandResponse result = new ObjectCommandResponse()

        Path pathToManifest = Paths.get(grailsApplication.config.camerasHomeDirectory as String, cmd.camera.recording.masterManifest as String)
        Map<Long, Double> map = new TreeMap<Long, Double>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(pathToManifest.toString()))

            Double timeTotal = 0
            Long epoch

            Long lastEpoch = -1
            Integer interManifestTime = 0
            String line = reader.readLine()

            while (line != null)
            {
                if(line.startsWith('#EXTINF:'))
                {
                    // Get the seconds on the #EXTINF:
                    String strSecs = line.substring('#EXTINF:'.length(), line.length()-1)
                    Double secs = Double.parseDouble(strSecs)
                    timeTotal += secs

                    // Get the epoch time from the .ts file name
                    line = reader.readLine()
                    String strEpoch = line.substring(line.lastIndexOf('-')+1, line.lastIndexOf('_'))
                    epoch = Long.parseLong(strEpoch)
                    if(epoch != lastEpoch) {
                        lastEpoch = epoch
                        interManifestTime = 0
                        map.put(epoch, timeTotal)
                    }
                    else {
                        interManifestTime += secs
                        map.put(epoch+interManifestTime, timeTotal)
                    }
                }
                line = reader.readLine()
            }
            reader.close()
            result.responseObject = map
            logService.cam.debug("createTimeVsOffsetMap: success")
        }
        catch(Exception ex)
        {
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
            logService.cam.error("Exception in createTimeVsOffsetMap: "+result.error)
        }
        logService.cam.debug("finishing createTimeVsOffsetMap: (timing check)")

        return result
    }

     /**
     * Get the names of the motion event files
     * @param cameraName
     * @return
     */
    def getMotionEvents(GetMotionEventsCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            def baseDir = grailsApplication.config.camerasHomeDirectory
            Path motionEventsDirectory = Paths.get(baseDir as String, grailsApplication.config.motion.motionEventsDirectory as String)
            File f = new File(motionEventsDirectory.toString())

            // Keep only the entries for the given cameraName, or return all if it's null
            result.responseObject = f.list(new FilenameFilter() {
                @Override
                boolean accept(File file, String s) {
                    if (cmd.camera.name == null)
                        return true
                    else
                        return s.startsWith(cmd.camera.motionName as String)
                }
            })

            if (result.responseObject == null) {
                result.status = PassFail.FAIL
                result.error = "Cannot access motion events"
                logService.cam.error("Error in getMotionEvents: "+result.error)
            }
            else
            {
                ObjectCommandResponse resp = createTimeVsOffsetMap(cmd)

                if(resp.status == PassFail.PASS) {
                    Map<Long, Double> map = resp.responseObject as Map<Long, Double>
                    resp = saveEpochToOffsetMap(map, cmd.camera.motionName as String)
                    if(resp.status == PassFail.FAIL) {
                        logService.cam.error("Exception in getMotionEvents: "+resp.error)
                        result = resp
                    }
                }
                else {
                    logService.cam.error("Exception in getMotionEvents: "+resp.error)
                    result = resp
                }
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

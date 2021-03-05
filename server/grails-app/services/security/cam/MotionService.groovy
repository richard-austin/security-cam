package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ObjectCommandResponse

import java.nio.file.Path
import java.nio.file.Paths

@Transactional
class MotionService {
    GrailsApplication grailsApplication
    CamService camService

    private Map<Long, Double> createTimeVsOffsetMap(GetMotionEventsCommand cmd)
    {
        Path pathToManifest = Paths.get(grailsApplication.config.camerasHomeDirectory as String, cmd.camera.recording.masterManifest as String)
        Map<Long, Double> retVal = new TreeMap<Long, Double>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(pathToManifest.toString()))

            Double timeTotal = 0
            Long epoch

            Long lastEpoch = -1
            Integer interManifestTime = 0
            String line = reader.readLine()
            while (line != null)
            {
                line = reader.readLine()
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
                        retVal.put(epoch, timeTotal)
                    }
                    else {
                        interManifestTime += secs
                        retVal.put(epoch+interManifestTime, timeTotal)
                    }
                }
            }
            reader.close()
        }
        catch(Exception ex)
        {
// TODO Error handling and return result in ObjectCommandResponse
        }
        return retVal
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
            }
            else
            {
                Map<Long, Double> manifest = createTimeVsOffsetMap(cmd)
                if(manifest == null || manifest.size() == 0) {
                    result.status = PassFail.FAIL
                    result.error = "Cannot get manifest file"
                }
                else
                {
                    saveEpochToOffsetMap(manifest, cmd.camera.motionName as String)
                    getOffsetForEpoch(1614629718, cmd.camera.motionName as String)
                }
            }
        }
        catch (Exception ex) {
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

    private Map<String, Map<Long, Double>> epochToOffsetMaps = new HashMap<String, Map<Long, Double>>()

    /**
     * saveEpochToOffsetMap: Save the TreeMap of epoch times vs offset time into recording generated from the
     *                       master manifest file. Note that as the recording plays, the oldest .ts and manifest files are
     *                       deleted and  new master manifest is generated (every 2 minutes at the time of writing, so this map must
     *                       be updated just before it is to be used each time.
     * @param map: The map to be saved
     * @param motionName: The name of the motion events (containing epoch times) that will be used against this map
     * @return The map
     */
    def saveEpochToOffsetMap(Map<Long, Double> map, String motionName)
    {
        epochToOffsetMaps[motionName] = map
    }

    /**
     * getOffsetForEpoch: Using the (just updated) epoch to offset times map, get the nearest offset time below the one which
     *                              will take you to the epoch time in the recording
     * @param epoch: Tje epoch time for which you want the corresponding time offset
     * @param motionName: The name of the motion events (containing epoch times) that will be used against this map
     * @return: The offset time
     */
    Double getOffsetForEpoch(Long epoch, String motionName)
    {
        Map.Entry<Long, Double> floorEntry
        Double retVal = -1
        TreeMap<Long, Double> map = epochToOffsetMaps[motionName]
        if(map) {
            floorEntry = map.floorEntry(epoch)
            if(floorEntry)
                retVal = floorEntry.value
        }

        return retVal
    }
}

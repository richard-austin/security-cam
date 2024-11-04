package com.securitycam.timers

/**
 * ResetPasswordParameterTimerTask: Removes the map entry for the reset password parameter after the timeout period
 */
class ResetPasswordParameterTimerTask extends  TimerTask{
    String uniqueId
    Map<String, String> map
    Map<String, Timer> timerMap

    ResetPasswordParameterTimerTask(String uniqueId, Map<String, String> map, Map<String, Timer> timerMap)
    {
        this.uniqueId = uniqueId
        this.map = map
        this.timerMap = timerMap
    }

    @Override
    void run() {
        map.remove(uniqueId)
        timerMap.remove(uniqueId)
    }
}

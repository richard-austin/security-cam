package com.securitycam.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.securitycam.configuration.Config
import com.securitycam.controllers.Camera
import com.securitycam.controllers.Stream
import com.securitycam.mediaserverutils.AVData
import com.securitycam.recording.Recorder
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RecorderFactory {
    LogService logService
    HttpServer server
    Map<String, Recorder> recorders
    Config config

    @Autowired
    RecorderFactory(Config config, LogService logService) {
        this.config = config
        this.logService = logService
        recorders = new HashMap<>()
        startHttpServer()
    }

    Recorder addRecorder(String cameraName, Camera cam, Stream stream, AVData avData) {
        Recorder recorder = null

        if (stream?.recording?.enabled && !recorders.keySet().contains(cameraName)) {
            recorders.put(cameraName, new Recorder(cam, stream, avData, config, logService))
        }
        return recorder
    }

    void write(AVPacket pkt, String cameraName) {
        Recorder rec = recorders.get(cameraName)
        if (rec != null)
            rec.write(pkt, cameraName)
    }

    void startHttpServer() {
        HttpServer server = HttpServer.create(new InetSocketAddress(8086), 0)
        server.createContext("/", new MyHandler())
        server.setExecutor(null) // default executor
        server.start()
        logService.cam.info("Recording trigger Server running on port 8086")
    }

    void closeThisRecorder(String cameraName) {
        final recorder = recorders.get(cameraName)
        if(recorder != null) {
            recorder.stop()
            recorders.remove(cameraName)
        }
    }

    class MyHandler implements HttpHandler {
        @Override
        void handle(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody()
            List<String> req = is.readLines()
            is.close()
            handleHttpRequest(req)
            String response = "OK"
            exchange.sendResponseHeaders(200, response.length())
            OutputStream os = exchange.getResponseBody()
            os.write(response.getBytes())
            os.close()
        }
    }

    boolean handleHttpRequest(List<String> reqList) {
        boolean isOk = true
        String request = reqList[0]
        ObjectMapper mapper = new ObjectMapper()
        if (request.length() > 0) {
            Map objReq = mapper.readValue(request, Map<String, String>.class)
            final String cameraName = objReq["camera_name"]
            Recorder recorder = recorders.get(cameraName)
            if (recorder != null) {
                switch (objReq["command"]) {
                    case "start_recording":
                        recorder.start()
                        logService.cam.info("Start recording on " + cameraName)
                        break
                    case "stop_recording":
                        recorder.stop()
                        logService.cam.info("Stop recording on " + cameraName)
                        break
                    default:
                        isOk = false
                }
            }
        } else
            isOk = false

        return isOk
    }

}

package com.securitycam.beans

import com.securitycam.configuration.Config
import com.securitycam.interfaceobjects.Asymmetric
import com.securitycam.services.LogService
import com.securitycam.services.RecorderFactory
import jakarta.annotation.PostConstruct
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.ftplet.FtpException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.apache.ftpserver.ftplet.FtpReply
import org.apache.ftpserver.ftplet.FtpRequest
import org.apache.ftpserver.ftplet.FtpSession
import org.apache.ftpserver.ftplet.Ftplet
import org.apache.ftpserver.ftplet.FtpletContext
import org.apache.ftpserver.ftplet.FtpletResult
import org.apache.ftpserver.ftplet.UserManager
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PasswordEncryptor
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

import java.util.stream.Stream


@Component
class RecorderFTPServer {
    LogService logService
    RecorderFactory recorderFactory
    Config config
    Map<UUID, String> sessionCameraIds

    @Autowired
    RecorderFTPServer(LogService logService, RecorderFactory recorderFactory, Config config) {
        this.logService = logService
        this.recorderFactory = recorderFactory
        this.config = config
        sessionCameraIds = new HashMap<UUID, String>()
        ((Logger) LoggerFactory.getLogger("org.apache.ftpserver")).setLevel(Level.ERROR)
        ((Logger) LoggerFactory.getLogger("org.apache.mina")).setLevel(Level.ERROR)
    }

    @PostConstruct
    private void startFTPServer() {
        Asymmetric asym = new Asymmetric()
        FtpServerFactory serverFactory = new FtpServerFactory()
        ListenerFactory factory = new ListenerFactory()
        factory.setPort(2121)
        serverFactory.addListener("default", factory.createListener())
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory()
        userManagerFactory.setFile(new File("${config.camerasHomeDirectory}/ftp-server.properties"))
        userManagerFactory.setPasswordEncryptor(new PasswordEncryptor()
        {
            @Override
            String encrypt(String password) {
                return asym.encrypt(password)
            }

            @Override
            boolean matches(String passwordToCheck, String storedPassword) {
                return passwordToCheck.equals(asym.decrypt(storedPassword))
            }
        })
        //Add a user, since our ftp-server.properties file is empty on our first test run
        BaseUser user = new BaseUser()
        // TODO: Set this up during installation, and the installer needs to create an empty ftp-server.properties file at install time
        user.setName("user")
        user.setPassword("12345")
        user.setHomeDirectory("${config.camerasHomeDirectory}")
        List<Authority> authorities = new ArrayList<Authority>()
        authorities.add(new WritePermission())
        user.setAuthorities(authorities)
        UserManager um = userManagerFactory.createUserManager()
        try {
            um.save(user)
        }
        catch (FtpException ex) {
            logService.cam.error("${ex.getClass().getName()} in FTP server: ${ex.getMessage()}")
        }
        serverFactory.setUserManager(um)
        Map<String, Ftplet> m = new HashMap<String, Ftplet>()
        m.put("miaFtplet", new Ftplet()
        {
            @Override
            void init(FtpletContext ftpletContext) throws FtpException {
            }

            @Override
            void destroy() {
            }

            @Override
            FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
                //   System.out.println("beforeCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine())
                if (request.command.toLowerCase() == "cwd") {
                    String[] requestParts = request.getArgument().split("/")
                    String cameraName
                    if(requestParts[0].contains("camera"))
                        cameraName = requestParts[0]
                    else if (requestParts.length > 1)
                        cameraName = requestParts[1]
                    else
                        return FtpletResult.DEFAULT

                    recorderFactory.startFromFtp(cameraName)
                    sessionCameraIds.put(session.sessionId, cameraName)
                }

                if (request.hasArgument() && request.argument.contains("camera"))
                    logService.cam.info("Starting recording on ${request.argument}")

                return FtpletResult.DEFAULT
            }



            @Override
            FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
               //  System.out.println("afterCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine() + " | " + reply.getMessage() + " : " + reply.toString())
                if (request.command.equalsIgnoreCase("stor") && reply.isPositive()) {
                    final String cameraId = sessionCameraIds[session.sessionId]

                    if (cameraId) {
                        final Path homeDir = Paths.get(user.getHomeDirectory()).toAbsolutePath().normalize()
                        final Path cameraDir = homeDir.resolve(cameraId).normalize()

                        // Ensure cameraDir is a direct child of homeDir (prevents path traversal)
                        if (cameraDir.getParent() == homeDir && Files.exists(cameraDir)) {
                            try (Stream<Path> walk = Files.walk(cameraDir)) {
                                walk.sorted(Comparator.reverseOrder())
                                        .filter { it != cameraDir } // Keep camera(number), delete <date>/images/<file>
                                        .map(Path::toFile)
                                        .forEach(File::delete)
                            }
                        }
                    }
                }
                return FtpletResult.DEFAULT
            }

            @Override
            FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
                return FtpletResult.DEFAULT
            }

            @Override
            FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
                return FtpletResult.DEFAULT
            }
        })
        serverFactory.setFtplets(m)
        //Map<String, Ftplet> mappa = serverFactory.getFtplets()
        //System.out.println(mappa.size())
        //System.out.println("Thread #" + Thread.currentThread().getId())
        //System.out.println(mappa.toString())
        FtpServer server = serverFactory.createServer()
        try {
            server.start()
        }
        catch (FtpException ex) {
            logService.cam.error("${ex.getClass().getName()} in FTP server start: ${ex.getMessage()}")
        }

    }
}

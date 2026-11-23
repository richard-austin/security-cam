package com.securitycam.beans

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
import java.util.stream.Stream


@Component
class RecorderFTPServer {
    LogService logService
    RecorderFactory recorderFactory
    Map<UUID, String> sessionCameraIds

    @Autowired
    RecorderFTPServer(LogService logService, RecorderFactory recorderFactory) {
        this.logService = logService
        this.recorderFactory = recorderFactory
        sessionCameraIds = new HashMap<UUID, String>()
    }

    @PostConstruct
    private void startFTPServer() {
        Asymmetric asym = new Asymmetric()
        FtpServerFactory serverFactory = new FtpServerFactory()
        ListenerFactory factory = new ListenerFactory()
        factory.setPort(2121)// set the port of the listener (choose your desired port, not 1234)
        serverFactory.addListener("default", factory.createListener())
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory()
        userManagerFactory.setFile(new File("/home/richard/myusers.properties"))
//choose any. We're telling the FTP-server where to read its user list
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
//Let's add a user, since our myusers.properties file is empty on our first test run
        BaseUser user = new BaseUser()
        // TODO: Set this up during installation
        user.setName("user")
        user.setPassword("12345")
        user.setHomeDirectory("/home/richard")
        List<Authority> authorities = new ArrayList<Authority>()
        authorities.add(new WritePermission())
        user.setAuthorities(authorities)
        UserManager um = userManagerFactory.createUserManager()
        try {
            um.save(user)//Save the user to the user list on the filesystem
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
                //System.out.println("init")
                //System.out.println("Thread #" + Thread.currentThread().getId())
            }

            @Override
            void destroy() {
                //System.out.println("destroy")
                //System.out.println("Thread #" + Thread.currentThread().getId())
            }

            @Override
            FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
                   System.out.println("beforeCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine())
                if (request.command.toLowerCase() == "cwd") {
                    String[] requestParts = request.getArgument().split("/")
                    String cameraName
                    if(requestParts[0].contains("camera"))
                        cameraName = requestParts[0]
                    else if (requestParts.length > 1)
                        cameraName = requestParts[1]
                    else {
                        logService.cam.error("Malformed request from ftp client ${request.getArgument()}")
                        return FtpletResult.DISCONNECT
                    }
                    recorderFactory.startFromFtp(cameraName)
                    sessionCameraIds.put(session.sessionId, cameraName)
                }
                //System.out.println("Thread #" + Thread.currentThread().getId())
                if (request.hasArgument() && request.argument.contains("camera"))
                    System.out.println(request.argument)

                return FtpletResult.DEFAULT
            }



            @Override
            FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
                 System.out.println("afterCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine() + " | " + reply.getMessage() + " : " + reply.toString())
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
                //System.out.println("onConnect " + session.getUserArgument() + " : " + session.toString())
                //System.out.println("Thread #" + Thread.currentThread().getId())

                //do something
                return FtpletResult.DEFAULT//...or return accordingly
            }

            @Override
            FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
                //System.out.println("onDisconnect " + session.getUserArgument() + " : " + session.toString())
                //System.out.println("Thread #" + Thread.currentThread().getId())

                //do something
                return FtpletResult.DEFAULT//...or return accordingly
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

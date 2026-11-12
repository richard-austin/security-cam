package com.securitycam.recording

import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.ftplet.FtpException
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

// Source - https://stackoverflow.com/a/8970126
// Posted by Unai Vivi, modified by community. See post 'Timeline' for change history
// Retrieved 2026-11-12, License - CC BY-SA 4.0

FtpServerFactory serverFactory = new FtpServerFactory()
ListenerFactory factory = new ListenerFactory()
factory.setPort(1234)// set the port of the listener (choose your desired port, not 1234)
serverFactory.addListener("default", factory.createListener())
PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory()
userManagerFactory.setFile(new File("/home/blablah/myusers.properties"))//choose any. We're telling the FTP-server where to read its user list
userManagerFactory.setPasswordEncryptor(new PasswordEncryptor()
{//We store clear-text passwords in this example

    @Override
    String encrypt(String password) {
        return password
    }

    @Override
    boolean matches(String passwordToCheck, String storedPassword) {
        return passwordToCheck.equals(storedPassword)
    }
})
//Let's add a user, since our myusers.properties file is empty on our first test run
BaseUser user = new BaseUser()
user.setName("test")
user.setPassword("test")
user.setHomeDirectory("/home/blablah")
List<Authority> authorities = new ArrayList<Authority>()
authorities.add(new WritePermission())
user.setAuthorities(authorities)
UserManager um = userManagerFactory.createUserManager()
try
{
    um.save(user)//Save the user to the user list on the filesystem
}
catch (FtpException ignored)
{
    //Deal with exception as you need
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
    FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException
    {
        //System.out.println("beforeCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine())
        //System.out.println("Thread #" + Thread.currentThread().getId())

        //do something
        return FtpletResult.DEFAULT//...or return accordingly
    }

    @Override
    FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException
    {
        //System.out.println("afterCommand " + session.getUserArgument() + " : " + session.toString() + " | " + request.getArgument() + " : " + request.getCommand() + " : " + request.getRequestLine() + " | " + reply.getMessage() + " : " + reply.toString())
        //System.out.println("Thread #" + Thread.currentThread().getId())

        //do something
        return FtpletResult.DEFAULT//...or return accordingly
    }

    @Override
    FtpletResult onConnect(FtpSession session) throws FtpException, IOException
    {
        //System.out.println("onConnect " + session.getUserArgument() + " : " + session.toString())
        //System.out.println("Thread #" + Thread.currentThread().getId())

        //do something
        return FtpletResult.DEFAULT//...or return accordingly
    }

    @Override
    FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException
    {
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
try
{
    server.start()//Your FTP server starts listening for incoming FTP-connections, using the configuration options previously set
}
catch (FtpException ex)
{
    //Deal with exception as you need
}

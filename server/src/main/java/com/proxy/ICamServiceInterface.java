package com.proxy;

public interface ICamServiceInterface {
    String cameraAdminUserName();
    String cameraAdminPassword();
    Integer getCameraType(String cameraHost);
}

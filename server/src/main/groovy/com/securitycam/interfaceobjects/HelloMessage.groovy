package com.securitycam.interfaceobjects

class HelloMessage {

    private String name;

    HelloMessage() {
    }

    HelloMessage(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }
}

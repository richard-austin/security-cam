package com.proxy;

import ch.qos.logback.classic.Logger;

import javax.annotation.PostConstruct;

public interface ILogService {
    Logger getCam ();

    void setLogLevel(String level);

    @PostConstruct
    Object initialise();
}

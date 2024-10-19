package com.proxy;

import ch.qos.logback.classic.Logger;
import jakarta.annotation.PostConstruct;


public interface ILogService {
    Logger getCam ();

    void setLogLevel(String level);

    @PostConstruct
    Object initialise();
}

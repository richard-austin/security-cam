package de.onvif.soap;

import org.onvif.ver10.events.wsdl.PullMessagesResponse;

public interface PullMessagesCallbacks {
    void onPullMessagesReceived(PullMessagesResponse pullMessages);
}

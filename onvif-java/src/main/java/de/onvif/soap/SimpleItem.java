package de.onvif.soap;

// Container for values returned in PullMessagesResponse
public class SimpleItem {
    public final String Name;
    public final String Value;
    public SimpleItem(String name, String value) {
        Name = name;
        Value = value;
    }
}

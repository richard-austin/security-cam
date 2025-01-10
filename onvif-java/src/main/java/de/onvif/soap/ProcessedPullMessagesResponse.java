package de.onvif.soap;

import org.onvif.ver10.events.wsdl.PullMessagesResponse;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class ProcessedPullMessagesResponse {
    public final List<PullMessagesResponseData> responseData;

    public ProcessedPullMessagesResponse(PullMessagesResponse response) {
        responseData = new ArrayList<>();

        final String tt = "http://www.onvif.org/ver10/schema";
        response.getNotificationMessage().forEach((nm) -> {
            final List<SimpleItem> data = new ArrayList<>();
            final List<SimpleItem> source = new ArrayList<>();

            var any = nm.getMessage().getAny();
            if (any instanceof Element) {
                var dataLst = ((Element) any).getElementsByTagNameNS(tt, "Data");
                for (int i = 0; i < dataLst.getLength(); ++i) {
                    var dataItm = dataLst.item(i);
                    var simpleItemLst = dataItm.getChildNodes();
                    for (int j = 0; j < simpleItemLst.getLength(); ++j) {
                        var simpleItem = simpleItemLst.item(j);
                        var attrs = simpleItem.getAttributes();
                        var attrName = attrs.getNamedItem("Name");
                        var attrNameValue = attrName.getNodeValue();
                        var attrVal = attrs.getNamedItem("Value");
                        var attrValValue = attrVal.getNodeValue();
                        data.add(new SimpleItem(attrNameValue, attrValValue));

                    }
                }
                var sourceLst = ((Element) any).getElementsByTagNameNS(tt, "Source");
                for (int i = 0; i < sourceLst.getLength(); ++i) {
                    var sourceItm = sourceLst.item(i);
                    var simpleItemLst = sourceItm.getChildNodes();
                    for (int j = 0; j < simpleItemLst.getLength(); ++j) {
                        var simpleItem = simpleItemLst.item(j);
                        var attrs = simpleItem.getAttributes();
                        var attrName = attrs.getNamedItem("Name");
                        var attrNameValue = attrName.getNodeValue();
                        var attrVal = attrs.getNamedItem("Value");
                        var attrValValue = attrVal.getNodeValue();
                        source.add(new SimpleItem(attrNameValue, attrValValue));
                    }
                }
                responseData.add(new PullMessagesResponseData(nm.getTopic().getContent().getFirst().toString(), source, data));
            }
        });
    }
}

package com.wearezeta.auto.common.legalhold;

import com.google.zxing.NotFoundException;
import com.wearezeta.auto.common.CommonSteps;
import com.wearezeta.auto.common.imagecomparator.QRCode;
import com.wearezeta.auto.common.log.ZetaLogger;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class LegalHoldServiceClient {

    private static final Logger log = ZetaLogger.getLog(CommonSteps.class.getSimpleName());

    private final String LEGAL_HOLD_SERVICE_BASE_URL = "https://legal-hold.integrations.zinfra.io";
    private final String conversationEventsUrlTemplate = LEGAL_HOLD_SERVICE_BASE_URL + "/events/{conversation-id}";
    private final String conversationRenderUrlTemplate = LEGAL_HOLD_SERVICE_BASE_URL + "/conv/{conversation-id}?html=true";
    private final String conversationImageUrlTemplate = LEGAL_HOLD_SERVICE_BASE_URL + "/images/{message-id}.{asset-extension}";
    private final String textMessageType = "conversation.otr-message-add.new-text";
    private final String imageMessageType = "conversation.otr-message-add.image-preview";
    private final String fileMessageType = "conversation.otr-message-add.file-preview";
    private final String audioMessageType = "conversation.otr-message-add.audio-preview";
    private final String videoMessageType = "conversation.otr-message-add.video-preview";
    private final String assetDataType = "conversation.otr-message-add.asset-data";

    public LegalHoldServiceClient() {
    }

    public List<Event> getEvents(String conversationId) throws IOException, ParseException {
        List<Event> events = new ArrayList<Event>();

        SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        String conversationEventsUrl = conversationEventsUrlTemplate.replace("{conversation-id}", conversationId);

        // fetch render in order to download conversation related images
        String conversationRenderUrl = conversationRenderUrlTemplate.replace("{conversation-id}", conversationId);
        Jsoup.connect(conversationRenderUrl).header("Authorization", String.format("Bearer %s", LegalHoldServiceSettings.SERVICE_AUTH_TOKEN)).get();

        log.info("Service fetching URL: " + conversationEventsUrl);
        Document doc = Jsoup.connect(conversationEventsUrl).header("Authorization", String.format("Bearer %s", LegalHoldServiceSettings.SERVICE_AUTH_TOKEN)).get();
        if(doc != null) {
            for (Element table : doc.select("table")) {
                if(table != null) {
                    int rowIndex = 0;
                    for (Element row : table.select("tr")) {
                        if(row != null && rowIndex > 0) {
                            Elements tds = row.select("td");
                            if(tds != null) {
                                Event event = new Event(tds.get(0).text(), tds.get(1).text(), inFormat.parse(tds.get(2).text()), tds.get(3).text());
                                events.add(event);
                            }
                        }
                        rowIndex++;
                    }
                }
            }
        }
        log.info(String.format("Service has number of events: %d", events.size()));

        return events;
    }

    private List<Event> getEventsForType(String conversationId, String type) throws IOException, ParseException {
        List<Event> eventsForType = new ArrayList<Event>();

        List<Event> events = getEvents(conversationId);
        for (Event current : events) {
            if (current.getType().equals(type)) {
                eventsForType.add(current);
            }
        }

        return eventsForType;
    }

    public List<String> getTextMessages(String conversationId) throws IOException, ParseException {
        List<String> textMessages = new ArrayList<String>();
        List<Event> eventsTextMessages = getEventsForType(conversationId, textMessageType);
        for(Event event : eventsTextMessages) {
            JSONObject jsonObject = new JSONObject(event.getPayload());
            textMessages.add(jsonObject.getString("text"));
        }
        return textMessages;
    }

    public List<String> getImagesQRCode(String conversationId) throws IOException, ParseException {
        List<String> codes = new ArrayList<>();
        List<Event> eventsImages = getEventsForType(conversationId, imageMessageType);
        for (Event current : eventsImages) {
            JSONObject preview = new JSONObject(current.getPayload());
            String assetExtension = "";
            switch (preview.getString("mimeType")) {
                case "image/png":
                    assetExtension = "png";
                    break;
                case "image/jpeg":
                    assetExtension = "jpeg";
                    break;
            }
            String imageUrl = conversationImageUrlTemplate.replace("{message-id}", preview.getString("messageId")).replace("{asset-extension}", assetExtension);
            log.fine("Service image URL: " + imageUrl);
            BufferedImage image = ImageIO.read(new URL(imageUrl));
            try {
                codes.addAll(QRCode.readMultipleCodes(image));
            } catch (NotFoundException e) {
                log.severe("No readable QR code found:" + e.getMessage());
            }
        }
        return codes;
    }

    public List<String> getFileNames(String conversationId) throws IOException, ParseException {
        List<String> fileNames = new ArrayList<String>();
        List<Event> eventsTextMessages = getEventsForType(conversationId, fileMessageType);
        for(Event event : eventsTextMessages) {
            JSONObject jsonObject = new JSONObject(event.getPayload());
            fileNames.add(jsonObject.getString("name"));
        }
        return fileNames;
    }

    public List<String> getAudioFiles(String conversationId) throws IOException, ParseException {
        List<String> fileNames = new ArrayList<String>();
        List<Event> eventsTextMessages = getEventsForType(conversationId, audioMessageType);
        for(Event event : eventsTextMessages) {
            JSONObject jsonObject = new JSONObject(event.getPayload());
            fileNames.add(jsonObject.getString("name"));
        }
        return fileNames;
    }

    public List<String> getVideoFiles(String conversationId) throws IOException, ParseException {
        List<String> fileNames = new ArrayList<String>();
        List<Event> eventsTextMessages = getEventsForType(conversationId, videoMessageType);
        for(Event event : eventsTextMessages) {
            JSONObject jsonObject = new JSONObject(event.getPayload());
            fileNames.add(jsonObject.getString("name"));
        }
        return fileNames;
    }
}

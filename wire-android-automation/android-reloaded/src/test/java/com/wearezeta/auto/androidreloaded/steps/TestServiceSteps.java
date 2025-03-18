package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.common.CommonSteps;
import com.wearezeta.auto.common.CommonUtils;
import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.backend.models.ReactionType;
import com.wearezeta.auto.common.misc.EphemeralTimeConverter;
import com.wearezeta.auto.common.testservice.models.LegalHoldStatus;
import com.wearezeta.auto.common.imagecomparator.QRCode;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static com.wearezeta.auto.common.CommonSteps.DEFAULT_AUTOMATION_MESSAGE;
import static com.wearezeta.auto.common.CommonSteps.FIRST_AVAILABLE_DEVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

public class TestServiceSteps {

    private static final Logger log = ZetaLogger.getLog(AndroidTestContext.class.getSimpleName());

    AndroidTestContext context;

    public TestServiceSteps(AndroidTestContext context) {
        this.context = context;
    }

    public List<String> userGetDeviceIds(String usernameAlias) {
        return context.getCommonSteps().getDeviceIds(usernameAlias);
    }

    private CommonSteps getCommonSteps() {
        return context.getCommonSteps();
    }

    // User Details

    @Given("^User (.*) sets their unique username$")
    public void userSetsUniqueUsername(String userAs) {
        context.getCommonSteps().usersSetUniqueUsername(userAs);
    }

    @When("^User (.*) sets availability status(?: via device (.*))? to (NONE|AVAILABLE|AWAY|BUSY)$")
    public void userSetsAvailability(String userAlias, String deviceName, String availability) {
        final List<String> availabilityStatuses = Arrays.asList("NONE", "AVAILABLE", "AWAY", "BUSY");
        context.getCommonSteps().userSetsAvailabilityStatus(userAlias,
                deviceName, availabilityStatuses.indexOf(availability));
    }

    // Connections

    @Given("^User (.*) sends connection request to (.*)$")
    public void ConnectionRequestIsSentTo(String userFromNameAlias, String usersToNameAliases) {
        context.getCommonSteps().connectionRequestIsSentTo(userFromNameAlias, usersToNameAliases);
    }

    @When("^User (.*) accepts? all requests$")
    public void acceptAllIncomingConnectionRequests(String userToNameAlias) {
        context.getCommonSteps().acceptAllIncomingConnectionRequests(userToNameAlias);
    }

    // Messaging

    @When("^User (.*) sends? message \"(.*?)\"\\s?(?:via device (.*)\\s)?to (?:User|group conversation) (.*)$")
    public void userSendMessageToConversation(String msgFromUserNameAlias, String msg, String deviceName,
                                              String dstConvoName) {
        context.getCommonSteps().userSendsGenericMessageToConversation(msgFromUserNameAlias, dstConvoName,
                deviceName, context.getSelfDeletingMessageTimeout(msgFromUserNameAlias, dstConvoName), msg, LegalHoldStatus.DISABLED);
    }

    @When("^User (.*) sends message \"(.*)\" under legal hold to conversation \"(.*)\" via device (.*)$")
    public void userSendsMessageWithLegalHoldStatus(String userNameAlias, String message, String dstNameAlias,
                                                    String deviceName) {
        context.startPinging();
        context.getCommonSteps().userSendsMessageToConversation(userNameAlias, dstNameAlias, deviceName,
                Timedelta.ofMillis(0), message, LegalHoldStatus.ENABLED);
        context.stopPinging();
    }

    @When("^User (.*) sends? mention \"(.*?)\"\\s?(?:via device (.*)\\s)?to (?:User|group conversation) (.*)$")
    public void userSendMentionToConversation(String msgFromUserNameAlias, String user, String deviceName,
                                              String dstConvoName) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        context.getCommonSteps().userSendsGenericMessageToConversation(msgFromUserNameAlias, dstConvoName,
            deviceName, context.getSelfDeletingMessageTimeout(msgFromUserNameAlias, dstConvoName), user, LegalHoldStatus.DISABLED);
    }

    @When("^User (.*) sends poll message \"(.*)\" with title \"(.*)\" and buttons \"(.*)\"(?: via device (.*))? to conversation (.*)$")
    public void userSendPollMessageToConversationViaETS(String msgFromUserNameAlias,
                                                        String msg, String title, String buttons, String deviceName, String convoName) {
        context.startPinging();
        context.getCommonSteps().userSendsPollMessageToConversation(msgFromUserNameAlias, convoName,
                deviceName, Timedelta.ofSeconds(0), msg, title, buttons, LegalHoldStatus.DISABLED);
        context.stopPinging();
    }

    @When("^User (.*) sends button action confirmation to user (.*) on the latest poll(?: via device (.*))? in conversation (.*) with button \"(.*)\"$")
    public void userSendsButtonActionConfirmationOnPollMessageViaETS(String senderAlias, String receiverAlias, String deviceName, String dstConvoName, String buttonText) {
        context.startPinging();
        context.getCommonSteps().userSendsButtonActionConfirmationToLatestPollMessage(senderAlias, receiverAlias, deviceName, dstConvoName, buttonText);
        context.stopPinging();
    }

    @When("^User (.*) switches (?:user|group conversation) (.*) to ephemeral mode with " +
            "(\\d+) (seconds?|minutes?) timeout$")
    public void UserSwitchesToEphemeralMode(String userAs, String convoName, int timeout, String timeMetrics) {
        final Timedelta timeoutObj = timeMetrics.startsWith("minute")
                ? Timedelta.ofMinutes(timeout)
                : Timedelta.ofSeconds(timeout);
        context.setLocalSelfDeletingMessageTimeout(userAs, convoName, timeoutObj);
    }

    @When("^User (.*) sends? ephemeral message \"?(.*?)\"?\\s? with timer (10 seconds|5 minutes|1 hour|1 day|1 week|4 weeks) (?:via device (.*)\\s)?to conversation (.*)$")
    public void userSendEphemeralMessageToConversation(String msgFromUserNameAlias,
                                                       String msg, String msgTimer, String deviceName, String dstConvoName) {
        long msgTimerInMs = EphemeralTimeConverter.asMillis(msgTimer);
        context.getCommonSteps().userSendsMessageToConversation(msgFromUserNameAlias, dstConvoName,
                deviceName, Timedelta.ofMillis(msgTimerInMs), msg, LegalHoldStatus.DISABLED);
    }

    @When("^User (.*) edits? the recent message to \"(.*)\" from (?:user|group conversation) (.*) via device (.*)$")
    public void UserXEditLastMessage(String userNameAlias, String newMessage, String dstNameAlias,
                                     String deviceName) {
        context.getCommonSteps().userUpdatesLatestMessageViaEts(userNameAlias, dstNameAlias, deviceName, newMessage);
    }

    @When("^User (.*) toggles reaction \"(.*)\" on the recent message from (?:user|group conversation) (.*) via device (.*)$")
    public void userReactsOnLastMessage(String userNameAlias, String reaction, String dstNameAlias, String deviceName) {
        context.startPinging();
        context.getCommonSteps().userTogglesReactionOnLatestMessage(userNameAlias, dstNameAlias,
                deviceName, reaction);
        context.stopPinging();
    }

    @When("^User (.*) (likes|unlikes) the recent message from (?:user|group conversation) (.*) via device (.*)$")
    @Deprecated // Use step to toggle reaction instead
    public void UserUnlikesLastMessage(String userNameAlias, String reactionType, String convoType, String deviceName) {
        if (reactionType.equals("likes")) {
            context.getCommonSteps().userReactsToLatestMessage(userNameAlias, convoType, deviceName, ReactionType.LIKE);
        } else {
            context.getCommonSteps().userReactsToLatestMessage(userNameAlias, convoType, deviceName, ReactionType.UNLIKE);
        }
    }

    @When("^User (.*) toggles reaction \"(.*)\" on the recent message from conversation (.*) via device (.*)$")
    public void userReactOnLastMessage(String userNameAlias, String reaction, String dstNameAlias, String deviceName) {
        context.startPinging();
        context.getCommonSteps().userTogglesReactionOnLatestMessage(userNameAlias, dstNameAlias,
            deviceName, reaction);
        context.stopPinging();
    }

    @When("^User (.*) sends message \"(.*)\" as reply to last message of conversation (.*) via device (.*)$")
    public void userRepliesToLastMessage(String userNameAlias, String message, String dstNameAlias, String deviceName) {
        context.getCommonSteps().userRepliesToLatestMessage(userNameAlias, dstNameAlias, deviceName, Timedelta.ofMillis(0), message);
    }

    @Given("^User (.*) sends (\\d+) (default|long|\".*\") messages? to conversation (.*)")
    public void UserSendsMultipleMessages(String senderUserNameAlias, int count,
                                          String msg, String dstConversationName) {
        context.startPinging();
        context.getCommonSteps().userSendsMultipleMessages(senderUserNameAlias, dstConversationName,
                context.getSelfDeletingMessageTimeout(senderUserNameAlias, dstConversationName),
                count, msg, DEFAULT_AUTOMATION_MESSAGE, LegalHoldStatus.DISABLED);
        context.stopPinging();
    }

    @When("^User (.*) deletes? the recent message (everywhere )?from (?:user|group conversation) (.*) via device (.*)$")
    public void UserXDeleteLastMessage(String userNameAlias, String deleteEverywhere, String dstNameAlias,
                                       String deviceName) {
        boolean isDeleteEverywhere = deleteEverywhere != null;
        context.getCommonSteps().userDeletesLatestMessage(userNameAlias, dstNameAlias, deviceName, isDeleteEverywhere);
    }

    @Then("^User (.*) sees message \"(.*)\" in conversation (.*) via device (.*)$")
    public void userXseesMessage(String userNameAlias, String message, String dstNameAlias,
                                   String deviceName) {
        assertThat("Message not in conversation",
                context.getCommonSteps().userXGetsAllTextMessages(userNameAlias, dstNameAlias, deviceName),
                hasItem(message));
    }

    @Then("^User (.*) does not see message \"(.*)\" in conversation (.*) via device (.*) anymore$")
    public void userXseesNoMessage(String userNameAlias, String message, String dstNameAlias,
                                   String deviceName) {
        assertThat("Message not in conversation",
                context.getCommonSteps().userXGetsAllTextMessages(userNameAlias, dstNameAlias, deviceName),
                not(hasItem(message)));
    }

    @When("^User (.*) reads the recent message from (?:user|group conversation) (.*) via device (.*)$")
    public void UserReadsLastMessage(String userNameAlias, String dstNameAlias, String deviceName) {
        context.getCommonSteps().userSendsDeliveryConfirmationForLastEphemeralMessage(userNameAlias, dstNameAlias, deviceName);
    }

    @When("^User (.*) remembers? the recent message from (?:user|group conversation) (.*) via device (.*)$")
    public void UserXRemembersLastMessage(String userNameAlias, String dstNameAlias, String deviceName) {
        context.getCommonSteps().userXRemembersLastMessageViaEts(userNameAlias, dstNameAlias, deviceName);
    }

    @Then("^User (.*) sees? the recent message from (?:user|group conversation) (.*) via device (.*) is( not)? changed( in \\d+ seconds?)?$")
    public void UserXFoundLastMessageChanged(String userNameAlias, String dstNameAlias,
                                             String deviceName, String shouldNotChanged, String waitDuration) {
        final int durationSeconds = (waitDuration == null) ? CommonSteps.DEFAULT_WAIT_UNTIL_TIMEOUT.asSeconds()
                : Integer.parseInt(waitDuration.replaceAll("[\\D]", ""));

        if (shouldNotChanged == null) {
            context.getCommonSteps().userXFoundLastMessageChanged(userNameAlias,
                    dstNameAlias, deviceName, Timedelta.ofSeconds(durationSeconds));
        } else {
            context.getCommonSteps().userXFoundLastMessageNotChanged(userNameAlias,
                    dstNameAlias, deviceName, Timedelta.ofSeconds(durationSeconds));
        }
    }

    @Given("^User (.*) sets read receipt option to (true|false) for conversation (.*)$")
    public void userSetsReadReceiptToConversation(String userWhoSets, String newState, String chatName) {
        context.getCommonSteps().userSetsReadReceiptToConversation(userWhoSets, chatName, Boolean.valueOf(newState));
    }

    @And("^User (.*) sends read receipt on last message in conversation (.*) via device (.*)$")
    public void userSendsReadReceiptOnLastMessage(String userName, String convoName, String deviceName) {
        context.getCommonSteps().userSendsReadConfirmationForRecentMessage(userName, convoName, deviceName);
    }

    // Assets

    @When("^User (.*) sends image with QR code containing \"(.*)\" to conversation (.*)$")
    public void userSendsImageWithQRCode(String senderAlias, String text, String dstConversationName) throws IOException {
        File tempFile = File.createTempFile("zautomation", ".png");
        tempFile.deleteOnExit();
        ImageIO.write(QRCode.generateCode(text, Color.BLACK, Color.WHITE, 500, 1), "png", tempFile);
        context.getCommonSteps().userSendsImageToConversationViaTestservice(senderAlias, dstConversationName,
                FIRST_AVAILABLE_DEVICE, tempFile.getPath(),
                context.getSelfDeletingMessageTimeout(senderAlias, dstConversationName));
    }

    @When("^User (.*) sends image \"(.*)\" to conversation (.*)$")
    public void userSendImageToConversation(String imageSenderUserNameAlias, String imageFileName, String dstConversationName) {
        final String imagePath = Config.current().getImagesPath(getClass()) + imageFileName;
        context.getCommonSteps().userSendsImageToConversationViaTestservice(imageSenderUserNameAlias, dstConversationName,
                FIRST_AVAILABLE_DEVICE, imagePath,
                context.getSelfDeletingMessageTimeout(imageSenderUserNameAlias, dstConversationName));
    }

    @Given("^User (.*) sends (\\d+) (image|video|audio|temporary) files? (.*) to conversation (.*)")
    public void UserSendsMultiplePictures(String senderUserNameAlias, int count,
                                          String fileType, String fileName,
                                          String dstConversationName) {
        context.getCommonSteps().userSendsMultipleMedias(senderUserNameAlias, dstConversationName,
                context.getSelfDeletingMessageTimeout(senderUserNameAlias, dstConversationName),
                count, fileType, fileName);
    }

    @When("User (.*) sends local image named \"(.*)\" via device (.*) to (?:user|group conversation) \"(.*)\"$")
    public void contactSendsLocalImage(String senderAlias, String fileFullName, String deviceName, String dstConvoName) {
        String basePath = Config.current().getImagesPath(getClass());
        String sourceFilePath = basePath + File.separator + fileFullName;
        context.getCommonSteps()
                .userSendsFileToConversation(senderAlias, dstConvoName, deviceName,
                        context.getSelfDeletingMessageTimeout(senderAlias, dstConvoName), sourceFilePath, "image/jpeg");
    }

    @When("User (.*) sends local video named \"(.*)\" via device (.*) to (?:user|group conversation) \"(.*)\"$")
    public void contactSendsLocalVideo(String senderAlias, String fileFullName, String deviceName, String dstConvoName) {
        String basePath = Config.current().getVideoPath(getClass());
        String sourceFilePath = basePath + File.separator + fileFullName;
        context.getCommonSteps()
                .userSendsFileToConversation(senderAlias, dstConvoName, deviceName,
                        context.getSelfDeletingMessageTimeout(senderAlias, dstConvoName), sourceFilePath, "video/mp4");
    }

    @When("User (.*) sends local audio file named \"(.*)\" via device (.*) to (?:user|group conversation) \"(.*)\"$")
    public void contactSendsLocalAudio(String senderAlias, String fileFullName, String deviceName, String dstConvoName) {
        String basePath = Config.current().getAudioPath(getClass());
        String sourceFilePath = basePath + File.separator + fileFullName;
        context.getCommonSteps()
                .userSendsFileToConversation(senderAlias, dstConvoName, deviceName,
                        context.getSelfDeletingMessageTimeout(senderAlias, dstConvoName), sourceFilePath, "audio/mp4");
    }

    @When("^User (.*) shares? the default location to (?:user|group conversation) (.*) via device (.*)")
    public void UserXSharesLocationTo(String senderAlias, String convoName, String deviceName) {
        getCommonSteps().userSendsLocationToConversation(senderAlias, convoName, deviceName,
                context.getSelfDeletingMessageTimeout(senderAlias, convoName),
                0, 0, "location", 1);
    }

    // The Method below generates a random file

    @When("^User (.*) sends (.*) file having name \"(.*)\" and MIME type \"(.*)\" via device (.*) to (?:user|group conversation) \"(.*)\"$")
    public void userSendsFile(String senderAlias, String size, String fileFullName, String mimeType,
                              String deviceName, String dstConvoName) {
        String basePath = Config.current().getBuildPath(getClass());
        String sourceFilePath = basePath + File.separator + fileFullName;
        CommonUtils.createRandomAccessFile(sourceFilePath, size);
        context.getCommonSteps().userSendsFileToConversation(senderAlias, dstConvoName, deviceName,
                context.getSelfDeletingMessageTimeout(senderAlias, dstConvoName), sourceFilePath, mimeType);
    }

    @When("^User (.*) sends (.*) sized file with MIME type (.*) and name (.*)(?: via device (.*))? to conversation (.*)$")
    public void iXSizedSendFile(String contact, String size, String mimeType, String fileName, String deviceName,
                                String dstConvoName) throws Exception {
        String path = Files.createTempDirectory("zautomation")
                .toAbsolutePath().toString().replace("%40", "@");
        RandomAccessFile f = new RandomAccessFile(path + "/" + fileName, "rws");
        int fileSize = Integer.valueOf(size.replaceAll("\\D+", "").trim());
        if (size.contains("MB")) {
            f.setLength(fileSize * 1024 * 1024);
        } else if (size.contains("KB")) {
            f.setLength(fileSize * 1024);
        } else {
            f.setLength(fileSize);
        }
        f.close();
        context.startPinging();
        context.getCommonSteps().userSendsFileToConversation(contact, dstConvoName,
                deviceName, context.getCommonSteps().getEphemeralTimeout(contact, dstConvoName),
                path + "/" + fileName, mimeType);
        context.stopPinging();
    }

    // Devices

    @When("^User (.*) adds a new device (.*) with label (.*)$")
    public void userAddsDevice(String userNameAlias, String deviceName, String label) {
        boolean developmentApiEnabled = context.getCommonSteps().isDevelopmentApiEnabled(userNameAlias);
        context.getCommonSteps().addDevice(userNameAlias, null, deviceName, Optional.of(label),
                developmentApiEnabled);
    }

    @When("^User (.*) adds a new 2FA device (.*) with label (.*)$")
    public void userAdds2FADevice(String userNameAlias, String deviceName, String label) {
        context.getCommonSteps().add2FADevice(userNameAlias, deviceName, Optional.of(label));
    }

    @When("^User (.*) adds (\\d+) devices?$")
    public void userAddsDevices(String userNameAlias, int amount) {
        boolean developmentApiEnabled = context.getCommonSteps().isDevelopmentApiEnabled(userNameAlias);
        for (int i = 1; i <= amount; i++) {
            context.getCommonSteps().addDevice(userNameAlias, null,
                    "Device" + i, Optional.of("Label" + i), developmentApiEnabled);
        }
    }

    @When("^User (.*) adds (\\d+) 2FA devices?$")
    public void userAdds2FADevices(String userNameAlias, int amount) {
        for (int i = 1; i <= amount; i++) {
            context.getCommonSteps().add2FADevice(userNameAlias,
                    "Device" + i, Optional.of("Label" + i));
        }
    }

    @When("^Users (.*) each add (\\d+) 2FA devices?$")
    public void usersAdd2FADevices(String userNameAliases, int amount) {
        for (String userNameAlias : userNameAliases.split(",")) {
            for (int i = 1; i <= amount; i++) {
                context.getCommonSteps().add2FADevice(userNameAlias,
                        "Device" + i, Optional.of("Label" + i));
            }
        }
    }

    @When("^User (.*) breaks the session via device (.*) with my current client$")
    public void userBreaksSession(String msgFromUserNameAlias, String deviceName) {
        if (context.getCurrentDeviceId() == null) {
            throw new RuntimeException(
                    "currentDeviceId was not remembered, please use the according step first");
        }
        context.startPinging();
        context.getCommonSteps().userBreaksSession(msgFromUserNameAlias, deviceName,
                context.getCurrentDeviceId().toLowerCase());
        context.stopPinging();
    }

    @Given("^User (.*) removes all their registered OTR clients$")
    public void UserRemovesAllRegisteredOtrClients(String userAs) {
        context.getCommonSteps().userRemovesAllRegisteredOtrClients(userAs);
    }

    @When("^User (.*) removes OTR client with device name (.*)$")
    public void userRemovesOTRClientWithModel(String userAs, String deviceName) {
        context.getCommonSteps().userRemovesSpecificOtrClientByModel(userAs, deviceName);
    }
}

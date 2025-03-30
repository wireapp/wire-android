package com.wearezeta.auto.common;

import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import com.wire.qa.picklejar.engine.TestContext;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class CommonTestContext extends TestContext {
    private final ClientUsersManager usersManager;
    private final CommonSteps commonSteps;
    private final CallingManager callingManager;
    private List<BufferedImage> additionalScreenshots = new ArrayList<>();
    private byte[] additionalAttachment = null;
    private String additionalAttachmentMimeType = "";

    public CommonTestContext(boolean useSpecialEmail) {
        this.usersManager = new ClientUsersManager(useSpecialEmail);
        this.callingManager = new CallingManager(this.usersManager);
        this.commonSteps = new CommonSteps(this.usersManager);
    }

    public CommonTestContext(ClientUsersManager userManager,
                       CallingManager callingManager, CommonSteps commonSteps) {
        this.usersManager = userManager;
        this.callingManager = callingManager;
        this.commonSteps = commonSteps;
    }

    public ClientUsersManager getUsersManager() {
        return usersManager;
    }

    public void reset() {
        // This only applies to iOS (For Android check teardown in Lifecycle class)
        try {
            this.getCommonSteps().cleanUpBackends();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.getCallingManager().cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CallingManager getCallingManager() {
        return callingManager;
    }

    public CommonSteps getCommonSteps() {
        return commonSteps;
    }

    public boolean hasAdditionalScreenshots() {
        return additionalScreenshots.size() > 0;
    }

    public List<BufferedImage> getAdditionalScreenshots() {
        return additionalScreenshots;
    }

    public void addAdditionalScreenshots(BufferedImage screenshot) {
        additionalScreenshots.add(screenshot);
    }

    public void clearAdditionalScreenshots() {
        additionalScreenshots.clear();
    }

    public boolean hasAdditionalAttachment() {
        return additionalAttachment != null;
    }

    public void addAdditionalAttachment(byte[] data, String mimeType) {
        additionalAttachment = data;
        additionalAttachmentMimeType = mimeType;
    }

    public byte[] getAdditionalAttachment() {
        return additionalAttachment;
    }

    public String getAdditionalAttachmentMimeType() {
        return additionalAttachmentMimeType;
    }

    public void clearAdditionalAttachment() {
        additionalAttachment = null;
    }
}

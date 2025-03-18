package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.DocumentsUIPage;
import com.wearezeta.auto.common.imagecomparator.QRCode;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.When;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

// See: https://source.android.com/docs/core/ota/modular-system/documentsui
public class DocumentsUISteps {

    private final AndroidTestContext context;

    public DocumentsUISteps(AndroidTestContext context) {
        this.context = context;
    }

    private DocumentsUIPage getDocumentsUI() {
        return context.getPage(DocumentsUIPage.class);
    }

    @When("^I push image with QR code containing \"(.*)\" to file storage$")
    public void iPushImage(String qrcode) throws IOException {
        final Path directory = Files.createTempDirectory("zautomation");
        final String fileFormat = "png";
        String fileName = String.format("%s.%s", qrcode, fileFormat);
        final File tempFile = new File(directory.toAbsolutePath() + File.separator + fileName);
        tempFile.deleteOnExit();
        ImageIO.write(QRCode.generateCode(qrcode, Color.BLACK, Color.WHITE, 500, 4), fileFormat, tempFile);
        getDocumentsUI().pushFile(tempFile);
    }

    @When("^I push (.*) sized file with name \"(.*)\" to file storage$")
    public void iPushFile(String size, String name) throws IOException {
        final Path directory = Files.createTempDirectory("zautomation");
        String fileName = directory.toAbsolutePath() + File.separator + name;
        RandomAccessFile f = new RandomAccessFile(fileName, "rws");
        int fileSize = Integer.valueOf(size.replaceAll("\\D+", "").trim());
        if (size.contains("MB")) {
            f.setLength(fileSize * 1024 * 1024);
        } else if (size.contains("KB")) {
            f.setLength(fileSize * 1024);
        } else {
            f.setLength(fileSize);
        }
        f.close();
        getDocumentsUI().pushFile(new File(fileName));
    }

    @When("^I tap on root menu button of DocumentsUI$")
    public void iTapRootMenu() {
        getDocumentsUI().tapRootsMenu();
    }

    @When("^I tap on Downloads in DocumentsUI$")
    public void iTapDownloads() {
        getDocumentsUI().tapDownloads();
    }

    @When("^I select image with name containing \"(.*)\" in DocumentsUI$")
    public void iSelectImage(String text) {
        getDocumentsUI().selectImageContaining(text);
    }

    @When("^I select image with QR code \"(.*)\" in DocumentsUI$")
    public void iSeeImageInConversation(String qrCode) {
        if (getDocumentsUI().isImageContainingTextVisible("Image.png")) {
            getDocumentsUI().selectImageContaining("Image.png");
        } else {
            BufferedImage actualImage = getDocumentsUI().getRecentImageScreenshot();
            context.addAdditionalScreenshots(actualImage);
            getDocumentsUI().selectImageWithQRCode();
        }
    }

    @When("^I select file with name containing \"(.*)\" in DocumentsUI$")
    public void iSelectFile(String text) {
        getDocumentsUI().selectFileContaining(text);
    }

    @When("^I select backup file with name containing \"(.*)\" in DocumentsUI$")
    public void iSelectBackupFile(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.UNIQUE_USERNAME_ALIAS);
        getDocumentsUI().selectBackupFileContaining(userName);
    }

    @When("^I select add button in DocumentsUI$")
    public void iSelectAddButton() {
        getDocumentsUI().selectAddButtonFiles();
    }
}

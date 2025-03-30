package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.PreviewPage;
import io.cucumber.java.en.When;

import java.awt.image.BufferedImage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;


public class PreviewPageSteps {

    private final AndroidTestContext context;

    public PreviewPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private PreviewPage getPreviewPage() {
        return context.getPage(PreviewPage.class);
    }

    @When("^I see image preview page$")
    public void iSeeImagePreviewPage() {
        getPreviewPage().isImagePreviewPageVisible();
    }

    @When("^I see file \"(.*)\" on preview page$")
    public void iSeeAssetPreviewPage(String filename) {
        getPreviewPage().isAssetPreviewPageVisible(filename);
    }

    @When("^I tap send button on preview page$")
    public void iTapSendButtonImagePreviewPage() {
        getPreviewPage().tapSendButtonImagePreviewPage();
    }
}

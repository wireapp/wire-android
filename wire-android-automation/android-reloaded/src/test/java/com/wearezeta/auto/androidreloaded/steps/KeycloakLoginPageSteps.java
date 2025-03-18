package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.common.PackageNameHolder;
import com.wearezeta.auto.androidreloaded.pages.KeycloakLoginPage;
import com.wire.qa.picklejar.engine.exception.SkipException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

public class KeycloakLoginPageSteps {

    private final AndroidTestContext context;

    public KeycloakLoginPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private KeycloakLoginPage getKeycloakLoginPage() {
        return context.getPage(KeycloakLoginPage.class);
    }

    @When("^I enter email (.*) on keycloak login page$")
    public void iEnterEmail(String idpEmail) {
        idpEmail = context.getUsersManager().findUserByEmailOrEmailAlias(idpEmail).getEmail();
        getKeycloakLoginPage().enterEmail(idpEmail);
    }

    @And("^I enter password (.*) on keycloak login page$")
    public void iEnterPassword(String idpPassword) {
        idpPassword = context.getUsersManager().findUserByPasswordAlias(idpPassword).getPassword();
        getKeycloakLoginPage().enterPassword(idpPassword);
    }

    @And("^I click sign in on keycloak login page$")
    public void iClickSignIn() {
        getKeycloakLoginPage().clickSignInButton();
    }

}

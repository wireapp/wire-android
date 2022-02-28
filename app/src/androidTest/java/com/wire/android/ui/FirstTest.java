package com.wire.android.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.wire.android.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class FirstTest {

    @Rule
    public ActivityScenarioRule<WireActivity> activityScenarioRule =
            new ActivityScenarioRule<WireActivity>(WireActivity.class);

    @Before
    public void intentsInit() {
        // initialize Espresso Intents capturing
        Intents.init();
    }

    @After
    public void intentsTeardown() {
        // release Espresso Intents capturing
        Intents.release();
    }

    @Test
    public void changeText_sameActivity() {
        onView(withId(R.id.browser_actions_header_text)).check(matches(withText(R.id.browser_actions_header_text)));
    }

    @Test
    public void checktText() {
        onView(withText("")).check(matches(isDisplayed()));
    }

    @Test
    public void checktButtonIsDisplayed() {
        onView(withId(R.id.bestChoice)).check(matches(isDisplayed()));
    }

    @Test
    public void checktTextIsDisplayed() {
        onView(withText("Create Enterprise Account")).check(matches(isDisplayed()));
    }
}

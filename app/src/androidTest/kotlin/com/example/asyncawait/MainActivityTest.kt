package com.example.asyncawait

import android.content.Intent
import android.os.Bundle
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.registerIdlingResources
import android.support.test.espresso.Espresso.unregisterIdlingResources
import android.support.test.espresso.IdlingResource
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.runner.AndroidJUnit4
import co.metalab.asyncawaitsample.MainActivity
import co.metalab.asyncawaitsample.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    val testRule: IntentsTestRule<MainActivity> = IntentsTestRule(MainActivity::class.java, true, false)

    private val asyncIdlingResource: IdlingResource = AsyncIdlingResource()

    @Before
    fun setUp() {
        registerIdlingResources(asyncIdlingResource)
    }

    @After
    fun tearDown() {
        unregisterIdlingResources(asyncIdlingResource)
    }

    @Test
    fun testAsync() {
        startActivity()

        clickOnAwaitWithProgressButton()

        onView(withId(R.id.txtResult))
                .check(matches(withText("Loaded Text")))
    }

    private fun clickOnAwaitWithProgressButton() {
        onView(withId(R.id.btnAwaitWithProgress))
                .perform(click())
    }

    private fun startActivity(args: Bundle = Bundle()): MainActivity {
        val intent = Intent()
        intent.putExtras(args)
        return testRule.launchActivity(intent)
    }
}
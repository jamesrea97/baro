package com.baro.ui.splash

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import com.baro.R
import com.baro.constants.AppCodes
import com.baro.constants.AppTags
import com.baro.constants.FileEnum
import com.baro.constants.JSONEnum
import com.baro.helpers.FileHelper
import com.baro.helpers.JSONHelper
import com.baro.models.User
import com.baro.ui.account.AccountActivity
import com.baro.ui.main.MainActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.json.JSONException
import org.junit.*
import org.junit.runner.RunWith
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4ClassRunner::class)
class SplashActivityAlreadyLoggedInUserTest {
    private var VALID_UUID: String? = "486d063c-58bf-4105-86ad-62f91036126c"

    @get:Rule
    var mActivityTestRule: ActivityTestRule<SplashActivity?>? = ActivityTestRule(SplashActivity::class.java)


    @Before
    fun setUpUser() {
        val userMetadataPath = Paths.get(
                ApplicationProvider.getApplicationContext<Context?>().getExternalFilesDir(null).toString(),
                FileEnum.USER_DIRECTORY.key,
                FileEnum.META_DATA_FILE.key)
        val userMetadataFile = FileHelper.createFileAtPath(userMetadataPath.toAbsolutePath())
        val hashMap = HashMap<String?, String?>()
        hashMap[JSONEnum.USER_UUID_KEY.key] = VALID_UUID
        hashMap[JSONEnum.USER_NAME_KEY.key] = "valid_username"
        val jsonObject = JSONHelper.createJSONFromHashMap(hashMap)
        FileHelper.writeToFile(userMetadataFile, jsonObject.toString())
    }

    @Test
    @Throws(JSONException::class)
    fun createUserTest() {

        val user = User(UUID.fromString(VALID_UUID), "valid_username")

        sleep(4)

        val imageButton = Espresso.onView(
                Matchers.allOf(ViewMatchers.withId(R.id.btn_account),
                        ViewMatchers.withParent(ViewMatchers.withParent(ViewMatchers.withId(android.R.id.content))),
                        ViewMatchers.isDisplayed()))
        imageButton.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        val imageButton2 = Espresso.onView(
                Matchers.allOf(ViewMatchers.withId(R.id.btn_share),
                        ViewMatchers.withParent(ViewMatchers.withParent(ViewMatchers.withId(android.R.id.content))),
                        ViewMatchers.isDisplayed()))
        imageButton2.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Intents.intended(IntentMatchers.hasComponent(MainActivity::class.java.name))
        Intents.intended(IntentMatchers.hasExtra(AppTags.USER_OBJECT.name, user))


    }

    companion object {
        private fun childAtPosition(
                parentMatcher: Matcher<View?>?, position: Int): Matcher<View?>? {
            return object : TypeSafeMatcher<View?>() {
                override fun describeTo(description: Description?) {
                    description!!.appendText("Child at position $position in parent ")
                    parentMatcher!!.describeTo(description)
                }

                public override fun matchesSafely(view: View?): Boolean {
                    val parent = view!!.parent
                    return (parent is ViewGroup && parentMatcher!!.matches(parent)
                            && view == (parent as ViewGroup).getChildAt(position))
                }
            }
        }
    }
}
package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.JarvisAction
import com.example.ai.JarvisBrain
import com.example.system.DeviceController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JARVIS AI", appName)
  }

  @Test
  fun `test voice call with contact disambiguation`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val deviceController = DeviceController(context)
    val brain = JarvisBrain(deviceController = deviceController)

    // "Call John" matches multiple contacts ("John Doe", "John Smith")
    val result = brain.processCommand("Call John")
    assertTrue(result.action is JarvisAction.DisambiguateCallAction)
    val disambiguate = result.action as JarvisAction.DisambiguateCallAction
    assertEquals("John", disambiguate.query)
    assertTrue(disambiguate.candidates.size >= 2)
  }

  @Test
  fun `test direct call to phone number`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val deviceController = DeviceController(context)
    val brain = JarvisBrain(deviceController = deviceController)

    val result = brain.processCommand("Call +1-555-0101")
    assertTrue(result.action is JarvisAction.CallAction)
    assertEquals("+1-555-0101", (result.action as JarvisAction.CallAction).target)
  }

  @Test
  fun `test text messaging voice command`() = runBlocking {
    val brain = JarvisBrain()
    val result = brain.processCommand("Send a message to John saying I'll be late")
    assertTrue(result.action is JarvisAction.MessageAction)
    val msgAction = result.action as JarvisAction.MessageAction
    assertEquals("John", msgAction.target)
    assertEquals("I'll be late", msgAction.message)
    assertEquals(false, msgAction.isWhatsApp)
  }

  @Test
  fun `test app open voice command`() = runBlocking {
    val brain = JarvisBrain()
    val result = brain.processCommand("Open YouTube")
    assertTrue(result.action is JarvisAction.OpenAppAction)
    assertEquals("YouTube", (result.action as JarvisAction.OpenAppAction).appName)
  }

  @Test
  fun `test settings voice commands for wifi and brightness`() = runBlocking {
    val brain = JarvisBrain()
    val wifiResult = brain.processCommand("Turn on Wi-Fi")
    assertTrue(wifiResult.action is JarvisAction.SettingAction)
    assertEquals("wifi", (wifiResult.action as JarvisAction.SettingAction).settingType)

    val brightnessResult = brain.processCommand("Increase brightness")
    assertTrue(brightnessResult.action is JarvisAction.BrightnessAction)
    assertEquals(true, (brightnessResult.action as JarvisAction.BrightnessAction).increase)

    val volumeResult = brain.processCommand("Increase volume")
    assertTrue(volumeResult.action is JarvisAction.VolumeAction)
    assertEquals(true, (volumeResult.action as JarvisAction.VolumeAction).raise)
  }
}

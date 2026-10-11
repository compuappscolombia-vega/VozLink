package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VozLink", appName)
  }

  @Test
  fun `test safety sanitization`() {
    val (safe, hadViolation) = com.example.safety.SafetyManager.sanitizeText("Hola amigo")
    assertEquals("Hola amigo", safe)
    assertEquals(false, hadViolation)
  }
}

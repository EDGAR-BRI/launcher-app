package com.example

import com.example.data.updater.AppUpdateManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun versionComparison_isCorrect() {
    assertTrue(AppUpdateManager.isVersionNewer("1.0.2", "1.0.1"))
    assertTrue(AppUpdateManager.isVersionNewer("v1.0.2", "1.0.1"))
    assertTrue(AppUpdateManager.isVersionNewer("2.0.0", "1.9.9"))
    assertTrue(AppUpdateManager.isVersionNewer("1.1.0", "1.0.9"))
    assertFalse(AppUpdateManager.isVersionNewer("1.0.1", "1.0.1"))
    assertFalse(AppUpdateManager.isVersionNewer("v1.0.1", "1.0.1"))
    assertFalse(AppUpdateManager.isVersionNewer("1.0.0", "1.0.1"))
    assertFalse(AppUpdateManager.isVersionNewer("0.9.9", "1.0.0"))
  }
}

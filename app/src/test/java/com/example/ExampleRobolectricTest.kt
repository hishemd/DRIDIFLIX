package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StreamType
import com.example.data.repository.M3uParser
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
    assertEquals("DRIDI FLIX", appName)
  }

  @Test
  fun `parse sample m3u playlist`() {
    val m3u = """
      #EXTM3U
      #EXTINF:-1 tvg-id="ch1" tvg-name="Al Jazeera" tvg-logo="http://logo.png" group-title="News",Al Jazeera HD
      http://server.com/live/ch1.m3u8
      #EXTINF:-1 tvg-id="mov1" group-title="Movies",Action Movie 2026
      http://server.com/movie/mov1.mp4
    """.trimIndent()

    val result = M3uParser.parse(m3u)
    assertEquals(2, result.items.size)
    assertEquals("Al Jazeera HD", result.items[0].name)
    assertEquals(StreamType.LIVE, result.items[0].streamType)
    assertEquals("Action Movie 2026", result.items[1].name)
    assertEquals(StreamType.VOD, result.items[1].streamType)
  }
}

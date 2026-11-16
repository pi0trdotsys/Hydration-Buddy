package com.kropi.hydration.update

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLogicTest {
    private val sha = "a".repeat(64)

    private fun release(tag: String = "v1.7.0", draft: Boolean = false, pre: Boolean = false, url: String =
        "https://github.com/pi0trdotsys/Hydration-Buddy/releases/download/v1.7.0/kropi-hydration-v1.7.0.apk") =
        JSONObject(
            """{"tag_name":"$tag","name":"Kropi 1.7.0","body":"notes","draft":$draft,"prerelease":$pre,
               "assets":[{"name":"kropi-hydration-v1.7.0.apk","browser_download_url":"$url","size":7000000,"digest":"sha256:$sha"}]}""",
        )

    @Test
    fun parsesLatestRelease() {
        val r = UpdateLogic.parseRelease(release())
        assertNotNull(r)
        assertEquals("1.7.0", r!!.version)
        assertEquals(sha, r.sha256)
        assertEquals(7_000_000L, r.size)
    }

    @Test
    fun skipsDraftsPreReleasesAndOddTags() {
        assertNull(UpdateLogic.parseRelease(release(draft = true)))
        assertNull(UpdateLogic.parseRelease(release(pre = true)))
        assertNull(UpdateLogic.parseRelease(release(tag = "nightly")))
        assertNull(UpdateLogic.parseRelease(null))
    }

    @Test
    fun onlyAcceptsApksFromThisRepoCaseInsensitively() {
        assertTrue(UpdateLogic.allowedUrl("https://github.com/pi0trdotsys/hydration-buddy/releases/download/v1/a.apk"))
        assertFalse(UpdateLogic.allowedUrl("https://github.com/someone/else/releases/download/v1/a.apk"))
        assertFalse(UpdateLogic.allowedUrl("https://github.com/pi0trdotsys/Hydration-Buddy/releases/download/v1/a.zip"))
        assertFalse(UpdateLogic.allowedUrl("https://github.com/pi0trdotsys/Hydration-Buddy/releases/download/../x.apk"))
        assertNull(UpdateLogic.parseRelease(release(url = "https://evil.example/kropi.apk")))
    }

    @Test
    fun comparesVersionsNumerically() {
        assertEquals(1, UpdateLogic.compare("1.10.0", "1.9.3"))
        assertEquals(0, UpdateLogic.compare("v1.7", "1.7.0"))
        assertEquals(-1, UpdateLogic.compare("1.6.0", "1.7.0"))
    }

    @Test
    fun normalizesDigests() {
        assertEquals(sha, UpdateLogic.normalizeSha("sha256:${sha.uppercase()}"))
        assertEquals("", UpdateLogic.normalizeSha("sha1:abc"))
        assertEquals("", UpdateLogic.normalizeSha(null))
    }
}

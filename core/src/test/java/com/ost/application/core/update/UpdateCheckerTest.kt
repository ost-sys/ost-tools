package com.ost.application.core.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class UpdateCheckerTest {
    @Test
    fun `stable release is newer than its own beta`() {
        assertTrue(UpdateChecker.isNewerVersion("4.0.0", "4.0.0-beta4"))
        assertFalse(UpdateChecker.isNewerVersion("4.0.0-beta4", "4.0.0"))
    }
    @Test
    fun `newer beta wins over older beta`() {
        assertTrue(UpdateChecker.isNewerVersion("4.0.0-beta5", "4.0.0-beta4"))
        assertFalse(UpdateChecker.isNewerVersion("4.0.0-beta4", "4.0.0-beta5"))
    }
    @Test
    fun `prerelease numbers compare numerically not lexicographically`() {
        assertTrue(UpdateChecker.isNewerVersion("4.0.0-beta10", "4.0.0-beta9"))
    }
    @Test
    fun `underscore and dash suffix separators are equivalent`() {
        assertEquals(0, UpdateChecker.compareVersions("4.0.0_beta4", "4.0.0-beta4"))
    }
    @Test
    fun `v prefix is ignored`() {
        assertEquals(0, UpdateChecker.compareVersions("v4.0.0", "4.0.0"))
        assertTrue(UpdateChecker.isNewerVersion("v4.1.0", "4.0.9"))
    }
    @Test
    fun `numeric components compare component-wise`() {
        assertTrue(UpdateChecker.isNewerVersion("4.10.0", "4.9.0"))
        assertTrue(UpdateChecker.isNewerVersion("5.0.0", "4.99.99"))
        assertFalse(UpdateChecker.isNewerVersion("3.9.9", "4.0.0"))
    }
    @Test
    fun `missing components count as zero`() {
        assertEquals(0, UpdateChecker.compareVersions("4.0", "4.0.0"))
        assertTrue(UpdateChecker.isNewerVersion("4.0.1", "4.0"))
    }
    @Test
    fun `equal versions are not an update`() {
        assertFalse(UpdateChecker.isNewerVersion("4.0.0-beta4", "4.0.0-beta4"))
        assertFalse(UpdateChecker.isNewerVersion("4.0.0", "4.0.0"))
    }
    @Test
    fun `null handling`() {
        assertEquals(0, UpdateChecker.compareVersions(null, null))
        assertTrue(UpdateChecker.compareVersions(null, "1.0") < 0)
        assertTrue(UpdateChecker.compareVersions("1.0", null) > 0)
        assertFalse(UpdateChecker.isNewerVersion(null, "1.0"))
    }
    @Test
    fun `prerelease detection`() {
        assertTrue(UpdateChecker.isPrereleaseVersion("4.0.0-beta4"))
        assertTrue(UpdateChecker.isPrereleaseVersion("4.0.0_beta4"))
        assertFalse(UpdateChecker.isPrereleaseVersion("4.0.0"))
        assertFalse(UpdateChecker.isPrereleaseVersion(null))
    }
    @Test
    fun `different prerelease labels compare by label`() {
        assertTrue(UpdateChecker.isNewerVersion("4.0.0-beta1", "4.0.0-alpha9"))
    }
}

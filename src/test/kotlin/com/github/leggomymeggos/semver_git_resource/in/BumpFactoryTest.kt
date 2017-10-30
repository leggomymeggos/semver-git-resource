package com.github.leggomymeggos.semver_git_resource.`in`

import com.github.leggomymeggos.semver_git_resource.models.BumpFactory
import com.github.zafarkhaja.semver.Version
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BumpFactoryTest {
    private val factory = BumpFactory()

    @Test
    fun `creates patch bumper`() {
        val bump = factory.create("patch", "")
        assertThat(bump.apply(Version.valueOf("0.0.1"))).isEqualTo(Version.valueOf("0.0.2"))
    }

    @Test
    fun `creates minor bumper`() {
        val bump = factory.create("minor", "")
        assertThat(bump.apply(Version.valueOf("0.0.1"))).isEqualTo(Version.valueOf("0.1.0"))
    }

    @Test
    fun `creates major bumper`() {
        val bump = factory.create("major", "")
        assertThat(bump.apply(Version.valueOf("0.0.1"))).isEqualTo(Version.valueOf("1.0.0"))
    }

    @Test
    fun `creates pre-release bumper`() {
        val bump = factory.create("", "pre")
        assertThat(bump.apply(Version.valueOf("0.0.1"))).isEqualTo(Version.valueOf("0.0.1-pre.1"))
    }

    @Test
    fun `creates version bumpers with pre releases`() {
        mapOf(
                "major" to "2.0.0",
                "minor" to "1.3.0",
                "patch" to "1.2.4"
        ).forEach { bump, version ->
            val result = factory.create(bump, "pre")
            assertThat(result.apply(Version.valueOf("1.2.3"))).isEqualTo(Version.valueOf("$version-pre.1"))
        }
    }

    @Test
    fun `increments pre-release bumper`() {
        val bump = factory.create("", "pre")
        assertThat(bump.apply(Version.valueOf("0.0.1-pre.1"))).isEqualTo(Version.valueOf("0.0.1-pre.2"))
    }

    @Test
    fun `can change pre-release bumper`() {
        val bump = factory.create("", "other")
        assertThat(bump.apply(Version.valueOf("0.0.1-pre.1"))).isEqualTo(Version.valueOf("0.0.1-other.1"))
    }

    @Test
    fun `creates final bumper`() {
        val bump = factory.create("final", "")
        assertThat(bump.apply(Version.valueOf("0.0.1-pre.2"))).isEqualTo(Version.valueOf("0.0.1"))
    }

    @Test
    fun `defaults to returning current version`() {
        val bump = factory.create("something not known", "")
        assertThat(bump.apply(Version.valueOf("1.2.3"))).isEqualTo(Version.valueOf("1.2.3"))
    }
}


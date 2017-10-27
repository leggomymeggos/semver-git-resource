package com.github.leggomymeggos.semver_git_resource.`in`

import com.github.leggomymeggos.semver_git_resource.models.BumpFactory
import com.github.leggomymeggos.semver_git_resource.models.getSuccess
import com.github.zafarkhaja.semver.Version
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BumpFactoryTest {
    private val factory = BumpFactory()

    @Test
    fun `creates patch bumper`() {
        val bump = factory.create("patch", "").getSuccess()
        assertThat(bump.apply(Version.valueOf("0.0.1"))).isEqualTo(Version.valueOf("0.0.2"))
    }

    @Test
    fun `creates minor bumper`() {
        val bump = factory.create("minor", "").getSuccess()
        assertThat(bump.apply(Version.valueOf("0.0.1"))).isEqualTo(Version.valueOf("0.1.0"))
    }
}


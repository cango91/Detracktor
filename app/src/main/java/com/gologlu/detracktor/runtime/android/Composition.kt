package com.gologlu.detracktor.runtime.android

import android.content.Context
import com.gologlu.detracktor.application.repo.SettingsRepository
import com.gologlu.detracktor.application.service.DefaultSettingsService
import com.gologlu.detracktor.application.service.SettingsService
import com.gologlu.detracktor.application.service.match.DefaultRuleEngine
import com.gologlu.detracktor.application.service.match.RuleEngine
import com.gologlu.detracktor.application.service.net.HostCanonicalizer as HostCanonicalizerSpi
import com.gologlu.detracktor.runtime.android.service.net.HostCanonicalizer as HostCanonicalizerImpl
import com.gologlu.detracktor.runtime.android.repo.AndroidSettingsRepository

object CompositionRoot {
    fun provideSettingsRepository(context: Context): SettingsRepository =
        AndroidSettingsRepository(context)

    fun provideSettingsService(context: Context): SettingsService =
        DefaultSettingsService(provideSettingsRepository(context))

    fun provideHostCanonicalizer(): HostCanonicalizerSpi = HostCanonicalizerSpi { raw ->
        HostCanonicalizerImpl.toAscii(raw)
    }

    fun provideRuleEngine(): RuleEngine = DefaultRuleEngine()
}



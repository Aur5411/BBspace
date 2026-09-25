package com.android.purebilibili.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode.Disable
import androidx.benchmark.macro.BaselineProfileMode.Require
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode.COLD
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiliPaiStartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupWithoutPreCompilation() = startup(CompilationMode.None())

    @Test
    fun startupPartialWithoutBaselineProfile() = startup(
        CompilationMode.Partial(
            baselineProfileMode = Disable,
            warmupIterations = 2
        )
    )

    @Test
    fun startupPartialWithBaselineProfile() = startup(
        CompilationMode.Partial(
            baselineProfileMode = Require
        )
    )

    @Test
    fun startupFullCompilation() = startup(CompilationMode.Full())

    private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        iterations = STARTUP_BENCHMARK_ITERATIONS,
        startupMode = COLD,
        setupBlock = {
            pressHome()
        }
    ) {
        startActivityAndWait()
        device.waitForIdle()
    }
}

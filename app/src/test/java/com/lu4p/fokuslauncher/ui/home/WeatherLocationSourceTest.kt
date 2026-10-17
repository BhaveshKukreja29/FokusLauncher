package com.lu4p.fokuslauncher.ui.home

import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.location.LocationManagerCompat
import androidx.core.util.Consumer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.util.concurrent.Executor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WeatherLocationSourceTest {
    private val manager = mockk<LocationManager>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(LocationManagerCompat::class)
        every { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns true
        every { manager.getLastKnownLocation(any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkStatic(LocationManagerCompat::class)
    }

    @Test
    fun `timeout cancels the request and falls back to cached location`() = runTest {
        val signal = slot<CancellationSignal>()
        val cached = Location(LocationManager.NETWORK_PROVIDER)
        every { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } returns cached
        every {
            LocationManagerCompat.getCurrentLocation(manager, any(), capture(signal), any<Executor>(), any<Consumer<Location?>>())
        } answers { }

        assertSame(cached, WeatherLocationSource(manager).getLocation())
        assertTrue(signal.captured.isCanceled)
        assertEquals(10_000L, testScheduler.currentTime)
    }

    @Test
    fun `caller cancellation cancels request without reading cached location`() = runTest {
        val signal = slot<CancellationSignal>()
        val callback = slot<Consumer<Location?>>()
        every {
            LocationManagerCompat.getCurrentLocation(manager, any(), capture(signal), any<Executor>(), capture(callback))
        } answers { }
        val job = launch { WeatherLocationSource(manager).getLocation() }
        runCurrent()
        job.cancel()
        advanceUntilIdle()

        assertTrue(job.isCancelled)
        assertTrue(signal.captured.isCanceled)
        verify(exactly = 0) { manager.getLastKnownLocation(any()) }
        callback.captured.accept(Location(LocationManager.NETWORK_PROVIDER))
    }

    @Test
    fun `disabled network uses passive cache even if GPS cache is inaccessible`() = runTest {
        val cached = Location(LocationManager.PASSIVE_PROVIDER)
        every { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns false
        every { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } throws SecurityException()
        every { manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } returns cached

        assertSame(cached, WeatherLocationSource(manager).getLocation())
        verify(exactly = 0) {
            LocationManagerCompat.getCurrentLocation(manager, any(), any<CancellationSignal>(), any<Executor>(), any<Consumer<Location?>>())
        }
    }

    @Test
    fun `null fix with no cache returns null for persisted-location fallback`() = runTest {
        every {
            LocationManagerCompat.getCurrentLocation(manager, any(), any<CancellationSignal>(), any<Executor>(), any<Consumer<Location?>>())
        } answers { arg<Consumer<Location?>>(4).accept(null) }

        assertNull(WeatherLocationSource(manager).getLocation())
    }

    @Test
    fun `permission revoked during request still allows cached fallback`() = runTest {
        val cached = Location(LocationManager.PASSIVE_PROVIDER)
        every {
            LocationManagerCompat.getCurrentLocation(manager, any(), any<CancellationSignal>(), any<Executor>(), any<Consumer<Location?>>())
        } throws SecurityException()
        every { manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } returns cached

        assertSame(cached, WeatherLocationSource(manager).getLocation())
    }

    @Test
    fun `uses cached GPS when network lookup fails`() = runTest {
        val cached = Location(LocationManager.GPS_PROVIDER)
        every { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns cached
        every {
            LocationManagerCompat.getCurrentLocation(manager, any(), any<CancellationSignal>(), any<Executor>(), any<Consumer<Location?>>())
        } throws IllegalArgumentException("provider unavailable")

        assertSame(cached, WeatherLocationSource(manager).getLocation())
    }

    @Test
    fun `requests a location when Android has no cached fix`() = runTest {
        val fresh = Location(LocationManager.NETWORK_PROVIDER).apply {
            latitude = 52.52
            longitude = 13.41
        }
        every {
            LocationManagerCompat.getCurrentLocation(
                manager, LocationManager.NETWORK_PROVIDER, any<CancellationSignal>(),
                any<Executor>(), any<Consumer<Location?>>()
            )
        } answers {
            arg<Consumer<Location?>>(4).accept(fresh)
        }

        assertSame(fresh, WeatherLocationSource(manager).getLocation())
        verify(exactly = 1) {
            LocationManagerCompat.getCurrentLocation(manager, LocationManager.NETWORK_PROVIDER, any<CancellationSignal>(), any<Executor>(), any<Consumer<Location?>>())
        }
    }
}

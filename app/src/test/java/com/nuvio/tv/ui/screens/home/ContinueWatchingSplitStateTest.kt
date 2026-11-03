package com.nuvio.tv.ui.screens.home

import com.nuvio.tv.domain.model.ContinueWatchingPriority
import com.nuvio.tv.domain.model.WatchProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueWatchingSplitStateTest {

    private val seriesItem = ContinueWatchingItem.InProgress(
        progress = WatchProgress(
            contentId = "tt0944947",
            contentType = "series",
            name = "Game of Thrones",
            poster = null,
            backdrop = null,
            logo = null,
            videoId = "tt0944947:1:1",
            season = 1,
            episode = 1,
            episodeTitle = "Winter Is Coming",
            position = 600_000L,
            duration = 3_600_000L,
            lastWatched = 2_000L
        )
    )

    private val movieItem = ContinueWatchingItem.InProgress(
        progress = WatchProgress(
            contentId = "tt1375666",
            contentType = "movie",
            name = "Inception",
            poster = null,
            backdrop = null,
            logo = null,
            videoId = "tt1375666",
            season = null,
            episode = null,
            episodeTitle = null,
            position = 900_000L,
            duration = 7_200_000L,
            lastWatched = 1_000L
        )
    )

    @Test
    fun `buildContinueWatchingUiState returns Combined when split is false`() {
        val state = buildContinueWatchingUiState(
            items = listOf(seriesItem, movieItem),
            isSplit = false,
            priority = ContinueWatchingPriority.SERIES_FIRST
        )

        assertTrue(state is ContinueWatchingUiState.Combined)
        val combined = state as ContinueWatchingUiState.Combined
        assertEquals(listOf(seriesItem, movieItem), combined.items)
    }

    @Test
    fun `buildContinueWatchingUiState orders series rail before movies rail when SERIES_FIRST`() {
        val state = buildContinueWatchingUiState(
            items = listOf(movieItem, seriesItem),
            isSplit = true,
            priority = ContinueWatchingPriority.SERIES_FIRST
        )

        assertTrue(state is ContinueWatchingUiState.Separate)
        val separate = state as ContinueWatchingUiState.Separate
        assertEquals(2, separate.orderedRails.size)
        assertEquals(ContinueWatchingMediaCategory.SERIES, separate.orderedRails[0].category)
        assertEquals(listOf(seriesItem), separate.orderedRails[0].items)
        assertEquals(ContinueWatchingMediaCategory.MOVIES, separate.orderedRails[1].category)
        assertEquals(listOf(movieItem), separate.orderedRails[1].items)
    }

    @Test
    fun `buildContinueWatchingUiState orders movies rail before series rail when MOVIES_FIRST`() {
        val state = buildContinueWatchingUiState(
            items = listOf(seriesItem, movieItem),
            isSplit = true,
            priority = ContinueWatchingPriority.MOVIES_FIRST
        )

        assertTrue(state is ContinueWatchingUiState.Separate)
        val separate = state as ContinueWatchingUiState.Separate
        assertEquals(2, separate.orderedRails.size)
        assertEquals(ContinueWatchingMediaCategory.MOVIES, separate.orderedRails[0].category)
        assertEquals(listOf(movieItem), separate.orderedRails[0].items)
        assertEquals(ContinueWatchingMediaCategory.SERIES, separate.orderedRails[1].category)
        assertEquals(listOf(seriesItem), separate.orderedRails[1].items)
    }

    @Test
    fun `buildContinueWatchingUiState omits empty category rail`() {
        val state = buildContinueWatchingUiState(
            items = listOf(seriesItem),
            isSplit = true,
            priority = ContinueWatchingPriority.MOVIES_FIRST
        )

        assertTrue(state is ContinueWatchingUiState.Separate)
        val separate = state as ContinueWatchingUiState.Separate
        assertEquals(1, separate.orderedRails.size)
        assertEquals(ContinueWatchingMediaCategory.SERIES, separate.orderedRails[0].category)
    }

    @Test
    fun `observeContinueWatchingUiState combines flows reactively`() = runTest {
        val historyFlow = MutableStateFlow(listOf(seriesItem, movieItem))
        val splitFlow = MutableStateFlow(false)
        val priorityFlow = MutableStateFlow(ContinueWatchingPriority.SERIES_FIRST)

        val observedFlow = observeContinueWatchingUiState(
            watchHistoryFlow = historyFlow,
            isSplitFlow = splitFlow,
            priorityFlow = priorityFlow
        )

        assertTrue(observedFlow.first() is ContinueWatchingUiState.Combined)

        splitFlow.value = true
        val splitSeriesFirst = observedFlow.first() as ContinueWatchingUiState.Separate
        assertEquals(ContinueWatchingMediaCategory.SERIES, splitSeriesFirst.orderedRails.first().category)

        priorityFlow.value = ContinueWatchingPriority.MOVIES_FIRST
        val splitMoviesFirst = observedFlow.first() as ContinueWatchingUiState.Separate
        assertEquals(ContinueWatchingMediaCategory.MOVIES, splitMoviesFirst.orderedRails.first().category)
    }

    @Test
    fun `buildContinueWatchingUiState filters out series when showSeries is false`() {
        val state = buildContinueWatchingUiState(
            items = listOf(seriesItem, movieItem),
            isSplit = false,
            priority = ContinueWatchingPriority.SERIES_FIRST,
            showSeries = false,
            showMovies = true
        )

        assertTrue(state is ContinueWatchingUiState.Combined)
        val combined = state as ContinueWatchingUiState.Combined
        assertEquals(listOf(movieItem), combined.items)
    }

    @Test
    fun `buildContinueWatchingUiState filters out movies when showMovies is false`() {
        val state = buildContinueWatchingUiState(
            items = listOf(seriesItem, movieItem),
            isSplit = false,
            priority = ContinueWatchingPriority.SERIES_FIRST,
            showSeries = true,
            showMovies = false
        )

        assertTrue(state is ContinueWatchingUiState.Combined)
        val combined = state as ContinueWatchingUiState.Combined
        assertEquals(listOf(seriesItem), combined.items)
    }

    @Test
    fun `buildContinueWatchingUiState forces Combined when split is requested but only one media type is enabled`() {
        val state = buildContinueWatchingUiState(
            items = listOf(seriesItem, movieItem),
            isSplit = true,
            priority = ContinueWatchingPriority.SERIES_FIRST,
            showSeries = true,
            showMovies = false
        )

        assertTrue(state is ContinueWatchingUiState.Combined)
        val combined = state as ContinueWatchingUiState.Combined
        assertEquals(listOf(seriesItem), combined.items)
    }

    @Test
    fun `filterContinueWatchingByMediaType preserves items when both are false as safe fallback`() {
        val items = listOf(seriesItem, movieItem)
        val filtered = items.filterContinueWatchingByMediaType(showSeries = false, showMovies = false)
        assertEquals(items, filtered)
    }

    @Test
    fun `observeContinueWatchingUiState reactively responds to showSeries and showMovies flows`() = runTest {
        val historyFlow = MutableStateFlow(listOf(seriesItem, movieItem))
        val splitFlow = MutableStateFlow(false)
        val priorityFlow = MutableStateFlow(ContinueWatchingPriority.SERIES_FIRST)
        val showSeriesFlow = MutableStateFlow(true)
        val showMoviesFlow = MutableStateFlow(true)

        val observedFlow = observeContinueWatchingUiState(
            watchHistoryFlow = historyFlow,
            isSplitFlow = splitFlow,
            priorityFlow = priorityFlow,
            showSeriesFlow = showSeriesFlow,
            showMoviesFlow = showMoviesFlow
        )

        val initial = observedFlow.first() as ContinueWatchingUiState.Combined
        assertEquals(listOf(seriesItem, movieItem), initial.items)

        showSeriesFlow.value = false
        val moviesOnly = observedFlow.first() as ContinueWatchingUiState.Combined
        assertEquals(listOf(movieItem), moviesOnly.items)

        showSeriesFlow.value = true
        showMoviesFlow.value = false
        val seriesOnly = observedFlow.first() as ContinueWatchingUiState.Combined
        assertEquals(listOf(seriesItem), seriesOnly.items)
    }
}


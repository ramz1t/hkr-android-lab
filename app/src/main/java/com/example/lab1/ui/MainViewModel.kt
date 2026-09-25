package com.example.lab1.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab1.data.repository.GitHubRepository
import com.example.lab1.domain.AlertDetector
import com.example.lab1.domain.DetectionResult
import com.example.lab1.domain.DetectorType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.time.Duration.Companion.seconds

data class MainUiState(
    val houseConfig: Map<String, Any>? = null,
    val detectionResult: DetectionResult? = null,
    val selectedDetector: DetectorType = DetectorType.HEURISTIC,
    val isLoading: Boolean = false,
    val configErrorMessage: String? = null,
    val detectorErrorMessage: String? = null,
    val updatedAt: Date = Date(),
)

class MainViewModel(
    private val repository: GitHubRepository,
    private val detectors: Map<DetectorType, AlertDetector>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var latestPullRequestTitles: List<String> = emptyList()
    private var analysisJob: Job? = null
    private var analysisGeneration = 0L
    private var isPolling = false
    private var isAnalyzing = false

    init {
        DetectorType.entries.forEach { detectorType ->
            checkNotNull(detectors[detectorType]) {
                "No detector is configured for $detectorType."
            }
        }
        startPolling()
    }

    fun selectDetector(detectorType: DetectorType) {
        if (detectorType == _uiState.value.selectedDetector) {
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedDetector = detectorType,
            detectorErrorMessage = null,
        )
        startAnalysis(latestPullRequestTitles)
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                setPolling(true)
                try {
                    pollData()
                } finally {
                    setPolling(false)
                }
                delay(30.seconds)
            }
        }
    }

    private suspend fun pollData() {
        val configResult = repository.getHouseConfig()
        configResult.onSuccess { config ->
            _uiState.value = _uiState.value.copy(
                houseConfig = config,
                configErrorMessage = null,
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                configErrorMessage = "Config Error: ${error.message ?: "Unknown error"}",
            )
        }

        val prsResult = repository.getPullRequests()
        prsResult.onSuccess { prs ->
            latestPullRequestTitles = prs.map { it.title }
            startAnalysis(latestPullRequestTitles)
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                detectorErrorMessage =
                    "Pull Request Error: ${error.message ?: "Unknown error"}",
            )
        }
    }

    private fun startAnalysis(titles: List<String>) {
        analysisJob?.cancel()
        val generation = ++analysisGeneration
        val detectorType = _uiState.value.selectedDetector

        if (titles.isEmpty()) {
            setAnalyzing(false)
            _uiState.value = _uiState.value.copy(
                detectionResult = null,
                detectorErrorMessage = null,
                updatedAt = Date(),
            )
            return
        }

        setAnalyzing(true)
        analysisJob = viewModelScope.launch {
            try {
                val detector = checkNotNull(detectors[detectorType])
                var highestDetection: DetectionResult? = null

                for (title in titles) {
                    val detection = detector.analyzeText(title)
                    if (
                        detection.confidenceScore >
                        (highestDetection?.confidenceScore ?: -1)
                    ) {
                        highestDetection = detection
                    }
                }

                if (isCurrentAnalysis(generation, detectorType)) {
                    _uiState.value = _uiState.value.copy(
                        detectionResult = highestDetection,
                        detectorErrorMessage = null,
                        updatedAt = Date(),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (isCurrentAnalysis(generation, detectorType)) {
                    _uiState.value = _uiState.value.copy(
                        detectorErrorMessage =
                            "${detectorType.label} Error: " +
                                (error.message ?: "Unknown error"),
                    )
                }
            } finally {
                if (generation == analysisGeneration) {
                    setAnalyzing(false)
                }
            }
        }
    }

    private fun isCurrentAnalysis(
        generation: Long,
        detectorType: DetectorType,
    ): Boolean {
        return generation == analysisGeneration &&
            detectorType == _uiState.value.selectedDetector
    }

    private fun setPolling(value: Boolean) {
        isPolling = value
        updateLoadingState()
    }

    private fun setAnalyzing(value: Boolean) {
        isAnalyzing = value
        updateLoadingState()
    }

    private fun updateLoadingState() {
        _uiState.value = _uiState.value.copy(
            isLoading = isPolling || isAnalyzing,
        )
    }

}

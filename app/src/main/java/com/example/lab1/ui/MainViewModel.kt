package com.example.lab1.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab1.data.model.PullRequest
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
    val actionMessage: String? = null,
    val actionErrorMessage: String? = null,
    val isActionInProgress: Boolean = false,
    val updatedAt: Date = Date(),
)

class MainViewModel(
    private val repository: GitHubRepository,
    private val detectors: Map<DetectorType, AlertDetector>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var latestPullRequests: List<PullRequest> = emptyList()
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
        startAnalysis(latestPullRequests)
    }

    fun forceReject() {
        executeAction("Force Reject") { pullRequestNumber ->
            repository.forceReject(pullRequestNumber)
        }
    }

    fun forceMerge() {
        executeAction("Force Merge") { pullRequestNumber ->
            repository.forceMerge(pullRequestNumber)
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                refreshNow()
                delay(30.seconds)
            }
        }
    }

    private suspend fun refreshNow() {
        setPolling(true)
        try {
            pollData()
        } finally {
            setPolling(false)
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
            latestPullRequests = prs
            startAnalysis(latestPullRequests)
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                detectorErrorMessage =
                    "Pull Request Error: ${error.message ?: "Unknown error"}",
            )
        }
    }

    private fun startAnalysis(pullRequests: List<PullRequest>) {
        analysisJob?.cancel()
        val generation = ++analysisGeneration
        val detectorType = _uiState.value.selectedDetector

        if (pullRequests.isEmpty()) {
            val actionWasPending = _uiState.value.isActionInProgress
            setAnalyzing(false)
            _uiState.value = _uiState.value.copy(
                detectionResult = null,
                detectorErrorMessage = null,
                isActionInProgress = false,
                actionMessage = if (actionWasPending) {
                    "Incident closed on GitHub."
                } else {
                    _uiState.value.actionMessage
                },
                updatedAt = Date(),
            )
            return
        }

        setAnalyzing(true)
        analysisJob = viewModelScope.launch {
            try {
                val detector = checkNotNull(detectors[detectorType])
                var highestDetection: DetectionResult? = null

                for (pullRequest in pullRequests) {
                    val comments = pullRequest.comments.joinToString("\n\n").trim()
                    if (comments.isEmpty()) {
                        continue
                    }

                    val detection = detector.analyzeText(comments).copy(
                        pullRequestNumber = pullRequest.number,
                    )
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

    private fun executeAction(
        actionName: String,
        action: suspend (Int) -> Result<Unit>,
    ) {
        if (_uiState.value.isActionInProgress) {
            return
        }

        val alertPullRequest = _uiState.value.detectionResult
            ?.takeIf { it.isAttack }
            ?.pullRequestNumber
        if (alertPullRequest == null) {
            _uiState.value = _uiState.value.copy(
                actionErrorMessage = "No active alert pull request is available.",
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            actionMessage = null,
            actionErrorMessage = null,
            isActionInProgress = true,
        )
        viewModelScope.launch {
            val result = try {
                action(alertPullRequest)
            } catch (error: Exception) {
                Result.failure(error)
            }

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    actionMessage =
                        "$actionName sent. Waiting for GitHub to close the pull request.",
                )
                refreshNow()
            } else {
                val error = result.exceptionOrNull()
                _uiState.value = _uiState.value.copy(
                    isActionInProgress = false,
                    actionErrorMessage = error?.message ?: "$actionName failed.",
                )
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

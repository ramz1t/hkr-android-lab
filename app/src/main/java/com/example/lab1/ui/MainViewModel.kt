package com.example.lab1.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab1.data.repository.GitHubRepository
import com.example.lab1.domain.DeceptionDetector
import com.example.lab1.domain.DetectionResult
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val updatedAt: Date = Date()
)

class MainViewModel(
    private val repository: GitHubRepository,
    private val detector: DeceptionDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                pollData()
                _uiState.value = _uiState.value.copy(isLoading = false, updatedAt = Date())
                delay(30.seconds)
            }
        }
    }

    private suspend fun pollData() {
        val configResult = repository.getHouseConfig()
        configResult.onSuccess { config ->
            _uiState.value = _uiState.value.copy(houseConfig = config, errorMessage = null)
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(errorMessage = "Config Error: ${error.message}")
        }

        val prsResult = repository.getPullRequests()
        prsResult.onSuccess { prs ->
            var highestDetection: DetectionResult? = null
            
            for (pr in prs) {
                val commentsResult = repository.getComments(pr.number)
                commentsResult.onSuccess { comments ->
                    for (comment in comments) {
                        val detection = detector.analyzeText(comment.body)
                        val currentHighest = highestDetection?.confidenceScore ?: -1
                        if (detection.confidenceScore > currentHighest) {
                            highestDetection = detection
                        }
                    }
                }
            }
            
            _uiState.value = _uiState.value.copy(detectionResult = highestDetection)
        }.onFailure { error ->
        }
    }
}

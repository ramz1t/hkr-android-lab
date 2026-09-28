package com.example.lab1.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lab1.domain.DetectorType
import java.text.SimpleDateFormat

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isAlert = uiState.detectionResult?.isAttack == true
    val accentColor = if (isAlert) Color.Red else Color(0xFF2E7D32)
    val formatter = SimpleDateFormat("HH:mm:ss", LocalConfiguration.current.locales[0])
    val timestamp = formatter.format(uiState.updatedAt)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.padding(vertical = 32.dp))

        if (uiState.isLoading) {
            Text("Updating...")
        } else {
            Text("Updated at $timestamp")
        }

        uiState.configErrorMessage?.let {
            Text(text = it, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
        }

        uiState.detectorErrorMessage?.let {
            Text(text = it, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
        }

        uiState.actionErrorMessage?.let {
            Text(text = it, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
        }

        uiState.actionMessage?.let {
            Text(
                text = it,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "House State", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                uiState.houseConfig?.let { config ->
                    config.forEach { (key, value) ->
                        InfoRow(
                            label = key.replace("_", " ").split(" ")
                                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } },
                            value = value.toString()
                        )
                    }
                } ?: Text("Loading house configuration...")
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Alert detector",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = uiState.selectedDetector.label,
                    )
                }
                Switch(
                    checked = uiState.selectedDetector == DetectorType.JEV,
                    onCheckedChange = { useJev ->
                        viewModel.selectDetector(
                            if (useJev) DetectorType.JEV else DetectorType.HEURISTIC,
                        )
                    },
                    enabled = !uiState.isLoading,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = accentColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isAlert) "SECURITY ALERT" else "SYSTEM NORMAL",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )

                uiState.detectionResult?.let { detection ->
                    if (isAlert) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Confidence Score: ${detection.confidenceScore}%",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Attack Text:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Text(
                            text = detection.rawText,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = viewModel::forceMerge,
                                enabled = !uiState.isLoading && !uiState.isActionInProgress,
                                border = BorderStroke(1.dp, Color.White),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Force Merge")
                            }
                            OutlinedButton(
                                onClick = viewModel::forceReject,
                                enabled = !uiState.isLoading && !uiState.isActionInProgress,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFFB71C1C),
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Force Reject")
                            }
                        }
                        if (uiState.isActionInProgress) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Command in progress...",
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Medium)
        Text(text = value)
    }
}

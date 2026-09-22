package com.example.lab1.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isAlert = uiState.detectionResult?.isAttack == true
    val accentColor = if (isAlert) Color.Red else Color(0xFF2E7D32)
    val formatter = SimpleDateFormat("HH:mm:ss")
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

        uiState.errorMessage?.let {
            Text(text = it, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
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
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (isAlert) Color.Red else accentColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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

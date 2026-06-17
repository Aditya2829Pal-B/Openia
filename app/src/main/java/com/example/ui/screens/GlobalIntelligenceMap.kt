package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalIntelligenceMapOverlay(
    onClose: () -> Unit
) {
    val trendingProblems = listOf(
        "AI Alignment Ethics" to 0.95f,
        "Resource Distribution" to 0.82f,
        "Truth Verification Systems" to 0.78f,
        "Neuro-Digital Intimacy" to 0.65f
    )
    
    val globalClusters = listOf(
        "Silicon Valley Node" to "System Architecture Focus",
        "Berlin Think-Tank" to "Social Protocol Synthesis",
        "Openia Global Hive" to "Philosophy & Consciousness"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Intelligence Map", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicBlack)
            )
        },
        containerColor = CosmicBlack
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    "TRENDING HUMAN PROBLEMS",
                    color = SolarCoral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                trendingProblems.forEach { (problem, intensity) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(CosmicGray, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(problem, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${(intensity * 100).toInt()}% Critical", color = SolarCoral, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { intensity },
                                modifier = Modifier.fillMaxWidth(),
                                color = SolarCoral,
                                trackColor = CosmicDark,
                            )
                        }
                    }
                }
            }
            
            item {
                Text(
                    "INNOVATION CLUSTERS (LIVE)",
                    color = NeoCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                globalClusters.forEach { (cluster, focus) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF1E2433), Color(0xFF161A24))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = NeoCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(cluster, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(focus, color = SoftText, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostEntity
import com.example.ui.theme.CosmicDark
import com.example.ui.theme.CosmicGray
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.SoftText
import com.example.ui.theme.SolarCoral

@Composable
fun ConsensusIntelligenceCard(post: PostEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "CONSENSUS INTELLIGENCE",
                color = NeoCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Credibility Score", color = Color.White, fontSize = 14.sp)
                    Text(
                        text = "${(post.consensusCredibility * 100).toInt()}%",
                        color = if (post.consensusCredibility >= 0.8f) Color(0xFF10B981) else SolarCoral,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Misinformation Risk", color = SoftText, fontSize = 14.sp)
                    Text(
                        text = "${(post.misinformationProbability * 100).toInt()}%",
                        color = if (post.misinformationProbability < 0.1f) NeoCyan else Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (post.misinformationProbability > 0.3f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "High Disputed Information detected via cross-referencing global statements.",
                        color = Color.Red.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Verified", tint = NeoCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Information aligns with highly rated scientific or philosophical Consensus Nodes.",
                        color = NeoCyan,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EvolutionLineageBanner(post: PostEntity, onEvolveClick: () -> Unit) {
    if (post.parentPostId != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color(0xFFA855F7).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Share, 
                contentDescription = "Evolved",
                tint = Color(0xFFA855F7),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Evolved Iteration", 
                    color = Color(0xFFA855F7), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 12.sp
                )
                Text(
                    "This idea branches from Post #${post.parentPostId}. It has been collaboratively expanded.",
                    color = SoftText,
                    fontSize = 11.sp,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onEvolveClick() }
                .background(CosmicDark, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Share, 
                contentDescription = "Evolve Idea",
                tint = NeoCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Evolve this Idea", 
                    color = NeoCyan, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 12.sp
                )
                Text(
                    "Fork this concept into a new version to refine the solution.",
                    color = SoftText,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AdvancedReputation
import com.example.ui.screens.*
import com.example.ui.theme.*

@Composable
fun AdvancedProfileCard(
    username: String,
    displayName: String,
    reputation: AdvancedReputation
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicGray)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prestige glowing rank ring
            val neoCyan = NeoCyan
            val solarCoral = SolarCoral
            val softText = SoftText

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        val brush = Brush.sweepGradient(
                            colors = listOf(neoCyan, solarCoral, softText, neoCyan)
                        )
                        rotate(rotation) {
                            drawCircle(
                                brush = brush,
                                radius = size.width / 2,
                                style = stroke
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(generateAvatarBrush(displayName.firstOrNull()?.uppercase() ?: "A")),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "A",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = displayName,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            Text(
                text = "@$username • Rank Level ${reputation.rankLevel} • ${reputation.civilizationClass}",
                color = SoftText,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
            
            // Archetype and Influence Domain
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ARCHETYPE", color = SoftText, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(reputation.thinkerArchetype, color = NeoCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DOMAIN", color = SoftText, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(reputation.influenceDomain, color = SolarCoral, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            // Civilizational Rank Name
            ContributorBadge(reputation)

            Spacer(modifier = Modifier.height(24.dp))

            // Multi-factor Scores
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScoreBar("Trust Level", reputation.trustScore, NeoCyan)
                ScoreBar("Empathy", reputation.empathyScore, SolarCoral)
                ScoreBar("Insight Depth", reputation.insightScore, Color(0xFFA855F7)) // Purple
                ScoreBar("Consensus", reputation.consensusScore, Color(0xFF10B981)) // Emerald
            }
        }
    }
}

@Composable
fun ScoreBar(label: String, score: Float, color: Color) {
    val animatedProgress by animateFloatAsState(targetValue = score, animationSpec = tween(1000))
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${(score * 100).toInt()}%", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(CosmicDark, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
fun ContributorBadge(reputation: AdvancedReputation?, simpleReputation: Int = 0) {
    val score = reputation?.totalScore?.takeIf { it > 0 } ?: simpleReputation
    val badgeColor = getReputationColor(score)
    val rankName = reputation?.rankName?.takeIf { it.isNotEmpty() } ?: getReputationText(score)
    val isHighValue = score > 50

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHighValue) badgeColor.copy(alpha = 0.15f) else CosmicDark)
            .border(
                1.dp,
                if (isHighValue) badgeColor.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        if (isHighValue && reputation != null) {
            ReputationGlowingIndicator(reputation, size = 10f)
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = "$rankName ($score rep)",
            color = if (isHighValue) badgeColor else SoftText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ReputationGlowingIndicator(reputation: AdvancedReputation, size: Float = 28f) {

    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val mainColor = when {
        reputation.isSpammer -> Color.Red
        reputation.totalScore > 100 -> SolarCoral
        reputation.totalScore > 50 -> NeoCyan
        else -> SoftText
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .background(mainColor.copy(alpha = alpha * 0.2f), CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((size * 0.6f).dp)
                .background(mainColor, CircleShape)
        )
    }
}

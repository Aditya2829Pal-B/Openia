package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PostViewModel

val ALL_CATEGORIES = listOf(
    "Technology", "Politics", "Sports", "Engineering", "Climate",
    "Science", "Philosophy", "Arts", "Business", "Startup",
    "Finance", "Literature", "History", "Entertainment", "Gaming",
    "Music", "Movies", "Travel", "Food", "Health",
    "Fitness", "Lifestyle", "Fashion", "Photography", "DIY",
    "Auto", "Real Estate", "Education", "Agriculture", "Space",
    "Math", "Crypto", "Law", "Medicine", "Psychology",
    "Sociology", "Environment", "Energy", "Robotics", "AI",
    "Software", "Hardware", "Network", "Security", "Design",
    "UX/UI", "Marketing", "Sales", "Management", "Leadership",
    "Productivity", "Writing", "Journalism", "Media", "Broadcasting",
    "Cooking", "Baking", "Diet", "Nutrition", "Yoga",
    "Meditation", "Mental Health", "Therapy", "Relationships", "Parenting",
    "Education Tech", "Language", "Culture", "Religion", "Spirituality",
    "Ethics", "Logic", "Maths", "Physics", "Chemistry",
    "Biology", "Astronomy", "Geology", "Meteorology", "Oceanography",
    "Botany", "Zoology", "Genetics", "Ecology", "Evolution",
    "Anatomy", "Physiology", "Immunology", "Pharmacology", "Pathology",
    "Nursing", "Dentistry", "Veterinary", "Agriculture Tech", "Forestry"
).sorted()

@Composable
fun CategoriesTabContent(
    viewModel: PostViewModel,
    headerContent: @Composable () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 0.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBlack)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                headerContent()
                
                Text(
                    text = "Categories",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Explore topics that matter to you",
                    color = SoftText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        items(ALL_CATEGORIES) { category ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicDark)
                    .clickable { onCategorySelected(category) }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    color = NeoCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

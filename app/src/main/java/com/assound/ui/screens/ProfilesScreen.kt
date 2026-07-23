package com.assound.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.assound.data.model.ProfileCategory
import com.assound.data.model.SoundProfile
import com.assound.data.model.SoundProfiles
import com.assound.ui.components.*
import com.assound.ui.theme.*
import com.assound.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedCategory by remember { mutableStateOf<ProfileCategory?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    
    val filteredProfiles = remember(selectedCategory, uiState.customProfiles) {
        val allProfiles = SoundProfiles.allProfiles + uiState.customProfiles
        if (selectedCategory == null) {
            allProfiles
        } else {
            allProfiles.filter { it.category == selectedCategory }
        }
    }
    
    val groupedProfiles = remember(filteredProfiles) {
        filteredProfiles.groupBy { it.brandName }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundGradientStart,
                        BackgroundGradientMid,
                        BackgroundGradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        "Sound Profiles",
                        style = ASsoundTypography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Save current as profile
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Save Profile",
                            tint = ASsoundPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            // Category Filter
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ASsoundChip(
                        text = "All",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                }
                
                items(ProfileCategory.values().toList()) { category ->
                    ASsoundChip(
                        text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profiles List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Currently selected profile
                uiState.selectedProfile?.let { currentProfile ->
                    item {
                        SectionHeader(title = "Currently Active")
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileCard(
                            profile = currentProfile,
                            isSelected = true,
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
                
                // Grouped by brand
                groupedProfiles.forEach { (brandName, profiles) ->
                    item {
                        SectionHeader(title = brandName)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    items(profiles) { profile ->
                        CompactProfileCard(
                            profile = profile,
                            isSelected = uiState.selectedProfile?.id == profile.id,
                            onClick = { viewModel.selectProfile(profile) }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        // Save Profile Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = {
                    Text(
                        "Save Custom Profile",
                        style = ASsoundTypography.titleMedium,
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            "Enter a name for your custom profile:",
                            style = ASsoundTypography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            label = { Text("Profile Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ASsoundPrimary,
                                unfocusedBorderColor = CardBorder,
                                cursorColor = ASsoundPrimary,
                                focusedLabelColor = ASsoundPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newProfileName.isNotBlank()) {
                                viewModel.saveCurrentAsCustomProfile(newProfileName.trim())
                                newProfileName = ""
                                showSaveDialog = false
                            }
                        },
                        enabled = newProfileName.isNotBlank()
                    ) {
                        Text("Save", color = ASsoundPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceContainer,
                shape = ASsoundCardShapes.controlCard
            )
        }
    }
}

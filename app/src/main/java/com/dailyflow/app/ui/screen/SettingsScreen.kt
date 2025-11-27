package com.dailyflow.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dailyflow.app.R
import com.dailyflow.app.ui.navigation.Screen
import com.dailyflow.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val privacyAccepted by viewModel.privacyAccepted.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Privacy section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_privacy_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!privacyAccepted) {
                        PrivacyConsentCard(
                            onAccept = { viewModel.acceptPrivacyPolicy() }
                        )
                    } else {
                        Text(stringResource(R.string.settings_privacy_accepted), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        
        // App settings section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_app_settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingItem(
                        icon = Icons.Default.Category,
                        title = stringResource(R.string.settings_categories_title),
                        subtitle = stringResource(R.string.settings_categories_subtitle),
                        onClick = { navController.navigate(Screen.CategoryManagement.route) }
                    )

                    SettingItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.settings_notifications_title),
                        subtitle = stringResource(R.string.settings_notifications_subtitle),
                        onClick = { navController.navigate(Screen.NotificationSettings.route) }
                    )
                    
                    SettingItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.settings_language),
                        subtitle = if (currentLanguage == "ru") "Русский" else "English",
                        onClick = { navController.navigate(Screen.LanguageSelection.route) }
                    )
                }
            }
        }
        
        // About section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_about_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_version_title),
                        subtitle = "1.0.0",
                        onClick = { /* TODO: Show version info */ }
                    )
                    
                    SettingItem(
                        icon = Icons.Default.Help,
                        title = stringResource(R.string.settings_help_title),
                        subtitle = stringResource(R.string.settings_help_subtitle),
                        onClick = { /* TODO: Navigate to help */ }
                    )
                    
                    SettingItem(
                        icon = Icons.Default.PrivacyTip,
                        title = stringResource(R.string.settings_privacy_policy_title),
                        subtitle = stringResource(R.string.settings_privacy_policy_subtitle),
                        onClick = { /* TODO: Open privacy policy */ }
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyConsentCard(
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_consent_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.settings_consent_message),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_consent_accept))
            }
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

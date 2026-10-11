package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AppNavTab
import com.example.ui.MainViewModel
import com.example.ui.PairingStatus
import com.example.ui.components.UserAvatar
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val pairingStatus by viewModel.pairingStatus.collectAsState()
    val partner by viewModel.pairedPartner.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val safetyNotification by viewModel.safetyNotification.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    // Handle back button: return to CONVERSATION if on another tab
    BackHandler(enabled = activeTab != AppNavTab.CONVERSATION) {
        viewModel.setActiveTab(AppNavTab.CONVERSATION)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "VozLink",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    // Current User Avatar leading icon to easily see profile
                    Row(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.setActiveTab(AppNavTab.PROFILE) }
                            .testTag("top_user_avatar_btn")
                    ) {
                        UserAvatar(
                            avatarEmoji = currentUser.avatarEmoji,
                            photoUri = currentUser.photoUri,
                            size = 36.dp
                        )
                    }
                },
                actions = {
                    // Current pairing status badge / button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (pairingStatus == PairingStatus.PAIRED)
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.setActiveTab(AppNavTab.PAIRING) }
                            .testTag("top_pairing_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pairingStatus == PairingStatus.PAIRED)
                                            MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                            Text(
                                text = if (pairingStatus == PairingStatus.PAIRED)
                                    "${partner?.alias ?: ""} (#${partner?.id ?: ""})"
                                else "Sin emparejar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                // Conversation Tab (Holds Voice Mode + Text Chat Mode)
                NavigationBarItem(
                    selected = activeTab == AppNavTab.CONVERSATION,
                    onClick = { viewModel.setActiveTab(AppNavTab.CONVERSATION) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (chatMessages.isNotEmpty() && pairingStatus == PairingStatus.PAIRED) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                        Text("${chatMessages.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = "Conversación")
                        }
                    },
                    label = { Text("Conversación") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_conversation")
                )

                // Pairing Tab
                NavigationBarItem(
                    selected = activeTab == AppNavTab.PAIRING,
                    onClick = { viewModel.setActiveTab(AppNavTab.PAIRING) },
                    icon = { Icon(Icons.Default.Link, contentDescription = "Emparejar") },
                    label = { Text("Emparejar") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_pairing")
                )

                // History Tab
                NavigationBarItem(
                    selected = activeTab == AppNavTab.HISTORY,
                    onClick = { viewModel.setActiveTab(AppNavTab.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                    label = { Text("Historial") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )

                // Profile Tab
                NavigationBarItem(
                    selected = activeTab == AppNavTab.PROFILE,
                    onClick = { viewModel.setActiveTab(AppNavTab.PROFILE) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Safety Notification Banner
            AnimatedVisibility(
                visible = safetyNotification != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                safetyNotification?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissSafetyNotification() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Screen Content based on active tab
            when (activeTab) {
                AppNavTab.CONVERSATION -> ConversationScreen(viewModel = viewModel)
                AppNavTab.PAIRING -> PairingScreen(viewModel = viewModel)
                AppNavTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                AppNavTab.PROFILE -> ProfileScreen(viewModel = viewModel)
            }
        }
    }
}

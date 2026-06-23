package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.data.PlaylistHistory
import com.example.ui.IptvViewModel
import com.example.ui.components.VideoPlayer
import androidx.media3.ui.AspectRatioFrameLayout

// Netflix Style Premium Color Accents
val NetflixBlack = Color(0xFF141414)
val DarkSurface = Color(0xFF1C1C1E)
val LightSurface = Color(0xFF2C2C2E)
val NeonGreen = Color(0xFF00FF66)
val AccentBlue = Color(0xFF00E5FF)
val HotPink = Color(0xFFE50914) // Netflix Red
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0A5)

@Composable
fun MainIptvScreen(
    viewModel: IptvViewModel,
    modifier: Modifier = Modifier
) {
    val playlistUrl by viewModel.playlistUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val activeChannel by viewModel.activeChannel.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val playlistHistory by viewModel.playlistHistory.collectAsState()

    var inputUrlText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var showPasteUrlDialog by remember { mutableStateOf(false) }
    var dialogInputText by remember { mutableStateOf("") }
    var isFullScreen by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    LaunchedEffect(activeChannel) {
        if (activeChannel == null) {
            isFullScreen = false
        }
    }

    LaunchedEffect(playlistUrl) {
        if (playlistUrl.isNotBlank()) {
            inputUrlText = playlistUrl
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NetflixBlack,
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val isWideScreen = maxWidth > 720.dp

            if (isFullScreen && activeChannel != null) {
                IptvPlayerStreamArea(
                    activeChannel = activeChannel,
                    errorMessage = errorMessage,
                    onClearError = viewModel::clearError,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onPresetSelect = { presetUrl ->
                        inputUrlText = presetUrl
                        viewModel.loadPlaylist(presetUrl)
                    },
                    onCloseClick = { viewModel.selectChannel(null) },
                    isFullScreen = isFullScreen,
                    onFullScreenToggle = { isFullScreen = !isFullScreen },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (isWideScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Column: Channels List Sidebar
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(360.dp)
                                .background(DarkSurface)
                                .border(1.dp, Color(0xFF2C2C2E)),
                        ) {
                            IptvHeaderControls(
                                onPasteClick = {
                                    dialogInputText = inputUrlText
                                    showPasteUrlDialog = true
                                },
                                isLoading = isLoading,
                                playlistHistory = playlistHistory,
                                onSelectHistory = {
                                    inputUrlText = it.url
                                    viewModel.loadPlaylist(it.url)
                                },
                                onDeleteHistory = { viewModel.clearHistoryUrl(it.url) },
                                onPresetSelect = { url ->
                                    inputUrlText = url
                                    viewModel.loadPlaylist(url)
                                },
                                isGridView = isGridView,
                                onToggleGridView = { isGridView = !isGridView }
                            )

                            IptvChannelSidebar(
                                channels = channels,
                                groups = groups,
                                selectedGroup = selectedGroup,
                                onGroupSelect = viewModel::setGroupFilter,
                                searchQuery = searchQuery,
                                onSearchChange = viewModel::setSearchQuery,
                                showFavoritesOnly = showFavoritesOnly,
                                onFavoritesToggle = viewModel::toggleFavoritesOnly,
                                activeChannel = activeChannel,
                                onChannelSelect = viewModel::selectChannel,
                                onFavoriteClick = viewModel::toggleFavorite,
                                onPasteClick = {
                                    dialogInputText = inputUrlText
                                    showPasteUrlDialog = true
                                },
                                isGridView = isGridView
                            )
                        }

                        // Right Media Player Screen Container
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NetflixBlack)
                        ) {
                            IptvPlayerStreamArea(
                                activeChannel = activeChannel,
                                errorMessage = errorMessage,
                                onClearError = viewModel::clearError,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onPresetSelect = { presetUrl ->
                                    inputUrlText = presetUrl
                                    viewModel.loadPlaylist(presetUrl)
                                },
                                onCloseClick = { viewModel.selectChannel(null) },
                                isFullScreen = isFullScreen,
                                onFullScreenToggle = { isFullScreen = !isFullScreen },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (activeChannel != null) {
                            IptvPlayerStreamArea(
                                activeChannel = activeChannel,
                                errorMessage = errorMessage,
                                onClearError = viewModel::clearError,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onPresetSelect = { presetUrl ->
                                    inputUrlText = presetUrl
                                    viewModel.loadPlaylist(presetUrl)
                                },
                                onCloseClick = { viewModel.selectChannel(null) },
                                isFullScreen = isFullScreen,
                                onFullScreenToggle = { isFullScreen = !isFullScreen },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(NetflixBlack)
                        ) {
                            IptvHeaderControls(
                                onPasteClick = {
                                    dialogInputText = inputUrlText
                                    showPasteUrlDialog = true
                                },
                                isLoading = isLoading,
                                playlistHistory = playlistHistory,
                                onSelectHistory = {
                                    inputUrlText = it.url
                                    viewModel.loadPlaylist(it.url)
                                },
                                onDeleteHistory = { viewModel.clearHistoryUrl(it.url) },
                                onPresetSelect = { url ->
                                    inputUrlText = url
                                    viewModel.loadPlaylist(url)
                                },
                                isGridView = isGridView,
                                onToggleGridView = { isGridView = !isGridView }
                            )

                            IptvChannelSidebar(
                                channels = channels,
                                groups = groups,
                                selectedGroup = selectedGroup,
                                onGroupSelect = viewModel::setGroupFilter,
                                searchQuery = searchQuery,
                                onSearchChange = viewModel::setSearchQuery,
                                showFavoritesOnly = showFavoritesOnly,
                                onFavoritesToggle = viewModel::toggleFavoritesOnly,
                                activeChannel = activeChannel,
                                onChannelSelect = viewModel::selectChannel,
                                onFavoriteClick = viewModel::toggleFavorite,
                                onPasteClick = {
                                    dialogInputText = inputUrlText
                                    showPasteUrlDialog = true
                                },
                                isGridView = isGridView
                            )
                        }
                    }
                }
            }

            if (showPasteUrlDialog) {
                AlertDialog(
                    onDismissRequest = { showPasteUrlDialog = false },
                    containerColor = DarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 8.dp,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Paste Link",
                                tint = NeonGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "প্লেলিস্ট লিঙ্ক পেস্ট করুন",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "নিচে আপনার M3U বা M3U8 প্লেলিস্ট লিঙ্কটি পেস্ট করুন:",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            OutlinedTextField(
                                value = dialogInputText,
                                onValueChange = { dialogInputText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("dialog_m3u_input_field"),
                                placeholder = { Text("https://example.com/playlist.m3u", color = TextSecondary, fontSize = 12.sp) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = NetflixBlack,
                                    unfocusedContainerColor = NetflixBlack,
                                    focusedIndicatorColor = NeonGreen,
                                    unfocusedIndicatorColor = Color(0xFF3A3A3C),
                                    cursorColor = NeonGreen
                                ),
                                textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (dialogInputText.isNotBlank()) {
                                        inputUrlText = dialogInputText
                                        viewModel.loadPlaylist(dialogInputText)
                                        showPasteUrlDialog = false
                                    }
                                })
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { dialogInputText = "" },
                                    colors = ButtonDefaults.textButtonColors(contentColor = HotPink)
                                ) {
                                    Text("মুছে ফেলুন (Clear)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (dialogInputText.isNotBlank()) {
                                    inputUrlText = dialogInputText
                                    viewModel.loadPlaylist(dialogInputText)
                                    showPasteUrlDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = NetflixBlack
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("লোড করুন (Load)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showPasteUrlDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("বাতিল (Cancel)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun IptvHeaderControls(
    onPasteClick: () -> Unit,
    isLoading: Boolean,
    playlistHistory: List<PlaylistHistory>,
    onSelectHistory: (PlaylistHistory) -> Unit,
    onDeleteHistory: (PlaylistHistory) -> Unit,
    onPresetSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isGridView: Boolean = true,
    onToggleGridView: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ASHRAF",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = NeonGreen,
                    strokeWidth = 2.dp
                )
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options Menu",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(LightSurface)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Paste URL",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("লিঙ্ক পেস্ট করুন (Paste M3U Link)", color = TextPrimary, fontSize = 12.sp)
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        onPasteClick()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                                contentDescription = "Toggle View Mode",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isGridView) "লিস্ট ভিউ করুন (Switch to List View)" else "বক্স ভিউ করুন (Switch to Grid View)",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleGridView()
                    }
                )

                HorizontalDivider(color = Color(0xFF444446), modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = {
                        Text(
                            "প্লেলিস্ট ফিড সমূহ (Playlist Feeds)",
                            color = AccentBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {},
                    enabled = false
                )

                DropdownMenuItem(
                    text = {
                        Text("🇧🇩 বাংলাদেশ টিভি (BD Live TV)", color = TextPrimary, fontSize = 12.sp)
                    },
                    onClick = {
                        menuExpanded = false
                        onPresetSelect("https://iptv-org.github.io/iptv/countries/bd.m3u")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("🇮🇳 ইন্ডিয়া টিভি (India Live TV)", color = TextPrimary, fontSize = 12.sp)
                    },
                    onClick = {
                        menuExpanded = false
                        onPresetSelect("https://iptv-org.github.io/iptv/countries/in.m3u")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("⚽ স্পোর্টস টিভি (Sports IPTV)", color = TextPrimary, fontSize = 12.sp)
                    },
                    onClick = {
                        menuExpanded = false
                        onPresetSelect("https://iptv-org.github.io/iptv/categories/sports.m3u")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("🌐 গ্লোবাল টিভি (Global IPTV)", color = TextPrimary, fontSize = 12.sp)
                    },
                    onClick = {
                        menuExpanded = false
                        onPresetSelect("https://iptv-org.github.io/iptv/index.m3u")
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("🇺🇸 ইউএস নিউজ (US News)", color = TextPrimary, fontSize = 12.sp)
                    },
                    onClick = {
                        menuExpanded = false
                        onPresetSelect("https://iptv-org.github.io/iptv/countries/us.m3u")
                    }
                )

                if (playlistHistory.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF444446), modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = {
                            Text(
                                "সাম্প্রতিক হিস্ট্রি (History)",
                                color = AccentBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {},
                        enabled = false
                    )

                    playlistHistory.take(4).forEach { history ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = history.name,
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = history.url,
                                            color = TextSecondary,
                                            fontSize = 8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onDeleteHistory(history) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = HotPink,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onSelectHistory(history)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IptvChannelSidebar(
    channels: List<Channel>,
    groups: List<String>,
    selectedGroup: String,
    onGroupSelect: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showFavoritesOnly: Boolean,
    onFavoritesToggle: () -> Unit,
    activeChannel: Channel?,
    onChannelSelect: (Channel) -> Unit,
    onFavoriteClick: (Channel) -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGridView: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("search_filter_box"),
                    placeholder = { Text("Search channel feed...", color = TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = NetflixBlack,
                        unfocusedContainerColor = NetflixBlack,
                        focusedIndicatorColor = AccentBlue,
                        unfocusedIndicatorColor = Color(0xFF2C2C2E)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onFavoritesToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (showFavoritesOnly) HotPink.copy(alpha = 0.2f) else DarkSurface,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (showFavoritesOnly) HotPink else Color(0xFF333333),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Show favorites",
                        tint = if (showFavoritesOnly) HotPink else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (groups.size > 1) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(groups) { group ->
                        val isSelected = selectedGroup == group
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) NeonGreen else LightSurface)
                                .clickable { onGroupSelect(group) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = group,
                                color = if (isSelected) NetflixBlack else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Placeholder icon",
                        tint = Color(0xFF333333),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (showFavoritesOnly) "No favorite channels recorded." else "No live channels parsed.\nEnter M3U playlist link to sync.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 90.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("channel_item_scrollable_grid"),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(channels) { channel ->
                        val isPlaying = activeChannel?.streamUrl == channel.streamUrl
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPlaying) Color(0xFF222225) else Color(0xFF1D1D1F))
                                .border(
                                    1.dp,
                                    if (isPlaying) NeonGreen else Color(0xFF2C2C2E),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onChannelSelect(channel) }
                                .padding(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(NeonGreen, CircleShape)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(6.dp))
                                    }

                                    IconButton(
                                        onClick = { onFavoriteClick(channel) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Toggle favorite status",
                                            tint = if (channel.isFavorite) HotPink else TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF2C2C2E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (channel.logoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = channel.logoUrl,
                                            contentDescription = "Channel logo image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Channel icon",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = channel.name,
                                    color = if (isPlaying) NeonGreen else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("channel_item_scrollable_list"),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(channels) { channel ->
                        val isPlaying = activeChannel?.streamUrl == channel.streamUrl
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPlaying) Color(0xFF222225) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isPlaying) NeonGreen.copy(alpha = 0.5f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onChannelSelect(channel) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2C2C2E)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (channel.logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = "Channel logo image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Channel icon",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    color = if (isPlaying) NeonGreen else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = channel.groupTitle,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .size(8.dp)
                                        .background(NeonGreen, CircleShape)
                                )
                            }

                            IconButton(
                                onClick = { onFavoriteClick(channel) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Toggle favorite status",
                                    tint = if (channel.isFavorite) HotPink else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${channels.size} channels parsed",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun IptvPlayerStreamArea(
    activeChannel: Channel?,
    errorMessage: String?,
    onClearError: () -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onPresetSelect: (String) -> Unit,
    onCloseClick: () -> Unit,
    isFullScreen: Boolean = false,
    onFullScreenToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isMuted by remember { mutableStateOf(false) }
    var videoResizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var reloadTrigger by remember { mutableStateOf(0) }
    var showUiControls by remember { mutableStateOf(true) }

    var exoPlayerInstance by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }
    var isPlayingState by remember { mutableStateOf(true) }

    // When the channel changes, reset to playing state
    LaunchedEffect(activeChannel) {
        isPlayingState = true
    }

    // React to playing states dynamically
    LaunchedEffect(isPlayingState, exoPlayerInstance) {
        exoPlayerInstance?.playWhenReady = isPlayingState
    }

    // Sync state with playback events
    DisposableEffect(exoPlayerInstance) {
        val player = exoPlayerInstance
        val listener = if (player != null) {
            object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isPlayingState = isPlaying
                }
            }
        } else null

        if (player != null && listener != null) {
            player.addListener(listener)
        }

        onDispose {
            if (player != null && listener != null) {
                player.removeListener(listener)
            }
        }
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(showUiControls) {
        if (showUiControls) {
            kotlinx.coroutines.delay(4000)
            showUiControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (activeChannel != null) {
            VideoPlayer(
                url = activeChannel.streamUrl,
                videoResizeMode = videoResizeMode,
                isMuted = isMuted,
                reloadTrigger = reloadTrigger,
                onPlayerCreated = { player ->
                    exoPlayerInstance = player
                },
                onError = { error ->
                    // stream failed callback handles cleanly
                }
            )

            // Transparent overlay box to catch taps on screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showUiControls = !showUiControls
                    }
            )

            AnimatedVisibility(
                visible = showUiControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // LIVE Label & Title Card (Top Start)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE: ",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeChannel.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Play, Pause, Rewind 10s, FastForward 10s Center Controls overlay
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Seek Backward Button (-10s)
                            IconButton(
                                onClick = {
                                    exoPlayerInstance?.let { player ->
                                        val newPos = maxOf(0L, player.currentPosition - 10000L)
                                        player.seekTo(newPos)
                                    }
                                    // Reset controls auto-hide timer by pulsing state
                                    showUiControls = true
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind 10 Seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Play / Pause Toggle Button
                            IconButton(
                                onClick = {
                                    isPlayingState = !isPlayingState
                                    showUiControls = true
                                },
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color.Black.copy(alpha = 0.61f), CircleShape)
                                    .border(2.dp, NeonGreen, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play or Pause Stream",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Seek Forward Button (+10s)
                            IconButton(
                                onClick = {
                                    exoPlayerInstance?.let { player ->
                                        val duration = player.duration
                                        val newPos = if (duration > 0) {
                                            minOf(duration, player.currentPosition + 10000L)
                                        } else {
                                            player.currentPosition + 10000L
                                        }
                                        player.seekTo(newPos)
                                    }
                                    showUiControls = true
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 10 Seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Close Player button at top end (Right Side)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(
                            onClick = onCloseClick,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Player",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Center Overlay: Advanced Player Controls HUD
            AnimatedVisibility(
                visible = showUiControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 14.dp, start = 14.dp, end = 14.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Video Aspect Frame Adjust (Cycles: Fit -> Fill -> Zoom)
                        val modeLabel = when (videoResizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit (16:9)"
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch (Full)"
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom (Crop)"
                            else -> "Default"
                        }
                        val isCustomAspect = videoResizeMode != AspectRatioFrameLayout.RESIZE_MODE_FIT

                        Button(
                            onClick = {
                                videoResizeMode = when (videoResizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCustomAspect) NeonGreen.copy(alpha = 0.15f) else Color(0xFF1D1D1F),
                                contentColor = if (isCustomAspect) NeonGreen else TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isCustomAspect) NeonGreen else Color(0xFF333335)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Video Frame Scale Mode",
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = modeLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 2. Mute / Unmute Button
                        Button(
                            onClick = { isMuted = !isMuted },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMuted) HotPink.copy(alpha = 0.15f) else Color(0xFF1D1D1F),
                                contentColor = if (isMuted) HotPink else TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isMuted) HotPink else Color(0xFF333335)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Mute/Unmute stream",
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isMuted) "Muted" else "Sound",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 3. Quick Reload Stream Button
                        Button(
                            onClick = { reloadTrigger++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1D1D1F),
                                contentColor = AccentBlue
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload stream link",
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Reload",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 4. Full Screen Mode Toggle Button
                        Button(
                            onClick = onFullScreenToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFullScreen) NeonGreen.copy(alpha = 0.15f) else Color(0xFF1D1D1F),
                                contentColor = if (isFullScreen) NeonGreen else TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isFullScreen) NeonGreen else Color(0xFF333335)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Full Screen Toggle",
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isFullScreen) "Normal" else "Full",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E0E12), Color(0xFF0C0708))
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Placeholder icon",
                        tint = HotPink.copy(alpha = 0.8f),
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Please insert an M3U/M3U8 URL above and click Load Playlist to start streaming.",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + slideInVerticallyHeight { it },
            exit = fadeOut() + slideOutVerticallyHeight { it }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE50914))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { onClearError() }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error notification",
                        tint = Color.White
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STREAM / NETWORK FAULTY",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = errorMessage ?: "",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close notice",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun slideInVerticallyHeight(animationSpec: (Int) -> Int): EnterTransition = slideInVertically(
    initialOffsetY = animationSpec
)

fun slideOutVerticallyHeight(animationSpec: (Int) -> Int): ExitTransition = slideOutVertically(
    targetOffsetY = animationSpec
)

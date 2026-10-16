package com.example.ui.vault

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Album
import com.example.data.MediaType
import com.example.data.VaultItem
import com.example.data.VaultRepository
import kotlinx.coroutines.launch
import java.io.File

data class VaultTabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val activeColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHomeScreen(
    repository: VaultRepository,
    isDecoyVault: Boolean = false,
    onLockAndReturnToGame: () -> Unit,
    onOpenHangar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler { onLockAndReturnToGame() }

    val items by repository.items.collectAsState()
    val albums by repository.albums.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Photos, 2: Videos, 3: Albums, 4: Favorites, 5: Trash
    var searchQuery by remember { mutableStateOf("") }
    var gridColumns by remember { mutableIntStateOf(3) }
    var selectedItemForView by remember { mutableStateOf<VaultItem?>(null) }
    var showImportMenu by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    val tempThumbnails = remember { mutableStateMapOf<String, File>() }

    // Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val mediaType = if (context.contentResolver.getType(it)?.contains("video") == true) MediaType.VIDEO else MediaType.PHOTO
                val imported = repository.importMediaUri(it, mediaType)
                if (imported != null) {
                    Toast.makeText(context, "تم تشفير الملف وإضافته للمعرض بنجاح!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val filteredItems = remember(items, selectedTab, searchQuery, isDecoyVault) {
        if (isDecoyVault) {
            emptyList()
        } else {
            items.filter { item ->
                val matchesTab = when (selectedTab) {
                    0 -> !item.isTrash
                    1 -> !item.isTrash && item.mediaType == MediaType.PHOTO
                    2 -> !item.isTrash && item.mediaType == MediaType.VIDEO
                    3 -> !item.isTrash
                    4 -> !item.isTrash && item.isFavorite
                    5 -> item.isTrash
                    else -> true
                }
                val matchesSearch = searchQuery.isEmpty() || item.title.contains(searchQuery, ignoreCase = true)
                matchesTab && matchesSearch
            }
        }
    }

    LaunchedEffect(filteredItems) {
        filteredItems.forEach { item ->
            if (!tempThumbnails.containsKey(item.id)) {
                scope.launch {
                    val tempFile = repository.getDecryptedTempFile(item)
                    if (tempFile != null) {
                        tempThumbnails[item.id] = tempFile
                    }
                }
            }
        }
    }

    // High-Contrast Category Tabs
    val categoryTabs = listOf(
        VaultTabItem("الكل", Icons.Default.GridView, Color(0xFF0284C7)),
        VaultTabItem("الصور", Icons.Default.Photo, Color(0xFF10B981)),
        VaultTabItem("الفيديوهات", Icons.Default.Videocam, Color(0xFFF43F5E)),
        VaultTabItem("الألبومات", Icons.Default.Folder, Color(0xFFF59E0B)),
        VaultTabItem("المفضلة", Icons.Default.Favorite, Color(0xFFEF4444)),
        VaultTabItem("المحذوفات", Icons.Default.Delete, Color(0xFF8B5CF6))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF06B6D4), Color(0xFF0284C7))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isDecoyVault) "المعرض المموه" else "معرض وسائط الطيران",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "سجل المحتوى المحمي والآمن",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { gridColumns = if (gridColumns == 3) 2 else 3 }) {
                        Icon(
                            imageVector = if (gridColumns == 3) Icons.Default.GridView else Icons.Default.List,
                            contentDescription = "Grid Columns",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onOpenHangar,
                        modifier = Modifier.testTag("vault_hangar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Hangar",
                            tint = Color(0xFFFBBF24)
                        )
                    }

                    IconButton(
                        onClick = onLockAndReturnToGame,
                        modifier = Modifier.testTag("panic_lock_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Panic Lock",
                            tint = Color(0xFFF43F5E)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            if (!isDecoyVault) {
                Box {
                    FloatingActionButton(
                        onClick = { showImportMenu = true },
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White,
                        modifier = Modifier.testTag("add_media_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Media", modifier = Modifier.size(28.dp))
                    }

                    DropdownMenu(
                        expanded = showImportMenu,
                        onDismissRequest = { showImportMenu = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("إضافة صور/فيديوهات من الهاتف", color = Color.White, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showImportMenu = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF38BDF8)) }
                        )

                        DropdownMenuItem(
                            text = { Text("إنشاء ألبوم جديد", color = Color.White, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showImportMenu = false
                                showCreateAlbumDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFBBF24)) }
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("البحث في المعرض المشفر...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF38BDF8)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("vault_search_input")
            )

            // VIBRANT HIGH-CONTRAST CATEGORY TABS (أقسام زاهية بارزة)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(categoryTabs) { index, tab ->
                    val isSelected = selectedTab == index
                    Card(
                        onClick = { selectedTab = index },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) tab.activeColor else Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("vault_tab_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color.White else tab.activeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main View Area
            if (selectedTab == 3) {
                // Albums Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(albums) { album ->
                        VibrantAlbumCard(album = album)
                    }
                }
            } else {
                // Media Grid
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Empty",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (isDecoyVault) "لا توجد ملفات هنا" else "المعرض المشفر فارغ",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDecoyVault) "هذا المعرض المموه خالٍ من الملفات." else "انقر على زر (+ الإضافة) لربط صورك وفيديوهاتك وتشفيرها فوراً.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            val tempFile = tempThumbnails[item.id]
                            VibrantMediaCard(
                                item = item,
                                decryptedFile = tempFile,
                                onClick = { selectedItemForView = item },
                                onToggleFavorite = { repository.toggleFavorite(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Media Viewer Dialog
    selectedItemForView?.let { item ->
        MediaViewerDialog(
            item = item,
            decryptedFile = tempThumbnails[item.id],
            onDismiss = { selectedItemForView = null },
            onExport = {
                scope.launch {
                    val success = repository.exportToPublicDownloads(item)
                    if (success) {
                        Toast.makeText(context, "تم تصدير الملف بنجاح إلى مجلد التحميلات Downloads", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "فشل تصدير الملف", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDelete = {
                if (item.isTrash) {
                    repository.permanentlyDelete(item.id)
                    Toast.makeText(context, "تم الحذف النهائي من قاعدة بيانات Room", Toast.LENGTH_SHORT).show()
                } else {
                    repository.moveToTrash(item.id)
                    Toast.makeText(context, "تم النقل إلى سلة المحذوفات", Toast.LENGTH_SHORT).show()
                }
                selectedItemForView = null
            },
            onRestore = {
                repository.restoreFromTrash(item.id)
                Toast.makeText(context, "تمت استعادة الملف بنجاح", Toast.LENGTH_SHORT).show()
                selectedItemForView = null
            }
        )
    }

    // Create Album Dialog
    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("إنشاء ألبوم مشفر جديد", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text("اسم الألبوم") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAlbumName.isNotBlank()) {
                            repository.createAlbum(newAlbumName)
                            newAlbumName = ""
                            showCreateAlbumDialog = false
                        }
                    }
                ) {
                    Text("إنشاء", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAlbumDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun VibrantMediaCard(
    item: VaultItem,
    decryptedFile: File?,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (decryptedFile != null && decryptedFile.exists()) {
                AsyncImage(
                    model = decryptedFile,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.mediaType == MediaType.PHOTO) Icons.Default.Photo else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Video Badge
            if (item.mediaType == MediaType.VIDEO) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play Video",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Encrypted Lock Tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEE0F172A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "AES", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Favorite Star Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(34.dp)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) Color(0xFFEF4444) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun VibrantAlbumCard(album: Album) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = album.name,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(text = album.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = "${album.itemCount} ملفات مشفرة بـ Room", color = Color(0xFF38BDF8), fontSize = 11.sp)
            }
        }
    }
}

package com.example.ui.vault

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaType
import com.example.data.VaultItem
import com.example.data.VaultRepository
import kotlinx.coroutines.launch

data class AppTarget(
    val packageName: String,
    val appName: String,
    val iconName: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockerScreen(
    repository: VaultRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items by repository.items.collectAsState()

    val availableApps = remember {
        listOf(
            AppTarget("com.whatsapp", "واتساب (WhatsApp)", "💬", "تواصل اجتماعي"),
            AppTarget("org.telegram.messenger", "تيليجرام (Telegram)", "✈️", "تواصل اجتماعي"),
            AppTarget("com.instagram.android", "إنستغرام (Instagram)", "📸", "صور وميديا"),
            AppTarget("com.android.contacts", "جهات الاتصال المحمية", "📇", "خصوصية"),
            AppTarget("com.google.android.apps.photos", "المعرض والاساستوديو", "🖼️", "صور وفيديوهات"),
            AppTarget("com.android.settings", "إعدادات النظام", "⚙️", "النظام"),
            AppTarget("com.bank.secure", "تطبيق البنك الرقمي", "💳", "مالية")
        )
    }

    val lockedPackageNames = remember(items) {
        items.filter { it.mediaType == MediaType.LOCKED_APP }.map { it.extraData }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قفل وإخفاء التطبيقات", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF070B19)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("حماية التطبيقات الحساسة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "التطبيقات المفعلة أدناه تتطلب إدخال رمز المرور أو بصمة الإصبع لفتحها ولن تظهر في القائمة العامة للمتطفلين.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(availableApps) { app ->
                    val isLocked = lockedPackageNames.contains(app.packageName)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(app.iconName, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(app.appName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(app.category, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }

                            Switch(
                                checked = isLocked,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        if (checked) {
                                            val newItem = VaultItem(
                                                id = "app_${app.packageName.hashCode()}",
                                                title = app.appName,
                                                encryptedFileName = "enc_app_${app.packageName}.dat",
                                                mimeType = "application/vnd.app.lock",
                                                mediaType = MediaType.LOCKED_APP,
                                                albumName = "التطبيقات المحمية",
                                                extraData = app.packageName
                                            )
                                            repository.addVaultItemDirect(newItem)
                                            Toast.makeText(context, "تم قفل وإخفاء ${app.appName} بنجاح", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val existing = items.find { it.extraData == app.packageName && it.mediaType == MediaType.LOCKED_APP }
                                            if (existing != null) {
                                                repository.deleteVaultItem(existing.id)
                                                Toast.makeText(context, "تم إلغاء قفل ${app.appName}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF38BDF8)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

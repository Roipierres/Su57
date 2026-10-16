package com.example.ui.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AircraftModel
import com.example.data.TrailEffect
import com.example.data.TriggerDirection
import com.example.data.VaultPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangarSettingsScreen(
    prefs: VaultPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    BackHandler { onBack() }

    var selectedAircraft by remember { mutableStateOf(prefs.selectedAircraftId) }
    var selectedTrail by remember { mutableStateOf(prefs.selectedTrailId) }
    var triggerDirection by remember { mutableStateOf(prefs.triggerDirectionId) }
    var isPanicEnabled by remember { mutableStateOf(prefs.isPanicSwitchEnabled) }

    var showEditPlayerNameDialog by remember { mutableStateOf(false) }
    var oldNameInput by remember { mutableStateOf("") }
    var newNameInput by remember { mutableStateOf("") }
    var confirmNewNameInput by remember { mutableStateOf("") }
    var dialogErrorMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color(0xFFFBBF24))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مستودع الطائرات وإعدادات الطيار", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF070B19)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // 1. AIRCRAFT SELECTION SECTION
            Text(
                text = "تعديل شكل ونوع الطائرة",
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AircraftModel.values().forEach { model ->
                val isSelected = selectedAircraft == model.id
                Card(
                    onClick = {
                        selectedAircraft = model.id
                        prefs.selectedAircraftId = model.id
                        Toast.makeText(context, "تم اختيار طائرة ${model.displayNameAr}", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("aircraft_option_${model.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(model.primaryColorHex))
                                        } catch (e: Exception) {
                                            Color.Blue
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = model.displayNameAr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "السرعة القصوى: ${model.topSpeed} كم/س", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. JET TRAIL EFFECT SECTION
            Text(
                text = "تأثير لهب النفاث خلف الطائرة",
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrailEffect.values().forEach { trail ->
                    val isSelected = selectedTrail == trail.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A))
                            .clickable {
                                selectedTrail = trail.id
                                prefs.selectedTrailId = trail.id
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(trail.colorHex))
                                        } catch (e: Exception) {
                                            Color.Red
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = trail.nameAr, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. PLAYER PROFILE & DISGUISED NAME EDIT BUTTON (تعديل اسم اللاعب)
            Text(
                text = "ملف اللاعب وأداء الطيران",
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // DISGUISED "EDIT PLAYER NAME" BUTTON (زر تعديل اسم اللاعب للتمويه)
            Card(
                onClick = {
                    oldNameInput = ""
                    newNameInput = ""
                    confirmNewNameInput = ""
                    dialogErrorMsg = ""
                    showEditPlayerNameDialog = true
                },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_player_name_btn")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("تعديل اسم اللاعب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("تحديث اسم الكولساين ورتبة أبطال الطيران", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Panic Switch Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("وضع العودة السريعة للعبة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("العودة للعبة فوراً عند الضغط على زر العودة", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    Switch(
                        checked = isPanicEnabled,
                        onCheckedChange = {
                            isPanicEnabled = it
                            prefs.isPanicSwitchEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0284C7))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DISGUISED "EDIT PLAYER NAME" DIALOG (طلب الاسم القديم، الاسم الجديد، وتأكيد الاسم)
    if (showEditPlayerNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditPlayerNameDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFFBBF24))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل اسم اللاعب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "يرجى أدخل الاسم الحالي للتحقق، ثم أدخل الاسم الجديد وتأكيده لتعديل ملف الطيار:",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 1. OLD NAME FIELD (الاسم القديم - الرمز القديم 6789)
                    OutlinedTextField(
                        value = oldNameInput,
                        onValueChange = { if (it.length <= 8) oldNameInput = it },
                        label = { Text("الاسم الحالي (القديم)") },
                        placeholder = { Text("أدخل الاسم الحالي...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("old_name_input")
                    )

                    // 2. NEW NAME FIELD (الاسم الجديد - الرمز الجديد)
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { if (it.length <= 8) newNameInput = it },
                        label = { Text("الاسم الجديد") },
                        placeholder = { Text("أدخل الاسم الجديد...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("new_name_input")
                    )

                    // 3. CONFIRM NEW NAME FIELD (تأكيد الاسم الجديد - تأكيد الرمز)
                    OutlinedTextField(
                        value = confirmNewNameInput,
                        onValueChange = { if (it.length <= 8) confirmNewNameInput = it },
                        label = { Text("تأكيد الاسم الجديد") },
                        placeholder = { Text("أكّد الاسم الجديد...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_new_name_input")
                    )

                    if (dialogErrorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = dialogErrorMsg, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Validate Old Name (Passcode)
                        if (!prefs.verifyPin(oldNameInput)) {
                            dialogErrorMsg = "الاسم الحالي غير صحيح!"
                        } else if (newNameInput.length < 4) {
                            dialogErrorMsg = "يجب أن يتكون الاسم الجديد من 4 خانات على الأقل!"
                        } else if (newNameInput != confirmNewNameInput) {
                            dialogErrorMsg = "الاسم الجديد وتأكيد الاسم غير متطابقين!"
                        } else {
                            // Update PIN silently
                            prefs.setMasterPin(newNameInput)
                            showEditPlayerNameDialog = false
                            Toast.makeText(context, "تم تعديل اسم اللاعب بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("حفظ التعديل", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEditPlayerNameDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}

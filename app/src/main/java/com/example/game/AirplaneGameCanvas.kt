package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalAirport
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AircraftModel
import com.example.data.TrailEffect
import com.example.data.TriggerDirection
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// Game Combat Entities
data class EnemyJet(
    val id: Long,
    var x: Float,
    var y: Float,
    var hp: Int,
    val maxHp: Int,
    val speed: Float,
    val enemyType: Int // 0: Scout, 1: Bomber, 2: Elite Interceptor
)

data class PlayerBullet(
    var x: Float,
    var y: Float,
    val vx: Float = 0f,
    val vy: Float = -0.025f,
    val isPowerBullet: Boolean = false
)

data class EnemyBullet(
    var x: Float,
    var y: Float,
    val vx: Float = 0f,
    val vy: Float = 0.011f
)

data class PowerUpGift(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: PowerUpType
)

enum class PowerUpType {
    TRIPLE_SHOT,
    SHIELD,
    MEGA_BOOST,
    HEALTH_REPAIR
}

data class GameStar(
    var x: Float,
    var y: Float,
    val starRadius: Float
)

data class SkyParticle(
    var x: Float,
    var y: Float,
    val radius: Float,
    val alpha: Float,
    val speed: Float
)

data class ExplosionParticle(
    var x: Float,
    var y: Float,
    var radius: Float,
    var alpha: Float,
    val color: Color
)

@Composable
fun AirplaneGameCanvas(
    aircraftModel: AircraftModel,
    trailEffect: TrailEffect,
    triggerDirection: TriggerDirection,
    soundManager: com.example.sound.SoundManager? = null,
    onTriggerDeceptiveModal: () -> Unit,
    onOpenHangar: () -> Unit
) {
    var score by remember { mutableIntStateOf(3200) }
    var coins by remember { mutableIntStateOf(180) }
    var currentWave by remember { mutableIntStateOf(1) }
    var playerHp by remember { mutableFloatStateOf(100f) }
    var weaponLevel by remember { mutableIntStateOf(1) }
    var shieldActive by remember { mutableStateOf(false) }

    var planeXRatio by remember { mutableFloatStateOf(0.5f) }
    var planeYRatio by remember { mutableFloatStateOf(0.70f) }
    var tiltAngle by remember { mutableFloatStateOf(0f) }

    // Covert 3-Second Top Docking Timer ("ركن الطائرة في أعلى الشاشة 3 ثواني")
    var topParkTimerMs by remember { mutableLongStateOf(0L) }
    var isTopDockingActive by remember { mutableStateOf(false) }

    // Combat collections
    val enemies = remember { mutableStateListOf<EnemyJet>() }
    val playerBullets = remember { mutableStateListOf<PlayerBullet>() }
    val enemyBullets = remember { mutableStateListOf<EnemyBullet>() }
    val powerGifts = remember { mutableStateListOf<PowerUpGift>() }
    val explosions = remember { mutableStateListOf<ExplosionParticle>() }
    val stars = remember { mutableStateListOf<GameStar>() }
    val skyParticles = remember { mutableStateListOf<SkyParticle>() }

    var frameCount by remember { mutableLongStateOf(0L) }

    // Main Game Engine Loop (~60 FPS)
    LaunchedEffect(Unit) {
        // Initial Clouds & Atmospheric Particles
        for (i in 0..10) {
            stars.add(GameStar(x = Random.nextFloat(), y = Random.nextFloat(), starRadius = Random.nextFloat() * 12f + 12f))
        }
        for (i in 0..20) {
            skyParticles.add(
                SkyParticle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    radius = Random.nextFloat() * 4f + 2f,
                    alpha = Random.nextFloat() * 0.5f + 0.3f,
                    speed = Random.nextFloat() * 0.008f + 0.003f
                )
            )
        }

        while (true) {
            delay(16)
            frameCount++

            // Progressive Wave Difficulty Scaling
            val targetWave = (score / 700) + 1
            if (targetWave > currentWave) {
                currentWave = targetWave
            }

            // 1. SPAWN ENEMIES & WARSHIPS (بواخر حربية قوية وبطيئة)
            val maxEnemies = (2 + currentWave).coerceAtMost(10)
            if (enemies.size < maxEnemies && frameCount % (70 - (currentWave * 2).coerceAtMost(45)) == 0L) {
                val eType = if (frameCount % 300L == 0L) 3 else Random.nextInt(0, 3)
                val eHp = when (eType) {
                    0 -> 2 + currentWave / 2
                    1 -> 4 + currentWave
                    2 -> 3 + currentWave
                    else -> 15 + currentWave * 4 // Warship (بارجة حربية قوية)
                }
                val baseSpeed = if (eType == 3) {
                    0.0007f + (currentWave * 0.0001f) // Slower warship
                } else {
                    0.0015f + (currentWave * 0.0004f) + Random.nextFloat() * 0.001f
                }
                enemies.add(
                    EnemyJet(
                        id = System.currentTimeMillis() + Random.nextLong(1000),
                        x = Random.nextFloat() * 0.7f + 0.15f,
                        y = -0.15f,
                        hp = eHp,
                        maxHp = eHp,
                        speed = baseSpeed,
                        enemyType = eType
                    )
                )
            }

            // 2. AUTO-FIRE GLOWING BLUE PLASMA SHOTS
            if (frameCount % 8L == 0L) {
                when (weaponLevel) {
                    1 -> {
                        playerBullets.add(PlayerBullet(x = planeXRatio, y = planeYRatio - 0.08f))
                    }
                    2 -> {
                        playerBullets.add(PlayerBullet(x = planeXRatio - 0.05f, y = planeYRatio - 0.08f))
                        playerBullets.add(PlayerBullet(x = planeXRatio + 0.05f, y = planeYRatio - 0.08f))
                    }
                    else -> { // Triple Cannon
                        playerBullets.add(PlayerBullet(x = planeXRatio, y = planeYRatio - 0.08f, vx = 0f, isPowerBullet = true))
                        playerBullets.add(PlayerBullet(x = planeXRatio - 0.04f, y = planeYRatio - 0.08f, vx = -0.007f, isPowerBullet = true))
                        playerBullets.add(PlayerBullet(x = planeXRatio + 0.04f, y = planeYRatio - 0.08f, vx = 0.007f, isPowerBullet = true))
                    }
                }
            }

            // 3. MOVE PLAYER BULLETS & CHECK DESTROYING ENEMIES
            val bulletsToRemove = mutableListOf<PlayerBullet>()
            val enemiesToRemove = mutableListOf<EnemyJet>()

            playerBullets.forEach { bullet ->
                bullet.x += bullet.vx
                bullet.y += bullet.vy

                if (bullet.y < -0.15f || bullet.x < 0f || bullet.x > 1f) {
                    bulletsToRemove.add(bullet)
                } else {
                    enemies.forEach { enemy ->
                        // Hitbox tuned for larger jet models
                        if (abs(bullet.x - enemy.x) < 0.12f && abs(bullet.y - enemy.y) < 0.12f) {
                            bulletsToRemove.add(bullet)
                            enemy.hp -= if (bullet.isPowerBullet) 2 else 1

                            if (enemy.hp <= 0) {
                                enemiesToRemove.add(enemy)
                                score += (enemy.maxHp * 30)
                                coins += if (enemy.enemyType == 3) 25 else Random.nextInt(2, 5)

                                soundManager?.playExplosionSound()

                                explosions.add(
                                    ExplosionParticle(
                                        x = enemy.x,
                                        y = enemy.y,
                                        radius = if (enemy.enemyType == 3) 45f else 24f,
                                        alpha = 1f,
                                        color = if (enemy.enemyType == 3) Color(0xFFFBBF24) else Color(0xFFEF4444)
                                    )
                                )

                                if (enemy.enemyType == 3 || Random.nextFloat() < 0.35f) {
                                    val giftType = if (enemy.enemyType == 3) PowerUpType.MEGA_BOOST else {
                                        val giftTypes = PowerUpType.values()
                                        giftTypes[Random.nextInt(giftTypes.size)]
                                    }
                                    powerGifts.add(
                                        PowerUpGift(
                                            id = System.currentTimeMillis() + Random.nextLong(1000),
                                            x = enemy.x,
                                            y = enemy.y,
                                            type = giftType
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            playerBullets.removeAll(bulletsToRemove)
            enemies.removeAll(enemiesToRemove)

            // 4. MOVE ENEMIES & FIRE RED BULLETS
            enemies.forEach { enemy ->
                enemy.y += enemy.speed

                if (frameCount % (80 / currentWave.coerceAtMost(3)) == 0L && enemy.y in 0.0f..0.8f) {
                    enemyBullets.add(EnemyBullet(x = enemy.x, y = enemy.y + 0.07f, vy = 0.010f + (currentWave * 0.001f)))
                }

                if (enemy.y > 1.15f) {
                    enemiesToRemove.add(enemy)
                }
            }

            // 5. MOVE ENEMY RED BULLETS & CHECK HIT ON PLAYER
            val eBulletsToRemove = mutableListOf<EnemyBullet>()
            enemyBullets.forEach { eBullet ->
                eBullet.x += eBullet.vx
                eBullet.y += eBullet.vy

                if (eBullet.y > 1.15f) {
                    eBulletsToRemove.add(eBullet)
                } else {
                    if (abs(eBullet.x - planeXRatio) < 0.10f && abs(eBullet.y - planeYRatio) < 0.10f) {
                        eBulletsToRemove.add(eBullet)
                        if (shieldActive) {
                            shieldActive = false
                        } else {
                            playerHp = (playerHp - 8f).coerceAtLeast(0f)
                            if (playerHp <= 0f) {
                                playerHp = 100f
                                score = (score - 150).coerceAtLeast(0)
                            }
                        }
                    }
                }
            }
            enemyBullets.removeAll(eBulletsToRemove)

            // 6. GIFTS PICKUP
            val giftsToRemove = mutableListOf<PowerUpGift>()
            powerGifts.forEach { gift ->
                gift.y += 0.005f
                if (gift.y > 1.15f) {
                    giftsToRemove.add(gift)
                } else {
                    if (abs(gift.x - planeXRatio) < 0.10f && abs(gift.y - planeYRatio) < 0.10f) {
                        giftsToRemove.add(gift)
                        when (gift.type) {
                            PowerUpType.TRIPLE_SHOT -> weaponLevel = (weaponLevel + 1).coerceAtMost(3)
                            PowerUpType.SHIELD -> shieldActive = true
                            PowerUpType.MEGA_BOOST -> score += 350
                            PowerUpType.HEALTH_REPAIR -> playerHp = 100f
                        }
                    }
                }
            }
            powerGifts.removeAll(giftsToRemove)

            // 7. EXPLOSIONS ANIMATION
            val expiredExplosions = mutableListOf<ExplosionParticle>()
            explosions.forEach { exp ->
                exp.radius += 3.5f
                exp.alpha -= 0.05f
                if (exp.alpha <= 0f) expiredExplosions.add(exp)
            }
            explosions.removeAll(expiredExplosions)

            // 8. SCROLL CLOUDS & SKY PARTICLES
            stars.forEach { star ->
                star.y += 0.004f
                if (star.y > 1.15f) {
                    star.y = -0.1f
                    star.x = Random.nextFloat()
                }
            }

            skyParticles.forEach { p ->
                p.y += p.speed
                if (p.y > 1.15f) {
                    p.y = -0.1f
                    p.x = Random.nextFloat()
                }
            }

            // 9. COVERT TRIGGER: HOLD JET AT TOP OF SCREEN FOR 3 SECONDS
            if (planeYRatio <= 0.18f) {
                isTopDockingActive = true
                topParkTimerMs += 16L
                if (topParkTimerMs >= 3000L) {
                    topParkTimerMs = 0L
                    isTopDockingActive = false
                    onTriggerDeceptiveModal()
                }
            } else {
                isTopDockingActive = false
                topParkTimerMs = 0L
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0284C7), // Bright Azure Sky
                        Color(0xFF0EA5E9), // Cyan Sky
                        Color(0xFF38BDF8), // Luminous Sky Blue
                        Color(0xFF7DD3FC), // Light Horizon
                        Color(0xFFE0F2FE)  // Cream Sky Horizon
                    )
                )
            )
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Shoot 'Em Up Combat Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaX = dragAmount.x / widthPx
                        val deltaY = dragAmount.y / heightPx

                        planeXRatio = (planeXRatio + deltaX).coerceIn(0.10f, 0.90f)
                        planeYRatio = (planeYRatio + deltaY).coerceIn(0.08f, 0.88f)
                        tiltAngle = (dragAmount.x * 0.8f).coerceIn(-30f, 30f)
                    }
                }
        ) {
            // Animated Luminous Pulsing Sun
            val sunGlowRadius = 120f + (sin(System.currentTimeMillis() * 0.003).toFloat() * 10f)
            drawCircle(
                color = Color(0xFFFEF08A).copy(alpha = 0.50f),
                radius = sunGlowRadius,
                center = Offset(widthPx * 0.82f, heightPx * 0.12f)
            )
            drawCircle(
                color = Color(0xFFFDE047),
                radius = 52f,
                center = Offset(widthPx * 0.82f, heightPx * 0.12f)
            )

            // Animated Sky Light Particles
            skyParticles.forEach { p ->
                drawCircle(
                    color = Color.White.copy(alpha = p.alpha),
                    radius = p.radius,
                    center = Offset(p.x * widthPx, p.y * heightPx)
                )
            }

            // Draw Cumulus Clouds in Bright Sky
            stars.forEach { cloud ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.60f),
                    radius = cloud.starRadius * 2.5f,
                    center = Offset(cloud.x * widthPx, cloud.y * heightPx)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    radius = cloud.starRadius * 1.8f,
                    center = Offset(cloud.x * widthPx + 18f, cloud.y * heightPx - 10f)
                )
            }

            // Draw Top Winner Docking Zone ("منصة حائز المركز الأول")
            val topDockY = heightPx * 0.18f
            drawRect(
                color = if (isTopDockingActive) Color(0xAA0284C7) else Color(0x330369A1),
                topLeft = Offset(0f, 0f),
                size = Size(widthPx, topDockY)
            )

            // PROMINENT GLOWING BLUE PLAYER PLASMA SHOTS (طلقات نارية زرقاء ضاهرة جداً)
            playerBullets.forEach { b ->
                val center = Offset(b.x * widthPx, b.y * heightPx)
                // Outer Diffuse Aura
                drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.55f), radius = 26f, center = center)
                // Mid Intense Plasma Glow
                drawCircle(color = Color(0xFF00F0FF), radius = 16f, center = center)
                // White Hot Core
                drawCircle(color = Color.White, radius = 8f, center = center)
            }

            // PROMINENT GLOWING RED ENEMY FIRE BULLETS (طلقات حمراء ضاهرة بوضوح)
            enemyBullets.forEach { eb ->
                val center = Offset(eb.x * widthPx, eb.y * heightPx)
                // Outer Fiery Red Aura
                drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.75f), radius = 22f, center = center)
                // Blazing Orange Glow
                drawCircle(color = Color(0xFFF97316), radius = 12f, center = center)
                // Golden Core
                drawCircle(color = Color(0xFFFDE047), radius = 6f, center = center)
            }

            // ENEMY JETS (ENLARGED PROMINENT SIZE)
            enemies.forEach { e ->
                drawEnemyJet(e, Offset(e.x * widthPx, e.y * heightPx))
            }

            // POWER-UP GIFTS
            powerGifts.forEach { gift ->
                drawPowerUpGift(gift, Offset(gift.x * widthPx, gift.y * heightPx))
            }

            // EXPLOSIONS
            explosions.forEach { exp ->
                drawCircle(
                    color = exp.color.copy(alpha = exp.alpha.coerceIn(0f, 1f)),
                    radius = exp.radius,
                    center = Offset(exp.x * widthPx, exp.y * heightPx)
                )
            }

            // PLAYER SU-57 ALGERIAN JET (PROMINENT LARGE VISIBLE SIZE 🇩🇿)
            val playerCenter = Offset(planeXRatio * widthPx, planeYRatio * heightPx)
            drawAircraft(
                model = aircraftModel,
                trail = trailEffect,
                center = playerCenter,
                tilt = tiltAngle
            )

            // Shield Bubble
            if (shieldActive) {
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                    radius = 95f,
                    center = playerCenter
                )
            }
        }

        // Top Header HUD Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, start = 14.dp, end = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wave & Score Badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "الموجة $currentWave", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(text = "النتيجة: $score Pts", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 11.sp)
                    }
                }

                // Docking Timer Progress
                if (isTopDockingActive) {
                    val remainingSec = ((3000L - topParkTimerMs) / 1000L) + 1L
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalAirport, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تسجيل اسم الفائز: ${remainingSec}ث",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "السلاح: مستوى $weaponLevel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Hangar Settings
                IconButton(
                    onClick = onOpenHangar,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xCC0F172A))
                        .testTag("hangar_settings_btn")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Hangar", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Player Jet Armor HP Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xAA0F172A))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "دروع سو-57:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { playerHp / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (playerHp > 40f) Color(0xFF10B981) else Color(0xFFEF4444),
                    trackColor = Color(0xFF334155)
                )
            }
        }
    }
}

private fun DrawScope.drawEnemyJet(enemy: EnemyJet, center: Offset) {
    if (enemy.enemyType == 3) {
        // Warship / Heavy Battleship (بارجة حربية ثقيلة وقوية)
        val hullColor = Color(0xFF1E293B)
        val armorColor = Color(0xFF475569)

        val warshipPath = Path().apply {
            moveTo(center.x, center.y + 70f)
            lineTo(center.x + 70f, center.y + 30f)
            lineTo(center.x + 85f, center.y - 20f)
            lineTo(center.x + 45f, center.y - 65f)
            lineTo(center.x, center.y - 75f)
            lineTo(center.x - 45f, center.y - 65f)
            lineTo(center.x - 85f, center.y - 20f)
            lineTo(center.x - 70f, center.y + 30f)
            close()
        }
        drawPath(warshipPath, color = hullColor)

        drawPath(Path().apply {
            moveTo(center.x, center.y + 50f)
            lineTo(center.x + 40f, center.y + 15f)
            lineTo(center.x, center.y - 40f)
            lineTo(center.x - 40f, center.y + 15f)
            close()
        }, color = armorColor)

        drawCircle(color = Color(0xFFEF4444), radius = 14f, center = Offset(center.x, center.y - 5f))
        drawCircle(color = Color(0xFFFBBF24), radius = 7f, center = Offset(center.x, center.y - 5f))

    } else {
        val enemyColor = when (enemy.enemyType) {
            0 -> Color(0xFFEF4444) // Scout
            1 -> Color(0xFFB91C1C) // Heavy Bomber
            else -> Color(0xFF9333EA) // Interceptor
        }

        val path = Path().apply {
            moveTo(center.x, center.y + 60f)
            lineTo(center.x + 64f, center.y - 30f)
            lineTo(center.x + 18f, center.y - 25f)
            lineTo(center.x, center.y - 50f)
            lineTo(center.x - 18f, center.y - 25f)
            lineTo(center.x - 64f, center.y - 30f)
            close()
        }
        drawPath(path, color = enemyColor)
        drawCircle(color = Color(0xFFFBBF24), radius = 10f, center = Offset(center.x, center.y + 14f))
    }

    // HP Bar over Enemy
    if (enemy.hp < enemy.maxHp) {
        val barWidth = if (enemy.enemyType == 3) 80f else 60f
        val hpRatio = enemy.hp.toFloat() / enemy.maxHp
        drawRect(
            color = Color.Gray,
            topLeft = Offset(center.x - barWidth / 2, center.y - 85f),
            size = Size(barWidth, 8f)
        )
        drawRect(
            color = Color(0xFFEF4444),
            topLeft = Offset(center.x - barWidth / 2, center.y - 85f),
            size = Size(barWidth * hpRatio, 8f)
        )
    }
}

private fun DrawScope.drawPowerUpGift(gift: PowerUpGift, center: Offset) {
    val giftColor = when (gift.type) {
        PowerUpType.TRIPLE_SHOT -> Color(0xFF38BDF8)
        PowerUpType.SHIELD -> Color(0xFF10B981)
        PowerUpType.MEGA_BOOST -> Color(0xFFFBBF24)
        PowerUpType.HEALTH_REPAIR -> Color(0xFFEC4899)
    }

    drawCircle(color = giftColor.copy(alpha = 0.45f), radius = 30f, center = center)
    drawRect(
        color = giftColor,
        topLeft = Offset(center.x - 16f, center.y - 16f),
        size = Size(32f, 32f)
    )
    drawRect(
        color = Color.White,
        topLeft = Offset(center.x - 5f, center.y - 16f),
        size = Size(10f, 32f)
    )
}

private fun DrawScope.drawAircraft(
    model: AircraftModel,
    trail: TrailEffect,
    center: Offset,
    tilt: Float
) {
    val algerianGreen = Color(0xFF006233)
    val algerianRed = Color(0xFFD21034)
    val algerianWhite = Color(0xFFFFFFFF)
    val stealthGrey = Color(0xFF334155)

    val trailColor = try {
        Color(android.graphics.Color.parseColor(trail.colorHex))
    } catch (e: Exception) {
        Color(0xFF38BDF8)
    }

    // 1. Su-57 Twin Engine Plasma Exhausts
    val leftFlame = Path().apply {
        moveTo(center.x - 22f, center.y + 60f)
        lineTo(center.x - 15f, center.y + 115f + (sin(System.currentTimeMillis() * 0.05).toFloat() * 12f))
        lineTo(center.x - 8f, center.y + 60f)
        close()
    }
    val rightFlame = Path().apply {
        moveTo(center.x + 8f, center.y + 60f)
        lineTo(center.x + 15f, center.y + 115f + (sin(System.currentTimeMillis() * 0.05).toFloat() * 12f))
        lineTo(center.x + 22f, center.y + 60f)
        close()
    }
    drawPath(leftFlame, color = trailColor)
    drawPath(rightFlame, color = trailColor)

    // 2. ENLARGED SU-57 STEALTH WINGS (WINGSPAN ~200dp FOR MAXIMUM VISIBILITY)
    val leftWingPath = Path().apply {
        moveTo(center.x, center.y - 95f)
        lineTo(center.x - 28f, center.y - 32f) // LEVCON Extension
        lineTo(center.x - 98f, center.y + 32f) // Main Swept Wing
        lineTo(center.x - 78f, center.y + 56f)
        lineTo(center.x, center.y + 25f)
        close()
    }
    drawPath(leftWingPath, color = algerianGreen)

    val rightWingPath = Path().apply {
        moveTo(center.x, center.y - 95f)
        lineTo(center.x + 28f, center.y - 32f) // LEVCON Extension
        lineTo(center.x + 98f, center.y + 32f) // Main Swept Wing
        lineTo(center.x + 78f, center.y + 56f)
        lineTo(center.x, center.y + 25f)
        close()
    }
    drawPath(rightWingPath, color = algerianWhite)

    // Wing Panel Lines
    val wingBorder = Path().apply {
        moveTo(center.x, center.y - 95f)
        lineTo(center.x - 98f, center.y + 32f)
        lineTo(center.x - 78f, center.y + 56f)
        lineTo(center.x + 78f, center.y + 56f)
        lineTo(center.x + 98f, center.y + 32f)
        close()
    }
    drawPath(wingBorder, color = stealthGrey, style = Stroke(width = 4f))

    // 3. Su-57 Stealth Fuselage Center Body
    val fuselagePath = Path().apply {
        moveTo(center.x, center.y - 100f)
        lineTo(center.x + 20f, center.y - 25f)
        lineTo(center.x + 18f, center.y + 65f)
        lineTo(center.x - 18f, center.y + 65f)
        lineTo(center.x - 20f, center.y - 25f)
        close()
    }
    drawPath(fuselagePath, color = stealthGrey)

    // 4. ALGERIAN FLAG RED CRESCENT & STAR EMBLEM ON CENTER FUSELAGE (العلم الجزائري 🇩🇿)
    // Red Crescent
    drawCircle(
        color = algerianRed,
        radius = 20f,
        center = Offset(center.x, center.y - 2f)
    )
    drawCircle(
        color = stealthGrey,
        radius = 16f,
        center = Offset(center.x + 5f, center.y - 2f)
    )

    // Red 5-Pointed Star
    val starPath = Path().apply {
        val cx = center.x - 5f
        val cy = center.y - 2f
        val outerR = 9f
        val innerR = 4f
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = i * Math.PI / 5 - Math.PI / 2
            val x = (cx + r * kotlin.math.cos(angle)).toFloat()
            val y = (cy + r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(starPath, color = algerianRed)

    // 5. Su-57 Canted Twin Stealth Tail Fins
    val leftTail = Path().apply {
        moveTo(center.x - 18f, center.y + 25f)
        lineTo(center.x - 52f, center.y + 72f)
        lineTo(center.x - 28f, center.y + 72f)
        close()
    }
    val rightTail = Path().apply {
        moveTo(center.x + 18f, center.y + 25f)
        lineTo(center.x + 52f, center.y + 72f)
        lineTo(center.x + 28f, center.y + 72f)
        close()
    }
    drawPath(leftTail, color = algerianGreen)
    drawPath(rightTail, color = algerianWhite)

    // 6. Su-57 Cockpit Glass
    drawOval(
        color = Color(0xFF38BDF8),
        topLeft = Offset(center.x - 10f, center.y - 75f),
        size = Size(20f, 42f)
    )
}

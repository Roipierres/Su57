package com.example.data

enum class MediaType {
    PHOTO,
    VIDEO
}

data class VaultItem(
    val id: String,
    val title: String,
    val encryptedFileName: String,
    val mimeType: String,
    val mediaType: MediaType,
    val albumName: String = "Main",
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val isDecoy: Boolean = false
)

data class Album(
    val id: String,
    val name: String,
    val icon: String = "folder",
    val itemCount: Int = 0
)

data class IntruderLog(
    val id: String,
    val attemptedCode: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AircraftModel(
    val id: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val topSpeed: Int,
    val primaryColorHex: String,
    val secondaryColorHex: String
) {
    SU_57_ALGERIAN("su57", "سوخوي سو-57 (العلم الجزائري 🇩🇿)", "Su-57 Algerian Felon", 1650, "#006233", "#D21034"),
    FALCON_JET("falcon", "صقر الجو F-22", "Falcon F-22", 950, "#0284C7", "#38BDF8"),
    STEALTH_FIGHTER("stealth", "الشبح B-2", "Stealth Wing", 880, "#1E293B", "#64748B"),
    GOLDEN_HAWK("golden", "الصقر الذهبي", "Golden Hawk", 1100, "#D97706", "#FBBF24"),
    CYBER_WING("cyber", "أجنحة السايبر", "Cyber Wing", 1200, "#9333EA", "#C084FC"),
    RETRO_PROP("retro", "طائرة الطليعة", "Retro Propeller", 720, "#DC2626", "#F87171")
}

enum class TrailEffect(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val colorHex: String
) {
    PLASMA_BLUE("blue", "بلازما زرقاء", "Blue Plasma", "#38BDF8"),
    FIRE_RED("fire", "لهب ناري", "Fire Flame", "#EF4444"),
    GOLD_LIGHT("gold", "شعاع ذهبي", "Gold Light", "#FBBF24"),
    EMERALD_NEON("emerald", "زمرد نيوني", "Emerald Neon", "#10B981"),
    RAINBOW("rainbow", "طيف قوسي", "Cosmic Rainbow", "#EC4899")
}

enum class TriggerDirection(
    val id: String,
    val labelAr: String,
    val labelEn: String
) {
    RIGHT("right", "الطيران إلى اليمين", "Fly Right"),
    LEFT("left", "الطيران إلى اليسار", "Fly Left"),
    BACKWARD("back", "الطيران إلى الخلف", "Fly Backward / Reverse"),
    ANY_SIDE("any", "أي اتجاه جانبي/خلفي (يمين/يسار/خلف)", "Any Direction (Left/Right/Back)")
}

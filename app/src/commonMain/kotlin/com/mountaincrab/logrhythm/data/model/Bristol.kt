package com.mountaincrab.logrhythm.data.model

/**
 * Bristol stool scale 1..7 with plain-English aliases.
 * Values mirror phone.jsx BRISTOL exactly.
 */
data class BristolType(
    val n: Int,
    val plain: String,
    val description: String,
)

val BRISTOL_TYPES = listOf(
    BristolType(1, "Hard lumps", "Separate hard lumps, like nuts"),
    BristolType(2, "Lumpy",      "Sausage-shaped but lumpy"),
    BristolType(3, "Cracked",    "Sausage with cracks on its surface"),
    BristolType(4, "Smooth",     "Smooth and soft, like a sausage"),
    BristolType(5, "Soft lumps", "Soft blobs with clear-cut edges"),
    BristolType(6, "Mushy",      "Fluffy pieces, ragged edges"),
    BristolType(7, "Liquid",     "Entirely liquid, no solid pieces"),
)

fun bristol(n: Int): BristolType = BRISTOL_TYPES.first { it.n == n }

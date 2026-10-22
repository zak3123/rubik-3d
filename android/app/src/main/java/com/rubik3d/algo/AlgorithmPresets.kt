package com.rubik3d.algo

data class AlgorithmItem(
    val id: String,
    val name: String,
    val category: String,
    val notation: String,
    val description: String
)

object AlgorithmPresets {
    val ITEMS = listOf(
        AlgorithmItem("sexy", "Sexy Move", "Basic Triggers", "R U R' U'", "Fundamental speedcube 4-move trigger. 6x returns to solved."),
        AlgorithmItem("rev_sexy", "Reverse Sexy", "Basic Triggers", "U R U' R'", "Inverse trigger frequently used in F2L inserts."),
        AlgorithmItem("sune", "Sune (OLL 27)", "Beginner / OLL", "R U R' U R U2 R'", "Classic 7-move fish orientation for yellow top corners."),
        AlgorithmItem("antisune", "Anti-Sune (OLL 26)", "Beginner / OLL", "R U2 R' U' R U' R'", "Mirror algorithm to Sune."),
        AlgorithmItem("tperm", "T-Perm (PLL)", "Advanced / PLL", "R U R' U' R' F R2 U' R' U' R U R' F'", "Permutes 2 adjacent corners and 2 adjacent edges."),
        AlgorithmItem("yperm", "Y-Perm (PLL)", "Advanced / PLL", "F R U' R' U' R U R' F' R U R' U' R' F R F'", "Diagonal corner swap and 2 edge swaps."),
        AlgorithmItem("checker", "Checkerboard", "Patterns", "R2 L2 U2 D2 F2 B2", "Classic symmetrical 6-face checkerboard pattern.")
    )
}

package com.rubik3d.tutorial

enum class TutorialCategory(val displayName: String) {
    BEGINNER("Beginner (Layer by Layer)"),
    ADVANCED("Advanced (CFOP / Speedcube)")
}

data class TutorialLesson(
    val id: String,
    val title: String,
    val category: TutorialCategory,
    val stage: String,
    val notation: String,
    val setupScramble: String = "",
    val description: String,
    val tips: String = ""
)

object TutorialRepository {
    val LESSONS: List<TutorialLesson> = listOf(
        // BEGINNER METHOD
        TutorialLesson(
            id = "beg_cross",
            title = "White Cross",
            category = TutorialCategory.BEGINNER,
            stage = "Step 1: Cross",
            notation = "F R U R' U' F'",
            setupScramble = "F U R U' R' F'",
            description = "Bentuk tanda tambah putih pada sisi bawah (D). Pastikan warna stiker samping setiap tepi sejajar dengan warna center di sekelilingnya.",
            tips = "Posisikan tepi putih satu per satu. Bila orientasi terbalik, gunakan F R U R' U' F' untuk membalik tepi."
        ),
        TutorialLesson(
            id = "beg_corners",
            title = "White Corners (First Layer)",
            category = TutorialCategory.BEGINNER,
            stage = "Step 2: First Layer",
            notation = "R U R' U'",
            setupScramble = "U R U' R'",
            description = "Selesaikan 4 sudut putih untuk melengkapi seluruh Lapisan Pertama. Arahkan sudut target tepat di atas slot tujuannya.",
            tips = "Ulangi trigger Sexy Move (R U R' U') 1 hingga 5 kali sampai stiker putih menghadap ke bawah."
        ),
        TutorialLesson(
            id = "beg_middle",
            title = "Middle Layer Edges",
            category = TutorialCategory.BEGINNER,
            stage = "Step 3: Second Layer",
            notation = "U R U' R' U' F' U F",
            setupScramble = "F' U' F U R U R' U'",
            description = "Masukkan keping tepi lapisan kedua ke slot kanan tanpa merusak lapisan pertama yang sudah selesai.",
            tips = "Jika tepi perlu masuk ke slot kiri, gunakan rumus simetris: U' L' U L U F U' F'."
        ),
        TutorialLesson(
            id = "beg_yellow_cross",
            title = "Yellow Cross",
            category = TutorialCategory.BEGINNER,
            stage = "Step 4: Top Cross",
            notation = "F R U R' U' F'",
            setupScramble = "F U R U' R' F'",
            description = "Bentuk tanda tambah kuning di sisi atas (U). Urutan transisi: Titik (Dot) -> Bentuk L -> Garis Lurus (Line) -> Salib (Cross).",
            tips = "Pastikan garis kuning horizontal menghadap Anda sebelum mengeksekusi algoritma."
        ),
        TutorialLesson(
            id = "beg_yellow_face",
            title = "Yellow Face (Sune)",
            category = TutorialCategory.BEGINNER,
            stage = "Step 5: OLL Corners",
            notation = "R U R' U R U2 R'",
            setupScramble = "R U2 R' U' R U' R'",
            description = "Orientasikan seluruh sudut kuning ke atas sehingga sisi U berwarna kuning penuh (Pola Ikan / Sune).",
            tips = "Bila pola ikan terbentuk, posisikan kepala ikan di sudut kiri-bawah (Front-Left), lalu jalankan Sune."
        ),
        TutorialLesson(
            id = "beg_last_layer",
            title = "Last Layer Permutation (T-Perm)",
            category = TutorialCategory.BEGINNER,
            stage = "Step 6: PLL Full Solve",
            notation = "R U R' U' R' F R2 U' R' U' R U R' F'",
            setupScramble = "F R U' R' U R U R2 F' R U R U' R'",
            description = "Tukar posisi sudut dan tepi lapisan atas untuk menyelesaikan seluruh kubus Rubik 3D.",
            tips = "Cari dua sudut berwarna sama pada satu sisi (Headlights), tempatkan di belakang (Back), lalu eksekusi."
        ),

        // ADVANCED CFOP
        TutorialLesson(
            id = "adv_f2l",
            title = "F2L (Corner + Edge Pair Insert)",
            category = TutorialCategory.ADVANCED,
            stage = "CFOP: First Two Layers",
            notation = "U R U' R'",
            setupScramble = "R U R' U'",
            description = "Gabungkan sudut dan tepi lapisan tengah menjadi pasangan 1x2x2 di lapisan atas, lalu masukkan langsung ke slot F2L.",
            tips = "Metode F2L memangkas waktu solve secara signifikan dengan menyelesaikan layer 1 dan 2 secara serentak."
        ),
        TutorialLesson(
            id = "adv_oll",
            title = "OLL 27 (Orientation of Last Layer)",
            category = TutorialCategory.ADVANCED,
            stage = "CFOP: OLL",
            notation = "R U R' U R U2 R'",
            setupScramble = "R U2 R' U' R U' R'",
            description = "Selesaikan orientasi seluruh stiker kuning di lapisan atas dalam 1 algoritma tunggal.",
            tips = "Sune merupakan salah satu dari 57 pola OLL yang paling mendasar dan dapat dieksekusi dalam tempo sub-1 detik."
        ),
        TutorialLesson(
            id = "adv_pll",
            title = "PLL Y-Perm (Diagonal Swap)",
            category = TutorialCategory.ADVANCED,
            stage = "CFOP: PLL",
            notation = "F R U' R' U' R U R' F' R U R' U' R' F R F'",
            setupScramble = "F' R' F R U R U' R' F R U' R' U R U R' F'",
            description = "Menukar 2 sudut diagonal (Front-Right & Back-Left) serta 2 tepi lapisan atas secara simultan.",
            tips = "Gunakan Y-Perm saat lapisan atas tidak memiliki headlights (tidak ada 2 sudut yang senada pada satu sisi)."
        )
    )
}

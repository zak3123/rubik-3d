package com.rubik3d.ui
import android.content.Context
import android.graphics.Typeface
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.rubik3d.R
import com.rubik3d.algo.AlgorithmPlayer
import com.rubik3d.algo.AlgorithmPresets
import com.rubik3d.audio.CubeAudioPlayer
import com.rubik3d.gl.CubeRenderer
import com.rubik3d.gl.RotationManager
import com.rubik3d.model.CubeState
import com.rubik3d.model.Move
import com.rubik3d.touch.CubeTouchHandler
import com.rubik3d.tutorial.TutorialLesson
import com.rubik3d.tutorial.TutorialRepository
import com.rubik3d.solver.KociembaSolver
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private enum class Mode { PLAY, ALGO, LEARN, SOLVE }
    private var currentMode = Mode.PLAY

    // GL & Core
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var cubeState: CubeState
    private lateinit var rotationManager: RotationManager
    private lateinit var renderer: CubeRenderer
    private lateinit var touchHandler: CubeTouchHandler
    private lateinit var audioPlayer: CubeAudioPlayer
    private lateinit var algorithmPlayer: AlgorithmPlayer
    private lateinit var learnPlayer: AlgorithmPlayer
    private lateinit var solvePlayer: AlgorithmPlayer
    private var vibrator: Vibrator? = null

    // Mode Navigation Tabs
    private lateinit var btnTabPlay: Button
    private lateinit var btnTabAlgo: Button
    private lateinit var btnTabLearn: Button
    private lateinit var btnTabSolve: Button
    private lateinit var btnSettings: Button

    // Play HUD & Controls
    private lateinit var layoutPlayHUD: View
    private lateinit var tvTimer: TextView
    private lateinit var tvMoves: TextView
    private lateinit var layoutPlayControls: View
    private lateinit var btnScramble: Button
    private lateinit var btnReset: Button
    private lateinit var tvStatus: TextView

    // Algorithm HUD & Controls
    private lateinit var layoutAlgoHUD: View
    private lateinit var tvAlgoTitle: TextView
    private lateinit var tvAlgoStep: TextView
    private lateinit var layoutAlgoControls: View
    private lateinit var spinnerPresets: Spinner
    private lateinit var etAlgoInput: EditText
    private lateinit var btnAlgoLoad: Button
    private lateinit var scrollAlgoMoves: HorizontalScrollView
    private lateinit var tvAlgoMovesDisplay: TextView
    private lateinit var btnAlgoPrev: Button
    private lateinit var btnAlgoPlay: Button
    private lateinit var btnAlgoNext: Button
    private lateinit var btnAlgoReset: Button

    // Tutorial HUD & Controls
    private lateinit var layoutLearnHUD: View
    private lateinit var tvLearnStage: TextView
    private lateinit var tvLearnTitle: TextView
    private lateinit var tvLearnStep: TextView
    private lateinit var layoutLearnControls: View
    private lateinit var spinnerLessons: Spinner
    private lateinit var tvLearnDesc: TextView
    private lateinit var tvLearnTips: TextView
    private lateinit var scrollLearnMoves: HorizontalScrollView
    private lateinit var tvLearnMovesDisplay: TextView
    private lateinit var btnLearnPrev: Button
    private lateinit var btnLearnPlay: Button
    private lateinit var btnLearnNext: Button
    private lateinit var btnLearnSetup: Button
    private lateinit var btnLearnReset: Button

    // Solver HUD & Controls
    private lateinit var layoutSolveHUD: View
    private lateinit var tvSolveTitle: TextView
    private lateinit var tvSolveStep: TextView
    private lateinit var layoutSolveControls: View
    private lateinit var btnSolveExecute: Button
    private lateinit var tvSolveInfo: TextView
    private lateinit var scrollSolveMoves: HorizontalScrollView
    private lateinit var tvSolveMovesDisplay: TextView
    private lateinit var btnSolvePrev: Button
    private lateinit var btnSolvePlay: Button
    private lateinit var btnSolveNext: Button
    private lateinit var btnSolveReset: Button
    // Settings overlay
    private lateinit var panelSettings: View
    private lateinit var switchSound: SwitchCompat
    private lateinit var tvVolume: TextView
    private lateinit var seekVolume: SeekBar
    private lateinit var tvSpeed: TextView
    private lateinit var seekSpeed: SeekBar
    private lateinit var btnCloseSettings: Button

    private var selectedLesson: TutorialLesson? = null
    private var moveCount = 0
    private var isTimerRunning = false
    private var timerStartTime = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                val elapsed = SystemClock.elapsedRealtime() - timerStartTime
                val minutes = (elapsed / 60000) % 60
                val seconds = (elapsed / 1000) % 60
                val centis = (elapsed % 1000) / 10
                tvTimer.text = String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
                handler.postDelayed(this, 30)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        initViews()
        initCoreEngine()
        setupModeNavigation()
        setupPlayControls()
        setupAlgorithmControls()
        setupTutorialControls()
        setupSolverControls()
        setupSettingsModal()
    }

    private fun initViews() {
        glSurfaceView = findViewById(R.id.glSurfaceView)
        btnTabPlay = findViewById(R.id.btnTabPlay)
        btnTabAlgo = findViewById(R.id.btnTabAlgo)
        btnTabLearn = findViewById(R.id.btnTabLearn)
        btnTabSolve = findViewById(R.id.btnTabSolve)
        btnSettings = findViewById(R.id.btnSettings)

        // Play views
        layoutPlayHUD = findViewById(R.id.layoutPlayHUD)
        tvTimer = findViewById(R.id.tvTimer)
        tvMoves = findViewById(R.id.tvMoves)
        layoutPlayControls = findViewById(R.id.layoutPlayControls)
        btnScramble = findViewById(R.id.btnScramble)
        btnReset = findViewById(R.id.btnReset)
        tvStatus = findViewById(R.id.tvStatus)

        // Algo views
        layoutAlgoHUD = findViewById(R.id.layoutAlgoHUD)
        tvAlgoTitle = findViewById(R.id.tvAlgoTitle)
        tvAlgoStep = findViewById(R.id.tvAlgoStep)
        layoutAlgoControls = findViewById(R.id.layoutAlgoControls)
        spinnerPresets = findViewById(R.id.spinnerPresets)
        etAlgoInput = findViewById(R.id.etAlgoInput)
        btnAlgoLoad = findViewById(R.id.btnAlgoLoad)
        scrollAlgoMoves = findViewById(R.id.scrollAlgoMoves)
        tvAlgoMovesDisplay = findViewById(R.id.tvAlgoMovesDisplay)
        btnAlgoPrev = findViewById(R.id.btnAlgoPrev)
        btnAlgoPlay = findViewById(R.id.btnAlgoPlay)
        btnAlgoNext = findViewById(R.id.btnAlgoNext)
        btnAlgoReset = findViewById(R.id.btnAlgoReset)

        // Tutorial views
        layoutLearnHUD = findViewById(R.id.layoutLearnHUD)
        tvLearnStage = findViewById(R.id.tvLearnStage)
        tvLearnTitle = findViewById(R.id.tvLearnTitle)
        tvLearnStep = findViewById(R.id.tvLearnStep)
        layoutLearnControls = findViewById(R.id.layoutLearnControls)
        spinnerLessons = findViewById(R.id.spinnerLessons)
        tvLearnDesc = findViewById(R.id.tvLearnDesc)
        tvLearnTips = findViewById(R.id.tvLearnTips)
        scrollLearnMoves = findViewById(R.id.scrollLearnMoves)
        tvLearnMovesDisplay = findViewById(R.id.tvLearnMovesDisplay)
        btnLearnPrev = findViewById(R.id.btnLearnPrev)
        btnLearnPlay = findViewById(R.id.btnLearnPlay)
        btnLearnNext = findViewById(R.id.btnLearnNext)
        btnLearnSetup = findViewById(R.id.btnLearnSetup)
        btnLearnReset = findViewById(R.id.btnLearnReset)

        // Solver views
        layoutSolveHUD = findViewById(R.id.layoutSolveHUD)
        tvSolveTitle = findViewById(R.id.tvSolveTitle)
        tvSolveStep = findViewById(R.id.tvSolveStep)
        layoutSolveControls = findViewById(R.id.layoutSolveControls)
        btnSolveExecute = findViewById(R.id.btnSolveExecute)
        tvSolveInfo = findViewById(R.id.tvSolveInfo)
        scrollSolveMoves = findViewById(R.id.scrollSolveMoves)
        tvSolveMovesDisplay = findViewById(R.id.tvSolveMovesDisplay)
        btnSolvePrev = findViewById(R.id.btnSolvePrev)
        btnSolvePlay = findViewById(R.id.btnSolvePlay)
        btnSolveNext = findViewById(R.id.btnSolveNext)
        btnSolveReset = findViewById(R.id.btnSolveReset)
        // Settings views
        panelSettings = findViewById(R.id.panelSettings)
        switchSound = findViewById(R.id.switchSound)
        tvVolume = findViewById(R.id.tvVolume)
        seekVolume = findViewById(R.id.seekVolume)
        tvSpeed = findViewById(R.id.tvSpeed)
        seekSpeed = findViewById(R.id.seekSpeed)
        btnCloseSettings = findViewById(R.id.btnCloseSettings)
    }

    private fun initCoreEngine() {
        audioPlayer = CubeAudioPlayer(this)
        cubeState = CubeState()
        rotationManager = RotationManager(cubeState)
        renderer = CubeRenderer(cubeState, rotationManager)
        touchHandler = CubeTouchHandler(this, renderer, rotationManager)
        algorithmPlayer = AlgorithmPlayer(cubeState, rotationManager)
        learnPlayer = AlgorithmPlayer(cubeState, rotationManager)
        solvePlayer = AlgorithmPlayer(cubeState, rotationManager)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        // Warm up Kociemba solver tables in background
        Thread {
            KociembaSolver.warmup()
        }.start()
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.setOnTouchListener { _, event -> touchHandler.onTouchEvent(event) }

        // Audio hooks
        rotationManager.onDragStartedListener = { audioPlayer.playFriction() }
        rotationManager.onMoveStartedListener = { audioPlayer.playFriction() }

        rotationManager.onMoveFinishedListener = { move ->
            val isDouble = Math.abs(move.angleDegrees) > 100f
            audioPlayer.playSnap(isDouble)
            triggerHaptic()
            runOnUiThread {
                when (currentMode) {
                    Mode.PLAY -> {
                        if (!isTimerRunning && !cubeState.isSolved()) {
                            startTimer()
                        }
                        moveCount++
                        tvMoves.text = getString(R.string.moves_format, moveCount)
                        if (cubeState.isSolved()) {
                            stopTimer()
                            tvStatus.visibility = View.VISIBLE
                            audioPlayer.playVictory()
                        }
                    }
                    Mode.ALGO -> algorithmPlayer.onMoveCompleted(move)
                    Mode.LEARN -> learnPlayer.onMoveCompleted(move)
                    Mode.SOLVE -> solvePlayer.onMoveCompleted(move)
                }
            }
        }
    }

    private fun triggerHaptic() {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(20L)
        }
    }

    private fun setupModeNavigation() {
        btnTabPlay.setOnClickListener { switchMode(Mode.PLAY) }
        btnTabAlgo.setOnClickListener { switchMode(Mode.ALGO) }
        btnTabLearn.setOnClickListener { switchMode(Mode.LEARN) }
        btnTabSolve.setOnClickListener { switchMode(Mode.SOLVE) }
    }
    private fun switchMode(mode: Mode) {
        currentMode = mode

        // Reset tab styles
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)
        val activeColor = ContextCompat.getColor(this, R.color.text_primary)
        btnTabPlay.setTextColor(inactiveColor)
        btnTabPlay.setTypeface(null, Typeface.NORMAL)
        btnTabAlgo.setTextColor(inactiveColor)
        btnTabAlgo.setTypeface(null, Typeface.NORMAL)
        btnTabLearn.setTextColor(inactiveColor)
        btnTabLearn.setTypeface(null, Typeface.NORMAL)
        btnTabSolve.setTextColor(inactiveColor)
        btnTabSolve.setTypeface(null, Typeface.NORMAL)

        // Hide all mode containers
        layoutPlayHUD.visibility = View.GONE
        layoutPlayControls.visibility = View.GONE
        layoutAlgoHUD.visibility = View.GONE
        layoutAlgoControls.visibility = View.GONE
        layoutLearnHUD.visibility = View.GONE
        layoutLearnControls.visibility = View.GONE
        layoutSolveHUD.visibility = View.GONE
        layoutSolveControls.visibility = View.GONE

        stopTimer()
        tvStatus.visibility = View.GONE
        algorithmPlayer.pause()
        learnPlayer.pause()
        solvePlayer.pause()
        when (mode) {
            Mode.PLAY -> {
                btnTabPlay.setTextColor(activeColor)
                btnTabPlay.setTypeface(null, Typeface.BOLD)
                layoutPlayHUD.visibility = View.VISIBLE
                layoutPlayControls.visibility = View.VISIBLE
            }
            Mode.ALGO -> {
                btnTabAlgo.setTextColor(activeColor)
                btnTabAlgo.setTypeface(null, Typeface.BOLD)
                layoutAlgoHUD.visibility = View.VISIBLE
                layoutAlgoControls.visibility = View.VISIBLE
            }
            Mode.LEARN -> {
                btnTabLearn.setTextColor(activeColor)
                btnTabLearn.setTypeface(null, Typeface.BOLD)
                layoutLearnHUD.visibility = View.VISIBLE
                layoutLearnControls.visibility = View.VISIBLE
                if (selectedLesson == null && TutorialRepository.LESSONS.isNotEmpty()) {
                    loadLesson(TutorialRepository.LESSONS[0])
                }
            }
                if (selectedLesson == null && TutorialRepository.LESSONS.isNotEmpty()) {
                    loadLesson(TutorialRepository.LESSONS[0])
                }
            }
            Mode.SOLVE -> {
                btnTabSolve.setTextColor(activeColor)
                btnTabSolve.setTypeface(null, Typeface.BOLD)
                layoutSolveHUD.visibility = View.VISIBLE
                layoutSolveControls.visibility = View.VISIBLE
                if (cubeState.isSolved()) {
                    tvSolveInfo.text = getString(R.string.solve_already_solved)
                }
            }
        }
    }

    private fun setupPlayControls() {
        btnScramble.setOnClickListener {
            if (rotationManager.isBusy()) return@setOnClickListener
            stopTimer()
            resetTimerText()
            moveCount = 0
            tvMoves.text = getString(R.string.moves_format, 0)
            tvStatus.visibility = View.GONE
            val moves = cubeState.generateScramble(20)
            rotationManager.queueMoves(moves)
        }

        btnReset.setOnClickListener {
            rotationManager.clearQueue()
            cubeState.reset()
            stopTimer()
            resetTimerText()
            moveCount = 0
            tvMoves.text = getString(R.string.moves_format, 0)
            tvStatus.visibility = View.GONE
        }
    }

    private fun setupAlgorithmControls() {
        val presetNames = AlgorithmPresets.ITEMS.map { "${it.name} (${it.category})" }
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, presetNames) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                tv.textSize = 13f
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_dark))
                tv.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                tv.setPadding(28, 24, 28, 24)
                tv.textSize = 13f
                return tv
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPresets.adapter = adapter

        spinnerPresets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = AlgorithmPresets.ITEMS[position]
                tvAlgoTitle.text = item.name
                etAlgoInput.setText(item.notation)
                rotationManager.clearQueue()
                cubeState.reset()
                algorithmPlayer.loadAlgorithm(item.notation)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnAlgoLoad.setOnClickListener {
            val notation = etAlgoInput.text.toString().trim()
            val parsed = Move.parseSequence(notation)
            if (parsed.isEmpty()) {
                Toast.makeText(this, "Formula tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            tvAlgoTitle.text = "Custom Algorithm"
            rotationManager.clearQueue()
            cubeState.reset()
            algorithmPlayer.loadAlgorithm(notation)
        }

        btnAlgoPlay.setOnClickListener {
            if (algorithmPlayer.state == AlgorithmPlayer.PlayState.PLAYING) {
                algorithmPlayer.pause()
            } else {
                if (algorithmPlayer.currentIndex >= algorithmPlayer.moves.size - 1) {
                    algorithmPlayer.reset()
                }
                algorithmPlayer.play()
            }
        }

        btnAlgoPrev.setOnClickListener { algorithmPlayer.prevStep() }
        btnAlgoNext.setOnClickListener { algorithmPlayer.nextStep() }
        btnAlgoReset.setOnClickListener { algorithmPlayer.reset() }

        algorithmPlayer.onStateChangedListener = { state, currentIndex, moves ->
            runOnUiThread {
                updateAlgoUI(state, currentIndex, moves)
            }
        }
    }

    private fun updateAlgoUI(state: AlgorithmPlayer.PlayState, currentIndex: Int, moves: List<Move>) {
        val total = moves.size
        val step = if (currentIndex >= 0) currentIndex + 1 else 0
        tvAlgoStep.text = getString(R.string.algo_step_format, step, total)

        btnAlgoPlay.text = if (state == AlgorithmPlayer.PlayState.PLAYING) {
            getString(R.string.algo_pause)
        } else {
            getString(R.string.algo_play)
        }

        if (moves.isEmpty()) {
            tvAlgoMovesDisplay.text = "-"
            return
        }

        val sb = StringBuilder()
        for (i in moves.indices) {
            val token = moves[i].name
            if (i == currentIndex) {
                sb.append("<font color='#4C9AFF'><b><u>").append(token).append("</u></b></font> ")
            } else if (i < currentIndex) {
                sb.append("<font color='#8C95A6'>").append(token).append("</font> ")
            } else {
                sb.append("<font color='#F0F2F5'>").append(token).append("</font> ")
            }
        }
        tvAlgoMovesDisplay.text = HtmlCompat.fromHtml(sb.toString().trim(), HtmlCompat.FROM_HTML_MODE_LEGACY)

        scrollAlgoMoves.post {
            if (total > 0 && currentIndex >= 0) {
                val scrollRange = tvAlgoMovesDisplay.width
                val targetX = (scrollRange * currentIndex) / total
                scrollAlgoMoves.smoothScrollTo(targetX, 0)
            } else {
                scrollAlgoMoves.smoothScrollTo(0, 0)
            }
        }
    }

    private fun setupTutorialControls() {
        val lessonNames = TutorialRepository.LESSONS.map {
            val prefix = if (it.category == com.rubik3d.tutorial.TutorialCategory.BEGINNER) "[BEG]" else "[CFOP]"
            "$prefix ${it.title}"
        }

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lessonNames) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getView(position, convertView, parent) as TextView
                tv.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                tv.textSize = 13f
                return tv
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = super.getDropDownView(position, convertView, parent) as TextView
                tv.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_dark))
                tv.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                tv.setPadding(28, 24, 28, 24)
                tv.textSize = 13f
                return tv
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLessons.adapter = adapter

        spinnerLessons.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val lesson = TutorialRepository.LESSONS[position]
                loadLesson(lesson)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnLearnSetup.setOnClickListener {
            val lesson = selectedLesson ?: return@setOnClickListener
            loadLesson(lesson, applySetup = true)
            Toast.makeText(this, "Kasus latihan disiapkan", Toast.LENGTH_SHORT).show()
        }

        btnLearnPlay.setOnClickListener {
            if (learnPlayer.state == AlgorithmPlayer.PlayState.PLAYING) {
                learnPlayer.pause()
            } else {
                if (learnPlayer.currentIndex >= learnPlayer.moves.size - 1) {
                    val lesson = selectedLesson
                    if (lesson != null && lesson.setupScramble.isNotBlank()) {
                        loadLesson(lesson, applySetup = true)
                    } else {
                        learnPlayer.reset()
                    }
                }
                learnPlayer.play()
            }
        }

        btnLearnPrev.setOnClickListener { learnPlayer.prevStep() }
        btnLearnNext.setOnClickListener { learnPlayer.nextStep() }
        btnLearnReset.setOnClickListener {
            val lesson = selectedLesson
            if (lesson != null && lesson.setupScramble.isNotBlank()) {
                loadLesson(lesson, applySetup = true)
            } else {
                learnPlayer.reset()
            }
        }

        learnPlayer.onStateChangedListener = { state, currentIndex, moves ->
            runOnUiThread {
                updateLearnUI(state, currentIndex, moves)
            }
        }
    }

    private fun loadLesson(lesson: TutorialLesson, applySetup: Boolean = false) {
        selectedLesson = lesson
        tvLearnStage.text = lesson.stage
        tvLearnTitle.text = lesson.title
        tvLearnDesc.text = lesson.description
        tvLearnTips.text = lesson.tips

        rotationManager.clearQueue()
        cubeState.reset()

        if (applySetup && lesson.setupScramble.isNotBlank()) {
            val setupMoves = Move.parseSequence(lesson.setupScramble)
            for (m in setupMoves) {
                cubeState.applyMove(m)
            }
        }

        learnPlayer.loadAlgorithm(lesson.notation)
    }

    private fun updateLearnUI(state: AlgorithmPlayer.PlayState, currentIndex: Int, moves: List<Move>) {
        val total = moves.size
        val step = if (currentIndex >= 0) currentIndex + 1 else 0
        tvLearnStep.text = getString(R.string.learn_step_format, step, total)

        btnLearnPlay.text = if (state == AlgorithmPlayer.PlayState.PLAYING) {
            getString(R.string.algo_pause)
        } else {
            getString(R.string.learn_demo_play)
        }

        if (moves.isEmpty()) {
            tvLearnMovesDisplay.text = "-"
            return
        }

        val sb = StringBuilder()
        for (i in moves.indices) {
            val token = moves[i].name
            if (i == currentIndex) {
                sb.append("<font color='#4C9AFF'><b><u>").append(token).append("</u></b></font> ")
            } else if (i < currentIndex) {
                sb.append("<font color='#8C95A6'>").append(token).append("</font> ")
            } else {
                sb.append("<font color='#F0F2F5'>").append(token).append("</font> ")
            }
        }
        tvLearnMovesDisplay.text = HtmlCompat.fromHtml(sb.toString().trim(), HtmlCompat.FROM_HTML_MODE_LEGACY)

        scrollLearnMoves.post {
            if (total > 0 && currentIndex >= 0) {
                val scrollRange = tvLearnMovesDisplay.width
                val targetX = (scrollRange * currentIndex) / total
                scrollLearnMoves.smoothScrollTo(targetX, 0)
            } else {
                scrollLearnMoves.smoothScrollTo(0, 0)
            }
        }
    }

    private fun setupSolverControls() {
        btnSolveExecute.setOnClickListener {
            if (rotationManager.isBusy()) return@setOnClickListener
            if (cubeState.isSolved()) {
                tvSolveInfo.text = getString(R.string.solve_already_solved)
                return@setOnClickListener
            }

            tvSolveInfo.text = getString(R.string.solve_computing)
            btnSolveExecute.isEnabled = false

            Thread {
                val solutionMoves = KociembaSolver.solve(cubeState)
                runOnUiThread {
                    btnSolveExecute.isEnabled = true
                    if (solutionMoves == null) {
                        tvSolveInfo.text = getString(R.string.solve_no_solution)
                    } else if (solutionMoves.isEmpty()) {
                        tvSolveInfo.text = getString(R.string.solve_already_solved)
                    } else {
                        tvSolveInfo.text = getString(R.string.solve_solution_format, solutionMoves.size)
                        val notation = solutionMoves.joinToString(" ") { it.name }
                        solvePlayer.loadAlgorithm(notation)
                    }
                }
            }.start()
        }

        btnSolvePlay.setOnClickListener {
            if (solvePlayer.state == AlgorithmPlayer.PlayState.PLAYING) {
                solvePlayer.pause()
            } else {
                solvePlayer.play()
            }
        }

        btnSolvePrev.setOnClickListener {
            solvePlayer.prevStep()
        }

        btnSolveNext.setOnClickListener {
            solvePlayer.nextStep()
        }

        btnSolveReset.setOnClickListener {
            solvePlayer.reset()
        }

        solvePlayer.onStateChangedListener = { state, currentIndex, moves ->
            runOnUiThread {
                updateSolveUI(state, currentIndex, moves)
            }
        }
    }

    private fun updateSolveUI(state: AlgorithmPlayer.PlayState, currentIndex: Int, moves: List<Move>) {
        val total = moves.size
        val step = if (currentIndex >= 0) currentIndex + 1 else 0
        tvSolveStep.text = getString(R.string.solve_step_format, step, total)

        btnSolvePlay.text = if (state == AlgorithmPlayer.PlayState.PLAYING) {
            getString(R.string.algo_pause)
        } else {
            getString(R.string.algo_play)
        }

        if (state == AlgorithmPlayer.PlayState.FINISHED && cubeState.isSolved()) {
            audioPlayer.playVictory()
        }

        if (moves.isEmpty()) {
            tvSolveMovesDisplay.text = ""
            return
        }

        val sb = StringBuilder()
        for (i in moves.indices) {
            val token = moves[i].name
            if (i == currentIndex) {
                sb.append("<font color='#4C9AFF'><b><u>").append(token).append("</u></b></font> ")
            } else if (i < currentIndex) {
                sb.append("<font color='#8C95A6'>").append(token).append("</font> ")
            } else {
                sb.append("<font color='#F0F2F5'>").append(token).append("</font> ")
            }
        }
        tvSolveMovesDisplay.text = HtmlCompat.fromHtml(sb.toString().trim(), HtmlCompat.FROM_HTML_MODE_LEGACY)

        scrollSolveMoves.post {
            if (total > 0 && currentIndex >= 0) {
                val scrollRange = tvSolveMovesDisplay.width
                val targetX = (scrollRange * currentIndex) / total
                scrollSolveMoves.smoothScrollTo(targetX, 0)
            } else {
                scrollSolveMoves.smoothScrollTo(0, 0)
            }
        }

    private fun setupSettingsModal() {
        btnSettings.setOnClickListener {
            panelSettings.visibility = View.VISIBLE
        }

        btnCloseSettings.setOnClickListener {
            panelSettings.visibility = View.GONE
        }

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            audioPlayer.isSoundEnabled = isChecked
        }

        seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                audioPlayer.volume = progress / 100f
                tvVolume.text = getString(R.string.volume_label, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                audioPlayer.playSnap()
            }
        })

        seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speedMs = progress + 50L
                rotationManager.setSpeedMs(speedMs)
                val delay = (speedMs / 2).coerceAtLeast(60L)
                algorithmPlayer.stepDelayMs = delay
                learnPlayer.stepDelayMs = delay
                solvePlayer.stepDelayMs = delay
                tvSpeed.text = getString(R.string.speed_label, speedMs)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startTimer() {
        if (isTimerRunning) return
        isTimerRunning = true
        timerStartTime = SystemClock.elapsedRealtime()
        handler.post(timerRunnable)
    }

    private fun stopTimer() {
        isTimerRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    private fun resetTimerText() {
        tvTimer.text = getString(R.string.timer_default)
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        stopTimer()
        if (::algorithmPlayer.isInitialized) {
            algorithmPlayer.pause()
        }
        if (::learnPlayer.isInitialized) {
            learnPlayer.pause()
        }
        if (::solvePlayer.isInitialized) {
            solvePlayer.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
        if (::algorithmPlayer.isInitialized) {
            algorithmPlayer.pause()
        }
        if (::learnPlayer.isInitialized) {
            learnPlayer.pause()
        }
        if (::solvePlayer.isInitialized) {
            solvePlayer.pause()
        }
    }
}

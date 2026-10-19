package com.james.anvil.ui.viewmodel

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.core.CombatManager
import com.james.anvil.core.DamageSource
import com.james.anvil.core.LevelManager
import com.james.anvil.core.QuestManager
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.FocusSession
import com.james.anvil.data.QuestCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar
import javax.inject.Inject

enum class FocusPhase { IDLE, WORK, BREAK, FINISHED }

@HiltViewModel
class FocusSessionViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val focusSessionDao = db.focusSessionDao()
    private val levelManager = LevelManager(application)
    private val questManager = QuestManager(application)
    private val combatManager = CombatManager(application)

    // Settings
    private val _workMinutes = MutableStateFlow(25)
    val workMinutes: StateFlow<Int> = _workMinutes.asStateFlow()

    private val _breakMinutes = MutableStateFlow(5)
    val breakMinutes: StateFlow<Int> = _breakMinutes.asStateFlow()

    private val _totalRounds = MutableStateFlow(4)
    val totalRounds: StateFlow<Int> = _totalRounds.asStateFlow()

    // Timer state
    private val _phase = MutableStateFlow(FocusPhase.IDLE)
    val phase: StateFlow<FocusPhase> = _phase.asStateFlow()

    private val _currentRound = MutableStateFlow(1)
    val currentRound: StateFlow<Int> = _currentRound.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _totalSeconds = MutableStateFlow(0L)
    val totalSeconds: StateFlow<Long> = _totalSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var sessionStartTime: Long = 0L
    private var completedRounds: Int = 0

    // Stats
    private val clockTicks = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(60_000L)
        }
    }

    val todayFocusMinutes: StateFlow<Int> = combine(
        focusSessionDao.observeAll(),
        clockTicks
    ) { sessions, _ ->
        sessions.filter { isSameLocalDay(it.startTime) }.sumOf { it.totalFocusMinutes }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val totalFocusMinutes: StateFlow<Int> = focusSessionDao.observeTotalFocusMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val totalSessionCount: StateFlow<Int> = focusSessionDao.observeTotalSessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val recentSessions = focusSessionDao.observeRecent(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Settings adjustments
    fun setWorkMinutes(minutes: Int) {
        if (_phase.value == FocusPhase.IDLE) {
            _workMinutes.value = minutes.coerceIn(5, 60)
        }
    }

    fun setBreakMinutes(minutes: Int) {
        if (_phase.value == FocusPhase.IDLE) {
            _breakMinutes.value = minutes.coerceIn(1, 30)
        }
    }

    fun setTotalRounds(rounds: Int) {
        if (_phase.value == FocusPhase.IDLE) {
            _totalRounds.value = rounds.coerceIn(1, 8)
        }
    }

    // Timer control
    fun startSession() {
        if (_isRunning.value || _phase.value != FocusPhase.IDLE) return
        sessionStartTime = System.currentTimeMillis()
        completedRounds = 0
        _currentRound.value = 1
        _phase.value = FocusPhase.WORK
        startWorkPhase()
    }

    private fun startWorkPhase() {
        _phase.value = FocusPhase.WORK
        val durationMs = _workMinutes.value * 60 * 1000L
        _totalSeconds.value = _workMinutes.value * 60L
        _remainingSeconds.value = _totalSeconds.value
        _isRunning.value = true
        startCountdown(durationMs)
    }

    private fun startBreakPhase() {
        _phase.value = FocusPhase.BREAK
        val durationMs = _breakMinutes.value * 60 * 1000L
        _totalSeconds.value = _breakMinutes.value * 60L
        _remainingSeconds.value = _totalSeconds.value
        _isRunning.value = true
        startCountdown(durationMs)
    }

    private fun startCountdown(durationMs: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingSeconds.value = millisUntilFinished / 1000
            }

            override fun onFinish() {
                _remainingSeconds.value = 0
                onPhaseComplete()
            }
        }.start()
    }

    private fun onPhaseComplete() {
        when (_phase.value) {
            FocusPhase.WORK -> {
                completedRounds++
                if (completedRounds >= _totalRounds.value) {
                    finishSession()
                } else {
                    _currentRound.value = completedRounds + 1
                    startBreakPhase()
                }
            }
            FocusPhase.BREAK -> {
                startWorkPhase()
            }
            else -> { /* no-op */ }
        }
    }

    private fun finishSession() {
        _phase.value = FocusPhase.FINISHED
        _isRunning.value = false
        countDownTimer?.cancel()

        val totalMinutes = completedRounds * _workMinutes.value
        val sessionStart = sessionStartTime
        val workDuration = _workMinutes.value
        val breakDuration = _breakMinutes.value
        val sessionEnd = System.currentTimeMillis()

        viewModelScope.launch {
            val session = FocusSession(
                startTime = sessionStart,
                endTime = sessionEnd,
                workMinutes = workDuration,
                breakMinutes = breakDuration,
                sessionsCompleted = completedRounds,
                totalFocusMinutes = totalMinutes,
                isCompleted = true
            )
            focusSessionDao.insert(session)
            levelManager.awardFocusSessionXp(totalMinutes)
            questManager.updateQuestProgress(QuestCategory.FOCUS)
            dealDamageToActiveMonster(totalMinutes)
        }
    }

    fun stopSession() {
        if (!_isRunning.value) return
        countDownTimer?.cancel()
        _isRunning.value = false

        if (completedRounds > 0) {
            val totalMinutes = completedRounds * _workMinutes.value
            val sessionStart = sessionStartTime
            val workDuration = _workMinutes.value
            val breakDuration = _breakMinutes.value
            val sessionEnd = System.currentTimeMillis()
            viewModelScope.launch {
                val session = FocusSession(
                    startTime = sessionStart,
                    endTime = sessionEnd,
                    workMinutes = workDuration,
                    breakMinutes = breakDuration,
                    sessionsCompleted = completedRounds,
                    totalFocusMinutes = totalMinutes,
                    isCompleted = false
                )
                focusSessionDao.insert(session)
                if (totalMinutes >= workDuration) {
                    levelManager.awardFocusSessionXp(totalMinutes)
                    questManager.updateQuestProgress(QuestCategory.FOCUS)
                    dealDamageToActiveMonster(totalMinutes)
                }
            }
        }

        resetToIdle()
    }

    fun resetToIdle() {
        _phase.value = FocusPhase.IDLE
        _remainingSeconds.value = 0
        _totalSeconds.value = 0
        _currentRound.value = 1
        _isRunning.value = false
        completedRounds = 0
    }

    private suspend fun dealDamageToActiveMonster(totalMinutes: Int) {
        val monster = db.monsterDao().getFirstActiveMonster() ?: return
        val damage = totalMinutes / 5
        if (damage > 0) {
            combatManager.dealDamage(monster.id, damage, DamageSource.FOCUS)
        }
    }

    private fun isSameLocalDay(timestamp: Long): Boolean {
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        return target.get(Calendar.ERA) == today.get(Calendar.ERA) &&
            target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}

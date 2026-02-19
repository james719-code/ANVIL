package com.james.anvil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.core.CombatManager
import com.james.anvil.core.CombatResult
import com.james.anvil.core.DamageSource
import com.james.anvil.core.LootResult
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.GearItem
import com.james.anvil.data.Monster
import com.james.anvil.data.MonsterLoot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DamageEvent(
    val damage: Int,
    val source: DamageSource,
    val isDefeated: Boolean
)

data class LootReveal(
    val coins: Int,
    val gearItem: GearItem?,
    val monsterName: String
)

@HiltViewModel
class CombatViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val combatManager = CombatManager(application)
    private val db = AnvilDatabase.getDatabase(application)
    private val gearDao = db.gearDao()

    private val _currentMonsterId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMonster: StateFlow<Monster?> = _currentMonsterId
        .flatMapLatest { id ->
            if (id != null) combatManager.observeMonster(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeMonsters = combatManager.observeActiveMonsters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val defeatedCount = combatManager.observeDefeatedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _damageEvents = MutableSharedFlow<DamageEvent>()
    val damageEvents = _damageEvents.asSharedFlow()

    private val _lootReveal = MutableStateFlow<LootReveal?>(null)
    val lootReveal: StateFlow<LootReveal?> = _lootReveal.asStateFlow()

    private val _combatLog = MutableStateFlow<List<String>>(emptyList())
    val combatLog: StateFlow<List<String>> = _combatLog.asStateFlow()

    fun setMonster(monsterId: Long) {
        _currentMonsterId.value = monsterId
        _combatLog.value = emptyList()
    }

    fun dealDamage(baseDamage: Int, source: DamageSource) {
        val monsterId = _currentMonsterId.value ?: return
        viewModelScope.launch {
            val result = combatManager.dealDamage(monsterId, baseDamage, source)
            if (result != null) {
                val sourceName = when (source) {
                    DamageSource.TASK -> "Task"
                    DamageSource.FOCUS -> "Focus"
                    DamageSource.QUIZ -> "Challenge"
                }
                addLog("$sourceName hit for ${result.damage} damage!")
                _damageEvents.emit(DamageEvent(result.damage, source, result.isDefeated))

                if (result.isDefeated) {
                    addLog("Monster defeated!")
                    loadLootReveal(monsterId)
                }
            }
        }
    }

    fun dealTaskDamage(hardnessLevel: Int) {
        viewModelScope.launch {
            val damage = combatManager.getTaskDamage(hardnessLevel)
            dealDamage(damage, DamageSource.TASK)
        }
    }

    fun dealFocusDamage(totalMinutes: Int) {
        viewModelScope.launch {
            val damage = combatManager.getFocusDamage(totalMinutes)
            dealDamage(damage, DamageSource.FOCUS)
        }
    }

    fun dealQuizDamage() {
        viewModelScope.launch {
            val damage = combatManager.getQuizDamage()
            dealDamage(damage, DamageSource.QUIZ)
        }
    }

    private suspend fun loadLootReveal(monsterId: Long) {
        val monster = combatManager.getMonsterById(monsterId) ?: return
        val lootList = combatManager.getLootForMonster(monsterId)
        val coins = lootList.filter { it.lootType == com.james.anvil.data.LootType.COINS }.sumOf { it.coinAmount }
        val gearLoot = lootList.firstOrNull { it.lootType == com.james.anvil.data.LootType.GEAR }
        val gearItem = gearLoot?.gearItemId?.let { gearDao.getById(it) }

        _lootReveal.value = LootReveal(coins, gearItem, monster.name)
    }

    fun dismissLoot() {
        _lootReveal.value = null
    }

    fun spawnMonsterForApp(packageName: String, appLabel: String, onSpawned: (Long) -> Unit) {
        viewModelScope.launch {
            val monster = combatManager.spawnMonsterForApp(packageName, appLabel)
            onSpawned(monster.id)
        }
    }

    private fun addLog(message: String) {
        _combatLog.value = (_combatLog.value + message).takeLast(20)
    }
}

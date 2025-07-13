package org.junction.catenin.utils

import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.random.Random

/**
 * Cross-platform random number utilities for consistent behavior
 * across JVM and JavaScript environments
 */
@JsExport
object GameRandom {
    private var instance: Random = Random.Default
    
    /**
     * Set a specific random instance for deterministic testing
     */
    fun setSeed(seed: Int) {
        instance = Random(seed)
    }
    
    /**
     * Reset to default random behavior
     */
    fun resetToDefault() {
        instance = Random.Default
    }
    
    /**
     * Generate random integer in range [min, max] inclusive
     */
    @JsName("nextIntRange")
    fun nextInt(min: Int, max: Int): Int {
        return instance.nextInt(min, max + 1)
    }
    
    /**
     * Generate random integer in range [0, until) exclusive
     */
    @JsName("nextIntUntil")
    fun nextInt(until: Int): Int {
        return instance.nextInt(until)
    }
    
    /**
     * Choose random element from array
     */
    @JsName("chooseFromArray")
    fun <T> choose(array: Array<T>): T? {
        return if (array.isEmpty()) null else array[nextInt(array.size)]
    }
    
    /**
     * Choose random element from list
     */
    @JsName("chooseFromList")
    fun <T> choose(list: List<T>): T? {
        return if (list.isEmpty()) null else list[nextInt(list.size)]
    }
    
    /**
     * Shuffle array in place and return it
     */
    @JsName("shuffleArray")
    fun <T> shuffle(array: Array<T>): Array<T> {
        for (i in array.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val temp = array[i]
            array[i] = array[j]
            array[j] = temp
        }
        return array
    }
    
    /**
     * Shuffle mutable list in place
     */
    @JsName("shuffleList")
    fun <T> shuffle(list: MutableList<T>): MutableList<T> {
        for (i in list.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val temp = list[i]
            list[i] = list[j]
            list[j] = temp
        }
        return list
    }
    
    /**
     * Generate a unique game ID using random numbers
     * Cross-platform alternative to System.currentTimeMillis()
     */
    fun generateGameId(): String {
        val timestamp = nextInt(1000000, 9999999) // 7-digit number
        val random = nextInt(1000, 9999) // 4-digit number
        return "game_${timestamp}_${random}"
    }
}
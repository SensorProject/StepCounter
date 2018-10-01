package com.example.timil.sensorproject.daos

import android.arch.persistence.room.*
import com.example.timil.sensorproject.entities.Score

@Dao
interface ScoreDao{
    @Query("SELECT * FROM score")
    fun getScore(): List<Score>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(score: Score)

    @Query("UPDATE score SET level = :level")
    fun updateLevel(level: Int)

    @Query("UPDATE score SET points = :points")
    fun updatePoints(points: Int)

    @Query("UPDATE score SET trophies = :trophies")
    fun updateTrophyCount(trophies: Int)

    @Update
    fun update(score: Score)
}
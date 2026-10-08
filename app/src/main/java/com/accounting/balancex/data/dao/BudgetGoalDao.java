package com.accounting.balancex.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.accounting.balancex.data.entity.BudgetGoalEntity;

import java.util.List;

@Dao
public interface BudgetGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertBudgetGoal(BudgetGoalEntity budgetGoal);

    @Update
    void updateBudgetGoal(BudgetGoalEntity budgetGoal);

    @Delete
    void deleteBudgetGoal(BudgetGoalEntity budgetGoal);

    @Query("DELETE FROM budget_goals WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM budget_goals")
    void deleteAll();

    @Query("SELECT * FROM budget_goals ORDER BY createdAt DESC")
    List<BudgetGoalEntity> getAll();

    @Query("SELECT * FROM budget_goals WHERE type = 'BUDGET' ORDER BY createdAt DESC")
    List<BudgetGoalEntity> getBudgets();

    @Query("SELECT * FROM budget_goals WHERE type = 'GOAL' ORDER BY createdAt DESC")
    List<BudgetGoalEntity> getGoals();

    @Query("SELECT * FROM budget_goals WHERE type = 'BUDGET' AND category = :category LIMIT 1")
    BudgetGoalEntity getBudgetByCategory(String category);

    @Query("SELECT COUNT(*) FROM budget_goals")
    int getCount();
}

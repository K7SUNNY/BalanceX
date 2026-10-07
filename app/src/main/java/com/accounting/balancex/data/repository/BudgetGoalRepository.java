package com.accounting.balancex.data.repository;

import android.content.Context;

import com.accounting.balancex.data.dao.BudgetGoalDao;
import com.accounting.balancex.data.db.AppDatabase;
import com.accounting.balancex.data.entity.BudgetGoalEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetGoalRepository {

    private final BudgetGoalDao budgetGoalDao;
    private final ExecutorService executorService;

    public BudgetGoalRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.budgetGoalDao = db.budgetGoalDao();
        this.executorService = Executors.newFixedThreadPool(4);

        android.content.SharedPreferences sp = context.getSharedPreferences("balancex_phase3_prefs", Context.MODE_PRIVATE);
        if (!sp.getBoolean("has_purged_phase3_goals_mock_v1", false)) {
            executorService.execute(() -> {
                List<BudgetGoalEntity> goals = budgetGoalDao.getAll();
                for (BudgetGoalEntity g : goals) {
                    if ("Dining & Outing".equalsIgnoreCase(g.title) ||
                        "Groceries & Household".equalsIgnoreCase(g.title) ||
                        "MacBook Pro Fund".equalsIgnoreCase(g.title)) {
                        budgetGoalDao.deleteBudgetGoal(g);
                    }
                }
                sp.edit().putBoolean("has_purged_phase3_goals_mock_v1", true).apply();
            });
        }
    }

    public interface OnBudgetGoalsLoaded {
        void onLoaded(List<BudgetGoalEntity> items);
    }

    public void insert(BudgetGoalEntity entity, Runnable onSuccess) {
        executorService.execute(() -> {
            budgetGoalDao.insertBudgetGoal(entity);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void update(BudgetGoalEntity entity, Runnable onSuccess) {
        executorService.execute(() -> {
            budgetGoalDao.updateBudgetGoal(entity);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void delete(long id, Runnable onSuccess) {
        executorService.execute(() -> {
            budgetGoalDao.deleteById(id);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void getAll(OnBudgetGoalsLoaded callback) {
        executorService.execute(() -> {
            List<BudgetGoalEntity> list = budgetGoalDao.getAll();
            if (callback != null) callback.onLoaded(list);
        });
    }

    public void getBudgets(OnBudgetGoalsLoaded callback) {
        executorService.execute(() -> {
            List<BudgetGoalEntity> list = budgetGoalDao.getBudgets();
            if (callback != null) callback.onLoaded(list);
        });
    }

    public void getGoals(OnBudgetGoalsLoaded callback) {
        executorService.execute(() -> {
            List<BudgetGoalEntity> list = budgetGoalDao.getGoals();
            if (callback != null) callback.onLoaded(list);
        });
    }
}

package com.accounting.balancex.data.repository;

import android.content.Context;

import com.accounting.balancex.data.dao.BudgetGoalDao;
import com.accounting.balancex.data.db.AppDatabase;
import com.accounting.balancex.data.entity.BudgetGoalEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetGoalRepository {

    private final BudgetGoalDao budgetGoalDao;
    private final ExecutorService executorService;

    public BudgetGoalRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.budgetGoalDao = db.budgetGoalDao();
        this.executorService = AppDatabase.databaseWriteExecutor;
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

    public void syncBudgetSpends(Map<String, Double> categoryExpenseMap, Runnable onComplete) {
        executorService.execute(() -> {
            List<BudgetGoalEntity> budgets = budgetGoalDao.getBudgets();
            if (budgets != null) {
                for (BudgetGoalEntity b : budgets) {
                    String catKey = (b.category != null ? b.category : b.title).trim().toLowerCase();
                    double spent = (categoryExpenseMap != null && categoryExpenseMap.containsKey(catKey))
                            ? categoryExpenseMap.get(catKey) : 0.0;
                    if (Math.abs(b.currentAmount - spent) > 0.001) {
                        b.currentAmount = spent;
                        budgetGoalDao.updateBudgetGoal(b);
                    }
                }
            }
            if (onComplete != null) onComplete.run();
        });
    }

    public void deleteAll(Runnable onSuccess) {
        executorService.execute(() -> {
            budgetGoalDao.deleteAll();
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void resetAllBudgetSpends(Runnable onSuccess) {
        executorService.execute(() -> {
            List<BudgetGoalEntity> budgets = budgetGoalDao.getBudgets();
            if (budgets != null) {
                for (BudgetGoalEntity b : budgets) {
                    b.currentAmount = 0.0;
                    budgetGoalDao.updateBudgetGoal(b);
                }
            }
            if (onSuccess != null) onSuccess.run();
        });
    }
}

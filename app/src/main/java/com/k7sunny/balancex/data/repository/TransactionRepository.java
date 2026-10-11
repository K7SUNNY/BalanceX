package com.k7sunny.balancex.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;

import com.k7sunny.balancex.data.dao.TransactionDao;
import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.data.model.BalanceSummary;
import com.k7sunny.balancex.data.model.CategorySummary;
import com.k7sunny.balancex.util.MoneyDateUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class TransactionRepository {

    private final AppDatabase database;
    private final TransactionDao transactionDao;
    private final ExecutorService executorService;

    public TransactionRepository(Context context) {
        this.database = AppDatabase.getDatabase(context);
        this.transactionDao = database.transactionDao();
        this.executorService = AppDatabase.databaseWriteExecutor;
    }

    private void normalizeEntity(TransactionEntity transaction) {
        if (transaction == null) return;
        if (transaction.amountMinor == 0 && transaction.amount != null && !transaction.amount.isEmpty()) {
            transaction.amountMinor = MoneyDateUtils.parseAmountToMinor(transaction.amount);
        }
        if (transaction.dateEpochDay == null && transaction.date != null && !transaction.date.isEmpty()) {
            transaction.dateEpochDay = MoneyDateUtils.parseDateToEpochDay(transaction.date);
            transaction.dateValid = (transaction.dateEpochDay != null) ? 1 : 0;
        }
    }

    public void insert(TransactionEntity transaction, Runnable onSuccess) {
        executorService.execute(() -> {
            normalizeEntity(transaction);
            long id = transactionDao.insert(transaction);
            transaction.entryId = id;
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void insertAll(List<TransactionEntity> transactions, Runnable onSuccess) {
        executorService.execute(() -> {
            if (transactions != null) {
                for (TransactionEntity t : transactions) {
                    normalizeEntity(t);
                }
                transactionDao.insertAll(transactions);
            }
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void insertOrReplaceAll(List<TransactionEntity> transactions, Runnable onSuccess) {
        executorService.execute(() -> {
            if (transactions != null) {
                for (TransactionEntity t : transactions) {
                    normalizeEntity(t);
                }
                transactionDao.insertOrReplaceAll(transactions);
            }
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void update(TransactionEntity transaction, Runnable onSuccess) {
        executorService.execute(() -> {
            normalizeEntity(transaction);
            transactionDao.update(transaction);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void delete(TransactionEntity transaction, Runnable onSuccess) {
        executorService.execute(() -> {
            transactionDao.delete(transaction);
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void getAllTransactions(OnDataLoaded callback) {
        executorService.execute(() -> {
            List<TransactionEntity> transactions = transactionDao.getAllTransactions();
            if (callback != null) callback.onDataLoaded(transactions);
        });
    }

    public void getTransactionById(long id, OnTransactionLoaded callback) {
        executorService.execute(() -> {
            TransactionEntity transaction = transactionDao.getTransactionById(id);
            if (callback != null) callback.onTransactionLoaded(transaction);
        });
    }

    public void getRecentPayees(int limit, OnPayeesLoaded callback) {
        executorService.execute(() -> {
            List<String> payees = transactionDao.getRecentPayees(limit);
            if (callback != null) callback.onPayeesLoaded(payees);
        });
    }

    public void deleteAll(Runnable onSuccess) {
        executorService.execute(() -> {
            transactionDao.deleteAll();
            if (onSuccess != null) onSuccess.run();
        });
    }

    /**
     * Executes atomic delete of transactions inside a single Room database transaction.
     */
    public void clearAllTransactionsAtomic(Runnable onComplete) {
        executorService.execute(() -> {
            database.runInTransaction(() -> {
                transactionDao.deleteAll();
            });
            if (onComplete != null) onComplete.run();
        });
    }

    // --- LiveData Observers ---

    public LiveData<List<TransactionEntity>> getAllTransactionsLive() {
        return transactionDao.getAllTransactionsLive();
    }

    public LiveData<List<TransactionEntity>> getUndatedTransactionsLive() {
        return transactionDao.getUndatedTransactionsLive();
    }

    public LiveData<List<TransactionEntity>> getTransactionsByDateRangeLive(long startEpochDay, long endEpochDay) {
        return transactionDao.getTransactionsByDateRangeLive(startEpochDay, endEpochDay);
    }

    public LiveData<BalanceSummary> getBalanceSummaryByDateRangeLive(long startEpochDay, long endEpochDay) {
        return transactionDao.getBalanceSummaryByDateRangeLive(startEpochDay, endEpochDay);
    }

    public LiveData<BalanceSummary> getBalanceSummaryAllTimeLive() {
        return transactionDao.getBalanceSummaryAllTimeLive();
    }

    public LiveData<List<CategorySummary>> getCategoryExpensesAllTimeLive() {
        return transactionDao.getCategoryExpensesAllTimeLive();
    }

    public LiveData<List<CategorySummary>> getCategoryExpensesByDateRangeLive(long startEpochDay, long endEpochDay) {
        return transactionDao.getCategoryExpensesByDateRangeLive(startEpochDay, endEpochDay);
    }

    public LiveData<Integer> getTransactionCountLive() {
        return transactionDao.getTransactionCountLive();
    }

    public void getTransactionCount(OnCountLoaded callback) {
        executorService.execute(() -> {
            int count = transactionDao.getTransactionCount();
            if (callback != null) callback.onCountLoaded(count);
        });
    }

    // --- Callbacks ---

    public interface OnDataLoaded {
        void onDataLoaded(List<TransactionEntity> transactions);
    }

    public interface OnTransactionLoaded {
        void onTransactionLoaded(TransactionEntity transaction);
    }

    public interface OnPayeesLoaded {
        void onPayeesLoaded(List<String> payees);
    }

    public interface OnCountLoaded {
        void onCountLoaded(int count);
    }
}

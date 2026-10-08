package com.accounting.balancex.data.repository;

import android.content.Context;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.accounting.balancex.data.db.AppDatabase;
import com.accounting.balancex.data.dao.TransactionDao;
import com.accounting.balancex.data.entity.TransactionEntity;

public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final ExecutorService executorService;

    public TransactionRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.transactionDao = db.transactionDao();
        this.executorService = AppDatabase.databaseWriteExecutor;
    }

    public void insert(TransactionEntity transaction, Runnable onSuccess) {
        executorService.execute(() -> {
            transactionDao.insert(transaction);
            if(onSuccess != null) onSuccess.run();
        });
    }

    public void insertAll(List<TransactionEntity> transactions, Runnable onSuccess) {
        executorService.execute(() -> {
            transactionDao.insertAll(transactions);
            if(onSuccess != null) onSuccess.run();
        });
    }

    public void getAllTransactions(OnDataLoaded callback) {
        executorService.execute(() -> {
            List<TransactionEntity> transactions = transactionDao.getAllTransactions();
            callback.onDataLoaded(transactions);
        });
    }

    public void deleteAll(Runnable onSuccess) {
        executorService.execute(() -> {
            transactionDao.deleteAll();
            if (onSuccess != null) onSuccess.run();
        });
    }

    public interface OnDataLoaded {
        void onDataLoaded(List<TransactionEntity> transactions);
    }
}

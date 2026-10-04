package com.accounting.balancex.data.repository;

import android.content.Context;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.accounting.balancex.data.db.AppDatabase;
import com.accounting.balancex.data.dao.TransactionDao;
import com.accounting.balancex.data.entity.TransactionEntity;

public class TransactionRepository {

    private TransactionDao transactionDao;
    private ExecutorService executorService;

    public TransactionRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        transactionDao = db.transactionDao();
        executorService = Executors.newFixedThreadPool(4);
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

    public interface OnDataLoaded {
        void onDataLoaded(List<TransactionEntity> transactions);
    }
}

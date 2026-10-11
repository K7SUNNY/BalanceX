package com.k7sunny.balancex.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.data.repository.TransactionRepository;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final LiveData<List<TransactionEntity>> allTransactions;
    private final LiveData<List<TransactionEntity>> undatedTransactions;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        this.repository = new TransactionRepository(application);
        this.allTransactions = repository.getAllTransactionsLive();
        this.undatedTransactions = repository.getUndatedTransactionsLive();
    }

    public LiveData<List<TransactionEntity>> getAllTransactions() {
        return allTransactions;
    }

    public LiveData<List<TransactionEntity>> getUndatedTransactions() {
        return undatedTransactions;
    }

    public void deleteTransaction(TransactionEntity transaction, Runnable onComplete) {
        repository.delete(transaction, onComplete);
    }

    public void insertTransaction(TransactionEntity transaction, Runnable onComplete) {
        repository.insert(transaction, onComplete);
    }
}

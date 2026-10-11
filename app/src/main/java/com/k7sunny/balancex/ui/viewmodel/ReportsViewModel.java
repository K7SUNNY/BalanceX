package com.k7sunny.balancex.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.data.model.BalanceSummary;
import com.k7sunny.balancex.data.model.CategorySummary;
import com.k7sunny.balancex.data.repository.TransactionRepository;

import java.util.List;

public class ReportsViewModel extends AndroidViewModel {
    private final TransactionRepository repository;

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new TransactionRepository(application);
    }

    public LiveData<List<TransactionEntity>> getAllTransactions() {
        return repository.getAllTransactionsLive();
    }

    public LiveData<List<TransactionEntity>> getTransactionsByDateRange(long startEpochDay, long endEpochDay) {
        return repository.getTransactionsByDateRangeLive(startEpochDay, endEpochDay);
    }

    public LiveData<BalanceSummary> getBalanceSummaryByDateRange(long startEpochDay, long endEpochDay) {
        return repository.getBalanceSummaryByDateRangeLive(startEpochDay, endEpochDay);
    }

    public LiveData<List<CategorySummary>> getCategoryExpensesByDateRange(long startEpochDay, long endEpochDay) {
        return repository.getCategoryExpensesByDateRangeLive(startEpochDay, endEpochDay);
    }

    public LiveData<List<TransactionEntity>> getUndatedTransactions() {
        return repository.getUndatedTransactionsLive();
    }
}

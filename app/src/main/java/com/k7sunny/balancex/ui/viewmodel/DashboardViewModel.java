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

public class DashboardViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final LiveData<List<TransactionEntity>> allTransactions;
    private final LiveData<BalanceSummary> balanceSummaryAllTime;
    private final LiveData<List<CategorySummary>> categoryExpensesAllTime;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        this.repository = new TransactionRepository(application);
        this.allTransactions = repository.getAllTransactionsLive();
        this.balanceSummaryAllTime = repository.getBalanceSummaryAllTimeLive();
        this.categoryExpensesAllTime = repository.getCategoryExpensesAllTimeLive();
    }

    public LiveData<List<TransactionEntity>> getAllTransactions() {
        return allTransactions;
    }

    public LiveData<BalanceSummary> getBalanceSummaryAllTime() {
        return balanceSummaryAllTime;
    }

    public LiveData<BalanceSummary> getBalanceSummaryByDateRange(long startEpochDay, long endEpochDay) {
        return repository.getBalanceSummaryByDateRangeLive(startEpochDay, endEpochDay);
    }

    public LiveData<List<CategorySummary>> getCategoryExpensesAllTime() {
        return categoryExpensesAllTime;
    }
}

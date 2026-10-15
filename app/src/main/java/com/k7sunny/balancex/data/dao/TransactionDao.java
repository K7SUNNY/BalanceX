package com.k7sunny.balancex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;

import androidx.lifecycle.LiveData;
import java.util.List;
import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.data.model.BalanceSummary;
import com.k7sunny.balancex.data.model.CategorySummary;

@Dao
public interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(TransactionEntity transaction);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertAll(List<TransactionEntity> transactions);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceAll(List<TransactionEntity> transactions);

    @Query("SELECT * FROM transactions ORDER BY CASE WHEN dateValid = 1 AND dateEpochDay IS NOT NULL THEN dateEpochDay ELSE -1 END DESC, entryId DESC")
    List<TransactionEntity> getAllTransactions();

    @Query("SELECT * FROM transactions ORDER BY CASE WHEN dateValid = 1 AND dateEpochDay IS NOT NULL THEN dateEpochDay ELSE -1 END DESC, entryId DESC")
    LiveData<List<TransactionEntity>> getAllTransactionsLive();

    @Query("SELECT * FROM transactions WHERE dateValid = 0 ORDER BY entryId DESC")
    List<TransactionEntity> getUndatedTransactions();

    @Query("SELECT * FROM transactions WHERE dateValid = 0 ORDER BY entryId DESC")
    LiveData<List<TransactionEntity>> getUndatedTransactionsLive();

    @Query("SELECT * FROM transactions WHERE dateValid = 1 AND dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay ORDER BY dateEpochDay DESC, entryId DESC")
    List<TransactionEntity> getTransactionsByDateRange(long startEpochDay, long endEpochDay);

    @Query("SELECT * FROM transactions WHERE dateValid = 1 AND dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay ORDER BY dateEpochDay DESC, entryId DESC")
    LiveData<List<TransactionEntity>> getTransactionsByDateRangeLive(long startEpochDay, long endEpochDay);

    @Query("SELECT * FROM transactions WHERE entryId = :id")
    TransactionEntity getTransactionById(long id);

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE receiver = :receiver AND date = :date AND description = :description LIMIT 1)")
    boolean hasAutoBilledTransaction(String receiver, String date, String description);

    @Query("SELECT COALESCE(SUM(CASE WHEN textType = 'Credit' THEN amountMinor ELSE 0 END), 0) AS totalCreditMinor, " +
           "COALESCE(SUM(CASE WHEN textType = 'Debit' THEN amountMinor ELSE 0 END), 0) AS totalDebitMinor " +
           "FROM transactions WHERE dateValid = 1 AND dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay")
    LiveData<BalanceSummary> getBalanceSummaryByDateRangeLive(long startEpochDay, long endEpochDay);

    @Query("SELECT COALESCE(SUM(CASE WHEN textType = 'Credit' THEN amountMinor ELSE 0 END), 0) AS totalCreditMinor, " +
           "COALESCE(SUM(CASE WHEN textType = 'Debit' THEN amountMinor ELSE 0 END), 0) AS totalDebitMinor " +
           "FROM transactions WHERE dateValid = 1 AND dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay")
    BalanceSummary getBalanceSummaryByDateRange(long startEpochDay, long endEpochDay);

    @Query("SELECT COALESCE(SUM(CASE WHEN textType = 'Credit' THEN amountMinor ELSE 0 END), 0) AS totalCreditMinor, " +
           "COALESCE(SUM(CASE WHEN textType = 'Debit' THEN amountMinor ELSE 0 END), 0) AS totalDebitMinor " +
           "FROM transactions")
    LiveData<BalanceSummary> getBalanceSummaryAllTimeLive();

    @Query("SELECT COALESCE(SUM(CASE WHEN textType = 'Credit' THEN amountMinor ELSE 0 END), 0) AS totalCreditMinor, " +
           "COALESCE(SUM(CASE WHEN textType = 'Debit' THEN amountMinor ELSE 0 END), 0) AS totalDebitMinor " +
           "FROM transactions")
    BalanceSummary getBalanceSummaryAllTime();

    @Query("SELECT category, COALESCE(SUM(amountMinor), 0) AS totalAmountMinor FROM transactions " +
           "WHERE textType = 'Debit' GROUP BY category ORDER BY totalAmountMinor DESC")
    LiveData<List<CategorySummary>> getCategoryExpensesAllTimeLive();

    @Query("SELECT category, COALESCE(SUM(amountMinor), 0) AS totalAmountMinor FROM transactions " +
           "WHERE textType = 'Debit' GROUP BY category ORDER BY totalAmountMinor DESC")
    List<CategorySummary> getCategoryExpensesAllTime();

    @Query("SELECT category, COALESCE(SUM(amountMinor), 0) AS totalAmountMinor FROM transactions " +
           "WHERE textType = 'Debit' AND dateValid = 1 AND dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay " +
           "GROUP BY category ORDER BY totalAmountMinor DESC")
    LiveData<List<CategorySummary>> getCategoryExpensesByDateRangeLive(long startEpochDay, long endEpochDay);

    @Query("SELECT category, COALESCE(SUM(amountMinor), 0) AS totalAmountMinor FROM transactions " +
           "WHERE textType = 'Debit' AND dateValid = 1 AND dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay " +
           "GROUP BY category ORDER BY totalAmountMinor DESC")
    List<CategorySummary> getCategoryExpensesByDateRange(long startEpochDay, long endEpochDay);

    @Query("SELECT COUNT(*) FROM transactions")
    int getTransactionCount();

    @Query("SELECT COUNT(*) FROM transactions")
    LiveData<Integer> getTransactionCountLive();

    @Query("SELECT DISTINCT receiver FROM transactions WHERE receiver IS NOT NULL AND receiver != '' AND receiver != 'Unknown' AND receiver != 'N/A' LIMIT :limit")
    List<String> getRecentPayees(int limit);

    @Query("SELECT * FROM transactions WHERE subscriptionId = :subscriptionId AND occurrenceDate = :occurrenceDate LIMIT 1")
    TransactionEntity getTransactionForSubscriptionOccurrence(long subscriptionId, String occurrenceDate);

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE subscriptionId = :subscriptionId AND occurrenceDate = :occurrenceDate LIMIT 1)")
    boolean hasSubscriptionOccurrence(long subscriptionId, String occurrenceDate);

    @Query("UPDATE transactions SET subscriptionId = NULL WHERE subscriptionId = :subscriptionId")
    void clearSubscriptionLinks(long subscriptionId);

    @Update
    void update(TransactionEntity transaction);

    @Delete
    void delete(TransactionEntity transaction);

    @Query("DELETE FROM transactions")
    void deleteAll();
}

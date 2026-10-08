package com.k7sunny.balancex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;

import java.util.List;
import com.k7sunny.balancex.data.entity.TransactionEntity;

@Dao
public interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TransactionEntity transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TransactionEntity> transactions);

    @Query("SELECT * FROM transactions ORDER BY entryId DESC")
    List<TransactionEntity> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE entryId = :id")
    TransactionEntity getTransactionById(long id);

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE receiver = :receiver AND date = :date AND description = :description LIMIT 1)")
    boolean hasAutoBilledTransaction(String receiver, String date, String description);

    @Update
    void update(TransactionEntity transaction);

    @Delete
    void delete(TransactionEntity transaction);

    @Query("DELETE FROM transactions")
    void deleteAll();
}

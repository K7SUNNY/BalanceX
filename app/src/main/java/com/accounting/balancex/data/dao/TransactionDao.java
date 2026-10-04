package com.accounting.balancex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;

import java.util.List;
import com.accounting.balancex.data.entity.TransactionEntity;

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

    @Update
    void update(TransactionEntity transaction);

    @Delete
    void delete(TransactionEntity transaction);
}

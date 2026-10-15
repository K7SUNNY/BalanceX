package com.k7sunny.balancex.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.k7sunny.balancex.data.dao.ReminderLogDao;
import com.k7sunny.balancex.data.dao.SubscriptionDao;
import com.k7sunny.balancex.data.dao.SubscriptionPriceHistoryDao;
import com.k7sunny.balancex.data.dao.TransactionDao;
import com.k7sunny.balancex.data.db.AppDatabase;
import com.k7sunny.balancex.data.entity.ReminderLogEntity;
import com.k7sunny.balancex.data.entity.SubscriptionEntity;
import com.k7sunny.balancex.data.entity.SubscriptionPriceHistoryEntity;
import com.k7sunny.balancex.data.entity.TransactionEntity;
import com.k7sunny.balancex.finance.RecurrenceCalculator;
import com.k7sunny.balancex.util.MoneyDateUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class SubscriptionRepository {

    private static final String TAG = "SubscriptionRepository";

    private final AppDatabase db;
    private final SubscriptionDao subscriptionDao;
    private final TransactionDao transactionDao;
    private final SubscriptionPriceHistoryDao priceHistoryDao;
    private final ReminderLogDao reminderLogDao;
    private final ExecutorService executorService;

    public SubscriptionRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.db = AppDatabase.getDatabase(appContext);
        this.subscriptionDao = db.subscriptionDao();
        this.transactionDao = db.transactionDao();
        this.priceHistoryDao = db.subscriptionPriceHistoryDao();
        this.reminderLogDao = db.reminderLogDao();
        this.executorService = AppDatabase.databaseWriteExecutor;
    }

    public interface OnSubscriptionsLoaded {
        void onLoaded(List<SubscriptionEntity> subscriptions);
    }

    public interface OnPriceHistoryLoaded {
        void onLoaded(List<SubscriptionPriceHistoryEntity> history);
    }

    public void insert(SubscriptionEntity subscription, Runnable onSuccess) {
        executorService.execute(() -> {
            if (subscription.amountMinor <= 0 && subscription.amount > 0) {
                subscription.amountMinor = Math.round(subscription.amount * 100.0);
            }
            if (subscription.anchorDate == null || subscription.anchorDate.isEmpty()) {
                subscription.anchorDate = subscription.nextDueDate;
            }
            if (subscription.customInterval <= 0) {
                subscription.customInterval = 1;
            }
            long id = subscriptionDao.insertSubscription(subscription);
            subscription.id = id;

            // Record initial price history
            SubscriptionPriceHistoryEntity initialPrice = new SubscriptionPriceHistoryEntity(
                    id,
                    subscription.anchorDate != null ? subscription.anchorDate : LocalDate.now().toString(),
                    subscription.amountMinor,
                    subscription.amount
            );
            priceHistoryDao.insert(initialPrice);

            if (onSuccess != null) onSuccess.run();
        });
    }

    public void update(SubscriptionEntity subscription, Runnable onSuccess) {
        executorService.execute(() -> {
            if (subscription.amountMinor <= 0 && subscription.amount > 0) {
                subscription.amountMinor = Math.round(subscription.amount * 100.0);
            }
            subscriptionDao.updateSubscription(subscription);
            if (onSuccess != null) onSuccess.run();
        });
    }

    /**
     * Deletes a subscription while preserving all past ledger transactions.
     * The foreign key / link on transactions is safely set to NULL.
     */
    public void delete(long id, Runnable onSuccess) {
        executorService.execute(() -> {
            db.runInTransaction(() -> {
                // Soft disconnect existing transactions to guarantee 0 data loss
                transactionDao.clearSubscriptionLinks(id);
                priceHistoryDao.deleteBySubscriptionId(id);
                reminderLogDao.deleteBySubscriptionId(id);
                subscriptionDao.deleteById(id);
            });
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void getAllSubscriptions(OnSubscriptionsLoaded callback) {
        executorService.execute(() -> {
            List<SubscriptionEntity> list = subscriptionDao.getAllSubscriptions();
            if (callback != null) callback.onLoaded(list);
        });
    }

    public void getActiveSubscriptions(OnSubscriptionsLoaded callback) {
        executorService.execute(() -> {
            List<SubscriptionEntity> list = subscriptionDao.getActiveSubscriptions();
            if (callback != null) callback.onLoaded(list);
        });
    }

    public void getPriceHistory(long subscriptionId, OnPriceHistoryLoaded callback) {
        executorService.execute(() -> {
            List<SubscriptionPriceHistoryEntity> list = priceHistoryDao.getHistoryForSubscription(subscriptionId);
            if (callback != null) callback.onLoaded(list);
        });
    }

    /**
     * Marks a subscription as paid for the specified occurrence date.
     * Idempotently creates a Debit transaction, advances nextDueDate,
     * updates lastPaidDate, and clears/logs reminder entries.
     */
    public void markAsPaid(long subscriptionId, @Nullable String occurrenceDate, @Nullable String paymentMethod, @Nullable Runnable onSuccess) {
        executorService.execute(() -> {
            db.runInTransaction(() -> {
                SubscriptionEntity sub = subscriptionDao.getSubscriptionById(subscriptionId);
                if (sub == null) return;

                String paymentDate = (occurrenceDate != null && !occurrenceDate.trim().isEmpty())
                        ? occurrenceDate.trim()
                        : (sub.nextDueDate != null ? sub.nextDueDate.trim() : LocalDate.now().toString());

                // Idempotent insertion check: only insert if not already recorded
                boolean alreadyBilled = transactionDao.hasSubscriptionOccurrence(sub.id, paymentDate);
                if (!alreadyBilled) {
                    TransactionEntity tx = new TransactionEntity();
                    tx.date = paymentDate;
                    tx.amount = String.format(Locale.ROOT, "%.2f", sub.amount);
                    tx.amountMinor = sub.amountMinor > 0 ? sub.amountMinor : Math.round(sub.amount * 100.0);
                    tx.dateEpochDay = MoneyDateUtils.parseDateToEpochDay(paymentDate);
                    tx.dateValid = (tx.dateEpochDay != null) ? 1 : 0;
                    tx.receiver = sub.name;
                    tx.description = "Recurring: " + sub.name;
                    tx.category = (sub.category != null && !sub.category.trim().isEmpty()) ? sub.category : "Subscription";
                    tx.paymentMethod = (paymentMethod != null && !paymentMethod.trim().isEmpty())
                            ? paymentMethod
                            : (sub.paymentMethod != null ? sub.paymentMethod : "UPI");
                    tx.textType = "Debit";
                    tx.subscriptionId = sub.id;
                    tx.occurrenceDate = paymentDate;

                    try {
                        transactionDao.insert(tx);
                    } catch (Exception e) {
                        Log.w(TAG, "Transaction already exists for subscription " + sub.id + " date " + paymentDate, e);
                    }
                }

                // Advance nextDueDate from anchor
                try {
                    LocalDate anchor = LocalDate.parse(sub.anchorDate != null ? sub.anchorDate : sub.nextDueDate);
                    RecurrenceCalculator.Cycle cycle = RecurrenceCalculator.Cycle.fromString(sub.billingCycle);
                    LocalDate paidDate = LocalDate.parse(paymentDate);

                    LocalDate next = RecurrenceCalculator.nextOccurrenceAfter(anchor, cycle, sub.customInterval, paidDate);
                    sub.nextDueDate = next.toString();
                    sub.lastPaidDate = paymentDate;

                    // If past endDate, deactivate
                    if (sub.endDate != null && !sub.endDate.trim().isEmpty()) {
                        LocalDate end = LocalDate.parse(sub.endDate.trim());
                        if (next.isAfter(end)) {
                            sub.isActive = false;
                        }
                    }

                    subscriptionDao.updateSubscription(sub);

                    // Clear any pending alerts for this occurrence and log as PAID
                    reminderLogDao.deleteForOccurrence(sub.id, paymentDate);
                    reminderLogDao.insert(new ReminderLogEntity(sub.id, paymentDate, "PAID"));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to compute next recurrence after payment for " + sub.name, e);
                }
            });

            if (onSuccess != null) onSuccess.run();
        });
    }

    /**
     * Skips the current billing cycle without logging a ledger transaction.
     */
    public void skipCycle(long subscriptionId, @Nullable Runnable onSuccess) {
        executorService.execute(() -> {
            db.runInTransaction(() -> {
                SubscriptionEntity sub = subscriptionDao.getSubscriptionById(subscriptionId);
                if (sub == null || sub.nextDueDate == null) return;

                try {
                    LocalDate anchor = LocalDate.parse(sub.anchorDate != null ? sub.anchorDate : sub.nextDueDate);
                    RecurrenceCalculator.Cycle cycle = RecurrenceCalculator.Cycle.fromString(sub.billingCycle);
                    LocalDate currentDue = LocalDate.parse(sub.nextDueDate);

                    LocalDate next = RecurrenceCalculator.nextOccurrenceAfter(anchor, cycle, sub.customInterval, currentDue);
                    sub.nextDueDate = next.toString();
                    subscriptionDao.updateSubscription(sub);

                    reminderLogDao.deleteForOccurrence(sub.id, currentDue.toString());
                    reminderLogDao.insert(new ReminderLogEntity(sub.id, currentDue.toString(), "SKIPPED"));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to skip cycle for " + sub.name, e);
                }
            });

            if (onSuccess != null) onSuccess.run();
        });
    }

    /**
     * Pauses the subscription until the specified horizon date.
     */
    public void pauseSubscription(long subscriptionId, String pauseUntilDate, @Nullable Runnable onSuccess) {
        executorService.execute(() -> {
            subscriptionDao.updatePausedUntil(subscriptionId, pauseUntilDate);
            if (onSuccess != null) onSuccess.run();
        });
    }

    /**
     * Resumes a paused subscription.
     */
    public void resumeSubscription(long subscriptionId, @Nullable Runnable onSuccess) {
        executorService.execute(() -> {
            SubscriptionEntity sub = subscriptionDao.getSubscriptionById(subscriptionId);
            if (sub != null) {
                sub.pausedUntil = null;
                // If nextDueDate is in the past, align it to today or future occurrence
                try {
                    LocalDate today = LocalDate.now();
                    LocalDate due = LocalDate.parse(sub.nextDueDate);
                    if (due.isBefore(today)) {
                        LocalDate anchor = LocalDate.parse(sub.anchorDate != null ? sub.anchorDate : sub.nextDueDate);
                        RecurrenceCalculator.Cycle cycle = RecurrenceCalculator.Cycle.fromString(sub.billingCycle);
                        sub.nextDueDate = RecurrenceCalculator.nextOccurrenceAfter(anchor, cycle, sub.customInterval, today.minusDays(1)).toString();
                    }
                } catch (Exception ignored) {}
                subscriptionDao.updateSubscription(sub);
            }
            if (onSuccess != null) onSuccess.run();
        });
    }

    /**
     * Updates subscription price and appends an entry to price history.
     */
    public void updatePrice(long subscriptionId, double newAmount, String effectiveDate, @Nullable Runnable onSuccess) {
        executorService.execute(() -> {
            db.runInTransaction(() -> {
                SubscriptionEntity sub = subscriptionDao.getSubscriptionById(subscriptionId);
                if (sub == null) return;

                sub.amount = newAmount;
                sub.amountMinor = Math.round(newAmount * 100.0);
                subscriptionDao.updateSubscription(sub);

                SubscriptionPriceHistoryEntity history = new SubscriptionPriceHistoryEntity(
                        sub.id,
                        (effectiveDate != null && !effectiveDate.isEmpty()) ? effectiveDate : LocalDate.now().toString(),
                        sub.amountMinor,
                        sub.amount
                );
                priceHistoryDao.insert(history);
            });

            if (onSuccess != null) onSuccess.run();
        });
    }

    public void deleteAll(Runnable onSuccess) {
        executorService.execute(() -> {
            db.runInTransaction(() -> {
                transactionDao.clearSubscriptionLinks(0); // Optional, clears any remaining links
                priceHistoryDao.deleteBySubscriptionId(0);
                reminderLogDao.deleteBySubscriptionId(0);
                subscriptionDao.deleteAll();
            });
            if (onSuccess != null) onSuccess.run();
        });
    }
}

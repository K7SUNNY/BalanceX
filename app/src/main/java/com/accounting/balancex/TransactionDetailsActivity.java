package com.accounting.balancex;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TransactionDetailsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private TransactionDetailsAdapter adapter;
    private List<Transaction> personTransactions;
    private String receiverName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_details);

        recyclerView = findViewById(R.id.recyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        receiverName = getIntent().getStringExtra("receiverName");
        personTransactions = new ArrayList<>();
        
        // Setup Toolbar
        toolbar.setTitle("Transactions with " + (receiverName != null ? receiverName : ""));
        toolbar.setNavigationOnClickListener(v -> finish());

        // Prepare Adapter early
        adapter = new TransactionDetailsAdapter(this, personTransactions);
        recyclerView.setAdapter(adapter);

        loadTransactionsForReceiver();
    }

    private void loadTransactionsForReceiver() {
        String transactionsJson = getIntent().getStringExtra("transactions_json");

        if (transactionsJson == null || transactionsJson.isEmpty()) {
            handleEmptyState();
            return;
        }

        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Transaction>>() {}.getType();
            List<Transaction> loaded = gson.fromJson(transactionsJson, listType);
            
            if (loaded != null) {
                personTransactions.clear();
                personTransactions.addAll(loaded);
            }
            
            adapter.notifyDataSetChanged();
            handleEmptyState();

        } catch (Exception e) {
            e.printStackTrace();
            handleEmptyState();
        }
    }
    
    private void handleEmptyState() {
        if (personTransactions.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
}

package com.accounting.balancex;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class    TransactionDetailsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Transaction> personTransactions;
    private String receiverName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_details);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        receiverName = getIntent().getStringExtra("receiverName");
        personTransactions = new ArrayList<>();

        loadTransactionsForReceiver();

        adapter = new TransactionAdapter(this, personTransactions);
        recyclerView.setAdapter(adapter);

        TextView title = findViewById(R.id.title);
        title.setText("Transactions with " + receiverName);
    }

    private void loadTransactionsForReceiver() {
        String transactionsJson = getIntent().getStringExtra("transactions_json");

        if (transactionsJson == null || transactionsJson.isEmpty()) {
            return;
        }

        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Transaction>>() {}.getType();
            personTransactions = gson.fromJson(transactionsJson, listType);

            // Show "No transactions" if the list is empty
            TextView noDataText = findViewById(R.id.noDataText);
            if (personTransactions.isEmpty()) {
                noDataText.setVisibility(View.VISIBLE);
            } else {
                noDataText.setVisibility(View.GONE);
            }

            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

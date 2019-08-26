package co.raisense.bluetoothdemo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

import co.raisense.bluetoothdemo.adapter.DataAdapter;

public class InfoActivity extends AppCompatActivity {
    private DBHelper db;
    private RecyclerView recyclerView;
    private DataAdapter adapter;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        db = new DBHelper(this);
        ArrayList<HashMap<String, String>> list = db.getAllData();

        adapter = new DataAdapter(list);
        layoutManager = new LinearLayoutManager(this);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }
}

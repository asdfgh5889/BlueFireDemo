package co.raisense.bluetoothdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import co.raisense.bluetoothdemo.adapter.DataAdapter;

public class InfoActivity extends AppCompatActivity {

    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        db = new DBHelper(this);
        ArrayList<HashMap<String, String>> list = db.getAllData();

        DataAdapter adapter = new DataAdapter(list);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);
    }
}

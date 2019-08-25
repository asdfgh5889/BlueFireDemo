package co.raisense.bluetoothdemo.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import co.raisense.bluetoothdemo.R;

import static co.raisense.bluetoothdemo.Contants.ACCEL_PEDAL;
import static co.raisense.bluetoothdemo.Contants.DRIVER_TORQUE;
import static co.raisense.bluetoothdemo.Contants.GPS;
import static co.raisense.bluetoothdemo.Contants.PCT_LOAD;
import static co.raisense.bluetoothdemo.Contants.PCT_TORQUE;
import static co.raisense.bluetoothdemo.Contants.RPM;
import static co.raisense.bluetoothdemo.Contants.SPEED;
import static co.raisense.bluetoothdemo.Contants.TIME;
import static co.raisense.bluetoothdemo.Contants.TORQUE_MODE;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.DataViewHolder>{

    private ArrayList<HashMap<String, String>> data;

    public DataAdapter(ArrayList<HashMap<String, String>> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public DataViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list, viewGroup, false);
        return new DataViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataViewHolder dataViewHolder, int i) {
        dataViewHolder.setData(data.get(i));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class DataViewHolder extends RecyclerView.ViewHolder{

        private TextView time;

        private TextView dataView1;
        private TextView dataView2;
        private TextView dataView3;
        private TextView dataView4;
        private TextView dataView5;
        private TextView dataView6;
        private TextView dataView7;
        private TextView dataView8;
        private TextView dataView9;

        private TextView textView1;
        private TextView textView2;
        private TextView textView3;
        private TextView textView4;
        private TextView textView5;
        private TextView textView6;
        private TextView textView7;
        private TextView textView8;
        private TextView textView9;

        DataViewHolder(@NonNull View itemView) {
            super(itemView);
            init(itemView);
        }

        private void init(View itemView) {
            time = itemView.findViewById(R.id.txt_time);

            dataView3 = itemView.findViewById(R.id.txt_dataLabel1);
            dataView1 = itemView.findViewById(R.id.txt_dataLabel2);
            dataView2 = itemView.findViewById(R.id.txt_dataLabel3);
            dataView4 = itemView.findViewById(R.id.txt_dataLabel4);
            dataView5 = itemView.findViewById(R.id.txt_dataLabel5);
            dataView6 = itemView.findViewById(R.id.txt_dataLabel6);
            dataView7 = itemView.findViewById(R.id.txt_dataLabel7);
            dataView8 = itemView.findViewById(R.id.txt_dataLabel8);
            dataView9 = itemView.findViewById(R.id.txt_dataLabel9);
            textView1 = itemView.findViewById(R.id.txt_dataText1);
            textView2 = itemView.findViewById(R.id.txt_dataText2);
            textView3 = itemView.findViewById(R.id.txt_dataText3);
            textView4 = itemView.findViewById(R.id.txt_dataText4);
            textView5 = itemView.findViewById(R.id.txt_dataText5);
            textView6 = itemView.findViewById(R.id.txt_dataText6);
            textView7 = itemView.findViewById(R.id.txt_dataText7);
            textView8 = itemView.findViewById(R.id.txt_dataText8);
            textView9 = itemView.findViewById(R.id.txt_dataText9);
        }

        void setData(HashMap<String, String> data){
            time.setText(TIME);

            dataView1.setText("RPM");
            dataView2.setText("Speed");
            dataView3.setText("Accel Pedal");
            dataView4.setText("Pct Load");
            dataView5.setText("Pct Torque");
            dataView6.setText("Driver Torque");
            dataView7.setText("Torque Mode");
            dataView8.setText("Longitude");
            dataView9.setText("Latitude");

            textView1.setText(data.get(RPM));
            textView2.setText(data.get(SPEED));
            textView3.setText(data.get(ACCEL_PEDAL));
            textView4.setText(data.get(PCT_LOAD));
            textView5.setText(data.get(PCT_TORQUE));
            textView6.setText(data.get(DRIVER_TORQUE));
            textView7.setText(data.get(TORQUE_MODE));
            String[] gps_data = data.get(GPS).split(" ");
            textView8.setText(gps_data[0]);
            textView9.setText(gps_data[1]);
        }
    }
}

package co.raisense.bluetoothdemo;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

import static co.raisense.bluetoothdemo.Contants.ACCEL_PEDAL;
import static co.raisense.bluetoothdemo.Contants.DATABASE_NAME;
import static co.raisense.bluetoothdemo.Contants.DRIVER_TORQUE;
import static co.raisense.bluetoothdemo.Contants.GPS;
import static co.raisense.bluetoothdemo.Contants.PCT_LOAD;
import static co.raisense.bluetoothdemo.Contants.PCT_TORQUE;
import static co.raisense.bluetoothdemo.Contants.RPM;
import static co.raisense.bluetoothdemo.Contants.SPEED;
import static co.raisense.bluetoothdemo.Contants.TABLE_NAME;
import static co.raisense.bluetoothdemo.Contants.TIME;
import static co.raisense.bluetoothdemo.Contants.TORQUE_MODE;

public class DBHelper extends SQLiteOpenHelper {

    SQLiteDatabase db = null;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table data " +
                        "(time text, RPM text, Speed text, AccelPedal text, PctLoad text, PctTorque text, DriverTorque text, TorqueMode text, gps text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void inserData(String time, String rpm, String speed, String accelpedal, String pctload, String pcttorque, String drivertorque, String torquemode, String gps){
        db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TIME, time);
        contentValues.put(RPM, rpm);
        contentValues.put(SPEED, speed);
        contentValues.put(ACCEL_PEDAL, accelpedal);
        contentValues.put(PCT_LOAD, pctload);
        contentValues.put(PCT_TORQUE, pcttorque);
        contentValues.put(DRIVER_TORQUE, drivertorque);
        contentValues.put(TORQUE_MODE, torquemode);
        contentValues.put(GPS, gps);
        db.insert(TABLE_NAME, null, contentValues);
    }

    public int numOfRows(){
        db = this.getWritableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME);
    }

    public void deleteAllData(){
        db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.close();
    }

    @SuppressLint("Recycle")
    public ArrayList<HashMap<String, String>> getAllData() {
        ArrayList<HashMap<String, String>> array_list = new ArrayList<>();

        db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME, null);
        res.moveToFirst();

        if (res.moveToFirst()) {
            do {
                HashMap<String, String> data = new HashMap<>();
                data.put(TIME, res.getString(res.getColumnIndex(TIME)));
                data.put(RPM, res.getString(res.getColumnIndex(RPM)));
                data.put(SPEED, res.getString(res.getColumnIndex(SPEED)));
                data.put(ACCEL_PEDAL, res.getString(res.getColumnIndex(ACCEL_PEDAL)));
                data.put(PCT_LOAD, res.getString(res.getColumnIndex(PCT_LOAD)));
                data.put(PCT_TORQUE, res.getString(res.getColumnIndex(PCT_TORQUE)));
                data.put(DRIVER_TORQUE, res.getString(res.getColumnIndex(DRIVER_TORQUE)));
                data.put(TORQUE_MODE, res.getString(res.getColumnIndex(TORQUE_MODE)));
                data.put(GPS, res.getString(res.getColumnIndex(GPS)));

                array_list.add(data);
            } while (res.moveToNext());
        }
        return array_list;
    }
}

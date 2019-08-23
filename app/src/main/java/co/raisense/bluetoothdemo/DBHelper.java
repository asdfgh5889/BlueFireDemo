package co.raisense.bluetoothdemo;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "enginedata.db";
    private static final String TABLE_NAME = "data";
    private static final String TIME = "time";
    private static final String RPM = "RPM";
    private static final String SPEED = "Speed";
    private static final String ACCEL_PEDAL = "AccelPedal";
    private static final String PCT_LOAD = "PctLoad";
    private static final String PCT_TORQUE = "PctTorque";
    private static final String DRIVER_TORQUE = "DriverTorque";
    private static final String TORQUE_MODE = "TorqueMode";
    private static final String GPS = "gps";

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
        SQLiteDatabase db = this.getWritableDatabase();
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
        SQLiteDatabase db = this.getWritableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME);
    }

    public void deleteAllData(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.close();
    }

    @SuppressLint("Recycle")
    public ArrayList<ArrayList<String>> getAllData(){
        ArrayList<ArrayList<String>> array_list = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME, null);
        res.moveToFirst();

        while (!res.isAfterLast()){
            ArrayList<String> row  = new ArrayList<>();
            row.add(res.getString(res.getColumnIndex(TIME)));
            row.add(res.getString(res.getColumnIndex(RPM)));
            row.add(res.getString(res.getColumnIndex(SPEED)));
            row.add(res.getString(res.getColumnIndex(ACCEL_PEDAL)));
            row.add(res.getString(res.getColumnIndex(PCT_LOAD)));
            row.add(res.getString(res.getColumnIndex(PCT_TORQUE)));
            row.add(res.getString(res.getColumnIndex(DRIVER_TORQUE)));
            row.add(res.getString(res.getColumnIndex(TORQUE_MODE)));
            row.add(res.getString(res.getColumnIndex(GPS)));
            array_list.add(row);
        }
        return array_list;
    }
}

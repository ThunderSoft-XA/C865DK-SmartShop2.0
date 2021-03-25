package com.thundercomm.eBox.Data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class AgeGenderHelper extends SQLiteOpenHelper{

    public static String CREATE_TABLE = "create table "+ DatabaseStatic.TABLE_NAME +"(" +
            DatabaseStatic.ID+" integer primary key autoincrement,"+
            DatabaseStatic.M_0_9 + " Integer not null, " +
            DatabaseStatic.F_0_9 + " Integer not null, " +
            DatabaseStatic.M_10_19 + " Integer not null, " +
            DatabaseStatic.F_10_19 + " Integer not null, " +
            DatabaseStatic.M_20_29 + " Integer not null, " +
            DatabaseStatic.F_20_29 + " Integer not null, " +
            DatabaseStatic.M_30_39 + " Integer not null, " +
            DatabaseStatic.F_30_39 + " Integer not null, " +
            DatabaseStatic.M_40_49 + " Integer not null, " +
            DatabaseStatic.F_40_49 + " Integer not null, " +
            DatabaseStatic.M_50_59 + " Integer not null, " +
            DatabaseStatic.F_50_59 + " Integer not null, " +
            DatabaseStatic.M_60_69 + " Integer not null, " +
            DatabaseStatic.F_60_69 + " Integer not null, " +
            DatabaseStatic.M_70_79 + " Integer not null, " +
            DatabaseStatic.F_70_79 + " Integer not null, " +
            DatabaseStatic.M_80_89 + " Integer not null, " +
            DatabaseStatic.F_80_89 + " Integer not null, " +
            DatabaseStatic.M_90_100 + " Integer not null, " +
            DatabaseStatic.F_90_100 + " Integer not null, " +
            DatabaseStatic.Date + " text"
        + ")";

    private Context mContext = null;

    public AgeGenderHelper(Context context, String name,
            CursorFactory factory, int version) {
        super(context, DatabaseStatic.DATABASE_NAME, null, DatabaseStatic.DATABASE_VERSION);
    }

    public AgeGenderHelper(Context context)
    {
        super(context, DatabaseStatic.DATABASE_NAME, null, DatabaseStatic.DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
    }
}

package com.thundercomm.eBox.Activity;

import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.thundercomm.eBox.Data.AgeGenderHelper;
import com.thundercomm.eBox.Data.DatabaseStatic;
import com.thundercomm.eBox.R;

public class StatisticsActivity extends AppCompatActivity {

    private AgeGenderHelper mAgeGenderHelper;
    private SQLiteDatabase database = null;
    private Spinner dateSpinner;
    private List<String> data_list;
    private ArrayAdapter<String> arr_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        dateSpinner = (Spinner) findViewById(R.id.date_spinner);
        data_list = new ArrayList<String>();
        data_list = getDatabase();
        arr_adapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data_list);
        dateSpinner.setAdapter(arr_adapter);
        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String itemString = dateSpinner.getItemAtPosition(position).toString();
                draw(itemString);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        draw(data_list.get(0));
    }

    void draw(String date) {
        List<Float> male = new ArrayList<Float>();
        List<Float> feMale = new ArrayList<Float>();
        BarChart twoBarChart = (BarChart) findViewById(R.id.bar_chart);
        List<Float> ageGenderList = getGender(date);

        for (int i = 0;i < ageGenderList.size(); i ++) {
            if ((i % 2) == 0 ) {
                male.add(ageGenderList.get(i));
            } else {
                feMale.add(ageGenderList.get(i));
            }
        }

        final List<String> xList = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            xList.add(i, i * 10  +"-" + (i * 10 + (i == 9 ? 10 : 9)));
        }

        twoBarChart.setNoDataText("No Data");
        twoBarChart.animateXY(1000, 1000);
        twoBarChart.getDescription().setEnabled(false);

        XAxis xAxis = twoBarChart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(xList.size());
        xAxis.setLabelCount(10,false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setCenterAxisLabels(true);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return xList.get((int) Math.abs(value) % xList.size());
            }
        });

        YAxis rightYAxis = twoBarChart.getAxisRight();
        rightYAxis.setEnabled(false);
        YAxis leftYAxis = twoBarChart.getAxisLeft();
        leftYAxis.enableGridDashedLine(10f, 10f, 0f);

        List<IBarDataSet> dataSets = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entries.add(new BarEntry(i, male.get(i)));
        }
        BarDataSet barDataSet = new BarDataSet(entries, "Male person-time");
        barDataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                    ViewPortHandler viewPortHandler) {
                return (int) value + " p/t";
            }
        });
        barDataSet.setColor(Color.BLUE);
        dataSets.add(barDataSet);

        List<BarEntry> entries2 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entries2.add(new BarEntry(i, feMale.get(i)));
        }
        BarDataSet barDataSet2 = new BarDataSet(entries2, "Female person-time");
        barDataSet2.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                    ViewPortHandler viewPortHandler) {
                return (int) value + " p/t";
            }
        });
        barDataSet2.setColor(Color.RED);
        dataSets.add(barDataSet2);

        BarData data = new BarData(dataSets);

        int barAmount = dataSets.size();

        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = (1f - groupSpace) / barAmount - 0.05f;

        data.setBarWidth(barWidth);
        data.groupBars(0f, groupSpace, barSpace);

        twoBarChart.setData(data);
    }

    private List<String> getDatabase()
    {

        if(mAgeGenderHelper == null) {
            mAgeGenderHelper = new AgeGenderHelper(this);
        }
        List<String> dates = new ArrayList<>();

        database = mAgeGenderHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery("select * from " + DatabaseStatic.TABLE_NAME ,null);

        while (cursor.moveToNext()) {
            dates.add(cursor.getString(21));
        }
        cursor.close();
        return dates;
    }

    private List<Float> getGender(String date) {
        if(mAgeGenderHelper == null) {
            mAgeGenderHelper = new AgeGenderHelper(this);
        }
        List<Float> AgeGenderdates = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DatabaseStatic.TABLE_NAME +
                " where "+ DatabaseStatic.Date + "=?", new String[] {date});

        if(cursor.moveToFirst()) {
            for (int i = 1; i < 21; i++) {
                AgeGenderdates.add(cursor.getFloat(i));
            }
        }
        cursor.close();
        return AgeGenderdates;
    }

}


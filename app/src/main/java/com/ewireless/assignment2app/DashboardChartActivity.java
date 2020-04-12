package com.ewireless.assignment2app;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ewireless.charts.ChartItem;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.text.DateFormatSymbols;
import com.ewireless.charts.BarChartItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.whiteelephant.monthpicker.MonthPickerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class DashboardChartActivity extends AppCompatActivity {

    // Tag for log prints
    private final static String TAG = "DashboardChartActivity";

    // Database reference initialisation
    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    private DatabaseReference mDatabaseReference = mDatabase.getReference().child("Users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_listview_chart);

        // add moneth picker
        //chooseMonthOnly();
        setNormalPicker();

        int year = parseInt(new SimpleDateFormat("yyyy", Locale.UK).format(new Date()));
        int month = parseInt(new SimpleDateFormat("MM", Locale.UK).format(new Date()));

        generateGraph(year, month);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setNormalPicker() {
        setContentView(R.layout.activity_listview_chart);
        final Calendar today = Calendar.getInstance();
        findViewById(R.id.month_picker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MonthPickerDialog.Builder builder = new MonthPickerDialog.Builder(DashboardChartActivity.this, new MonthPickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(int selectedMonth, int selectedYear) {
                        Log.d(TAG, "selectedMonth : " + selectedMonth + " selectedYear : " + selectedYear);
                        generateGraph(selectedYear, selectedMonth + 1);
                        //Toast.makeText(DashboardChartActivity.this, "Date set with month" + selectedMonth + " year " + selectedYear, Toast.LENGTH_SHORT).show();
                    }
                }, today.get(Calendar.YEAR), today.get(Calendar.MONTH));

                builder.setActivatedMonth(Calendar.JANUARY)
                        .setMinYear(1990)
                        .setActivatedYear(2020)
                        .setMaxYear(2030)
                        .setMinMonth(Calendar.JANUARY)
                        .setTitle("Select trading month")
                        .setMonthRange(Calendar.JANUARY, Calendar.DECEMBER)
                        .setOnMonthChangedListener(new MonthPickerDialog.OnMonthChangedListener() {
                            @Override
                            public void onMonthChanged(int selectedMonth) {
                                Log.d(TAG, "Selected month : " + selectedMonth);
                            }
                        })
                        .setOnYearChangedListener(new MonthPickerDialog.OnYearChangedListener() {
                            @Override
                            public void onYearChanged(int selectedYear) {
                                Log.d(TAG, "Selected year : " + selectedYear);
                                // Toast.makeText(MainActivity.this, " Selected year : " + selectedYear, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build()
                        .show();

            }
        });
    }


    public int[] activities = {0, 0, 0, 0};
    float[] cadenceAverages = new float[31];

    private void generateGraph(int year, int month) {
        // clear activities and cadence data
        Arrays.fill(activities,0);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userKey = prefs.getString("User ID", null);
        
        DatabaseReference userRoot = mDatabaseReference.child(userKey);
        //DatabaseReference activityRoot = userRoot.child("Activity Data").child("YEAR: " + year).child("MONTH: " + String.format("%02d", month));
        //DatabaseReference cadenceRoot = userRoot.child("Cadence Data").child("YEAR: " + year).child("MONTH: " + String.format("%02d", month));

        // Find number of days in this month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        int numDays = calendar.getActualMaximum(Calendar.DATE);


        // listener to get snapshot of database, this method triggers once when created
        userRoot.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // For each day
                for (DataSnapshot activitySnap : dataSnapshot.child("Activity Data")
                        .child("YEAR: " + year)
                        .child("MONTH: " + String.format("%02d", month))
                        .getChildren()) {
                    // For each entry in that day
                    for (DataSnapshot activitySnap2 : activitySnap.getChildren()) {
                        getActivityData(activitySnap2);
                    }
                }

                // For each entry in that day
                for (DataSnapshot cadenceSnap : dataSnapshot.child("Cadence Data")
                        .child("YEAR: " + year)
                        .child("MONTH: " + String.format("%02d", month))
                        .getChildren()) {
                        getCadenceData(cadenceSnap);
                }

                // call function to create barchart
                drawGraphs(createBarData(month));
                // remove listener once finished
                userRoot.removeEventListener(this);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting data failed, log a message
                Log.w(TAG, "userRoot:onCancelled", databaseError.toException());
            }
        });

    }

    private void getCadenceData(DataSnapshot snapshot) {
//        Map day = snapshot.getValue(Map.class);
//        int dayNum;

  //      Pattern day_pattern = Pattern.compile("(\\d+)");

        /*Matcher dayMatcher = day_pattern.matcher(day);

        if(dayMatcher.find()) {
            dayNum = parseInt(dayMatcher.group(1));
        } else {
            return;
        }

        List<Float> snapAverage = new ArrayList<Float>();
        // find average of all cadence values stored for this day
        for (DataSnapshot walkSnaps : snapshot.getChildren()) {
            List<Float> cadenceData = new ArrayList<Float>();
            // add all value to list
            cadenceData = walkSnaps.getValue(List.class);
            // find average
            snapAverage.add(getAverage(cadenceData));
        }

        cadenceAverages[dayNum] = getAverage(snapAverage);
*/
        return;

    }

    private Float getAverage(List<Float> cadenceData) {
        Float sum = 0f;
        if(!cadenceData.isEmpty()) {
            for (Float datum: cadenceData) {
                sum += datum;
            }
            return sum / cadenceData.size();
        }
        return sum;
    }


    private void getActivityData(DataSnapshot snapshot2) {
        String user = snapshot2.getValue(String.class);
        // get activity type
        int activityType = determineActivity(user);
        // return activity type
        if (activityType > -1) {
            activities[activityType] ++;
        }
    }

    private int determineActivity(String user) {
        Pattern activity_pattern = Pattern.compile("(\\s\\w+)");
        Pattern transition_pattern = Pattern.compile("ENTER");

        Matcher transitionMatcher = transition_pattern.matcher(user);
        Matcher activityMatcher = activity_pattern.matcher(user);

        if(!transitionMatcher.find()) {
            return -1;
        }


        if (activityMatcher.find()) {
            String activityType = activityMatcher.group(1);

            int identifier;

            switch (activityType) {
                case " WALKING":
                    identifier = 0;
                    break;
                case " RUNNING":
                    identifier = 1;
                    break;
                case " IN VEHICLE":
                    identifier = 2;
                    break;
                case " ON BICYCLE":
                    identifier = 3;
                    break;

                default:
                    identifier = -1;
            }
            return identifier;
        } else {
            return -1;
        }

    }

    private BarData createBarData(int month) {
        ArrayList<BarEntry> barEntries = new ArrayList<>();

        String monthWord = new DateFormatSymbols().getMonths()[month - 1];

        for (int i = 0; i < 4; i++) {
            barEntries.add(new BarEntry(i, activities[i]));
        }

        BarDataSet d = new BarDataSet(barEntries, "Activities in " + monthWord);
        d.setColors(ColorTemplate.VORDIPLOM_COLORS);
        d.setHighLightAlpha(255);

        BarData cd = new BarData(d);
        cd.setBarWidth(0.9f);
        return cd;
    }

    private void drawGraphs(BarData barData) {
        // add cadence title
        ListView lv = findViewById(R.id.listView1);

        ArrayList<ChartItem> list = new ArrayList<>();

        // add charts to the list
        //list.add(new LineChartItem(generateDataLine(1), getApplicationContext()));

        final String[] activityStrings = {"Walking", "Running", "In Vehicle", "On Bicycle"};
        final String barTitle = "Patient Activity Count";
        BarChartItem barChart = new BarChartItem(barData, activityStrings, barTitle, getApplicationContext());

        list.add(barChart);

        ChartDataAdapter cda = new ChartDataAdapter(getApplicationContext(), list);

        // this calls the getView method of the chartItem classes and formats them appropriately
        if (cda != null) {
            lv.setAdapter(cda);
            return;
        }

        Log.d(TAG, "listView could not be generated");
        return;

    }

    /** adapter that supports 3 different item types */
    private class ChartDataAdapter extends ArrayAdapter<ChartItem> {

        ChartDataAdapter(Context context, List<ChartItem> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            //noinspection ConstantConditions
            return getItem(position).getView(position, convertView, getContext());
        }

        @Override
        public int getItemViewType(int position) {
            // return the views type
            ChartItem ci = getItem(position);
            return ci != null ? ci.getItemType() : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 3; // we have 3 different item-types
        }
    }

    /**
     * generates a random ChartData object with just one DataSet
     *
     * @return Line data
     */
    private LineData generateDataLine(int cnt) {

        ArrayList<Entry> values1 = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            values1.add(new Entry(i, (int) (Math.random() * 65) + 40));
        }

        LineDataSet d1 = new LineDataSet(values1, "New DataSet " + cnt + ", (1)");
        d1.setLineWidth(2.5f);
        d1.setCircleRadius(4.5f);
        d1.setHighLightColor(Color.rgb(244, 117, 117));
        d1.setDrawValues(false);

        ArrayList<Entry> values2 = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            values2.add(new Entry(i, values1.get(i).getY() - 30));
        }

        LineDataSet d2 = new LineDataSet(values2, "New DataSet " + cnt + ", (2)");
        d2.setLineWidth(2.5f);
        d2.setCircleRadius(4.5f);
        d2.setHighLightColor(Color.rgb(244, 117, 117));
        d2.setColor(ColorTemplate.VORDIPLOM_COLORS[0]);
        d2.setCircleColor(ColorTemplate.VORDIPLOM_COLORS[0]);
        d2.setDrawValues(false);

        ArrayList<ILineDataSet> sets = new ArrayList<>();
        sets.add(d1);
        sets.add(d2);

        return new LineData(sets);
    }


    /**
     * generates a random ChartData object with just one DataSet
     *
     * @return Pie data
     */
    private PieData generateDataPie() {

        ArrayList<PieEntry> entries = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            entries.add(new PieEntry((float) ((Math.random() * 70) + 30), "Quarter " + (i+1)));
        }

        PieDataSet d = new PieDataSet(entries, "");

        // space between slices
        d.setSliceSpace(2f);
        d.setColors(ColorTemplate.VORDIPLOM_COLORS);

        return new PieData(d);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.only_github, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(myIntent, 0);
        return true;
    }

}

package com.ewireless.assignment2app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.ewireless.charts.BarChartItem;
import com.ewireless.charts.ChartItem;
import com.ewireless.charts.LineChartItem;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.whiteelephant.monthpicker.MonthPickerDialog;

import java.text.DateFormatSymbols;
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

/**
 * Using the MPCHART library two graphs displaying activity and cadence data for a given month
 * are created. Using regex the appropriate data is extracted from the database.
 * This data is used to to create a dataset for a bar and line chart.
 * The month and year which the data is acquired from can be changed using a choose month button.
 * This button uses the MonthAndYearPicker library to create a month and year selection item.
 * Upon month and year selection the charts are redrawn with the new data.
 * @author Fergus Brown s1525959
 */
public class DashboardChartActivity extends AppCompatActivity {

    // Tag for log prints
    private final static String TAG = "DashboardChartActivity";

    // Create firebase database object
    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    // create database reference pointing to users
    private DatabaseReference mDatabaseReference = mDatabase.getReference().child("Users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_listview_chart);

        // add month picker
        setNormalPicker();

        int year = parseInt(new SimpleDateFormat("yyyy", Locale.UK).format(new Date()));
        int month = parseInt(new SimpleDateFormat("MM", Locale.UK).format(new Date()));

        generateGraph(year, month);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    // Method used to handle month and year selection upon pressing "choose month" button
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


    // there are 4 activity types to chart
    public int[] activities = new int[4];

    // max days in a month is 31
    float[] cadenceAverages = new float[31];

    // creates and adds line and bar chart to the list view
    private void generateGraph(int year, int month) {
        // clear activities and cadence data
        Arrays.fill(activities,0);
        Arrays.fill(cadenceAverages,0f);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userKey = prefs.getString("User ID", null);

        // create a databser reference pointing to the user's unique key
        DatabaseReference userRoot = mDatabaseReference.child(userKey);

        /** Obselete references **/
        //DatabaseReference activityRoot = userRoot.child("Activity Data").child("YEAR: " + year).child("MONTH: " + String.format("%02d", month));
        //DatabaseReference cadenceRoot = userRoot.child("Cadence Data").child("YEAR: " + year).child("MONTH: " + String.format("%02d", month));

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

                // For each day
                for (DataSnapshot cadenceSnap : dataSnapshot.child("Cadence Data")
                        .child("YEAR: " + year)
                        .child("MONTH: " + String.format("%02d", month))
                        .getChildren()) {

                    String childName = cadenceSnap.getKey();
                    getCadenceData(cadenceSnap, childName);
                }

                // call function to create barchart
                drawGraphs(year, month);
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

    // creates a dataset suitable for a LineChartItem object
    private LineData createLineData(int year, int month) {
        ArrayList<Entry> lineEntries = new ArrayList<>();

        String monthWord = new DateFormatSymbols().getMonths()[month - 1];

        // Find number of days in this month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        int numDays = calendar.getActualMaximum(Calendar.DATE);

        // only enter data up to the number of days in the selected month
        for (int i = 0; i < numDays; i++) {
            lineEntries.add(new BarEntry(i + 1, cadenceAverages[i]));
        }

        LineDataSet d1 = new LineDataSet(lineEntries, "Days in " + monthWord);
        d1.setLineWidth(2.5f);
        d1.setCircleRadius(4.5f);
        d1.setHighLightColor(Color.rgb(244, 117, 117));
        d1.setDrawValues(false);

        ArrayList<ILineDataSet> sets = new ArrayList<>();
        sets.add(d1);

        // return dataset for a linechart
        return new LineData(sets);

    }

    GenericTypeIndicator<List<Float>> newType = new GenericTypeIndicator<List<Float>>() {};

    // Uses regular expressions to obtain the appropriate cadence data from the database snapshot
    private void getCadenceData(DataSnapshot snapshot, String childName) {
        // int to store the day number
        int dayNum;

        //regex  pattern to obtain day number
        Pattern day_pattern = Pattern.compile("(\\d+)");

        Matcher dayMatcher = day_pattern.matcher(childName);

        // If a match is found then assign the day number to dayNum
        if(dayMatcher.find()) {
            dayNum = parseInt(dayMatcher.group(1));
        } else {
            return;
        }

        // List to save average cadence associated with each timestamp
        List<Float> snapAverage = new ArrayList<Float>();

        // find average of all cadence values stored for this day
        for (DataSnapshot walkSnaps : snapshot.getChildren()) {
            List<Float> cadenceData = new ArrayList<Float>();
            // add all value to list
            cadenceData = walkSnaps.getValue(newType);
            // find average
            snapAverage.add(getAverage(cadenceData));
        }

        // starts at index 0, find average of cadence data under all timestamps and add to
        // cadenceAverages
        cadenceAverages[dayNum-1] = getAverage(snapAverage);

        return;

    }

    // returns the average of a list of floats
    private Float getAverage(List<Float> cadenceData) {
        Float sum = 0f;
        if(!cadenceData.isEmpty()) {
            for (float datum: cadenceData) {
                sum += datum;
            }
            return sum / cadenceData.size();
        }
        return sum;
    }


    // adds a count to the appropriate index in activities array
    private void getActivityData(DataSnapshot snapshot2) {
        String user = snapshot2.getValue(String.class);
        // get activity type
        int activityType = determineActivity(user);
        // return activity type
        if (activityType > -1) {
            activities[activityType] ++;
        }
    }

    // using regex extract the activity name and return the associated int
    private int determineActivity(String user) {
        // pattern to extract activity type
        Pattern activity_pattern = Pattern.compile("(\\s\\w+)");
        // pattern to match with a string identifying the entering of an activity
        Pattern transition_pattern = Pattern.compile("ENTER");

        // Match the patterns with the user string
        Matcher transitionMatcher = transition_pattern.matcher(user);
        Matcher activityMatcher = activity_pattern.matcher(user);

        // if no match return
        if(!transitionMatcher.find()) {
            return -1;
        }

        // based on the activity string return the appropriate int
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

    // creates dataset for BarChartItem object
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


    // creates LineChartItem and BarChartItem objects to add to the list view
    private void drawGraphs(int year, int month) {

        // create chart data
        BarData barData = createBarData(month);
        LineData lineData = createLineData(year, month);

        // add cadence title
        ListView lv = findViewById(R.id.listView1);

        ArrayList<ChartItem> list = new ArrayList<>();

        // define X axis ticks for bar chart
        final String[] activityStrings = {"Walking", "Running", "In Vehicle", "On Bicycle"};
        final String barTitle = "Patient Activity Count";
        // define BarChartItem object
        BarChartItem barChart = new BarChartItem(barData, activityStrings, barTitle, getApplicationContext());

        // Find number of days in this month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        int numDays = calendar.getActualMaximum(Calendar.DATE);
        final String lineTitle = "Average Cadence Frequency (Hz)";
        LineChartItem lineChart = new LineChartItem(lineData, numDays, lineTitle, getApplicationContext());

        // add charts to the list
        list.add(barChart);
        list.add(lineChart);

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

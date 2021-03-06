package com.aero2.android.DefaultClasses.Azure;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.aero2.android.DefaultClasses.DataTables.ResultDataTable;
import com.aero2.android.DefaultClasses.DataTables.SampleDataTable;
import com.aero2.android.DefaultClasses.SQLite.SamplesSQLite;

/**
 * Handlers all azure tasks including:
 *
 *      - Posting samples in SampleDataTable
 *      - Retrieving results from ResultsDataTable
 *      - Retrieving properties from PropertiesDataTable
 *
 *
 * Created by usmankhan on 12/30/2015.
 */
public class AzureHandler {

    Context context;
    DBWriter dbWriter;

    public AzureHandler(Context context){
        this.context = context;

        Log.v("AzureHandler", "Instantiated.");
    }


    /**
     * Retrieves data from local SQLite data storage and
     * posts it in Azure.
     * arg: SamplesSQLite object
     * exception: ExecutionException, InterruptedException
     * return: No return value.
     */

    public void postSamples(final SamplesSQLite samplesSqLite) {

        //Initiate Azure for posting samples in SampleDataTable
        dbWriter = new DBWriter(context, SampleDataTable.class);

        //Number of parameters in integrator array
        int N = 6;

        Log.v("AzureHandler", "Starting off!");

        final Double[][] integrators = samplesSqLite.getAllAirDouble();
        final int count = samplesSqLite.getRowCountInLocal();

        //Initialize new 2-d array with 1 less column
        final Double[][] nIntegrators = new Double[count][N];

        //Remove the 'row key' columns
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < N; j++) {

                nIntegrators[i][j] = integrators[i][j + 1];
                Log.v("Integrator: ", String.valueOf(nIntegrators[i][j]));
            }
        }
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                //Add item in mobile activity
                for (int i = 0; i < count; i++) {

                    String rowId = String.valueOf(integrators[i][0]);
                    try {
                        Log.v("Azure Handler","Attempting to add data");
                        dbWriter.addItem(null, nIntegrators[i]);
                    } catch (final Exception e) {
                        Log.e("AzureHandler", "Data not saved.");
                    }
                    Log.v("AzureHandler", "Added Item in postSamples()" + String.valueOf(i));
                    samplesSqLite.deleteEntry(rowId);
                }
                return null;
            }
        };
        runAsyncTask(task);
    }

    /**
     * Runs AsyncTask.
     * arg: None
     * exception: None
     * return: No return value.
     */

    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        return task.execute();
    }

    public void retrieveSamples(final double currLat, final double currLong, final double verticalInt,
                                final double horizontalInt, final Context context) {
        //Initiate Azure for posting samples in SampleDataTable
        dbWriter = new DBWriter(context, ResultDataTable.class);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    dbWriter.getItems(currLat,currLong,verticalInt,horizontalInt, context);
                }
                catch (Exception e){
                    Log.v("AzureHandler","Exception retrieving samples.");
                }

                return null;
            }
        };
        runAsyncTask(task);
    }



}









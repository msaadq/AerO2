package com.aero2.android.DefaultClasses.SQLite;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.aero2.android.DefaultClasses.Integrator;

/**
 * Calls SamplesSQLite and upload data in local storage.
 * USAGE:
 *      - Initialize SQLiteAsycnTask by passing on activity &
 *      SamplesSQLite object.
 *      - Call .execute() method and pass on a 1-d array
 *
 *
 * Created by usmankhan on 12/13/2015.
 */
public class SQLiteAsyncTask extends AsyncTask<Double[][], Void, Void> {

    Activity activity;
    SamplesSQLite samplesSqLite;

    public SQLiteAsyncTask(Activity activity, SamplesSQLite samplesSqLite) {  // can take other params if needed

        this.activity = activity;
        this.samplesSqLite = samplesSqLite;
        Log.v("SQLiteAsyncTask", "Instantiated.");

    }

    @Override
    protected Void doInBackground(Double[][]... params) {

        Log.v("SQLiteAsyncTask", "Entered doInBackground");
        int count  = Integrator.value_count;
        Log.v("Count:",String.valueOf(count));

        //Push all values to SQLite
        for (int i=0; i< count; i++) {
            samplesSqLite.addAirValue(params[0][i]);
            Log.v("SQLiteAsyncTask", "Added Item " + String.valueOf(i));
        }

        Integrator.value_count = 0;
        long row_count = samplesSqLite.getRowCountInLocal();
        Log.v("Row Count: ",String.valueOf(row_count));

        return null;

    }

}

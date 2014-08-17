package com.adstrosoftware.simpleopenhelper;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * An open helper that reads SQL statements from assses.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class SimpleOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = SimpleOpenHelper.class.getSimpleName();

    private final Context context;
    private final String dbName;

    /**
     * Creates a new instance of the simple open helper.
     *
     * @param context Context to to read assets. This will be help by the instance.
     * @param dbName The file name of the database
     * @param schemaVersion The version of the databse.
     */
    public SimpleOpenHelper(Context context, String dbName, int schemaVersion) {
        super(context, dbName, null, schemaVersion);

        this.context = context;
        this.dbName = dbName;

        // TODO (APS): Does this need to happen somewhere else?
        // This will happen in onConfigure for API >= 16
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            SQLiteDatabase db = getWritableDatabase();
            db.enableWriteAheadLogging();
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        applySqlFile(db, 1);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int i = oldVersion; i <= newVersion; i++) {
            applySqlFile(db, i);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        db.enableWriteAheadLogging();
        db.setForeignKeyConstraintsEnabled(true);
    }

    private void applySqlFile(SQLiteDatabase db, int version) {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open(String.format("%s.%d.sql", dbName, version))));

            final StringBuilder statement = new StringBuilder();

            for (String line; (line = reader.readLine()) != null;) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Reading line -> " + line);

                // Ignore empty lines
                if (line != null && !TextUtils.isEmpty(line) && !line.startsWith("#")) {
                    statement.append(line);
                }

                if (line.endsWith(";")) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Running statement " + statement);
                    db.execSQL(statement.toString());
                    statement.setLength(0);
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Could not apply SQL file", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.w(TAG, "Could not close reader", e);
                }
            }
        }
    }
}

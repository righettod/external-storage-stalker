package eu.righettod.externalstoragestalker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "ExternalStorageStalker";
    private static final int MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private ScheduledThreadPoolExecutor scheduler;
    private ScheduledFuture task;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Check if the application has read access to the external storage, if not then ask for it...
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
        }
        //Get component
        final TextView searchFilesView = findViewById(R.id.searchFiles);
        searchFilesView.setMovementMethod(new ScrollingMovementMethod());
        //Get delay
        final EditText watchDelay = findViewById(R.id.watchDelay);
        //Get file extension
        final EditText fileExtension = findViewById(R.id.fileExtension);
        //Affect event listeners
        Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchFilesView.setText("");
            }
        });
        ToggleButton captureButton = findViewById(R.id.captureButton);
        captureButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //See https://developer.android.com/training/data-storage
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        final List<File> locations = new ArrayList<>();
                        //Add all referenced external storage locations into the system by starting from the app external data directory and jumping up to the root
                        //From "/storage/emulated/0000-0000/Android/data/eu.righettod.externalstoragestalker/files" to "/storage/emulated/0000-0000"
                        //Explicitly ignore the check regarding if the storage is mounted on not...
                        for (File location : getApplicationContext().getExternalFilesDirs(null)) {
                            locations.add(location.getParentFile().getParentFile().getParentFile().getParentFile());
                        }
                        scheduler = new ScheduledThreadPoolExecutor(1);
                        if (watchDelay.getText().length() == 0) {
                            watchDelay.setText("15");
                        }
                        if (fileExtension.getText().length() == 0) {
                            fileExtension.setText("pdf");
                        }
                        Runnable stalkerRunnable = new Runnable() {
                            @Override
                            public void run() {
                                new StalkingTask(searchFilesView, locations, fileExtension.getText().toString()).execute();
                            }
                        };
                        long delay = Long.parseLong(watchDelay.getText().toString());
                        task = scheduler.scheduleWithFixedDelay(stalkerRunnable, 1, delay, TimeUnit.SECONDS);

                    } else {
                        Toast.makeText(getApplicationContext(), "Access not granted to external storage!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (scheduler != null && !scheduler.isShutdown()) {
                        scheduler.shutdown();
                    }
                    if (task != null && !task.isCancelled()) {
                        task.cancel(false);
                    }
                }
            }
        });

        AppCenter.start(getApplication(), "3031a4b4-8f17-4928-98ed-d25b33531238", Analytics.class, Crashes.class);
    }
}
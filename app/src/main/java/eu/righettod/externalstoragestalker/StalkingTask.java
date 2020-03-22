package eu.righettod.externalstoragestalker;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.List;

/**
 * In charge of searching the specified files on the external storage.
 */
public class StalkingTask extends AsyncTask<List<File>, Integer, String> {

    private TextView searchFilesView;
    private List<File> locations;
    private String fileExtension;

    public StalkingTask(TextView searchFilesView, List<File> locations, String fileExtension) {
        this.searchFilesView = searchFilesView;
        this.locations = locations;
        this.fileExtension = "." + fileExtension;
    }

    @Override
    protected String doInBackground(List<File>... lists) {
        String data = null;
        try {
            if (locations != null && !locations.isEmpty()) {
                StringBuilder buffer = new StringBuilder();
                for (File location : locations) {
                    process(location, buffer);
                }
                data = buffer.toString();
            }
        } catch (Exception e) {
            Log.e(MainActivity.TAG, "Error during catching!", e);
        }

        return data;
    }

    private void process(File folder, StringBuilder result) {
        for (final File f : folder.listFiles()) {
            if (f.isDirectory()) {
                process(f, result);
            }
            if (f.isFile() && f.canRead() && f.getName().endsWith(fileExtension)) {
                result.append(String.format("%s\n", f.toString()));
            }
        }
    }

    @Override
    protected void onPostExecute(String s) {
        if (s != null && !s.trim().isEmpty()) {
            this.searchFilesView.append(s);
            this.searchFilesView.append("\n");
        }
    }
}

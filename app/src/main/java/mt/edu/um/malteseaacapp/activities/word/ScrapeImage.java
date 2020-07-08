package mt.edu.um.malteseaacapp.activities.word;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.zeroturnaround.zip.commons.IOUtils;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import mt.edu.um.malteseaacapp.R;
public class ScrapeImage extends AppCompatActivity {

    private WebView scrapeView;
    private Button scrapeEnter;
    private EditText scrapeUrl;
    private WebView.HitTestResult testResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_scrape);

        scrapeView = this.findViewById(R.id.scrapeView);
        scrapeUrl = this.findViewById(R.id.scrapeUrl);
        scrapeEnter = this.findViewById(R.id.scrapeEnter);

        // set javascript to on as some pages will refuse to work
        WebSettings viewSettings =  scrapeView.getSettings();
        viewSettings.setJavaScriptEnabled(true);

        // load uri set in strings.xml by default
        scrapeView.loadUrl(getResources().getString(R.string.scrape_default_uri));
        scrapeUrl.setText(getResources().getString(R.string.scrape_default_uri));

        // register the web view to allow context menu on long press
        registerForContextMenu(scrapeView);

        // intercepting user web view navigation to allow us to capture the URL and update scrapeUrl field
        scrapeView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String urlIn) {

                scrapeUrl.setText(urlIn);
                return false;
            }
        });

        scrapeEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String urlText = scrapeUrl.getText().toString();

                String[] splitUrl = urlText.split("\\.");

                // partially re-constructs url to reduce error burden
                if (splitUrl.length > 1 && !splitUrl[0].toLowerCase().equals("http://www") && !splitUrl[0].toLowerCase().equals("https://www")) {

                    if (!splitUrl[0].contains("http://") && !splitUrl[0].contains("https://")) {

                        if (splitUrl[0].equals("www"))
                            urlText = "http://" + urlText;
                        else
                            urlText = "http://www." + urlText;
                    }
                }

                // load url if it is valid
                if (URLUtil.isValidUrl(urlText)) {

                    scrapeView.loadUrl(urlText);

                    // hide user keyboard
                    InputMethodManager imm = (InputMethodManager)getApplicationContext().getSystemService(Service.INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(scrapeUrl.getWindowToken(), 0);

                    // de-focus url field
                    scrapeUrl.clearFocus();

                } else {
                    Toast.makeText(getApplicationContext(), "Specified URL is invalid", Toast.LENGTH_LONG).show();
                }

            }
        });

        // enter pressed on url field performs button click
        scrapeUrl.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_ENTER) {

                    scrapeEnter.performClick();

                    return true; // consumes the event
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {

        if (scrapeView.canGoBack())
            // go back in web view
            scrapeView.goBack();
        else
            // minimize app
            super.onBackPressed();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);

        // before showing any context menu, we must first test the content beneath the user's finger
        testResult = scrapeView.getHitTestResult();

        if (testResult.getType() == WebView.HitTestResult.IMAGE_TYPE)
            getMenuInflater().inflate(R.menu.scraper_context, menu);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.use_image:

                String imageUrl = testResult.getExtra(); // image url stored inside getExtra() by HitTestResult

                if (imageUrl == null) {

                    Toast.makeText(this, "Unable to get image URL", Toast.LENGTH_LONG).show();

                } else {

                    new ScrapeImage.DownloadTask(ScrapeImage.this).execute(imageUrl, null, null);

                }
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    private static class DownloadTask extends AsyncTask<String, Void, Void> {

        private WeakReference<ScrapeImage> scrapeImageWeakReference;

        // Constructor of DownloadTask, retains a weak reference to the ScrapeImage activity to prevent memory leaks
        DownloadTask(ScrapeImage context) {
            scrapeImageWeakReference = new WeakReference<>(context);
        }

        Exception e = null;
        byte[] imgTemp = null;

        @Override
        protected Void doInBackground(String... urlIn) {

            // attempt a regular url image download and if it fails, attempt a base64 decode

            InputStream is = null;

            try {

                is = new java.net.URL(urlIn[0]).openStream();

                imgTemp = IOUtils.toByteArray(is);

            } catch (Exception downloadEx) {

                e = downloadEx;

            } finally {

                if (is != null) {

                    try {

                        is.close();

                    } catch (Exception closeEx) {

                        e = closeEx;

                    }
                }
            }

            if (e != null) { // regular download failed, attempting base64 decode...

                e = null; // reset

                try {

                    String[] explodedUrl = urlIn[0].split(",");

                    // ensure the actual encoded image part exists
                    if(explodedUrl.length == 2) {

                        imgTemp = Base64.decode(explodedUrl[1], Base64.DEFAULT);

                    } else {

                        throw new ArrayIndexOutOfBoundsException();

                    }

                } catch (Exception base64Ex) {

                    e = base64Ex;

                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            if(e != null) {

                Toast.makeText(scrapeImageWeakReference.get().getApplicationContext(), "Something went wrong", Toast.LENGTH_LONG).show();

            } else {

                // send back image in the form of a byte array
                Intent returnBack = new Intent();
                returnBack.putExtra("imagebytes", imgTemp);
                scrapeImageWeakReference.get().setResult(RESULT_OK, returnBack);
                scrapeImageWeakReference.get().finish();

            }

        }
    }
}
package mt.edu.um.malteseaacapp.activities.settings;

import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.activities.Home;

public class History extends AppCompatActivity {

    // To check whether the pin has been entered
    private boolean pinEntered;
    // To check whether pin has been entered successfully
    private final int ENTER_PIN_REQUEST_CODE = 0;

    // To update predictions
    private UpdatePredictionsTask updatePredictionsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initially the pin is not entered
        pinEntered = false;

        // If no pin has been set, show history menu without prompting for pin
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getString("pin", "").equals(""))
            setUpLayout();
        else {
            // If the correct pin has been entered, show the history menu
            // Otherwise, open the EnterPin activity
            if (pinEntered)
                setUpLayout();
            else {
                Intent enterPin = new Intent(this, EnterPin.class);
                startActivityForResult(enterPin, ENTER_PIN_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Do not allow user to go to Home activity while predictions are being updated
        if(PreferenceManager.getDefaultSharedPreferences(
                this).getBoolean("update_in_progress", false))
        {
            // Indicate that the predictions are being updated
            Toast.makeText(getApplicationContext(), "Please wait, predictions are" +
                            " being updated.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Launch Home activity
        Intent home = new Intent(getApplicationContext(), Home.class);
        startActivity(home);
        finish();
    }

    /**
     * Set up the layout of the "History" activity (including onClickListeners)
     */
    private void setUpLayout() {
        // Indicate that update is not yet in progress
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .edit().putBoolean("update_in_progress", false).commit();

        setContentView(R.layout.activity_history);
        // Find ListView containing entered sentences
        final ListView historyListView = this.findViewById(R.id.historyListView);

        // Find TextView containing message to indicate that no history is available
        final TextView noHistoryTextView = this.findViewById(R.id.noHistoryTextView);

        // Read entered sentences from text file and store in ArrayList
        ArrayList<String> enteredSentences = new ArrayList<>();
        String line = "";
        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader(
                            getFilesDir().getPath() + "/predictions/history.txt"));
            // Read text file line by line and add every line which is not empty to the list
            while ((line = reader.readLine()) != null) {
                if (!line.equals(""))
                    enteredSentences.add(line);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        // Check if there is any history
        if (enteredSentences.size() == 0)
        {
            // Indicate that there is no history to show
            noHistoryTextView.setVisibility(View.VISIBLE);
            historyListView.setVisibility(View.GONE);
        } else
        {
            // Reverse the order of the list so as to show the newest entries first
            Collections.reverse(enteredSentences);
            // Set adapter to ListView and make visible
            ArrayAdapter<String> historyAdapter =
                    new ArrayAdapter<String>(this, R.layout.spinner_item, enteredSentences);
            historyListView.setAdapter(historyAdapter);
            historyListView.setVisibility(View.VISIBLE);
            noHistoryTextView.setVisibility(View.GONE);
        }

        // Find buttons from layout and set their onClickListeners
        final Button cancelButton = this.findViewById(R.id.cancelButton);
        final Button clearHistoryButton = this.findViewById(R.id.clearHistoryButton);
        final Button updatePredictionsButton =
                this.findViewById(R.id.updatePredictionsButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cancel the update predictions task
                if (updatePredictionsTask != null)
                    updatePredictionsTask.cancel(true);

                // Indicate that the update is no longer in progress
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit().putBoolean("update_in_progress", false).commit();

                // Show the 'update predictions' and 'clear history' buttons
                clearHistoryButton.setVisibility(View.VISIBLE);
                updatePredictionsButton.setVisibility(View.VISIBLE);

                // Hide the linear layout containing progress components
                LinearLayout progressLayout = findViewById(R.id.progressLayout);
                progressLayout.setVisibility(View.GONE);

                // Reload layout
               setUpLayout();
            }
        });
        clearHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if there is any history
                if (enteredSentences.size() == 0)
                {
                    // Indicate that there is no history to clear
                    Toast.makeText(getApplicationContext(), "No history to clear.",
                            Toast.LENGTH_SHORT).show();
                } else
                {
                    // Animate button when clicked
                    Animation bounce
                            = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.scale);
                    bounce.setInterpolator(new DecelerateInterpolator());
                    clearHistoryButton.startAnimation(bounce);
                    // Delete 'history' and 'add_to_corpus' text files
                    new File(getFilesDir().getPath() + "/predictions/history.txt")
                            .delete();
                    new File(getFilesDir().getPath() + "/predictions/add_to_corpus.txt")
                            .delete();
                    // Reload layout
                    setUpLayout();
                }
            }
        });

        // Set text on update predictions button
        String filePath = getFilesDir().getPath() + "/predictions";
        File addToCorpus = new File(filePath, "/add_to_corpus.txt");
        // Check if there are any sentences which need to be added to the corpus
        if(!addToCorpus.exists()) {
            // Indicate that the predictions are up-to-date
            String happy = "\uD83D\uDE0A";
            String text = "Predictions are up-to-date " + happy;
            updatePredictionsButton.setText(text);
        } else
        {
            String text = "Update predictions";
            updatePredictionsButton.setText(text);
        }
        updatePredictionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if there are any sentences which need to be added to the corpus
                String filePath = getFilesDir().getPath() + "/predictions";
                File addToCorpus = new File(filePath, "/add_to_corpus.txt");
                if(!addToCorpus.exists()) {
                    // Indicate that the predictions are up-to-date
                    Toast.makeText(getApplicationContext(), "Predictions are up-to-date.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Animate button when clicked
                Animation bounce
                        = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.scale);
                bounce.setInterpolator(new DecelerateInterpolator());
                updatePredictionsButton.startAnimation(bounce);
                // Indicate that update is not yet successful in shared preferences
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit().putBoolean("update_in_progress", true).commit();
                // Update predictions
                updatePredictionsTask = new History.UpdatePredictionsTask(History.this);
                updatePredictionsTask.execute();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ENTER_PIN_REQUEST_CODE:
                // Show history menu if pin was entered successfully and
                // go back to Home activity if pin was not entered successfully
                if (resultCode == RESULT_OK) {
                    pinEntered = true;
                    setUpLayout();
                } else
                    onBackPressed();
                break;
        }
    }

    /**
     * Static class used for updating predictions
     * Declared static and uses a weak reference to the History activity to prevent memory leaks
     */
    public static class UpdatePredictionsTask extends AsyncTask<Void, Integer, Void> {

        private WeakReference<History> historyWeakReference;

        // Declare HashMaps
        private HashMap<String, HashMap<String, Integer>> unigrams;
        private HashMap<String, HashMap<String, Integer>> bigrams;
        private HashMap<String, HashMap<String, Integer>> trigrams;
        private HashMap<String, HashMap<String, Double>> unigramsPKN;
        private HashMap<String, HashMap<String, Double>> bigramsPKN;

        /**
         * Constructor of UpdatePredictionsTask. Retains a weak reference to the History
         * activity to prevent memory leaks
         *
         * @param context The context
         */
        UpdatePredictionsTask(History context) {
            historyWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Find linear layout containing progress components and make it visible
            LinearLayout progressLayout = historyWeakReference.get()
                    .findViewById(R.id.progressLayout);
            progressLayout.setVisibility(View.VISIBLE);
            // Find ProgressBar from layout and set colour
            ProgressBar progressBar = historyWeakReference.get()
                    .findViewById(R.id.progressBar);
            progressBar.getIndeterminateDrawable().setColorFilter(historyWeakReference.get()
                            .getResources().getColor(R.color.colorPrimary),
                    android.graphics.PorterDuff.Mode.MULTIPLY);

            // Hide 'update predictions' and 'clear history' buttons
            Button updatePredictionsButton = historyWeakReference.get()
                    .findViewById(R.id.updatePredictionsButton);
            Button clearHistoryButton = historyWeakReference.get()
                    .findViewById(R.id.clearHistoryButton);
            updatePredictionsButton.setVisibility(View.GONE);
            clearHistoryButton.setVisibility(View.GONE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String happy = "\uD83D\uDE0A";
            String sad = "\uD83D\uDE1E";
            String text;

            // Start restore and indicate if successful or not.
            if (updatePredictions()) {
                text = "Predictions updated successfully " + happy;

            } else
                text = "Failed to update predictions " + sad;

            // Indicate that predictions update is complete
            showNotification(text);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            // Find ProgressBar from layout
            ProgressBar progressBar = historyWeakReference.get()
                    .findViewById(R.id.progressBar);

            // Animate progress changes
            ObjectAnimator progressBarAnimation
                    = ObjectAnimator.ofInt(progressBar, "progress", progress);
            progressBarAnimation.setInterpolator(new DecelerateInterpolator());
            progressBarAnimation.setDuration(500);
            progressBarAnimation.start();

            // Find progress TextView from layout and set text
            TextView progressText = historyWeakReference.get()
                    .findViewById(R.id.progressText);
            String text = progress + "%";
            progressText.setText(text);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Indicate that the update is no longer in progress
            PreferenceManager.getDefaultSharedPreferences(historyWeakReference.get())
                    .edit().putBoolean("update_in_progress", false).commit();

            // Delete "add_to_corpus.txt"
            new File(historyWeakReference.get()
                    .getFilesDir().getPath() + "/predictions/add_to_corpus.txt")
                    .delete();

            // Show the 'update predictions' and 'clear history' buttons
            Button updatePredictionsButton = historyWeakReference.get()
                    .findViewById(R.id.updatePredictionsButton);
            Button clearHistoryButton = historyWeakReference.get()
                    .findViewById(R.id.clearHistoryButton);
            updatePredictionsButton.setVisibility(View.VISIBLE);
            clearHistoryButton.setVisibility(View.VISIBLE);

            // Hide the linear layout containing progress components
            LinearLayout progressLayout = historyWeakReference.get()
                    .findViewById(R.id.progressLayout);
            progressLayout.setVisibility(View.GONE);

            // Reload the history layout
            historyWeakReference.get().setUpLayout();
        }

        /**
         * Update corpus with new sentences, generate new ngrams,
         * and generate new Kneser-Ney probabilities
         *
         * @return Returns true if restore was successful and false otherwise
         */
        private boolean updatePredictions() {
            // Check if task is cancelled
            if (isCancelled())
                return false;

            try {
                // Get new sentences from "add_to_corpus.txt" and store in ArrayList
                ArrayList<String> newSentences = new ArrayList<>();
                String line = "";
                BufferedReader reader = new BufferedReader(
                        new FileReader(
                                historyWeakReference.get().getFilesDir().getPath() +
                                        "/predictions/add_to_corpus.txt"));
                // Read text file line by line and add every line which is not empty to the list
                while ((line = reader.readLine()) != null) {
                    // Check if task is cancelled
                    if (isCancelled())
                        return false;

                    if (!line.equals(""))
                        newSentences.add(line);
                }

                // Update corpus with new sentences
                String corpusPath = historyWeakReference.get()
                        .getFilesDir().getPath() + "/predictions/korpus.txt";
                FileOutputStream fileOutputStream;
                OutputStreamWriter outputStreamWriter;
                // Go through each sentence and add it to the corpus
                for (String sentence : newSentences) {
                    // Check if task is cancelled
                    if (isCancelled())
                        return false;

                    fileOutputStream = new FileOutputStream(new File(corpusPath), true);
                    outputStreamWriter =
                            new OutputStreamWriter(fileOutputStream, "UTF-8");
                    outputStreamWriter.append(sentence);
                    outputStreamWriter.append("\r\n");
                    outputStreamWriter.flush();
                    outputStreamWriter.close();
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
                publishProgress(10);

                // Read updated corpus from text file into String
                String corpus = "";
                line = "";
                reader = new BufferedReader(new FileReader(corpusPath));
                while ((line = reader.readLine()) != null) {
                    // Check if task is cancelled
                    if (isCancelled())
                        return false;

                    if (!line.equals("")) {
                        corpus = corpus.concat(line);
                        corpus = corpus.concat(" ");
                    }
                }

                // Generate n-grams
                unigrams = generateNGrams(corpus, 1);
                publishProgress(15);
                // Check if task is cancelled
                if (isCancelled())
                    return false;
                bigrams = generateNGrams(corpus, 2);
                publishProgress(25);
                // Check if task is cancelled
                if (isCancelled())
                    return false;
                trigrams = generateNGrams(corpus, 3);
                publishProgress(40);
                // Check if task is cancelled
                if (isCancelled())
                    return false;

                // Calculate Interpolated PKN probabilities for each n-gram
                if (!calculateUnigramsPKN())
                    return false;
                publishProgress(55);
                if (!calculateBigramsPKN())
                    return false;
                publishProgress(75);
                if (!calculateTrigramsPKN())
                    return false;
                publishProgress(100);

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Split the corpus into n-grams based on the value of n
         *
         * @param corpus String read from text file to be split into n-grams
         * @param n      Number of words
         * @return Returns a hashmap containing all prefixes,
         * the following word, and the number of times the n-gram occured in the corpus
         */
        HashMap<String, HashMap<String, Integer>> generateNGrams(String corpus, int n) {
            // HashMap to hold ngrams in the form of " prefix + nextWord ", where the prefix is n-1
            // words long, and the nextWord includes the number of times the word occurred following
            // that prefix
            HashMap<String, HashMap<String, Integer>> ngrams = new HashMap<>();
            HashMap<String, Integer> nextWordCounts;
            String prefix;
            String nextWord;
            Integer count;

            // Split words when a space is encountered
            String[] words = corpus.split(" ");

            for (int i = 0; i <= words.length - n; i++) {
                // Check if task is cancelled
                if (isCancelled())
                    return null;

                // Set the prefix depending on the value of n -> i.e. prefix length = n-1 words
                switch (n) {
                    case 1:
                        prefix = " ";
                        break;
                    case 2:
                        prefix = words[i];
                        break;
                    case 3:
                        prefix = words[i] + " " + words[i + 1];
                        break;
                    default:
                        System.out.println("Value of n must be 2 or 3");
                        prefix = "";
                        return null;
                }

                // Skip prefix if it contains the terminating symbol
                if (prefix.contains("</s>"))
                    continue;

                // Get all existing next words and their corresponding number of occurrences for an
                // already existing prefix or create a new hashmap with next words and number of
                // occurrences if the prefix is non-existing
                if (ngrams.containsKey(prefix)) {
                    nextWordCounts = ngrams.get(prefix);
                } else {
                    nextWordCounts = new HashMap<>();
                }

                nextWord = words[i + n - 1];

                // Add 1 to the number of occurrences of the next word following the prefix if it
                // has already followed the prefix or set the count to 1 if it never occurred
                // following the prefix
                count = nextWordCounts.get(nextWord);
                if (count == null) {
                    nextWordCounts.put(nextWord, 1);
                } else {
                    nextWordCounts.put(nextWord, count + 1);
                }
                ngrams.put(prefix, nextWordCounts);
            }
            return ngrams;
        }

        /**
         * Calculate PKN for all unigrams using continuation counts
         *
         * @return Returns true if successful
         */
        boolean calculateUnigramsPKN() {
            String unigram;
            // Interpolated Kneser-Ney smoothing parameters
            int continuationCount, totalContinuationCounts;
            double pkn;

            HashMap<String, Integer> nextWordCounts;
            HashMap<String, HashMap<String, Double>> nGramsPlusPKN = new HashMap<>();
            HashMap<String, Double> nextWordPKNs;

            // totalContinuationCounts is equal to the total number of different
            // combinations of prefix and word which form an n-gram at least once,
            // which is effectively equal to the sum of the number of entries
            // in each sub-HashMap (nextWordCounts) in the bigrams HashMap
            totalContinuationCounts = 0;
            for (HashMap<String, Integer> nextWordsHashMap : bigrams.values()) {
                totalContinuationCounts += nextWordsHashMap.size();
                // Check if task has been cancelled
                if (isCancelled())
                    return false;
            }

            // Calculate pkn for all unigrams
            for (Map.Entry<String, HashMap<String, Integer>> entry : unigrams.entrySet()) {
                nextWordCounts = entry.getValue();
                nextWordPKNs = new HashMap<>();
                for (Map.Entry<String, Integer> entry2 : nextWordCounts.entrySet()) {
                    // Check if task has been cancelled
                    if (isCancelled())
                        return false;

                    unigram = entry2.getKey();

                    // continuationCount is equal to the number of different
                    // prefixes which precede the word at least once
                    continuationCount = 0;
                    // 1) Go through all prefixes.
                    // 2) If word follows prefix, increment continuationCount
                    for (HashMap<String, Integer> nextWordsHashMap : bigrams.values()) {
                        // Check if task has been cancelled
                        if (isCancelled())
                            return false;

                        if (nextWordsHashMap.containsKey(unigram))
                            continuationCount++;
                    }

                    // Evaluate pkn
                    pkn = (double) continuationCount / totalContinuationCounts;
                    nextWordPKNs.put(unigram, pkn);
                    System.out.println("Unigram: " + unigram
                            + " PKN: " + pkn);
                }
                nGramsPlusPKN.put(entry.getKey(), nextWordPKNs);
            }
            unigramsPKN = nGramsPlusPKN;
            return true;
        }

        /**
         * Calculate pkn for all bigrams using continuation counts
         *
         * @return Returns true if successful
         */
        boolean calculateBigramsPKN() {
            // Interpolated Kneser-Ney smoothing parameters
            int n1 = 0, n2 = 0;
            double d;
            int n1Plus;
            int continuationCount;
            int totalContinuationCounts;
            double firstPart, secondPart, lastPart;
            double pkn;

            String nextWord;
            String prefixSplit[];
            String p2, p2Test;

            HashMap<String, Integer> nextWordCounts;
            HashMap<String, HashMap<String, Double>> nGramsPlusPKN = new HashMap<>();
            HashMap<String, Double> nextWordPKNs;

            // Calculate n1 and n2
            for (HashMap<String, Integer> nextWordsHashMap : bigrams.values()) {
                // Check if task has been cancelled
                if (isCancelled())
                    return false;

                // n1, n2 are the total number of n-grams with exactly one and two
                // counts, respectively, in the training data
                n1 += Collections.frequency(nextWordsHashMap.values(), 1);
                n2 += Collections.frequency(nextWordsHashMap.values(), 2);
            }
            // Calculate discount D
            d = (double) n1 / (n1 + (2 * n2));

            // Go through each bigram
            for (Map.Entry<String, HashMap<String, Integer>> entry : bigrams.entrySet()) {
                p2 = entry.getKey();
                nextWordCounts = entry.getValue();
                nextWordPKNs = new HashMap<>();

                // n1Plus = the number of words that appear after the prefix at least once
                n1Plus = nextWordCounts.size();

                // totalContinuationCounts is equal to the total number of different combinations of P1 and Word
                // which form an n-gram at least once, given P2
                totalContinuationCounts = 0;
                for (Map.Entry<String, HashMap<String, Integer>> trigramEntry : trigrams.entrySet()) {
                    // Check if task has been cancelled
                    if (isCancelled())
                        return false;

                    prefixSplit = trigramEntry.getKey().split(" ");
                    p2Test = prefixSplit[1];
                    if (p2Test.equals(p2))
                        totalContinuationCounts += trigramEntry.getValue().size();
                }

                for (Map.Entry<String, Integer> entry2 : nextWordCounts.entrySet()) {
                    // Check if task has been cancelled
                    if (isCancelled())
                        return false;

                    nextWord = entry2.getKey();

                    // continuationCount is equal to the number of different P1s, followed by the given P2 and Word
                    // which form at least one n-gram
                    continuationCount = 0;
                    // 1) Go through all trigram prefixes.
                    // 2) If word follows prefix and p2Test = p2, increment continuationCount
                    for (Map.Entry<String, HashMap<String, Integer>> trigramEntry : trigrams.entrySet()) {
                        // Check if task has been cancelled
                        if (isCancelled())
                            return false;

                        prefixSplit = (trigramEntry.getKey()).split(" ");
                        p2Test = prefixSplit[1];
                        if (trigramEntry.getValue().containsKey(nextWord) && p2Test.equals(p2))
                            continuationCount++;
                    }

                    // Evaluate firstPart
                    firstPart = (Math.max(continuationCount - d, 0)) / totalContinuationCounts;
                    // Evaluate secondPart
                    secondPart = (d / totalContinuationCounts) * n1Plus;
                    // Evaluate lastPart
                    lastPart = unigramsPKN.get(" ").get(nextWord);
                    // Evaluate PKN and add n-gram to hashmap containing n-grams and PKNs
                    pkn = firstPart + (secondPart * lastPart);
                    nextWordPKNs.put(nextWord, pkn);
                    System.out.println("Prefix: " + p2
                            + " Next word: " + nextWord
                            + " PKN: " + pkn);
                }
                nGramsPlusPKN.put(p2, nextWordPKNs);
            }
            bigramsPKN = nGramsPlusPKN;

            // Check if task has been cancelled
            if (isCancelled())
                return false;

            // Store bigrams hashmap in serialised file for future retrieval
            try {
                FileOutputStream fileOutputStream =
                        new FileOutputStream("C:\\Users\\Sylvan\\Google Drive\\UOM-Sylvan\\" +
                                "3rd Year\\FYP\\Resources\\Evaluation\\Ngrams\\bigrams.ser");
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(nGramsPlusPKN);
                objectOutputStream.close();
                fileOutputStream.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("Check line: " + e.getStackTrace()[0].getLineNumber()
                        + " in class: " + e.getStackTrace()[0].getClassName());
            }
            return true;
        }

        /**
         * Calculate pkn for all trigrams using actual corpus counts
         *
         * @return Returns true if successful
         */
        boolean calculateTrigramsPKN() {
            // Interpolated Kneser-Ney smoothing parameters
            int n1 = 0, n2 = 0;
            double d;
            int n1Plus;
            int totalCounts;
            double firstPart, secondPart, lastPart;
            double pkn;

            String prefix, nextWord;
            String prefixSplit[];
            String p2;

            HashMap<String, Integer> nextWordCounts;
            HashMap<String, HashMap<String, Double>> nGramsPlusPKN = new HashMap<>();
            HashMap<String, Double> nextWordPKNs;

            // Calculate n1 and n2
            for (HashMap<String, Integer> nextWordsHashMap : trigrams.values()) {
                // Check if task has been cancelled
                if (isCancelled())
                    return false;

                // n1, n2 are the total number of n-grams with exactly one and two
                // counts, respectively, in the training data
                n1 += Collections.frequency(nextWordsHashMap.values(), 1);
                n2 += Collections.frequency(nextWordsHashMap.values(), 2);
            }
            // Calculate discount D
            d = (double) n1 / (n1 + (2 * n2));

            // Go through each n-gram
            for (Map.Entry<String, HashMap<String, Integer>> entry : trigrams.entrySet()) {
                // Check if task has been cancelled
                if (isCancelled())
                    return false;

                prefix = entry.getKey();
                prefixSplit = prefix.split(" ");
                p2 = prefixSplit[1];
                nextWordCounts = entry.getValue();
                nextWordPKNs = new HashMap<>();

                // Get the sum of counts for all n-grams having the same prefix
                totalCounts = 0;
                for (Integer wordCount : nextWordCounts.values()) {
                    // Check if task has been cancelled
                    if (isCancelled())
                        return false;

                    totalCounts += wordCount;
                }

                for (Map.Entry<String, Integer> entry2 : nextWordCounts.entrySet()) {
                    // Check if task has been cancelled
                    if (isCancelled())
                        return false;

                    nextWord = entry2.getKey();

                    // n1Plus = the number of words that appear after the prefix at least once
                    n1Plus = nextWordCounts.size();

                    // Evaluate firstPart
                    firstPart = (Math.max(entry2.getValue() - d, 0)) / totalCounts;
                    // Evaluate secondPart
                    secondPart = (d / totalCounts) * n1Plus;
                    // Evaluate lastPart
                    lastPart = bigramsPKN.get(p2).get(nextWord);
                    // Evaluate PKN and add n-gram to hashmap containing n-grams and PKNs
                    pkn = firstPart + (secondPart * lastPart);
                    nextWordPKNs.put(entry2.getKey(), pkn);
                    System.out.println("Prefix: " + prefix
                            + " Next word: " + nextWord
                            + " PKN: " + pkn);
                }
                nGramsPlusPKN.put(prefix, nextWordPKNs);
            }
            // Store trigrams hashmap in serialised file for future retrieval

            // Check if task has been cancelled
            if (isCancelled())
                return false;

            try {
                FileOutputStream fileOutputStream =
                        new FileOutputStream("C:\\Users\\Sylvan\\Google Drive\\UOM-Sylvan\\" +
                                "3rd Year\\FYP\\Resources\\Evaluation\\Ngrams\\trigrams.ser");
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(nGramsPlusPKN);
                objectOutputStream.close();
                fileOutputStream.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("Check line: " + e.getStackTrace()[0].getLineNumber()
                        + " in class: " + e.getStackTrace()[0].getClassName());
            }
            return true;
        }

        /**
         * Show notification that predictions have been updated
         * @param content   The message to show in the notification
         */
        private void showNotification(String content) {
            String title = "AAC predictions update";
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(
                            historyWeakReference.get(), "Predictions")
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(
                                    historyWeakReference.get().getResources(),
                                    R.mipmap.ic_launcher_round))
                            .setContentTitle(title)
                            .setContentText(content)
                            .setTicker(content)
                            .setAutoCancel(true)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            // Start Home when notification is clicked
            Intent startHome = new Intent(historyWeakReference.get(), Home.class);
            PendingIntent pendingIntent
                    = PendingIntent.getActivity(historyWeakReference.get(), 0,
                    startHome, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);

            // Set notification channel for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "AAC Predictions Update";
                String description = "Notifications related to predictions update";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel =
                        new NotificationChannel("Predictions", name, importance);
                channel.setDescription(description);
                // Register the channel with the system
                NotificationManager notificationManager =
                        (NotificationManager) historyWeakReference.get()
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }


            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(historyWeakReference.get());
            notificationManager.notify((int)System.currentTimeMillis()%10000, builder.build());
        }
    }
}

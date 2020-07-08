package mt.edu.um.malteseaacapp.activities;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import mt.edu.um.malteseaacapp.ImageObject;
import mt.edu.um.malteseaacapp.ImageSelectionFragment;
import mt.edu.um.malteseaacapp.Predictions;
import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.VariantManager;
import mt.edu.um.malteseaacapp.activities.category.AddCategory;
import mt.edu.um.malteseaacapp.activities.settings.EnterPin;
import mt.edu.um.malteseaacapp.activities.settings.History;
import mt.edu.um.malteseaacapp.activities.settings.Profile;
import mt.edu.um.malteseaacapp.activities.settings.ResetPin;
import mt.edu.um.malteseaacapp.activities.settings.Settings;
import mt.edu.um.malteseaacapp.activities.settings.ShowHidden;
import mt.edu.um.malteseaacapp.activities.settings.ShowRecovery;
import mt.edu.um.malteseaacapp.activities.word.AddWord;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.imagegrid.fragments.CoreFragment;
import mt.edu.um.malteseaacapp.imagegrid.fragments.ImageButtonsFragment;
import mt.edu.um.malteseaacapp.imagegrid.fragments.ImageGridFragment;
import mt.edu.um.malteseaacapp.imagegrid.fragments.KategorijiFragment;
import mt.edu.um.malteseaacapp.imagegrid.fragments.PredictionsFragment;
import mt.edu.um.malteseaacapp.imagegrid.fragments.VariantsFragment;

public class Home extends AppCompatActivity
        implements ImageGridFragment.OnImageButtonClickedListener,
        ImageGridFragment.OnCoreButtonClickedListener,
        ImageGridFragment.onHomeButtonClickedListener,
        KategorijiFragment.OnKategorijaClickedListener,
        ImageSelectionFragment.OnSelectionChangedListener,
        PredictionsFragment.FragmentRequestListener,
        SearchView.OnQueryTextListener{

    // Declare fragments
    private ImageSelectionFragment mImageSelectionFragment;
    private ImageButtonsFragment mImageButtonsFragment;
    private PredictionsFragment mPredictionsFragment;
    private KategorijiFragment mCategoryFragment;
    private ImageGridFragment mActiveFragment;
    private ImageGridFragment mBackFragment; // Keeps track of the fragment beneath the predictions fragment
    private VariantsFragment mVariantsFragment;
    private CoreFragment mCoreFragment;

    // To check whether pin has been entered successfully
    private final int ENTER_PIN_REQUEST_CODE = 0;
    private final int ENABLE_EDIT_MODE_CODE = 3;
    private final int RECOVERY_CALLBACK = 4;

    // To refresh fragments when necessary
    public static final int REFRESH_KATEGORIJI_FRAGMENT_CODE = 1;
    public static final int REFRESH_IMAGE_BUTTONS_FRAGMENT_CODE = 2;

    //  Set tags for fragments so that they can be found on configuration changes
    public static final String IMAGE_SELECTION_TAG = "ImageSelectionFragment";
    public static final String IMAGE_BUTTONS_TAG = "ImageButtonsFragment";
    public static final String PREDICTIONS_TAG = "PredictionsFragment";
    public static final String KATEGORIJI_TAG = "KategorijiFragment";
    public static final String VARIANTS_TAG = "VariantsFragment";
    public static final String CORE_TAG = "CoreFragment";

    private DatabaseAccess mDatabase;    // To access database
    private MenuItem mSearchMenuItem;    // Search view in action bar
    private MenuItem mBackspaceMenuItem; // Backspace view in action bar
    private Predictions mPredictions;    // To check for predictions and get n-grams HashMap
    private VariantManager mVariantManager; // Stores the variant cache

    // Flags
    private boolean mHasCoreWords;
    private boolean mShowVariants;
    private boolean mShowPredictions;
    private boolean mIsPinSet;

    private ActionMode.Callback mEditModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.edit, menu);

            Home.this.mActiveFragment.setEditable(true);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(R.string.edit);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Intent intent;

            switch (item.getItemId())
            {
                case R.id.action_add_word:
                    // Launch AddWord activity
                    intent = new Intent(Home.this, AddWord.class);
                    Home.this.startActivityForResult(intent, REFRESH_IMAGE_BUTTONS_FRAGMENT_CODE);
                    break;
                case R.id.action_add_category:
                    // Launch AddCategory activity
                    intent = new Intent(Home.this, AddCategory.class);
                    Home.this.startActivityForResult(intent, REFRESH_KATEGORIJI_FRAGMENT_CODE);
                    break;
                case R.id.action_show_items:
                    // Launch ShowHidden activity
                    intent = new Intent(Home.this, ShowHidden.class);
                    Home.this.startActivityForResult(intent, REFRESH_KATEGORIJI_FRAGMENT_CODE);
                    break;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Home.this.mActiveFragment.setEditable(false);
        }
    };

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Check if this is the first run, and start FirstRun activity if it is
        if (!prefs.getBoolean("first_run_complete", false)) {
            Intent firstRun = new Intent(getApplicationContext(),  FirstRun.class);

            startActivity(firstRun);
            finish();
        // Redirect user to History activity if predictions are being updated
        } else if (prefs.getBoolean("update_in_progress", false)) {
            Intent history = new Intent(getApplicationContext(), History.class);
            history.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            startActivity(history);
            finish();
        } else {
            // Set view to the home activity layout
            setContentView(R.layout.activity_home);

            // Get instance of DatabaseAccess and open connection
            mDatabase = DatabaseAccess.getInstance(getApplicationContext());
            mDatabase.open();

            // Set toolbar according to layout specified in xml
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            // Create instance of Predictions class
            mPredictions = new Predictions(this);

            // Create an instance of the VariantManager class
            mVariantManager = VariantManager.getInstance(getApplicationContext());

            // Set up image grid and image selection fragments
            setUpFragments(savedInstanceState);

            // Set flags
            mHasCoreWords = mDatabase.hasCoreUnigrams();
            mShowVariants = prefs.getString("variant_grouping", "Show").equals("Group");
            mShowPredictions = prefs.getBoolean("show_predictions", true);
            mIsPinSet = !prefs.getString("pin", "").isEmpty();

            // Make recovery if it does not exist
            if (!prefs.getBoolean("recovery_code_exists", false)) {
                // Set recovery code
                prefs.edit().putString("recovery_code", ResetPin.generateRecoveryCode(6)).commit();

                // Mark as created
                prefs.edit().putBoolean("recovery_code_exists", true).apply();

                // Show the recovery code
                Intent showRecovery = new Intent(Home.this, ShowRecovery.class);
                showRecovery.putExtra("title", getResources().getString(R.string.view_recovery_code));
                showRecovery.putExtra("recovery_k", prefs.getString("recovery_code", ""));

                startActivity(showRecovery);
            }
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();

        // The predictions fragment is always added on top of others, so we can simply remove it
        if (mActiveFragment == mPredictionsFragment) {
            onRemoveRequest();
        }
        else if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();

            setActiveFragment((ImageGridFragment) fm.getFragments().get(1)); // Rather hackish...
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);

        // Find menu items
        mBackspaceMenuItem = menu.findItem(R.id.action_backspace);
        mSearchMenuItem = menu.findItem(R.id.action_search);

        // If no items are selected, hide the backspace action and exit
        mBackspaceMenuItem.setVisible(mImageSelectionFragment.getImageObjects().size() > 0);

        // Set up search bar
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint(getString(R.string.search_hint)); // Set search hint
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName())); // Get suggestions provider
        searchView.setSubmitButtonEnabled(false); // Disable submit button (submission only possible through clicking suggestions)
        searchView.setOnQueryTextListener(this);

        // Hide if pin is not set
        MenuItem forgotPin = menu.findItem(R.id.action_forgot_pin);
        if (!mIsPinSet) {
            forgotPin.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.action_settings:
                // Launch Settings activity
                Intent settings = new Intent(getApplicationContext(), Settings.class);
                startActivity(settings);
                finish();
                return true;

            case R.id.action_profile:
                // Cannot access Profile activity if Dropbox upload service is running
                if (serviceRunning(Profile.DropboxUploadService.class))
                {
                    Toast.makeText(this,
                            "Please wait, profile backup in progress.",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                // Cannot access Profile activity if restore service is running
                if (serviceRunning(Profile.RestoreService.class))
                {
                    Toast.makeText(this,
                            "Please wait, profile restore in progress.",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                // Launch Profile activity otherwise
                Intent profile = new Intent(getApplicationContext(), Profile.class);
                startActivity(profile);
                finish();
                return true;

            case R.id.action_history:
                // Launch History activity
                Intent history = new Intent(getApplicationContext(), History.class);
                startActivity(history);
                finish();
                return true;

            case R.id.action_forgot_pin:
                // Launch reset pin activity
                Intent resetPin = new Intent(Home.this, ResetPin.class);
                Home.this.startActivityForResult(resetPin, RECOVERY_CALLBACK);

                return true;

            case R.id.action_backspace:
                // Delete last selected image
                mImageSelectionFragment.removeImageSelection();
                return true;

            case R.id.action_lock:
                // If no pin has been set, show launcher chooser immediately
                if (!mIsPinSet) {

                    showLauncherSelectionPrompt();

                } else { // Otherwise, ask for pin first

                    Intent enterPin = new Intent(this, EnterPin.class);
                    startActivityForResult(enterPin, ENTER_PIN_REQUEST_CODE);
                }
                return true;

            case R.id.action_edit:
                // If no pin is set, show edit mode immediately
                if (!mIsPinSet) {

                    startSupportActionMode(mEditModeCallback);

                } else { // Otherwise, ask for pin first

                    Intent enterPin = new Intent(this, EnterPin.class);
                    startActivityForResult(enterPin, ENABLE_EDIT_MODE_CODE);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onImageButtonClicked(final ImageObject imageObject, ImageView tappedImage) {
        // Animate button when clicked
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.scale);
        bounce.setInterpolator(new DecelerateInterpolator());

        tappedImage.startAnimation(bounce);

        if (mShowVariants && mVariantManager.isRoot(imageObject.getId()) && !(mActiveFragment == mCoreFragment || mActiveFragment == mPredictionsFragment || mActiveFragment == mVariantsFragment)) {
            showVariants(imageObject.getId());
        } else {
            mImageSelectionFragment.addImageSelection(imageObject, getApplicationContext());
        }
    }

    @Override
    public void onCoreButtonClicked() {
        if (mActiveFragment != mCoreFragment) {
            clearBackstack();

            showCore();
        }
    }

    @Override
    public void onHomeButtonClicked() {
        if (mActiveFragment != mCategoryFragment) {
            clearBackstack();

            if (mCategoryFragment == null) {
                mCategoryFragment = new KategorijiFragment();
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.imageGridFragment, mCategoryFragment, KATEGORIJI_TAG);
            ft.commit();

            setActiveFragment(mCategoryFragment);
        }
    }

    @Override
    public void onKategorijaClicked(String kategorija) {
        // Prepare bundle containing category to pass to imageButtonsFragment
        Bundle args = new Bundle();
        args.putString("Kategorija", kategorija);

        mImageButtonsFragment = new ImageButtonsFragment();
        mImageButtonsFragment.setArguments(args);

        // Add imageButtonsFragment to layout
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.imageGridFragment, mImageButtonsFragment, IMAGE_BUTTONS_TAG);
        ft.addToBackStack(mCategoryFragment.getTag());
        ft.commit();

        setActiveFragment(mImageButtonsFragment);
    }

    @Override
    public void onRemoveRequest() {
        // This is called by the predictions fragment to request its removal
        if (mActiveFragment == mPredictionsFragment) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(mPredictionsFragment);
            ft.commit();

            setActiveFragment(mBackFragment);
        }
    }

    @Override
    public void onSelectionChanged(ArrayList<ImageObject> imageObjects) {
        // If no items are selected, hide the backspace action and exit
        if (imageObjects.size() == 0) {
            mBackspaceMenuItem.setVisible(false);

            if (mActiveFragment == mPredictionsFragment) {
                onBackPressed();
            }

            return;
        }

        // Reveal the backspace action
        mBackspaceMenuItem.setVisible(true);

        // Load the predictions fragment if the setting is enabled and if predictions are available
        if (mShowPredictions && mActiveFragment != mCoreFragment) {
            HashMap<String, HashMap<String, Double>> ngrams = mPredictions.getPredictionsNGrams(imageObjects);

            if (ngrams != null) {
                // Find fragment by tag if it already exists
                mPredictionsFragment = (PredictionsFragment) getSupportFragmentManager().findFragmentByTag(PREDICTIONS_TAG);

                // If non-existent, create an instance of the PredictionsFragment class, add it to activity and associate a tag
                if (mPredictionsFragment == null) {
                    // Prepare bundle containing n-grams and prefix to be used in predictionsFragment
                    Bundle args = new Bundle();
                    args.putSerializable("Ngrams", ngrams);
                    args.putString("Prefix", mPredictions.getPrefix());

                    mPredictionsFragment = new PredictionsFragment();
                    mPredictionsFragment.setArguments(args);

                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.add(R.id.imageGridFragment, mPredictionsFragment, PREDICTIONS_TAG);
                    ft.commit();

                    setActiveFragment(mPredictionsFragment);
                }
                else {
                    // Replace predictions with new ones
                    mPredictionsFragment.setPrefix(mPredictions.getPrefix());
                    mPredictionsFragment.setNgrams(ngrams);
                    mPredictionsFragment.populateGridWithPredictions(ngrams, mPredictions.getPrefix());
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ENTER_PIN_REQUEST_CODE:
                // Show launcher selection prompt (to allow user to lock device to app)
                // if the correct pin was entered
                if (resultCode == RESULT_OK) showLauncherSelectionPrompt();
                break;
            case REFRESH_KATEGORIJI_FRAGMENT_CODE:
            case REFRESH_IMAGE_BUTTONS_FRAGMENT_CODE:
                if (resultCode == RESULT_OK) {
                    // Force a full refresh
                    mCoreFragment = null;
                    mCategoryFragment = new KategorijiFragment();
                    mHasCoreWords = mDatabase.hasCoreUnigrams();

                    onHomeButtonClicked();
                }
                break;

            case ENABLE_EDIT_MODE_CODE:
                if(resultCode == RESULT_OK) startSupportActionMode(mEditModeCallback);
                break;
            case RECOVERY_CALLBACK:
                if (resultCode == RESULT_OK) {
                    Intent showRecovery = new Intent(Home.this, ShowRecovery.class);
                    showRecovery.putExtra("title", "Your New Recovery Code");
                    showRecovery.putExtra("recovery_k", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("recovery_code", ""));

                    startActivity(showRecovery);
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDatabase != null)
            mDatabase.close();
    }

    @Override
    protected void onSaveInstanceState(Bundle outBundle) {
        super.onSaveInstanceState(outBundle);

        // Save the active and the back fragments' tags
        if (mActiveFragment != null) {
            outBundle.putString("ACTIVE_FRAGMENT", mActiveFragment.getTag());
        }

        if (mBackFragment != null) {
            outBundle.putString("BACK_FRAGMENT", mBackFragment.getTag());
        }
    }

    /**
     * Called when the user submits the query. This could be due to a key press on the
     * keyboard or due to pressing a submit button.
     * The listener can override the standard behavior by returning true
     * to indicate that it has handled the submit request. Otherwise return false to
     * let the SearchView handle the submission by launching any associated intent.
     *
     * @param query the query text that is to be submitted
     * @return true if the query has been handled by the listener, false to let the
     * SearchView perform the default action.
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    /**
     * Called when the query text is changed by the user.
     *
     * @param newText the new content of the query text field.
     * @return false if the SearchView should perform the default action of showing any
     * suggestions if available, true if the action was handled by the listener.
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent); // Handle the received intent
    }

    /**
     * Checks whether the specified service is currently running
     * @param service   The specified service
     * @return          Returns true if service is running and false otherwise
     */
    private boolean serviceRunning(Class<?> service) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo
                : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.getName().equals(runningServiceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Show prompt to select the default launcher
     * Achieved by enabling and then disabling a dummy launcher activity (MakeLauncher.class)
     */
    private void showLauncherSelectionPrompt() {
        // Disable Make Launcher activity
        PackageManager packageManager = this.getPackageManager();
        ComponentName componentName = new ComponentName(this, MakeLauncher.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        // Open launcher chooser
        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        this.startActivity(selector);

        // Disable MakeLauncher activity again
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }

    /**
     * Method used to set up the image grid and image selection fragments
     */
    private void setUpFragments(Bundle savedInstanceState) {
        // Create instance of FragmentManager (to add Fragments to the activity)
        FragmentManager fm = getSupportFragmentManager();

        // Find fragments by tag if they already exist
        mImageSelectionFragment = (ImageSelectionFragment) fm.findFragmentByTag(IMAGE_SELECTION_TAG);
        mCategoryFragment = (KategorijiFragment) fm.findFragmentByTag(KATEGORIJI_TAG);
        mImageButtonsFragment = (ImageButtonsFragment) fm.findFragmentByTag(IMAGE_BUTTONS_TAG);
        mPredictionsFragment = (PredictionsFragment) fm.findFragmentByTag(PREDICTIONS_TAG);
        mVariantsFragment = (VariantsFragment) fm.findFragmentByTag(VARIANTS_TAG);
        mCoreFragment = (CoreFragment) fm.findFragmentByTag(CORE_TAG);

        // If non-existent, create an instance of the ImageSelectionFragment class,
        // add it to the activity and associate a tag
        if (mImageSelectionFragment == null) {
            mImageSelectionFragment = new ImageSelectionFragment();

            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.imageSelectionFragment, mImageSelectionFragment, IMAGE_SELECTION_TAG);
            ft.commitNow();
        }

        // If no other fragment are being managed, create an instance of the category fragment
        if (fm.getFragments().size() == 1) {
            mCategoryFragment = new KategorijiFragment();

            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.imageGridFragment, mCategoryFragment, KATEGORIJI_TAG);
            ft.commit();

            setActiveFragment(mCategoryFragment);
        } else if (savedInstanceState != null) {
            // Retrieve the active fragment from the last saved state
            String tag1 = savedInstanceState.getString("ACTIVE_FRAGMENT", "");
            String tag2 = savedInstanceState.getString("BACK_FRAGMENT", "");

            if (!tag1.isEmpty()) {
                setActiveFragment((ImageGridFragment) getSupportFragmentManager().findFragmentByTag(tag1));
            }

            if (!tag2.isEmpty()) {
                mBackFragment = (ImageButtonsFragment) fm.findFragmentByTag(tag2);
            }
        }
    }

    /**
     * Handles the received intent
     * @param intent The received intent
     */
    private void handleIntent(Intent intent) {
        // Handle search intent by adding clicked suggestion to image selection
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // Obtain word from clicked suggestion
            String word = intent.getDataString();
            // Get image object corresponding to the word
            ImageObject imageObject = mDatabase.getImageObject(getApplicationContext(), word);
            // Collapse search view on click
            mSearchMenuItem.collapseActionView();
            // Add image object to the image selection fragment at the top
            mImageSelectionFragment.addImageSelection(imageObject, getApplicationContext());
        }
    }

    private void showVariants(int id) {
        Bundle args = new Bundle();
        args.putInt("RootId", id);

        mVariantsFragment = new VariantsFragment();
        mVariantsFragment.setArguments(args);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.imageGridFragment, mVariantsFragment, VARIANTS_TAG);
        ft.addToBackStack(mActiveFragment.getTag());
        ft.commit();

        setActiveFragment(mVariantsFragment);
    }

    private void showCore() {
        if (!mHasCoreWords) {
            Toast toast = Toast.makeText(getApplicationContext(), "No core words available", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        } else {
            if (mCoreFragment == null) {
                mCoreFragment = new CoreFragment();
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.imageGridFragment, mCoreFragment, CORE_TAG);
            ft.commit();

            setActiveFragment(mCoreFragment);
        }
    }

    private void invalidateCore(boolean goHome) {
        if (mActiveFragment == mCoreFragment) {
            onHomeButtonClicked();
        }

        mHasCoreWords = mDatabase.hasCoreUnigrams();
        mCoreFragment = null;
    }

    private void setActiveFragment(ImageGridFragment fragment) {
        if (mActiveFragment != fragment) {
            // If the active fragment is currently editable, set the new one to editable as well
            if (mActiveFragment != null && mActiveFragment.isEditable()) {
                mActiveFragment.setEditable(false);

                fragment.setEditable(true);
            }

            mBackFragment = fragment == mPredictionsFragment ? mActiveFragment : null;
            mActiveFragment = fragment;
        }
    }

    private void clearBackstack() {
        FragmentManager fm = getSupportFragmentManager();

        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack();
        }
    }
}
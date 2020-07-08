package mt.edu.um.malteseaacapp.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import mt.edu.um.malteseaacapp.R;
import mt.edu.um.malteseaacapp.activities.Home;
import mt.edu.um.malteseaacapp.database.DatabaseAccess;
import mt.edu.um.malteseaacapp.fragments.CheckListFragment;

public class ShowHidden extends AppCompatActivity implements View.OnClickListener {

    private CheckListFragment mWordsFragment;
    private CheckListFragment mCategoriesFragment;
    private DatabaseAccess mDatabase;

    private final String WORDS_TAG = "WordsFragment";
    private final String CATEGORIES_TAG = "CategoriesFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_show_hidden);

        // Get database connector
        mDatabase = DatabaseAccess.getInstance(getApplicationContext());

        // Retrieve views
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        Button showSelectedButton = findViewById(R.id.showAllButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        showSelectedButton.setOnClickListener(this);
        cancelButton.setOnClickListener(view -> onBackPressed());

        // Set up fragments
        Bundle b1 = new Bundle();
        Bundle b2 = new Bundle();
        b1.putStringArrayList("ITEMS", mDatabase.getHiddenWords());
        b2.putStringArrayList("ITEMS", mDatabase.getHiddenCategories());

        mWordsFragment = new CheckListFragment();
        mWordsFragment.setArguments(b1);
        mCategoriesFragment = new CheckListFragment();
        mCategoriesFragment.setArguments(b2);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.frameLayout, mWordsFragment, WORDS_TAG);
        ft.add(R.id.frameLayout, mCategoriesFragment, CATEGORIES_TAG);
        ft.commit();

        ft = getSupportFragmentManager().beginTransaction();
        ft.hide(mCategoriesFragment);
        ft.commit();

        // Set up tab listener
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

                switch (tab.getPosition()) {
                    case 1:
                        ft.hide(mWordsFragment);
                        ft.show(mCategoriesFragment);
                        break;
                    default:
                        ft.hide(mCategoriesFragment);
                        ft.show(mWordsFragment);
                }

                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }

    @Override
    public void onClick(View view) {
        CheckListFragment target = mWordsFragment.isVisible() ? mWordsFragment : mCategoriesFragment;
        List<String> items = target.getSelected();

        if (items.size() > 0) {
            boolean result = (target == mWordsFragment) ? mDatabase.showWords(items) : mDatabase.showCategories(items);

            if (result) {
                target.removeSelected();
                setResult(RESULT_OK);
            } else {
                Toast.makeText(getApplicationContext(), R.string.database_error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
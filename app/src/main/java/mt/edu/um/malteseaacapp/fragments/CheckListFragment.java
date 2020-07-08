package mt.edu.um.malteseaacapp.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import mt.edu.um.malteseaacapp.CheckListModel;
import mt.edu.um.malteseaacapp.CheckListViewAdapter;
import mt.edu.um.malteseaacapp.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class CheckListFragment extends Fragment implements SearchView.OnQueryTextListener {
    private List<CheckListModel> mItems;
    private List<String> mItemsSource;
    private RecyclerView mRecyclerView;

    public CheckListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Retrieve items
        mItemsSource = getArguments() != null ? getArguments().getStringArrayList("ITEMS") : null;

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_check_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mItemsSource != null) {
            mItems = new ArrayList<>(mItemsSource.size());

            for (String s : mItemsSource) {
                mItems.add(new CheckListModel(s, false));
            }

            mRecyclerView = getView().findViewById(R.id.wordRecycler);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            mRecyclerView.setAdapter(new CheckListViewAdapter(mItems));

            // Set up search
            SearchView searchView = getView().findViewById(R.id.searchView);
            searchView.setOnQueryTextListener(this);

            // Set up "Select All" checkbox
            AppCompatCheckBox checkBoxSelectAll = Objects.requireNonNull(getView()).findViewById(R.id.checkBoxSelectAll);
            checkBoxSelectAll.setOnCheckedChangeListener((compoundButton, b) -> {
                for (CheckListModel i : mItems) {
                    i.setSelected(b);
                }

                refresh();
            });

            mItemsSource = null;
        }
    }

    public List<String> getSelected() {
        List<String> result = new ArrayList<>();

        if (mItems != null) {
            for (CheckListModel m : mItems) {
                if (m.isSelected()) result.add(m.getText());
            }
        }

        return result;
    }

    public void removeSelected() {
        if (mItems != null) {
            Iterator<CheckListModel> iter = mItems.iterator();

            while (iter.hasNext()) {
                if (iter.next().isSelected()) iter.remove();
            }

            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    public void refresh() {
        if (mRecyclerView != null) {
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        CheckListViewAdapter adapter = (CheckListViewAdapter) mRecyclerView.getAdapter();
        adapter.filter(query);

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        CheckListViewAdapter adapter = (CheckListViewAdapter) mRecyclerView.getAdapter();
        adapter.filter(newText);

        return true;
    }
}

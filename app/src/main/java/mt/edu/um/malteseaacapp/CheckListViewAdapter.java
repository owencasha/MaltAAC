package mt.edu.um.malteseaacapp;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class CheckListViewAdapter extends RecyclerView.Adapter<CheckListViewAdapter.CheckListViewHolder> {
    public class CheckListViewHolder extends RecyclerView.ViewHolder {
        AppCompatCheckBox checkBox;

        public CheckListViewHolder(View itemView) {
            super(itemView);

            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    private List<CheckListModel> mItems;
    private List<CheckListModel> mItemsCopy;

    public CheckListViewAdapter(List<CheckListModel> items) {
        mItems = items;
        mItemsCopy = new ArrayList<>(items);
    }

    @NonNull
    @Override
    public CheckListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.check_list_item, parent, false);
        return new CheckListViewAdapter.CheckListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckListViewHolder holder, int position) {
        CheckListModel item = mItems.get(position);

        holder.checkBox.setText(item.getText());
        holder.checkBox.setChecked(item.isSelected());
        holder.checkBox.setOnCheckedChangeListener((compoundButton, b) -> mItems.get(position).setSelected(b));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void filter(String text) {
        mItems.clear();

        if (text.isEmpty()) {
            mItems.addAll(mItemsCopy);
        }
        else {
            text = text.toLowerCase();

            for (CheckListModel m: mItemsCopy) {
                if(m.getText().toLowerCase().startsWith(text)) {
                    mItems.add(m);
                }
            }
        }

        notifyDataSetChanged();
    }
}
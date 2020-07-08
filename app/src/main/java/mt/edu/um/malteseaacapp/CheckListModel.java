package mt.edu.um.malteseaacapp;

public class CheckListModel {
    private String mText;
    private Boolean mSelected;

    public CheckListModel(String text, Boolean selected) {
        mText = text;
        mSelected = selected;
    }

    public String getText() {
        return mText;
    }

    public Boolean isSelected() {
        return mSelected;
    }

    public void setText(String text) {
        mText = text;
    }

    public void setSelected(Boolean selected) {
        mSelected = selected;
    }
}

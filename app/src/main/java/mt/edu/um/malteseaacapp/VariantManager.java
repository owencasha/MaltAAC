package mt.edu.um.malteseaacapp;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import mt.edu.um.malteseaacapp.database.DatabaseAccess;

public class VariantManager {
    private static VariantManager mInstance;
    private HashMap<Integer, Collection<Integer>> mRoots; // Maps Roots to Variants
    private HashMap<Integer, Integer> mVariants; // Maps Variants to Roots
    private DatabaseAccess mDatabase;

    private VariantManager(Context context) {
        mDatabase = DatabaseAccess.getInstance(context);
        buildCache();
    }

    public static VariantManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VariantManager(context);
        }

        return mInstance;
    }

    public void buildCache() {
        // Fetch all variants from the database, i.e. records with non-null roots
        try (Cursor cursor = mDatabase.getAllVariants()) {
            mRoots = new HashMap<>();
            mVariants = new HashMap<>();

            while (cursor.moveToNext()) {
                Collection<Integer> coll;
                int val = cursor.getInt(0); // Id
                int key = cursor.getInt(1); // Root Id

                if ((coll = mRoots.get(key)) != null) {
                    coll.add(val);
                } else {
                    mRoots.put(key, new ArrayList<>(Collections.singletonList(val)));
                }

                mVariants.put(val, key);
            }
        }
    }

    public boolean isRootOrVariant(int id) {
        return isVariant(id) || isRoot(id);
    }

    public boolean isVariant(int id) {
        return mVariants.containsKey(id);
    }

    public boolean isRoot(int id) {
        return mRoots != null && mRoots.containsKey(id);
    }

    public int getRootId(int variantId) {
        Integer value = mVariants.get(variantId);

        return value != null ? value : 0;
    }

    public void addVariant(int rootId, int variantId) {
        if (BuildConfig.DEBUG && !(rootId > 0 && variantId > 0)) {
            throw new AssertionError("Assertion failed");
        }

        Collection<Integer> coll;

        if ((coll = mRoots.get(rootId)) != null) {
            coll.add(variantId);
        } else {
            mRoots.put(rootId, new ArrayList<>(Collections.singletonList(variantId)));
        }

        mVariants.put(variantId, rootId);
    }

    public void modifyVariant(int newRootId, int variantId) {
        if (BuildConfig.DEBUG && !(newRootId > 0 && variantId > 0 && mVariants.containsKey(variantId))) {
            throw new AssertionError("Assertion failed");
        }

        detach(variantId);
        addVariant(newRootId, variantId);
    }

    public void removeEntry(int id) {
        if (BuildConfig.DEBUG && id <= 0) {
            throw new AssertionError("Assertion failed");
        }

        if (isRoot(id)) {
            removeAll(id);
        } else if (isVariant(id)) {
            removeVariant(id);
        }
    }

    public void removeVariant(int variantId) {
        if (BuildConfig.DEBUG && !(variantId > 0 && mVariants.containsKey(variantId))) {
            throw new AssertionError("Assertion failed");
        }

        detach(variantId);
    }

    public void removeAll(int rootId) {
        if (BuildConfig.DEBUG && !(rootId > 0 && mRoots.containsKey(rootId))) {
            throw new AssertionError("Assertion failed");
        }

        Collection<Integer> coll;

        if ((coll = mRoots.get(rootId)) != null) {
            for (Integer i : coll) {
                mVariants.remove(i);
            }

            mRoots.remove(rootId);
        }
    }

    private void detach(int variantId) {
        Integer id = mVariants.get(variantId);

        if (id != null) {
            Collection<Integer> coll = mRoots.get(id);

            if (coll != null) {
                coll.remove(variantId);

                mVariants.remove(variantId);

                // If the old root no longer has any variants, remove its entry entirely
                if (coll.isEmpty()) {
                    mRoots.remove(id);
                }
            }
        }
    }
}

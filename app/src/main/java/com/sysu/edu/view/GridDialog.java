package com.sysu.edu.view;

import android.view.View;
import android.widget.GridLayout;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.databinding.DialogGridBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class GridDialog {

    private final DialogGridBinding menuBinding;
    private final BottomSheetDialog menuDialog;
    private final FragmentActivity activity;
    ArrayList<Integer> referenceIds;

    public GridDialog(FragmentActivity activity) {
        this.activity = activity;
        menuDialog = new BottomSheetDialog(activity);
        menuBinding = DialogGridBinding.inflate(activity.getLayoutInflater());
        menuDialog.setContentView(menuBinding.getRoot());
    }

    public void loadMenu(List<Integer> menuTitle, List<Integer> menuIcon, List<Consumer<MaterialButton>> menuAction) {
        referenceIds = new ArrayList<>();
        IntStream.range(0, menuTitle.size()).forEach(i -> {
            MaterialButton menu = new MaterialButton(activity, null, androidx.appcompat.R.attr.borderlessButtonStyle);
            menu.setText(menuTitle.get(i));
            menu.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_TOP);
            if (menuIcon.size() > i && menuIcon.get(i) != 0)
                menu.setIconResource(menuIcon.get(i));
            int id = View.generateViewId();
            referenceIds.add(id);
            menu.setId(id);
            if (menuAction.size() > i && menuAction.get(i) != null)
                menu.setOnClickListener(_ -> menuAction.get(i).accept(menu));
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 0;
            gp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1.0f);
            menuBinding.grid.addView(menu, gp);
        });
    }

    public void show() {
        menuDialog.show();
        menuDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public BottomSheetDialog getDialog() {
        return menuDialog;
    }

    public MaterialButton getMenu(int position) {
        if (referenceIds == null || position >= referenceIds.size())
            return null;
        return menuBinding.grid.findViewById(referenceIds.get(position));
    }
}

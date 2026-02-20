package com.sysu.edu.view;

import android.content.res.ColorStateList;
import android.view.View;
import android.widget.GridLayout;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.R;
import com.sysu.edu.databinding.DialogGridBinding;
import com.sysu.edu.databinding.ItemButtonBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class GridDialog {

    private final DialogGridBinding menuBinding;
    private final BottomSheetDialog menuDialog;
    private final FragmentActivity activity;
    int selected = -1;
    boolean selectable = false;
    ArrayList<Integer> referenceIds = new ArrayList<>();

    public GridDialog(FragmentActivity activity) {
        this.activity = activity;
        menuDialog = new BottomSheetDialog(activity);
        menuBinding = DialogGridBinding.inflate(activity.getLayoutInflater());
        menuDialog.setContentView(menuBinding.getRoot());
    }


    public void setColumn(int column) {
        ((GridLayout.LayoutParams) menuBinding.handler.getLayoutParams()).columnSpec = GridLayout.spec(GridLayout.UNDEFINED, column, GridLayout.FILL, 1.0f);
        menuBinding.grid.setColumnCount(column);
    }

    public void setIconGravity(int gravity) {
        referenceIds.forEach(id -> ((MaterialButton) menuBinding.grid.findViewById(id)).setIconGravity(gravity));
    }

    public void setGravity(int gravity) {
        referenceIds.forEach(id -> ((MaterialButton) menuBinding.grid.findViewById(id)).setGravity(gravity));
    }

    public <T> void loadMenu(List<T> menuTitle, List<Integer> menuIcon, List<Consumer<MaterialButton>> menuAction, Class<T> type) {
        referenceIds.clear();
        IntStream.range(0, menuTitle.size()).forEach(i -> {
            MaterialButton menu = ItemButtonBinding.inflate(activity.getLayoutInflater(), menuBinding.grid, false).getRoot();
            if (type == Integer.class)
                menu.setText((Integer) menuTitle.get(i));
            else
                menu.setText((String) menuTitle.get(i));
            if (menuIcon.size() > i && menuIcon.get(i) != 0)
                menu.setIconResource(menuIcon.get(i));
            int id = View.generateViewId();
            referenceIds.add(id);
            menu.setId(id);
            menu.addOnCheckedChangeListener((_, isChecked) -> menu.setStrokeWidth(isChecked && selectable ? 3 : 0));
            if (menuAction.size() > i && menuAction.get(i) != null)
                menu.setOnClickListener(_ -> {
                    menuAction.get(i).accept(menu);
                    if (selectable)
                        selectMenu(i);
                });
            menuBinding.grid.addView(menu);
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
        if (referenceIds == null || position >= referenceIds.size() || position < 0)
            return null;
        return menuBinding.grid.findViewById(referenceIds.get(position));
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public void selectMenu(int position) {
        MaterialButton menu = getMenu(position);
        if (menu == null || position == selected)
            return;
        menu.setChecked(true);
        if (selected >= 0) {
            MaterialButton selectedMenu = getMenu(selected);
            if (selectedMenu != null)
                selectedMenu.setChecked(false);
        }
        selected = position;
    }

    public void toggleMenu(int position) {
        MaterialButton menu = getMenu(position);
        if (menu != null)
            menu.setChecked(!menu.isChecked());
    }

    public void toggleMenu(int position, boolean toggle) {
        MaterialButton menu = getMenu(position);
        if (menu != null)
            menu.setChecked(toggle);
    }


    public void selectMenu(int[] positions) {
        IntStream.of(positions).forEach(this::selectMenu);
    }

    public void setTogglable(int[] positions, boolean togglable) {
        IntStream.of(positions).forEach(i -> setTogglable(i, togglable));
    }

    public void setTogglable(int position, boolean togglable) {
        MaterialButton menu = getMenu(position);
        if (menu != null) {
            ColorStateList colorStateList = AppCompatResources.getColorStateList(activity, R.color.toggle);
            menu.setToggleCheckedStateOnClick(togglable);
            menu.setCheckable(togglable);
            menu.setIconTint(togglable ? colorStateList : null);
            menu.setTextColor(togglable ? colorStateList : null);
        }
    }

}

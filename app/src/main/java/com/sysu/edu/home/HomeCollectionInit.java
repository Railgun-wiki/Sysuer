package com.sysu.edu.home;
/*

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.DialogServiceActionBinding;
import com.sysu.edu.databinding.DialogServiceOrderBinding;
import com.sysu.edu.databinding.FragmentDashboardBinding;

import java.util.stream.IntStream;

import io.noties.markwon.Markwon;

public class HomeCollectionInit {

    FragmentActivity activity;
    private BottomSheetDialog orderDialog;
    private CollectionAdapter collectionAdapter;
    private BottomSheetDialog actionDialog;
    private DialogServiceActionBinding actionBinding;
    private HomeViewModel viewModel;
    private HomeCollectionHelper db;
    private Params params;

    public void init(FragmentActivity fragmentActivity, LayoutInflater inflater) {
        activity = fragmentActivity;
        viewModel = new ViewModelProvider(fragmentActivity).get(HomeViewModel.class);
        db = new HomeCollectionHelper(fragmentActivity);
        params = new Params(fragmentActivity);
        initOrder(inflater);
        initAction(inflater);
    }

    void initOrder(@NonNull LayoutInflater inflater) {
        orderDialog = new BottomSheetDialog(activity);
        DialogServiceOrderBinding orderBinding = DialogServiceOrderBinding.inflate(inflater);
        orderBinding.recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        collectionAdapter = new CollectionAdapter();
        orderBinding.recyclerView.setAdapter(collectionAdapter);
        orderBinding.confirm.setOnClickListener(v -> {
            updateShortcut();
            getShortcutCollection();
            orderDialog.dismiss();
        });
        orderDialog.setContentView(orderBinding.getRoot());
        new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                collectionAdapter.swap(source.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }
        }).attachToRecyclerView(orderBinding.recyclerView);
    }

    void updateShortcut() {
        IntStream.range(0, collectionAdapter.getItemCount()).forEach(i -> {
            collectionAdapter.getItem(i);
            db.updateDashboardShortcutPosition(collectionAdapter.getItem(i).getInteger("id"), i);
        });
    }

    void initAction(@NonNull LayoutInflater inflater) {
        actionDialog = new BottomSheetDialog(activity);
        actionBinding = DialogServiceActionBinding.inflate(inflater);
        actionBinding.order.setOnClickListener(v -> orderDialog.show());
        actionDialog.setContentView(actionBinding.getRoot());
    }

    boolean initActionDialog(JSONObject item) {
        int itemId = item.getIntValue("id");
        MutableLiveData<Boolean> isServiceCollected = new MutableLiveData<>(db.isServiceCollected(itemId));
        MutableLiveData<Boolean> isShortcutCollected = new MutableLiveData<>(db.isDashboardShortcutCollected(itemId));
        actionBinding.collect.setText(Boolean.TRUE.equals(isServiceCollected.getValue()) ? R.string.cancel_collect : R.string.collect);
        actionBinding.addShortcut.setText(Boolean.TRUE.equals(isShortcutCollected.getValue()) ? R.string.cancel_add_shortcut : R.string.add_shortcut);
        actionBinding.collect.setOnClickListener(w -> {
            boolean isServiceCollect = Boolean.TRUE.equals(isServiceCollected.getValue());
            if (isServiceCollect) {
                db.deleteService(itemId);
                params.toast(R.string.cancel_collect_success);
            } else {
                db.addService(itemId, item.toJSONString(), collectionAdapter.getItemCount());
                params.toast(R.string.collect_success);
            }
            getShortcutCollection();
            actionBinding.collect.setText(isServiceCollect ? R.string.collect : R.string.cancel_collect);
            isServiceCollected.setValue(!isServiceCollect);
        });
        actionBinding.addShortcut.setOnClickListener(w -> {
            boolean isShortcutCollect = Boolean.TRUE.equals(isShortcutCollected.getValue());
            if (isShortcutCollect) {
                db.deleteDashboardShortcut(itemId);
                params.toast(R.string.cancel_add_shortcut_success);
            } else {
                db.addDashboardShortcut(itemId, item.toJSONString(), collectionAdapter.getItemCount());
                params.toast(R.string.add_shortcut_success);
            }
            viewModel.updateDashboardShortcut.setValue(true);
            actionBinding.addShortcut.setText(isShortcutCollect ? R.string.add_shortcut : R.string.cancel_add_shortcut);
            isShortcutCollected.setValue(!isShortcutCollect);
        });
        actionBinding.feedback.setOnClickListener(w -> activity.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format("https://github.com/%s/%s/issues/new?title=反馈：服务->%s&labels=bug,crash-report", "SYSU-Tang", "Sysuer", item.getString("name")))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        Markwon.create(activity).setMarkdown(actionBinding.description, String.format("### %s\n%s", item.getString("name"), item.getString("description")));
        actionDialog.show();
        return true;
    }

    void getShortcutCollection(FragmentDashboardBinding binding) {
        if (binding.shortcutGroup.getChildCount() > 4)
            IntStream.range(3, binding.shortcutGroup.getChildCount() - 1).forEach(i -> binding.shortcutGroup.removeViewAt(3));
        try (Cursor cursor = db.getWritableDatabase().query("dashboard_shortcut_collection", null, null, null, null, null, "position")) {
            if (cursor.moveToFirst()) {
                collectionAdapter.clear();
                do {
                    Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("shortcutId"));
                    JSONObject shortcut = JSON.parseObject(cursor.getString(cursor.getColumnIndexOrThrow("shortcutJson")));
                    MaterialButton button = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonTonalStyle);
                    button.setText(shortcut.getString("name"));
                    binding.shortcutGroup.addView(button);
                    if (viewModel.actionMap.containsKey(id))
                        button.setOnClickListener(viewModel.actionMap.get(id));
                    button.setOnLongClickListener(v -> initActionDialog(shortcut));
                    collectionAdapter.add(shortcut);
                } while (cursor.moveToNext());
            }
        }
    }

    void updateService() {
        IntStream.range(0, collectionAdapter.getItemCount()).forEach(i -> {
            collectionAdapter.getItem(i);
            db.updateServicePosition(collectionAdapter.getItem(i).getInteger("id"), i);
        });
    }


}
*/

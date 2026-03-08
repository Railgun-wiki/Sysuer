package com.sysu.edu.home;

import static android.text.TextUtils.isEmpty;
import static com.sysu.edu.api.CommonUtil.trim;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sysu.edu.R;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.Params;
import com.sysu.edu.browser.BrowserActivity;
import com.sysu.edu.databinding.DialogServiceActionBinding;
import com.sysu.edu.databinding.DialogServiceOrderBinding;
import com.sysu.edu.databinding.FragmentServiceBinding;
import com.sysu.edu.databinding.ItemActionChipBinding;
import com.sysu.edu.databinding.ItemServiceBoxBinding;
import com.sysu.edu.template.RecyclerAdapter;

import org.commonmark.node.Heading;
import org.commonmark.node.Node;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.core.CoreProps;
import io.noties.markwon.core.spans.LastLineSpacingSpan;

public class ServiceFragment extends Fragment {

    FragmentServiceBinding binding;
    Params params;
    BottomSheetDialog actionDialog;
    HomeCollectionHelper db;
    DialogServiceActionBinding actionBinding;
    BottomSheetDialog orderDialog;
    CollectionAdapter collectionAdapter;
    ItemServiceBoxBinding collectionBinding;
    HomeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentServiceBinding.inflate(inflater);
            params = new Params(this);
            // 初始化 actions HashMap
            viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
            // 初始化 dialog
            initAction(inflater);
            initOrder(inflater);
            JSONReader reader = JSONReader.of(getResources().openRawResource(R.raw.service), StandardCharsets.UTF_8);
            JSONArray array = reader.readJSONArray();
            reader.close();
            db = new HomeCollectionHelper(requireContext());
            addCollection(inflater);
            IntStream.range(0, array.size()).forEach(i -> {
                JSONObject serviceGroup = array.getJSONObject(i);
                this.binding.serviceContainer.addView(initBoxWithHashMap(inflater, serviceGroup.getString("name"), serviceGroup.getJSONArray("items")));
            });
        }
        return binding.getRoot();
    }

    void initAction(@NonNull LayoutInflater inflater) {
        actionDialog = new BottomSheetDialog(requireContext());
        actionBinding = DialogServiceActionBinding.inflate(inflater);
        actionBinding.order.setOnClickListener(_ -> orderDialog.show());
        actionDialog.setContentView(actionBinding.getRoot());
    }

    void initOrder(@NonNull LayoutInflater inflater) {
        Context context = requireContext();
        orderDialog = new BottomSheetDialog(context);
        DialogServiceOrderBinding orderBinding = DialogServiceOrderBinding.inflate(inflater);
        orderBinding.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        collectionAdapter = new CollectionAdapter();
        orderBinding.recyclerView.setAdapter(collectionAdapter);
        orderBinding.confirm.setOnClickListener(_ -> {
            updateService();
            updateServiceCollection();
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

    void addCollection(@NonNull LayoutInflater inflater) {
        JSONArray collection = getCollection();
        ViewGroup container = initBoxWithHashMap(inflater, getString(R.string.collect), collection);
        collectionBinding = ItemServiceBoxBinding.bind(container);
        if (collection.isEmpty()) container.setVisibility(View.GONE);
        container.getChildAt(0).setOnClickListener(_ -> orderDialog.show());
        binding.serviceContainer.addView(container, 0);
    }

    void updateServiceCollection() {
        JSONArray collection = getCollection();
        if (collection.isEmpty()) {
            collectionBinding.getRoot().setVisibility(View.GONE);
        } else {
            collectionBinding.getRoot().setVisibility(View.VISIBLE);
            collectionBinding.serviceBoxItems.removeAllViews();
            addItems(getLayoutInflater(), collection, collectionBinding);
        }
    }

    @NonNull
    private JSONArray getCollection() {
        Cursor cursor = db.getWritableDatabase().query("service_collection", null, null, null, null, null, "position ASC");
        JSONArray collection = new JSONArray();
        collectionAdapter.clear();
        while (cursor.moveToNext()) {
            JSONObject serviceJson = JSONObject.parse(cursor.getString(cursor.getColumnIndexOrThrow("serviceJson")));
            collection.add(serviceJson);
            collectionAdapter.add(serviceJson);
        }
        cursor.close();
        return collection;
    }


    ViewGroup initBoxWithHashMap(LayoutInflater inflater, String box_title, JSONArray items) {
        ItemServiceBoxBinding binding = ItemServiceBoxBinding.inflate(inflater);
        binding.serviceBoxTitle.setText(box_title);
        addItems(inflater, items, binding);
        return binding.getRoot();
    }

    void addItems(LayoutInflater inflater, JSONArray items, ItemServiceBoxBinding binding) {
        IntStream.range(0, items.size()).forEach(index -> {
            JSONObject item = items.getJSONObject(index);
            int itemId = item.getIntValue("id");
            ItemActionChipBinding chip = ItemActionChipBinding.inflate(inflater, binding.serviceBoxItems, false);
            View.OnClickListener action = viewModel.actionMap.get(itemId);
            String url = item.getString("url");
            chip.getRoot().setOnClickListener(action != null ? action : isEmpty(url) ? _ -> params.toast(R.string.undeveloped) : v -> startActivity(new Intent(requireContext(), BrowserActivity.class).setData(Uri.parse(url)), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v, "miniapp").toBundle()));
            chip.getRoot().setOnLongClickListener(_ -> showActionDialog(item));
            chip.getRoot().setText(item.getString("name"));
            binding.serviceBoxItems.addView(chip.getRoot());
        });
    }

    boolean showActionDialog(JSONObject item) {
        int itemId = item.getIntValue("id");
        MutableLiveData<Boolean> isServiceCollected = new MutableLiveData<>(db.isServiceCollected(itemId));
        MutableLiveData<Boolean> isShortcutCollected = new MutableLiveData<>(db.isDashboardShortcutCollected(itemId));
        actionBinding.collect.setText(Boolean.TRUE.equals(isServiceCollected.getValue()) ? R.string.cancel_collect : R.string.collect);
        actionBinding.addShortcut.setText(Boolean.TRUE.equals(isShortcutCollected.getValue()) ? R.string.cancel_add_shortcut : R.string.add_shortcut);
        actionBinding.collect.setOnClickListener(_ -> {
            boolean isServiceCollect = Boolean.TRUE.equals(isServiceCollected.getValue());
            if (isServiceCollect) {
                db.deleteService(itemId);
                params.toast(R.string.cancel_collect_success);
            } else {
                db.addService(itemId, item.toJSONString(), collectionAdapter.getItemCount());
                params.toast(R.string.collect_success);
            }
            updateServiceCollection();
            actionBinding.collect.setText(isServiceCollect ? R.string.collect : R.string.cancel_collect);
            isServiceCollected.setValue(!isServiceCollect);
        });
        actionBinding.addShortcut.setOnClickListener(_ -> {
            boolean isShortcutCollect = Boolean.TRUE.equals(isShortcutCollected.getValue());
            if (isShortcutCollect) {
                db.deleteDashboardShortcut(itemId);
                params.toast(R.string.cancel_add_shortcut_success);
            } else {
                db.addDashboardShortcut(itemId, item.toJSONString(), null);
                params.toast(R.string.add_shortcut_success);
            }
            viewModel.updateDashboardShortcut.setValue(true);
            actionBinding.addShortcut.setText(isShortcutCollect ? R.string.add_shortcut : R.string.cancel_add_shortcut);
            isShortcutCollected.setValue(!isShortcutCollect);
        });
        actionBinding.feedback.setOnClickListener(_ -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format("https://github.com/%s/%s/issues/new?title=反馈：服务->%s&labels=bug,crash-report", "SYSU-Tang", "Sysuer", item.getString("name")))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        actionBinding.openAsUrl.setOnClickListener(_ -> {
            String url = item.getString("url");
            if (!isEmpty(url))
                startActivity(new Intent(requireContext(), BrowserActivity.class).setData(Uri.parse(url)));
        });
        ContextUtil contextUtil = new ContextUtil(requireContext());
        Markwon.builder(requireContext()).usePlugin(new AbstractMarkwonPlugin() {
            @Override
            public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                super.configureSpansFactory(builder);
                builder.appendFactory(Heading.class, (_, configuration) -> {
                    if (CoreProps.HEADING_LEVEL.require(configuration) == 3)
                        return new ForegroundColorSpan(contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorPrimaryContainer));
                    return null;
                });
                builder.appendFactory(Heading.class, (_, _) -> new LastLineSpacingSpan(24));
            }


            @Override
            public void configureVisitor(@NonNull MarkwonVisitor.Builder builder) {
                super.configureVisitor(builder);
                builder.blockHandler(new MarkwonVisitor.BlockHandler() {
                    @Override
                    public void blockStart(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                    }

                    @Override
                    public void blockEnd(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                        if (visitor.hasNext(node))
                            visitor.ensureNewLine();
                    }
                });
            }
        }).build().setMarkdown(actionBinding.description, String.format("### %s\n%s\n\n%s", item.getString("name"), item.getString("description"), trim(item.getString("url"))));
        actionDialog.show();
        return true;
    }

    void updateService() {
        IntStream.range(0, collectionAdapter.getItemCount()).forEach(i -> {
            collectionAdapter.get(i);
            db.updateServicePosition(collectionAdapter.get(i).getInteger("id"), i);
        });
    }

    static class CollectionAdapter extends RecyclerAdapter<JSONObject> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(data.get(position).getString("name"));
            super.onBindViewHolder(holder, position);
        }
    }
}
package com.sysu.edu.view;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.academic.CourseDetailActivity;
import com.sysu.edu.databinding.ItemCourseSelectionBinding;

import java.util.ArrayList;
import java.util.function.Consumer;

public class CourseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final String[] info = new String[]{"credit", "clazzNum", "scheduleExamTime", "examFormName", "statusName"};
    final ArrayList<JSONObject> data = new ArrayList<>();
    Consumer<Integer> selectAction;
    Consumer<String> likeAction;

    public CourseAdapter() {
        super();
    }

    public void add(JSONObject e) {
        data.add(e);
        notifyItemInserted(getItemCount() - 1);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        ItemCourseSelectionBinding binding = ItemCourseSelectionBinding.inflate(LayoutInflater.from(context), parent, false);
        for (int i = 0; i < info.length; i++) {
            Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_action_chip, binding.courseInfo, false);
            chip.setOnLongClickListener(a -> {
                ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("", ((Chip) a).getText()));
                return false;
            });
            chip.setOnClickListener(a -> Snackbar.make(context, chip, ((Chip) a).getText(), Snackbar.LENGTH_LONG).show());
            binding.courseInfo.addView(chip);
        }
        return new RecyclerView.ViewHolder(binding.getRoot()) {
        };
    }

    public void setSelectAction(Consumer<Integer> action) {
        this.selectAction = action;
    }

    public void setLikeAction(Consumer<String> action) {
        this.likeAction = action;
    }

    public JSONObject getItem(int position) {
        return data.get(position);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemCourseSelectionBinding binding = ItemCourseSelectionBinding.bind(holder.itemView);
        Context context = binding.getRoot().getContext();
        binding.courseName.setText(String.format("%s-%s", convert(position, "courseNum"), convert(position, "courseName")));
        JSONObject item = data.get(position);
        Integer selectedStatus = item.getInteger("selectedStatus");
//        selectedStatus == 4 ? R.string.status_selected : selectedStatus == 3 ? R.string.filtering : selectedStatus==1? R.string.retired : R.string.unselected;
        item.fluentPut("statusName", context.getString(selectedStatus == 4 ? R.string.status_selected : selectedStatus == 3 ? R.string.filtering : selectedStatus == 1 ? R.string.retired : R.string.unselected));
        binding.like.setSelected(item.containsKey("collectionStatus") && item.getInteger("collectionStatus") == 1);
        binding.select.setSelected(item.containsKey("selectedStatus") && (selectedStatus == 3 || selectedStatus == 4));
        binding.select.setText(binding.select.isSelected() ? context.getString(R.string.drop_course) : context.getString(R.string.select_course));
        binding.like.setText(binding.like.isSelected() ? context.getString(R.string.unlike) : context.getString(R.string.like));
        binding.select.setOnClickListener(_ -> {
            if (selectAction != null)
                selectAction.accept(position);
        });
        binding.like.setOnClickListener(v -> {
            Snackbar.make(v, context.getString(R.string.already) + ((MaterialButton) v).getText(), Snackbar.LENGTH_LONG).show();
            ((MaterialButton) v).setText(v.isSelected() ? context.getString(R.string.unlike) : context.getString(R.string.like));
            if (likeAction != null)
                likeAction.accept(convert(position, "teachingClassId"));
            v.setSelected(!v.isSelected());
        });
        binding.open.setOnClickListener(v -> context.startActivity(new Intent(context, CourseDetailActivity.class).putExtra("code", convert(position, "courseNum")).putExtra("id", convert(position, "courseId")).putExtra("class", convert(position, "clazzNum")), ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context, v, "miniapp").toBundle()));
        binding.head.setText(convert(position, "teachingTimePlace").replace(";", " | ").replace(",", "\n"));
        String[] courseInfoLabels = context.getResources().getStringArray(R.array.course_info_labels);
        String[] seatInfoLabels = context.getResources().getStringArray(R.array.seat_info_labels);
        for (int i = 0; i < info.length; i++)
            ((Chip) binding.courseInfo.getChildAt(i)).setText(String.format("%s：%s", courseInfoLabels[i], convert(position, info[i])));
        String[] seats = new String[]{"baseReceiveNum", "filterSelectedNum", "courseSelectedNum"};
        for (int i = 0; i < seats.length; i++) {
            String content = convert(position, seats[i]);
            (new MaterialButton[]{binding.left, binding.filtering, binding.selected}[i]).setText(String.format("%s\n%s", seatInfoLabels[i], content));
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public String convert(int position, String key) {
        String a = data.get(position).getString(key);
        return (a == null ? "" : a).replace("\n\n", "\n");
    }

    public void clear() {
        int tmp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0, tmp);
    }
}

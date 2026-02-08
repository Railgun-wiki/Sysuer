package com.sysu.edu.academic;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityClassroomQueryBinding;
import com.sysu.edu.databinding.ItemClassroomResultBinding;
import com.sysu.edu.databinding.ItemFilterChipBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ClassroomQueryActivity extends AppCompatActivity {

    final HashMap<Integer, String> office = new HashMap<>();
    Handler handler;
    HttpManager http;
    String dateStr;
    String startClassTime = "1";
    String endClassTime = "11";
    final MutableLiveData<String> campusLiveData = new MutableLiveData<>();
    List<String> classType = List.of("002", "003");
    RoomAdapter roomAdapter;
    int page = 1;
    int total = 0;
    ActivityClassroomQueryBinding binding;
    Params params;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final MaterialDatePicker<Long> dateDialog = MaterialDatePicker.Builder.datePicker().build();
        final HashMap<String, ArrayList<Chip>> classroom = new HashMap<>();
        binding = ActivityClassroomQueryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        params.setCallback(this::getCampus);
        binding.campusSelectAll.setOnClickListener(v -> {
            for (int i = 1; i < ((ChipGroup) v.getParent()).getChildCount(); i++) {
                Chip chip = (Chip) ((ChipGroup) v.getParent()).getChildAt(i);
                chip.setChecked(!chip.isChecked());
            }
        });
        binding.officeSelectAll.setOnClickListener(v -> {
            for (int i = 1; i < ((ChipGroup) v.getParent()).getChildCount(); i++) {
                Chip chip = (Chip) ((ChipGroup) v.getParent()).getChildAt(i);
                chip.setChecked(!chip.isChecked());
            }
        });
        dateDialog.addOnPositiveButtonClickListener(selection -> {
            dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(selection));
            binding.dateText.setText(new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date(selection)));
        });
        roomAdapter = new RoomAdapter(this);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.result.setAdapter(roomAdapter);
        binding.result.setLayoutManager(new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL));
        BottomSheetBehavior.from(binding.resultSheet).setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.date.setOnClickListener(_ -> dateDialog.show(getSupportFragmentManager(), null));
        binding.timeSlider.addOnChangeListener((slider, _, _) -> {
            startClassTime = String.format(Locale.getDefault(), "%.0f", slider.getValues().get(0));
            endClassTime = String.format(Locale.getDefault(), "%.0f", slider.getValues().get(1));
            binding.time.setText(String.format(getString(R.string.section_range_x), startClassTime, endClassTime));
        });
        binding.query.setOnClickListener(_ -> {
            roomAdapter.clear();
            page = 1;
            getRoom();
        });
        binding.result.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1) && total / 20 + 1 >= page) {
                    getRoom();
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        binding.reset.setOnClickListener(_ -> {
            binding.officeGroup.getCheckedChipIds().forEach(e -> ((Chip) binding.officeGroup.findViewById(e)).setChecked(false));
            binding.campusGroup.getCheckedChipIds().forEach(e -> ((Chip) binding.campusGroup.findViewById(e)).setChecked(false));
            binding.typeGroup.getCheckedChipIds().forEach(e -> ((Chip) binding.typeGroup.findViewById(e)).setChecked(true));
            dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            binding.timeSlider.setValues(List.of(1.0f, 11.0f));
            dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            binding.dateText.setText(new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date()));
        });
        dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        binding.dateText.setText(new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date()));
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    params.toast((String) msg.obj);
                    return;
                } else if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                    return;
                }
                JSONObject dataString = JSON.parseObject((String) msg.obj);
                if (dataString.getInteger("code") == 200) {
                    if (msg.what == 3) {
                        JSONObject data = dataString.getJSONObject("data");
                        total = data.getInteger("total");
                        data.getJSONArray("rows").forEach(a -> roomAdapter.add((JSONObject) a));
                        BottomSheetBehavior.from(binding.resultSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                    } else {
                        binding.timeSlider.setValueFrom(1);
                        dataString.getJSONArray("data").forEach(campusInfo -> {
                            switch (msg.what) {
                                case 1: {
                                    String id = ((JSONObject) campusInfo).getString("id");
                                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, binding.campusGroup, false);
                                    binding.campusGroup.addView(chip);
                                    chip.setOnCheckedChangeListener((_, isChecked) -> {
                                        if (isChecked) {
                                            if (classroom.containsKey(id)) {
                                                Objects.requireNonNull(classroom.get(id)).forEach(e -> e.setVisibility(View.VISIBLE));
                                            } else {
                                                getOffice(id);
                                            }
                                        } else {
                                            Objects.requireNonNull(classroom.get(id)).forEach(e -> e.setVisibility(View.GONE));
                                        }
                                    });
                                    chip.setText(((JSONObject) campusInfo).getString("campusName"));
                                    break;
                                }
                                case 2: {
                                    classroom.computeIfAbsent(campusLiveData.getValue(), _ -> new ArrayList<>());
                                    Chip chip = ItemFilterChipBinding.inflate(getLayoutInflater(), binding.officeGroup, false).getRoot();
                                    binding.officeGroup.addView(chip);
                                    office.put(chip.getId(), ((JSONObject) campusInfo).getString("id"));
                                    chip.setText(((JSONObject) campusInfo).getString("dataName"));
                                    Objects.requireNonNull(classroom.get(campusLiveData.getValue())).add(chip);
                                    break;
                                }
                            }
                        });
                    }
                } else {
                    params.toast(R.string.login_warning);
                    params.gotoLogin(binding.toolbar, TargetUrl.JWXT);
                }
            }
        };
        http = new HttpManager(handler);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt//yd/studyRoom/");
        http.setParams(params);
        getCampus();
    }

    public void getCampus() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox", 1);
    }

    public void getOffice(String campus) {
        campusLiveData.setValue(campus);
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/schedule/agg/selfStudyClassRoom/buildingConditionPull", "{\"campusIdList\":[\"" + campus + "\"]}", 2);
    }

    public void getRoom() {
        ArrayList<String> teachingBuildIDs = new ArrayList<>();
        classType = new ArrayList<>();
        binding.typeGroup.getCheckedChipIds().forEach(e -> classType.add(((Chip) findViewById(e)).getText().toString().equals("自习室") ? "003" : "002"));
        binding.officeGroup.getCheckedChipIds().forEach(e -> {
            if (findViewById(e).getVisibility() == View.VISIBLE) {
                teachingBuildIDs.add(office.get(e));
            }
        });
        if (teachingBuildIDs.isEmpty()) {
            Message message = new Message();
            message.what = 0;
            message.obj = "请先选择教学楼";
            handler.sendMessage(message);
            return;
        }
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/schedule/agg/selfStudyClassRoom/pageListStudyClassroom", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":20,\"param\":{\"dateStr\":\"%s\",\"teachingBuildIDs\":%s,\"startClassTimes\":%s,\"endClassTimes\":%s,\"classRoomTagList\":%s}}", page++, dateStr, JSON.toJSONString(teachingBuildIDs), startClassTime, endClassTime, JSON.toJSONString(classType)), 3);
    }

    static class RoomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final Context context;
        final ArrayList<JSONObject> json = new ArrayList<>();

        public RoomAdapter(Context context) {
            super();
            this.context = context;
        }

        public void add(JSONObject jsonobject) {
            json.add(jsonobject);
            notifyItemInserted(getItemCount());
        }

        public void clear() {
            int temp = getItemCount();
            json.clear();
            notifyItemRangeRemoved(0, temp);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemClassroomResultBinding.inflate(LayoutInflater.from(context), parent, false).getRoot()) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemClassroomResultBinding binding = ItemClassroomResultBinding.bind(holder.itemView);
            binding.location.setText(json.get(position).getString("teachingBuildingName"));
            binding.time.setText(json.get(position).getString("classTimes"));
            binding.floor.setText(json.get(position).getString("floor"));
            binding.seat.setText(json.get(position).getString("seats"));
            binding.type.setText(json.get(position).getString("classRoomTag"));
            binding.name.setText(json.get(position).getString("classRoomNum"));
            Glide.with(context)
                    .load(new GlideUrl("https://jwxt.sysu.edu.cn/jwxt/base-info/classroom/classRoomView?fileName=jspic.png&filePath=" + json.get(position).get("photoPath"), new LazyHeaders.Builder()
                            .addHeader("Cookie", ((ClassroomQueryActivity) context).params.getCookie())
                            .addHeader("Referer", "https://jwxt.sysu.edu.cn/jwxt//yd/studyRoom/")
                            .build()))
                    .placeholder(R.drawable.logo)
                    .override((int) (145 * 3.6), (int) (132 * 3.6))
                    .fitCenter()
                    .into(binding.image);
            binding.getRoot().setOnClickListener(_ -> {
            });
        }

        @Override
        public int getItemCount() {
            return json.size();
        }
    }
}

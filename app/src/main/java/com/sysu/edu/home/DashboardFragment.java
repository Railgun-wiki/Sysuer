package com.sysu.edu.home;

import static com.sysu.edu.api.CommonUtil.isEmpty;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.academic.CourseDetailActivity;
import com.sysu.edu.academic.CourseScheduleActivity;
import com.sysu.edu.api.CalendarManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.PreferenceViewModel;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.DialogServiceActionBinding;
import com.sysu.edu.databinding.DialogServiceOrderBinding;
import com.sysu.edu.databinding.FragmentDashboardBinding;
import com.sysu.edu.databinding.ItemCourseBinding;
import com.sysu.edu.databinding.ItemExamBinding;
import com.sysu.edu.template.RecyclerAdapter;
import com.sysu.edu.todo.TodoManager;
import com.sysu.edu.todo.info.TodoInfo;

import org.commonmark.node.Heading;
import org.commonmark.node.Node;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.core.CoreProps;

public class DashboardFragment extends Fragment {

    final ArrayList<JSONObject> todayCourse = new ArrayList<>();
    final ArrayList<JSONObject> tomorrowCourse = new ArrayList<>();
    final LinkedList<JSONObject> thisWeekExams = new LinkedList<>();
    final LinkedList<JSONObject> nextWeekExams = new LinkedList<>();
    HttpManager http;
    Params params;
    HomeCollectionHelper db;
    FragmentDashboardBinding binding;
    boolean isRefreshRequired = true;
    HomeViewModel viewModel;
    BottomSheetDialog orderDialog;
    ServiceFragment.CollectionAdapter collectionAdapter;
    BottomSheetDialog actionDialog;
    DialogServiceActionBinding actionBinding;
    MutableLiveData<String> todoDate = new MutableLiveData<>("");
    private TodoManager todoManager;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (isRefreshRequired) {
            binding = FragmentDashboardBinding.inflate(inflater, container, false);
            CalendarManager calendar = new CalendarManager();

            db = new HomeCollectionHelper(requireContext());
            viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
            viewModel.updateDashboardShortcut.observe(requireActivity(), _ -> getShortcutCollection());
            binding.scan.setOnClickListener(_ -> {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"));
                    intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction("android.intent.initActionDialog.VIEW");
                    startActivity(intent);
                } catch (ActivityNotFoundException _) {
                }
            });
            binding.qrcode.setOnClickListener(_ -> {
                String linking = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("qrcode", "");
                if (!linking.isEmpty()) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(linking)));
                    } catch (ActivityNotFoundException e) {
                        // Toast.makeText(requireContext(), R.string.no_app, Toast.LENGTH_LONG).show();
                    }
                } /*else {
                    //new LaunchMiniProgram(requireActivity()).launchMiniProgram("gh_85575b9f544e");
                }*/
            });
            binding.agenda.setOnClickListener(view -> startActivity(new Intent(getContext(), CourseScheduleActivity.class), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp").toBundle()));
            binding.courseList.addItemDecoration(new DividerItemDecoration(requireContext(), 0));
            binding.courseList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.examList.addItemDecoration(new DividerItemDecoration(requireContext(), 0));
            binding.examList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            params = new Params(this);
            params.setCallback(this::getTerm);
            CourseAdapter courseAdapter = new CourseAdapter();
            courseAdapter.setParams(params);
            courseAdapter.setOnClick((jsonObject, view) -> startActivity(new Intent(getContext(), CourseDetailActivity.class).putExtra("code", jsonObject.getString("courseNum")).putExtra("class", jsonObject.getString("classesNum")), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp").toBundle()));
            binding.courseList.setAdapter(courseAdapter);
            ExamAdapter examAdapter = new ExamAdapter();
            examAdapter.setParams(params);
            binding.examList.setAdapter(examAdapter);
            binding.toggle.addOnButtonCheckedListener((_, checkedId, isChecked) -> {
                if (R.id.today == checkedId) {
                    courseAdapter.set(isChecked ? todayCourse : tomorrowCourse);
                    binding.noClass.setVisibility(courseAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
            binding.toggle2.addOnButtonCheckedListener((_, checkedId, isChecked) -> {
                if (R.id.week_18 == checkedId) {
                    examAdapter.set(new ArrayList<>(isChecked ? thisWeekExams : nextWeekExams));
                    binding.noExam.setVisibility(examAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
            binding.date.setText(String.format("%s/星期%s", new SimpleDateFormat("M月dd日", Locale.CHINESE).format(new Date()), new String[]{"日", "一", "二", "三", "四", "五", "六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]));

            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                        binding.nextClass.setText(R.string.no_wifi_warning);
                        return;
                    }
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response.get("code").equals(200)) {
                        switch (msg.what) {
                            case 1:
                                ArrayList<JSONObject> beforeArray = new ArrayList<>();
                                ArrayList<JSONObject> afterArray = new ArrayList<>();
                                response.getJSONArray("data").forEach(e -> {
                                    JSONObject jsonObject = (JSONObject) e;
                                    String status = getTimePosition(jsonObject.getString("teachingDate") + " " + jsonObject.getString("startTime"), jsonObject.getString("teachingDate") + " " + jsonObject.getString("endTime"));
                                    jsonObject.put("status", status);
                                    jsonObject.put("time", jsonObject.get("startTime") + "~" + jsonObject.get("endTime"));
                                    jsonObject.put("course", "第" + jsonObject.get("startClassTimes") + "~" + jsonObject.get("endClassTimes") + "节课");
                                    String flag = (String) jsonObject.get("useflag");
                                    if ("TD".equals(flag))
                                        (Objects.equals(status, "before") ? beforeArray : afterArray).add(jsonObject);
                                    ("TD".equals(flag) ? todayCourse : tomorrowCourse).add(jsonObject);
//                                    addCourse(flag.equals("TD") ? todayCourse : tomorrowCourse, (String) ((JSONObject) e).get("courseName"), (String) ((JSONObject) e).get("teachingPlace"), ((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime")
//                                            , "第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课", (String) ((JSONObject) e).get("teacherName"), flag);
                                });
                                binding.progress.setMax(todayCourse.size());
                                binding.progress.setProgress(beforeArray.size());
                                binding.courseList.scrollToPosition(beforeArray.size());
                                Markwon.builder(requireContext()).usePlugin(new AbstractMarkwonPlugin() {
                                    @Override
                                    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                                        super.configureSpansFactory(builder);
                                        builder.appendFactory(Heading.class, (_, configuration) -> {
                                            if (CoreProps.HEADING_LEVEL.require(configuration) == 3)
                                                return new ForegroundColorSpan(Color.parseColor("#6750a4"));
                                            return null;
                                        });
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
                                }).build().setMarkdown(binding.nextClass, afterArray.isEmpty() ? String.format("### %s\n\n%s：**%s**\n\n%s：**%s**\n\n%s：**%s**",
                                        getString(R.string.noClass),
                                        getString(R.string.next_class), tomorrowCourse.isEmpty() ? getString(R.string.none) : tomorrowCourse.get(0).getString("courseName"),
                                        getString(R.string.location), tomorrowCourse.isEmpty() ? getString(R.string.none) : tomorrowCourse.get(0).getString("teachingPlace"),
                                        getString(R.string.time), tomorrowCourse.isEmpty() ? getString(R.string.none) : tomorrowCourse.get(0).getString("time")) :
                                        String.format("### %s\n\n%s：**%s**\n\n%s：**%s**\n\n%s：**%s**",
                                                todayCourse.get(beforeArray.size()).getString("courseName"),
                                                getString(R.string.location), todayCourse.get(beforeArray.size()).getString("teachingPlace"),
                                                getString(R.string.time), todayCourse.get(beforeArray.size()).getString("time"),
                                                getString(R.string.date), todayCourse.get(beforeArray.size()).getString("teachingDate")));
                                binding.toggle.clearChecked();
                                binding.toggle.check(R.id.today);
                                break;
                            case 2:
                                JSONArray dataArray = response.getJSONArray("data");
                                if (!dataArray.isEmpty()) {
                                    for (int i = 0; i < dataArray.size(); i++) {
                                        LinkedList<JSONObject> exams = List.of(thisWeekExams, nextWeekExams).get(i);
                                        TreeMap<Integer, JSONArray> sortedTimetable = new TreeMap<>();
                                        dataArray.getJSONObject(i).getJSONObject("timetable").forEach((s, t) -> {
                                            if (t != null)
                                                sortedTimetable.put(Integer.parseInt(s), (JSONArray) t);
                                        });
                                        sortedTimetable.forEach((key, value) -> {
                                            if (key.equals(sortedTimetable.firstKey()))
                                                value.forEach(c -> exams.addFirst((JSONObject) c));
                                            else
                                                value.forEach(c -> exams.addLast((JSONObject) c));
                                        });
                                    }
                                    binding.toggle2.clearChecked();
                                    binding.toggle2.check(R.id.week_18);
                                }
                                break;
                            case 3:
                                String term = response.getJSONObject("data").getString("acadYearSemester");
                                binding.date.setText(String.format("第%s学期\n%s\n星期%s", term, new SimpleDateFormat("M月dd日", Locale.getDefault()).format(new Date()), new String[]{"日", "一", "二", "三", "四", "五", "六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]));
                                getTodayCourses(term);
                                getExams(term);
                                getWeek(term);
                                isRefreshRequired = false;
                                break;
                            case 4:
                                String week = response.getJSONArray("data").getJSONObject(0).getString("weekTimes");
                                binding.date.setText(String.format("第%s周\n%s", week, binding.date.getText().toString()));
                                binding.toggle2.check("19".equals(week) ? R.id.week_19 : R.id.week_18);
                                break;
                        }
                    } else {
                        params.gotoLogin(binding.getRoot(), TargetUrl.JWXT);
                    }
                }
            };
            handler.post(new Runnable() {
                @Override
                public void run() {
                    binding.time.setText(new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new Date()));
                    handler.postDelayed(this, 500);
                }
            });
            http = new HttpManager(handler);
            http.setParams(params);

            PreferenceViewModel preferenceViewModel = new ViewModelProvider(requireActivity()).get(PreferenceViewModel.class);
            preferenceViewModel.setPM(androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireActivity()));
            preferenceViewModel.getIsAgreeLiveData().observe(getViewLifecycleOwner(), a -> {
                if (!a) getTerm();
            });
            preferenceViewModel.initLiveData();

            ConcatAdapter todoAdapter = new ConcatAdapter();
            binding.todoList.setLayoutManager(new LinearLayoutManager(requireActivity()));
            binding.todoList.setAdapter(todoAdapter);
            todoManager = new TodoManager(requireActivity(), todoAdapter);
            todoManager.setOnRefreshListener(this::refresh);
            binding.add.setOnClickListener(_ -> todoManager.showTodoAddDialog());
            PopupMenu pop = new PopupMenu(requireActivity(), binding.todoDate, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
            Menu menu = pop.getMenu();
            menu.add(0, Menu.NONE, 0, R.string.all).setChecked(true).setOnMenuItemClickListener(_ -> {
                todoDate.setValue("");
                return false;
            });
            menu.add(0, Menu.NONE, 0, R.string.today).setOnMenuItemClickListener(_ -> {
                todoDate.setValue(calendar.toDateStringAdd(0));
                return false;
            });
            menu.add(0, Menu.NONE, 0, R.string.tomorrow).setOnMenuItemClickListener(_ -> {
                todoDate.setValue(calendar.toDateStringAdd(1));
                return false;
            });
            menu.add(1, Menu.NONE, 0, R.string.select).setOnMenuItemClickListener(_ -> {
//                item.setChecked(true);
                MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                if (isEmpty(todoDate.getValue()))
                    builder.setSelection(System.currentTimeMillis());
                else {
                    try {
                        builder.setSelection(Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(todoDate.getValue())).getTime()+86400000);
                    } catch (ParseException _) {
                    }
                }
                MaterialDatePicker<Long> datePicker = builder.build();
                datePicker.show(requireActivity().getSupportFragmentManager(), "date_picker");
                datePicker.addOnPositiveButtonClickListener(aLong -> todoDate.setValue(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(aLong)));

                return false;
            });
            todoDate.observe(getViewLifecycleOwner(), _ -> refresh());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) menu.setGroupDividerEnabled(true);
            binding.todoDate.setOnClickListener(_ -> pop.show());
            binding.toggle3.addOnButtonCheckedListener((_, checkedId, _) -> {
                if (checkedId == R.id.filter_todo) todoManager.performRefresh();
            });
            binding.toggle3.check(R.id.filter_todo);
            preferenceViewModel.getDashboardLiveData().observe(getViewLifecycleOwner(), s -> {
                HashSet<String> visible = new HashSet<>(List.of("0", "1", "2", "3", "4", "5"));
                if (!s.isEmpty()) visible.removeAll(s);
                visible.forEach(i -> List.of(binding.shortcutGroup, binding.nextClassCard, binding.timeCard, binding.courseGroup, binding.examGroup, binding.todoGroup).get(Integer.parseInt(i)).setVisibility(View.GONE));
            });
            initOrder(inflater);
            initAction(inflater);
            getShortcutCollection();
        }
        return binding.getRoot();
    }

    void refresh() {
        String value = todoDate.getValue();
        boolean isAll = isEmpty(value);
        if (isAll)
            todoManager.refresh("status = ?", new String[]{String.valueOf(binding.filterTodo.isChecked() ? TodoInfo.TODO : TodoInfo.DONE)});
        else todoManager.refresh("due_date = ? AND status = ?", new String[]{
                value, String.valueOf(binding.filterTodo.isChecked() ? TodoInfo.TODO : TodoInfo.DONE)
        });
        binding.todoDate.setText(isAll ? getString(R.string.all) : value);
    }

    void getTerm() {
//        System.out.println("getTerm");
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/");
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist", 3);
    }

    void getWeek(String term) {
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/yd/index/");
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=" + term, 4);
    }

    void getTodayCourses(String term) {
        http.setReferrer(null);
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=" + term, 1);
    }

    void getExams(String term) {
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/");
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck" + term, String.format("{\"acadYear\":\"%s\",\"examWeekId\":\"1928284621349085186\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", term), 2);
    }

    String getTimePosition(String from, String to) {
        Date now = new Date();
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd hh:mm", Locale.getDefault());
            Date fromDate = simpleDateFormat.parse(from);
            Date toDate = simpleDateFormat.parse(to);
            return now.before(fromDate) ? "after" : now.after(toDate) ? "before" : "in";
        } catch (ParseException _) {
        }
        return "before";
    }

    void getShortcutCollection() {
        if (binding.shortcutGroup.getChildCount() > 4)
            IntStream.range(3, binding.shortcutGroup.getChildCount() - 1).forEach(_ -> binding.shortcutGroup.removeViewAt(3));
        try (Cursor cursor = db.getWritableDatabase().query("dashboard_shortcut_collection", null, null, null, null, null, "position")) {
            if (cursor.moveToFirst()) {
                collectionAdapter.clear();
                do {
                    Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("shortcutId"));
                    JSONObject shortcut = JSON.parseObject(cursor.getString(cursor.getColumnIndexOrThrow("shortcutJson")));
                    MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle);
                    button.setText(shortcut.getString("name"));
                    binding.shortcutGroup.addView(button);
                    if (viewModel.actionMap.containsKey(id))
                        button.setOnClickListener(viewModel.actionMap.get(id));
                    button.setOnLongClickListener(_ -> action(shortcut));
                    collectionAdapter.add(shortcut);
                } while (cursor.moveToNext());
            }
        }
    }

    void initOrder(@NonNull LayoutInflater inflater) {
        Context context = requireContext();
        orderDialog = new BottomSheetDialog(context);
        DialogServiceOrderBinding orderBinding = DialogServiceOrderBinding.inflate(inflater);
        orderBinding.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        collectionAdapter = new ServiceFragment.CollectionAdapter();
        orderBinding.recyclerView.setAdapter(collectionAdapter);
        orderBinding.confirm.setOnClickListener(_ -> {
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
            collectionAdapter.get(i);
            db.updateDashboardShortcutPosition(collectionAdapter.get(i).getInteger("id"), i);
        });
    }

    void initAction(@NonNull LayoutInflater inflater) {
        actionDialog = new BottomSheetDialog(requireContext());
        actionBinding = DialogServiceActionBinding.inflate(inflater);
        actionBinding.order.setOnClickListener(_ -> orderDialog.show());
        actionDialog.setContentView(actionBinding.getRoot());
    }

    boolean action(JSONObject item) {
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
            getShortcutCollection();
            actionBinding.collect.setText(isServiceCollect ? R.string.collect : R.string.cancel_collect);
            isServiceCollected.setValue(!isServiceCollect);
        });
        actionBinding.addShortcut.setOnClickListener(_ -> {
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
        actionBinding.feedback.setOnClickListener(_ -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format("https://github.com/%s/%s/issues/new?title=反馈：服务->%s&labels=bug,crash-report", "SYSU-Tang", "Sysuer", item.getString("name")))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        Markwon.create(requireContext()).setMarkdown(actionBinding.description, String.format("### %s\n%s", item.getString("name"), item.getString("description")));
        actionDialog.show();
        return true;
    }
}

class CourseAdapter extends RecyclerAdapter<JSONObject> {
    BiConsumer<JSONObject, View> onClick;


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemCourseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
        };
    }

    public void setOnClick(BiConsumer<JSONObject, View> onClick) {
        this.onClick = onClick;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemCourseBinding binding = ItemCourseBinding.bind(holder.itemView);
        holder.itemView.setOnClickListener(v -> onClick.accept(get(position), v));
        Map.of(binding.courseTitle, "courseName", binding.location, "teachingPlace", binding.time, "time", binding.teacher, "teacherName", binding.course, "course").forEach((v, s) -> {
            v.setText(get(position).getString(s));
            v.setOnLongClickListener(_ -> {
                params.copy(s + "：", get(position).getString(s));
                params.toast(R.string.copy_successfully);
                return true;
            });
        });
        TypedValue colorSurfaceDim = new TypedValue();
        TypedValue colorSurface = new TypedValue();
        Resources.Theme theme = holder.itemView.getContext().getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceDim, colorSurfaceDim, true);
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, colorSurface, true);
        boolean isBefore = Objects.equals(get(position).getString("status"), "before");
        binding.courseTitle.setTextAppearance(isBefore ? com.google.android.material.R.style.TextAppearance_Material3_TitleMedium : com.google.android.material.R.style.TextAppearance_Material3_TitleMedium_Emphasized);
        holder.itemView.getBackground().setTint(Objects.equals(get(position).getString("status"), "in") ? colorSurfaceDim.data : isBefore ? 0x0 : colorSurface.data);
        binding.item.setAlpha(isBefore ? 0.64f : 1.0f);
        super.onBindViewHolder(holder, position);
    }
}

class ExamAdapter extends RecyclerAdapter<JSONObject> {

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemExamBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemExamBinding binding = ItemExamBinding.bind(holder.itemView);
        Context context = holder.itemView.getContext();
        holder.itemView.setOnClickListener(_ -> {
        });
        JSONObject examData = get(position);
        int startClassTimes = examData.getIntValue("startClassTimes");
        int endClassTimes = examData.getIntValue("endClassTimes");
        String[] text = new String[]{examData.getString("examSubjectName"),
                examData.getString("classroomNumber"),
                examData.getString("examDate"),
                String.format("%s%s", examData.getString("duration"), context.getString(R.string.minute)),
                examData.getString("durationTime"),
                String.format(context.getString(R.string.section_range), startClassTimes, endClassTimes),
                String.format("%s：%s", context.getString(R.string.exam_mode), examData.getString("examMode")),
                String.format("%s：%s", context.getString(R.string.exam_stage), examData.getString("examStage"))};
        TextView[] materialTextButtons = {binding.examName, binding.examLocation, binding.examDate, binding.examDuration, binding.examTime, binding.examClassTime, binding.examMode, binding.examStage};
        for (int i = 0; i < 8; i++) {
            materialTextButtons[i].setText(text[i]);
            int finalI = i;
            materialTextButtons[i].setOnClickListener(_ -> {
                params.copy(finalI + "：", text[finalI]);
                params.toast(R.string.copy_successfully);
            });
        }
        super.onBindViewHolder(holder, position);
    }
}
package com.sysu.edu.life;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.DialogGymReservationBinding;
import com.sysu.edu.databinding.FragmentGymDetailBinding;
import com.sysu.edu.databinding.ItemDateBinding;
import com.sysu.edu.databinding.ItemFieldDetailBinding;
import com.sysu.edu.template.RecyclerAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;


public class GymDetailFragment extends Fragment {

    final HashMap<String, JSONObject> fee = new HashMap<>();
    HttpManager http;
    GymReservationViewModel viewModel;
    String id;
    DateAdapter date;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentGymDetailBinding binding = FragmentGymDetailBinding.inflate(inflater, container, false);
        DialogGymReservationBinding dialogBinding = DialogGymReservationBinding.inflate(inflater, container, false);
        binding.date.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        date = new DateAdapter(requireContext());

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(dialogBinding.getRoot());
        initReservationDialog(dialogBinding);
        viewModel = new ViewModelProvider(requireActivity()).get(GymReservationViewModel.class);
        if (viewModel.position.getValue() == null) {
            viewModel.position.postValue(0);
        }
        date.setAction(viewModel.position::setValue);
        Params params = new Params(this);
        binding.date.recyclerView.setAdapter(date);
        binding.date.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollHorizontally(1) && dx > 0) date.offset(7);
            }
        });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 4, GridLayoutManager.HORIZONTAL, false);
        binding.field.recyclerView.setLayoutManager(gridLayoutManager);
        FieldAdapter field = new FieldAdapter();
        binding.field.recyclerView.setAdapter(field);
        id = requireArguments().getString("id");
        field.setAction((JSONObject p) -> {
            JSONObject studentFee = fee.get("学生");
            if (studentFee != null)
                updateReservationDialog(dialogBinding, p.getString("Venue"), p.getString("Date"), p.getString("Duration"), String.format(Locale.getDefault(), "运动时￥%d或现金￥%d", studentFee.getInteger("CreditFee"), studentFee.getInteger("CashFee")), p.getString("Type"));
            dialog.show();
        });
        viewModel.position.observe(getViewLifecycleOwner(), p -> {
            if (p != null) getInfo();
        });
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    String response = (String) msg.obj;
                    if (msg.getData().getBoolean("isJSON")) {
//                    System.out.println(msg.getData().getString("data"));
                        switch (msg.what) {
                            case 0:
                                field.clear();
                                AtomicInteger availableCapacity = new AtomicInteger();
                                MutableLiveData<Boolean> name = new MutableLiveData<>(false);
                                JSONArray.parse(response).forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    JSONArray timeslots = item.getJSONArray("Timeslots");
                                    if (timeslots != null) {
                                        gridLayoutManager.setSpanCount(timeslots.size() + 1);
                                        if (Boolean.FALSE.equals(name.getValue())) {
                                            field.add(new JSONObject().fluentPut("Name", getString(R.string.time)).fluentPut("Type", 2));
                                            timeslots.forEach(o -> field.add(new JSONObject().fluentPut("Name", String.format("%s\n%s", ((JSONObject) o).getString("Start"), ((JSONObject) o).getString("End"))).fluentPut("Type", 2)));
                                            name.setValue(true);
                                        }
                                        String fieldName = Pattern.compile("(.+)-").matcher(item.getString("VenueName")).replaceAll("");
                                        field.add(new JSONObject().fluentPut("VenueName", fieldName).fluentPut("Type", 0));
                                        timeslots.forEach(o -> {
                                            JSONObject jsonObject = (JSONObject) o;
                                            field.add(jsonObject.fluentPut("Type", 1).fluentPut("Venue", fieldName).fluentPut("Duration", String.format(Locale.getDefault(), "%s~%s", jsonObject.getString("Start"), jsonObject.getString("End"))));
                                            int capacity = Integer.parseInt(jsonObject.getString("AvailableCapacity"));
                                            if (capacity > 0)
                                                availableCapacity.addAndGet(capacity);
                                        });
                                        if (viewModel.position.getValue() != null)
                                            date.setAvailableCapacity(viewModel.position.getValue(), availableCapacity.get());
                                    }
                                });
                                getFee(id);
                                break;
                            case 1:
                                fee.clear();
                                JSONArray feeTemplates = JSONArray.parse(response);
                                if (feeTemplates != null)
                                    feeTemplates.forEach(e -> fee.put(((JSONObject) e).getString("UserRole"), (JSONObject) e));
                                break;
                        }
                    } else if (!viewModel.authorizationManager.isAuthorized(response)) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.getRoot(), viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                    } else if (!viewModel.authorizationManager.isAccessible(response)) {
                        params.toast(R.string.educational_wifi_warning);
                        getInfo();
                    } else if (Pattern.compile("人机识别检测").matcher(response).find()) {
                        params.gotoLogin(binding.getRoot(), viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                    }

                }
            }
        });

        /*viewModel.loginRequired.observe(requireActivity(), b -> {
            if (!b)

        });*/
        http.setParams(params);
        http.setUA(viewModel.ua);
        http.setHeader(Map.of("Accept", "application/json, text/plain, */*"));
        http.setAuthorizationRequired(true);
        return binding.getRoot();
    }

    void getInfo() {
        Integer positionValue = viewModel.position.getValue();
        if (positionValue != null)
            getInfo(id, date.getFormattedDate(positionValue), date.getFormattedDate(positionValue));
    }

    void getInfo(String id, String from, String to) {
        http.getRequest(viewModel.authorizationManager.getBaseUrl() + String.format("api/venue/available-slots/range?venueTypeId=%s&start=%s&end=%s", id, from, to), 0);
    }

    void getFee(String id) {
        http.getRequest(viewModel.authorizationManager.getBaseUrl() + String.format("api/venuetype/%s/feetemplates", id), 1);
    }


    @SuppressLint("SetJavaScriptEnabled")
    public void generateToken(String id) {
        String uuid = generateUUID();
        WebView web = new WebView(requireContext());
        WebSettings webSettings = web.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)");
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setJavaScriptEnabled(true);
        webSettings.supportZoom();
        webSettings.setSupportZoom(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //viewModel.token = cookie.getCookie(url);
//                handler.postDelayed(() -> {
                web.evaluateJavascript("(function(){var c=document.querySelector(\".main-container\").__vue__;var uuid=c.generateUUID();return [uuid,c.genToken(uuid)];})()", string -> {
                    if (!Objects.equals(string, "null")) {
                        JSONArray tokenArray = JSONArray.parse(string);
                        if (tokenArray != null && tokenArray.size() == 2) {
                            System.out.println(tokenArray);
//                                viewModel.token = tokenArray.getString(0);
//                                viewModel.authorization = tokenArray.getString(1);
                        }
                    }
                    web.destroy();
                });
//                }, 500);
                super.onPageFinished(view, url);
            }
        });

        //System.out.println(id);
        web.loadUrl("https://gym.sysu.edu.cn/#/booking/" + id);

        //((ViewGroup) requireActivity().findViewById(android.R.id.content)).addView(web);
    }

    public String generateUUID() {
        final Random RANDOM = new Random();
        final String TEMPLATE = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx";
        StringBuilder uuid = new StringBuilder();

        for (int i = 0; i < TEMPLATE.length(); i++) {
            char templateChar = TEMPLATE.charAt(i);

            if (templateChar == 'x' || templateChar == 'y') {
                int randomNum = RANDOM.nextInt(16); // 0-15的随机数
                int value;

                if (templateChar == 'x') {
                    value = randomNum; // 0-15
                } else { // templateChar == 'y'
                    value = (randomNum & 0x3) | 0x8; // 8, 9, 10, 11
                }

                uuid.append(Integer.toHexString(value));
            } else {
                uuid.append(templateChar);
            }
        }

        return uuid.toString();
    }


    void initReservationDialog(DialogGymReservationBinding binding) {
        binding.field.key.setText(R.string.field);
        binding.date.key.setText(R.string.date);
        binding.time.key.setText(R.string.time);
        binding.fee.key.setText(R.string.fee);
        binding.type.key.setText(R.string.type);
        binding.reserve.setOnClickListener(_ -> generateToken(id));
    }

    void updateReservationDialog(DialogGymReservationBinding binding, String field, String date, String time, String fee, String type) {
        binding.field.value.setText(field);
        binding.date.value.setText(date);
        binding.time.value.setText(time);
        binding.fee.value.setText(fee);
        binding.type.value.setText(type);
    }

    static class DateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final Context context;
        final HashMap<Integer, Integer> availableCapacity = new HashMap<>();
        final Calendar calendar = Calendar.getInstance();
        Consumer<Integer> action;
        int page = 7;

        public DateAdapter(Context context) {
            super();
            this.context = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(
                    ItemDateBinding.inflate(LayoutInflater.from(context), parent, false).getRoot()) {
            };
        }

        public void setAction(Consumer<Integer> action) {
            this.action = action;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemDateBinding binding = ItemDateBinding.bind(holder.itemView);
            binding.date.setText(getDate(position));
            binding.week.setText(String.format("星期%s", getWeek(position)));
            binding.getRoot().setOnClickListener(_ -> action.accept(position));
            Integer capacity = availableCapacity.getOrDefault(position, -1);
            binding.availableCapacity.setText(capacity != null && capacity >= 0 ? String.format(Locale.getDefault(), "%d", capacity) : "");
        }

        public void setAvailableCapacity(int position, int i) {
            availableCapacity.put(position, i);
            notifyItemChanged(position);
        }

        @Override
        public int getItemCount() {
            return page;
        }

        public void offset(int offset) {
            page += offset;
            notifyItemRangeInserted(page - 1, offset);
        }

        public String getDate(int distanceDay) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, distanceDay);
            return new SimpleDateFormat("MM月dd日", Locale.getDefault()).format(calendar.getTime());
        }

        public String getFormattedDate(int distanceDay) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, distanceDay);
            return new SimpleDateFormat("MM-dd", Locale.getDefault()).format(calendar.getTime());
        }

        String getWeek(int distanceDay) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DATE, distanceDay);
            int week = calendar.get(Calendar.DAY_OF_WEEK);
//            week = (week == 1) ? 6 : week - 1;
            return context.getResources().getStringArray(R.array.weeks)[week - 1];
        }
    }

    static class FieldAdapter extends RecyclerAdapter<JSONObject> {

        Consumer<JSONObject> action;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemFieldDetailBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }

        void setAction(Consumer<JSONObject> action) {
            this.action = action;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            int position = holder.getBindingAdapterPosition();
            ItemFieldDetailBinding binding = ItemFieldDetailBinding.bind(holder.itemView);
            binding.fieldDetail.setAlpha(1.0f);
            JSONObject item = get(position);
            Context context = holder.itemView.getContext();
            binding.getRoot().setOnClickListener(_ -> {
                if (item.getInteger("Type") == 1 && item.getInteger("AvailableCapacity") > 0)
                    action.accept(item);
            });
            switch (item.getInteger("Type")) {
                case 0 ->
                        binding.fieldDetail.setText(String.format(Locale.getDefault(), "%s", item.getString("VenueName")));
                case 2 ->
                        binding.fieldDetail.setText(String.format(Locale.getDefault(), "%s", item.getString("Name")));
                case 1 -> {
                    if (item.getInteger("AvailableCapacity") == 0) {
                        binding.fieldDetail.setText(context.getString(R.string.reserved));
                        binding.fieldDetail.setAlpha(0.5f);
                    } else {
                        binding.fieldDetail.setText(context.getString(R.string.reservable));
                    }
                }
            }
        }

    }
}
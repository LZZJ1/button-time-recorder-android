package com.example.buttontimerecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 主Activity - 对应原Python中的ButtonTimeRecorderApp类
 *
 * 功能：
 * 1. 实时时钟显示
 * 2. 动态添加/删除切换按钮
 * 3. 记录按钮按下/弹起时间
 * 4. 记录列表展示（含序号、按钮名、按下时间、弹起时间、时长）
 * 5. 搜索/过滤按钮
 * 6. 导入/导出Excel
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 100;
    private static final int REQUEST_PICK_FILE = 101;

    // UI组件
    private TextView tvClock;
    private EditText etButtonName, etSearch;
    private FlowLayout flowLayoutButtons;
    private TextView tvButtonCount, tvRecordCount;
    private RecyclerView rvRecords;
    private RecordAdapter recordAdapter;

    // 数据
    private final List<ToggleButtonView> buttonList = new ArrayList<>();
    private final List<TimeRecord> records = new ArrayList<>();
    private int nextRecordId = 1;

    // 时间格式
    private final SimpleDateFormat fullFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat hmFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat clockFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // Handler用于定时刷新时钟
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 1000);
        }
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
        startClock();
    }

    private void initViews() {
        tvClock = findViewById(R.id.tvClock);
        etButtonName = findViewById(R.id.etButtonName);
        etSearch = findViewById(R.id.etSearch);
        flowLayoutButtons = findViewById(R.id.flowLayoutButtons);
        tvButtonCount = findViewById(R.id.tvButtonCount);
        tvRecordCount = findViewById(R.id.tvRecordCount);
        rvRecords = findViewById(R.id.rvRecords);

        // 设置RecyclerView
        recordAdapter = new RecordAdapter(records);
        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        rvRecords.setAdapter(recordAdapter);
    }

    private void setupListeners() {
        // 添加按钮
        Button btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(v -> addButton());

        // 回车键添加
        etButtonName.setOnEditorActionListener((v, actionId, event) -> {
            addButton();
            return true;
        });

        // 搜索框
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchButtons(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 清空记录
        Button btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> clearRecords());

        // 删除全部按钮
        Button btnDeleteAll = findViewById(R.id.btnDeleteAll);
        btnDeleteAll.setOnClickListener(v -> deleteAllButtons());

        // 导入Excel
        Button btnImport = findViewById(R.id.btnImport);
        btnImport.setOnClickListener(v -> importFromExcel());

        // 导出Excel
        Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> exportToExcel());
    }

    // ==================== 时钟 ====================

    private void startClock() {
        handler.post(clockRunnable);
    }

    private void updateClock() {
        tvClock.setText(clockFmt.format(new Date()));
    }

    // ==================== 添加按钮 ====================

    /**
     * 添加新的切换按钮 - 对应Python中的add_button方法
     */
    private void addButton() {
        String name = etButtonName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入按钮名称！", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否已存在
        for (ToggleButtonView btn : buttonList) {
            if (btn.getName().equals(name)) {
                Toast.makeText(this, "按钮名称 '" + name + "' 已存在！", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        createToggleButton(name);
        etButtonName.setText("");
        updateButtonCount();
    }

    private void createToggleButton(String name) {
        ToggleButtonView toggleBtn = new ToggleButtonView(
                this,
                name,
                this::onButtonToggle,
                this::onButtonDelete
        );

        buttonList.add(toggleBtn);
        flowLayoutButtons.addView(toggleBtn.getRootView());

        // 添加layout参数
        FlowLayout.LayoutParams lp = new FlowLayout.LayoutParams(
                FlowLayout.LayoutParams.WRAP_CONTENT,
                FlowLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(4, 4, 4, 4);
        toggleBtn.getRootView().setLayoutParams(lp);
    }

    // ==================== 按钮切换回调 ====================

    /**
     * 按钮切换回调 - 对应Python中的on_button_toggle方法
     */
    private void onButtonToggle(ToggleButtonView btn) {
        Date now = new Date();
        String currentTime = fullFmt.format(now);
        String hmTime = hmFmt.format(now);

        if (btn.isPressed()) {
            // 按下：创建新记录（未闭合）
            TimeRecord record = new TimeRecord(
                    nextRecordId++,
                    btn.getName(),
                    now.getTime(),
                    hmTime,
                    currentTime
            );
            records.add(record);
        } else {
            // 弹起：找到最近一条未闭合的同名记录，填写弹起时间和时长
            TimeRecord unclosed = findUnclosedRecord(btn.getName());
            if (unclosed != null) {
                unclosed.setReleaseTime(hmTime);
                unclosed.setReleaseFullTime(currentTime);
                unclosed.setReleaseTimestamp(now.getTime());

                // 计算时长
                long diffMs = now.getTime() - unclosed.getPressTimestamp();
                long totalSeconds = diffMs / 1000;
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;

                String duration;
                if (hours > 0) {
                    duration = hours + "时" + minutes + "分" + seconds + "秒";
                } else if (minutes > 0) {
                    duration = minutes + "分" + seconds + "秒";
                } else {
                    duration = seconds + "秒";
                }
                unclosed.setDuration(duration);
            }
        }

        refreshTable();
    }

    private TimeRecord findUnclosedRecord(String name) {
        // 从后往前找，找最近一条同名未闭合记录
        for (int i = records.size() - 1; i >= 0; i--) {
            TimeRecord r = records.get(i);
            if (r.getName().equals(name) && r.isUnclosed()) {
                return r;
            }
        }
        return null;
    }

    // ==================== 删除按钮 ====================

    /**
     * 删除单个按钮 - 对应Python中的delete_button方法
     */
    private void onButtonDelete(ToggleButtonView btn) {
        // 检查是否有未闭合记录
        boolean hasUnclosed = false;
        for (TimeRecord r : records) {
            if (r.getName().equals(btn.getName()) && r.isUnclosed()) {
                hasUnclosed = true;
                break;
            }
        }

        String msg = hasUnclosed
                ? "按钮 '" + btn.getName() + "' 有未闭合的记录，是否删除该按钮？\n未闭合的记录将保留。"
                : "确定要删除按钮 '" + btn.getName() + "' 吗？";

        new AlertDialog.Builder(this)
                .setTitle("确认")
                .setMessage(msg)
                .setPositiveButton("是", (dialog, which) -> {
                    flowLayoutButtons.removeView(btn.getRootView());
                    buttonList.remove(btn);
                    updateButtonCount();
                })
                .setNegativeButton("否", null)
                .show();
    }

    /**
     * 删除所有按钮 - 对应Python中的delete_all_buttons方法
     */
    private void deleteAllButtons() {
        if (buttonList.isEmpty()) {
            Toast.makeText(this, "没有可删除的按钮！", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("确认")
                .setMessage("确定要删除所有按钮吗？\n记录将保留。")
                .setPositiveButton("是", (dialog, which) -> {
                    flowLayoutButtons.removeAllViews();
                    buttonList.clear();
                    updateButtonCount();
                })
                .setNegativeButton("否", null)
                .show();
    }

    // ==================== 搜索 ====================

    /**
     * 搜索/过滤按钮 - 对应Python中的search_buttons方法
     */
    private void searchButtons(String keyword) {
        int matchCount = 0;
        for (ToggleButtonView btn : buttonList) {
            boolean match = keyword.isEmpty() ||
                    btn.getName().toLowerCase().contains(keyword.toLowerCase());
            btn.setVisible(match);
            if (match) matchCount++;
        }

        if (!keyword.isEmpty()) {
            tvButtonCount.setText("找到 " + matchCount + "/" + buttonList.size() + " 个按钮");
        } else {
            updateButtonCount();
        }
    }

    private void updateButtonCount() {
        tvButtonCount.setText("共 " + buttonList.size() + " 个");
    }

    // ==================== 刷新表格 ====================

    /**
     * 刷新记录表格 - 对应Python中的refresh_table方法
     */
    private void refreshTable() {
        recordAdapter.notifyDataSetChanged();
        tvRecordCount.setText("记录数: " + records.size());
        // 滚动到最新记录
        if (!records.isEmpty()) {
            rvRecords.smoothScrollToPosition(records.size() - 1);
        }
    }

    // ==================== 清空记录 ====================

    /**
     * 清空所有记录 - 对应Python中的clear_records方法
     */
    private void clearRecords() {
        if (records.isEmpty()) {
            Toast.makeText(this, "没有可清空的记录！", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("确认")
                .setMessage("确定要清空所有记录吗？")
                .setPositiveButton("是", (dialog, which) -> {
                    records.clear();
                    nextRecordId = 1;
                    refreshTable();

                    // 重置所有按钮状态（如果有按下状态的按钮，需要注意）
                    // 这里保持按钮状态不变（原PC版行为）
                })
                .setNegativeButton("否", null)
                .show();
    }

    // ==================== 导出 Excel ====================

    /**
     * 导出到Excel - 对应Python中的export_to_excel方法
     */
    private void exportToExcel() {
        if (records.isEmpty()) {
            Toast.makeText(this, "没有可导出的记录！", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "作业时间记录_" + timestamp + ".xlsx";

                // 保存到应用私有目录（无需权限）
                File outputFile = new File(getExternalFilesDir(null), fileName);

                boolean success = ExcelHelper.exportToExcel(
                        this, records, buttonList, outputFile
                );

                runOnUiThread(() -> {
                    if (success) {
                        showExportSuccess(outputFile);
                    } else {
                        Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "导出失败:\n" + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void showExportSuccess(File file) {
        new AlertDialog.Builder(this)
                .setTitle("成功")
                .setMessage("记录已导出到:\n" + file.getAbsolutePath())
                .setPositiveButton("分享文件", (dialog, which) -> shareFile(file))
                .setNeutralButton("确定", null)
                .show();
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".provider",
                file
        );
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "分享 Excel 文件"));
    }

    // ==================== 导入 Excel ====================

    /**
     * 从Excel导入按钮名称 - 对应Python中的import_from_excel方法
     */
    private void importFromExcel() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "选择 Excel 文件"), REQUEST_PICK_FILE);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                processImport(uri);
            }
        }
    }

    private void processImport(Uri uri) {
        executor.execute(() -> {
            try {
                List<String> buttonNames = ExcelHelper.importFromExcel(this, uri);

                runOnUiThread(() -> {
                    if (buttonNames.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("提示")
                                .setMessage("未找到有效的按钮名称！\n\nExcel格式要求：\n" +
                                        "- D列（作业站场）或L列（对应占用区段名称）\n" +
                                        "- 按钮名称 = 作业站场+对应占用区段名称\n" +
                                        "- 从第4行开始读取（跳过表头）")
                                .setPositiveButton("确定", null)
                                .show();
                        return;
                    }

                    int createdCount = 0;
                    for (String name : buttonNames) {
                        // 检查是否已存在
                        boolean exists = false;
                        for (ToggleButtonView btn : buttonList) {
                            if (btn.getName().equals(name)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            createToggleButton(name);
                            createdCount++;
                        }
                    }
                    updateButtonCount();

                    Toast.makeText(this,
                            "已从 Excel 导入 " + createdCount + " 个按钮！",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "导入失败:\n" + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    // ==================== 生命周期 ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(clockRunnable);
        executor.shutdown();
    }
}

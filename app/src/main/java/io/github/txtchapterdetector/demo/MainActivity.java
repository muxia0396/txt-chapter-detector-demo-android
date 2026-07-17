package io.github.txtchapterdetector.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;

/** Small UI demo for the offline chapter detector. */
public class MainActivity extends Activity {
    private static final int OPEN_FILE = 100;
    private final List<ChapterDetector.Chapter> chapters = new ArrayList<>();
    private TextView title;
    private TextView status;
    private TextView reader;
    private ScrollView scroll;
    private int currentChapter;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(250, 248, 255));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
        if (getIntent() != null && getIntent().getData() != null) loadUri(getIntent().getData());
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(250, 248, 255));
        root.setPadding(dp(16), dp(12), dp(16), dp(8));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            view.setPadding(dp(16), bars.top + dp(8), dp(16), bars.bottom + dp(8));
            return insets;
        });

        title = new TextView(this);
        title.setText("请选择一本 TXT 小说");
        title.setTextColor(Color.rgb(35, 32, 40));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        title.setTypeface(null, 1);
        title.setSingleLine(false);
        title.setMaxLines(3);
        title.setEllipsize(null);
        title.setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY);
        title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, dp(4), 0, dp(4));
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("支持 UTF-8、UTF-16、GB18030；离线识别章节目录");
        status.setTextColor(Color.rgb(85, 81, 91));
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        status.setMaxLines(2);
        status.setPadding(0, 0, 0, dp(6));
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        addActionRow(root, "打开 TXT", v -> openFile(), "目录", v -> showChapters());
        addActionRow(root, "上一章", v -> jumpRelative(-1), "下一章", v -> jumpRelative(1));

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(220, 217, 225));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.topMargin = dp(8);
        dividerParams.bottomMargin = dp(4);
        root.addView(divider, dividerParams);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        reader = new TextView(this);
        reader.setTextColor(Color.rgb(45, 43, 48));
        reader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        reader.setLineSpacing(dp(7), 1f);
        reader.setPadding(dp(2), dp(10), dp(2), dp(80));
        reader.setText("打开 TXT 后，正文会显示在这里。\n\n长文件名会在顶部自动换行，四个操作按钮始终以两行两列显示。");
        scroll.addView(reader, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private void addActionRow(LinearLayout root, String leftText, View.OnClickListener leftAction,
                              String rightText, View.OnClickListener rightAction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));
        addActionButton(row, leftText, leftAction, false);
        addActionButton(row, rightText, rightAction, true);
        root.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void addActionButton(LinearLayout row, String text, View.OnClickListener action, boolean hasLeftMargin) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setOnClickListener(action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        if (hasLeftMargin) params.leftMargin = dp(8);
        row.addView(button, params);
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, OPEN_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != OPEN_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (SecurityException ignored) { }
        loadUri(uri);
    }

    private void loadUri(Uri uri) {
        status.setText("正在读取并识别章节…");
        new Thread(() -> {
            try {
                InputStream input = getContentResolver().openInputStream(uri);
                if (input == null) throw new IllegalStateException("无法打开文件");
                String text = decode(readAll(input)).replace("\r\n", "\n").replace('\r', '\n');
                List<ChapterDetector.Chapter> detected = ChapterDetector.detect(text);
                runOnUiThread(() -> showBook(uri, text, detected));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    status.setText("打开失败");
                    Toast.makeText(this, "打开失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showBook(Uri uri, String text, List<ChapterDetector.Chapter> detected) {
        chapters.clear();
        chapters.addAll(detected);
        currentChapter = 0;
        reader.setText(text);
        String name = uri.getLastPathSegment();
        title.setText(name == null || name.isEmpty() ? "TXT 小说" : name);
        status.setText("共 " + text.length() + " 字，离线识别到 " + chapters.size() + " 个目录项");
        scroll.scrollTo(0, 0);
    }

    private void showChapters() {
        if (chapters.isEmpty()) {
            Toast.makeText(this, "没有识别到章节标题", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = new String[chapters.size()];
        for (int i = 0; i < chapters.size(); i++) items[i] = (i + 1) + ". " + chapters.get(i).title;
        new AlertDialog.Builder(this)
                .setTitle("目录（" + chapters.size() + "项）")
                .setItems(items, (dialog, which) -> jumpToChapter(which))
                .show();
    }

    private void jumpRelative(int direction) {
        if (chapters.isEmpty()) return;
        int target = Math.max(0, Math.min(chapters.size() - 1, currentChapter + direction));
        jumpToChapter(target);
    }

    private void jumpToChapter(int index) {
        currentChapter = index;
        int offset = chapters.get(index).offset;
        reader.post(() -> {
            Layout layout = reader.getLayout();
            if (layout == null) return;
            int line = layout.getLineForOffset(Math.max(0, Math.min(offset, reader.length())));
            scroll.smoothScrollTo(0, Math.max(0, layout.getLineTop(line) - dp(8)));
        });
    }

    private static byte[] readAll(InputStream input) throws Exception {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        } finally { input.close(); }
    }

    private static String decode(byte[] data) throws CharacterCodingException {
        int start = 0;
        Charset charset;
        if (data.length >= 3 && (data[0] & 255) == 0xEF && (data[1] & 255) == 0xBB && (data[2] & 255) == 0xBF) {
            charset = Charset.forName("UTF-8"); start = 3;
        } else if (data.length >= 2 && (data[0] & 255) == 0xFF && (data[1] & 255) == 0xFE) {
            charset = Charset.forName("UTF-16LE"); start = 2;
        } else if (data.length >= 2 && (data[0] & 255) == 0xFE && (data[1] & 255) == 0xFF) {
            charset = Charset.forName("UTF-16BE"); start = 2;
        } else {
            charset = isUtf8(data) ? Charset.forName("UTF-8") : Charset.forName("GB18030");
        }
        return charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .decode(ByteBuffer.wrap(data, start, data.length - start)).toString();
    }

    private static boolean isUtf8(byte[] data) {
        try {
            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            decoder.decode(ByteBuffer.wrap(data));
            return true;
        } catch (Exception ignored) { return false; }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}

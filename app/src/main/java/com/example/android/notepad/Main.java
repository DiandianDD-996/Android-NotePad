package com.example.android.notepad;

import static com.example.android.notepad.NotePad.Notes.*;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.text.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main extends Activity {
  final String[] categories = {"未分类", "学习", "生活", "工作"},
      orders =
          {"modified DESC, _id DESC", "modified ASC, _id ASC", "title COLLATE NOCASE ASC, _id ASC"};
  final ArrayList<Note> rows = new ArrayList<>();
  EditText query;
  Spinner filter, sort;
  ListView list;
  TextView status;
  ArrayAdapter<String> items;

  static class Note {
    long id, time;
    String title, body, category;
  }

  Spinner spinner(String[] values) {
    Spinner s = new Spinner(this);
    ArrayAdapter<String> a =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
    s.setAdapter(a);
    return s;
  }

  int dp(int x) {
    return Math.round(x * getResources().getDisplayMetrics().density);
  }

  public void onCreate(Bundle state) {
    super.onCreate(state);
    setTitle("NotePad · 我的笔记");
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(1);
    root.setPadding(dp(12), dp(8), dp(12), 0);
    setContentView(root);
    query = new EditText(this);
    query.setSingleLine();
    query.setHint("搜索标题或正文");
    root.addView(query);
    LinearLayout bar = new LinearLayout(this);
    root.addView(bar);
    filter = spinner(new String[] {"全部分类", "未分类", "学习", "生活", "工作"});
    sort = spinner(new String[] {"最近修改", "最早修改", "标题升序"});
    bar.addView(filter, new LinearLayout.LayoutParams(0, -2, 1));
    bar.addView(sort, new LinearLayout.LayoutParams(0, -2, 1));
    Button add = new Button(this);
    add.setText("新建笔记");
    root.addView(add);
    add.setOnClickListener(v -> edit(null));
    status = new TextView(this);
    root.addView(status);
    list = new ListView(this);
    root.addView(list);
    items = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    list.setAdapter(items);
    list.setOnItemClickListener((a, v, i, id) -> edit(rows.get(i)));
    list.setOnItemLongClickListener(
        (a, v, i, id) -> {
          Note n = rows.get(i);
          new AlertDialog.Builder(this)
              .setTitle("删除「" + n.title + "」？")
              .setNegativeButton("取消", null)
              .setPositiveButton(
                  "删除",
                  (d, w) -> {
                    getContentResolver()
                        .delete(ContentUris.withAppendedId(CONTENT_URI, n.id), null, null);
                    refresh();
                  })
              .show();
          return true;
        });
    if (state != null) {
      query.setText(state.getString("q", ""));
      filter.setSelection(state.getInt("f"));
      sort.setSelection(state.getInt("s"));
    }
    AdapterView.OnItemSelectedListener pick =
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> a) {}

          public void onItemSelected(AdapterView<?> a, android.view.View v, int p, long id) {
            refresh();
          }
        };
    filter.setOnItemSelectedListener(pick);
    sort.setOnItemSelectedListener(pick);
    query.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            refresh();
          }

          public void afterTextChanged(Editable e) {}
        });
    refresh();
  }

  protected void onSaveInstanceState(Bundle s) {
    super.onSaveInstanceState(s);
    s.putString("q", query.getText().toString());
    s.putInt("f", filter.getSelectedItemPosition());
    s.putInt("s", sort.getSelectedItemPosition());
  }

  void refresh() {
    String key = query.getText().toString().trim();
    ArrayList<String> args = new ArrayList<>();
    String where = "(instr(lower(title),lower(?))>0 OR instr(lower(note),lower(?))>0)";
    args.add(key);
    args.add(key);
    if (filter.getSelectedItemPosition() > 0) {
      where += " AND category=?";
      args.add(filter.getSelectedItem().toString());
    }
    rows.clear();
    items.clear();
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    try (Cursor c =
        getContentResolver()
            .query(
                CONTENT_URI,
                new String[] {_ID, "title", "note", "category", "modified"},
                where,
                args.toArray(new String[0]),
                orders[Math.max(0, sort.getSelectedItemPosition())])) {
      if (c != null)
        while (c.moveToNext()) {
          Note n = new Note();
          n.id = c.getLong(0);
          n.title = c.getString(1);
          n.body = c.getString(2);
          n.category = c.getString(3);
          n.time = c.getLong(4);
          rows.add(n);
          items.add(n.title + "\n" + n.category + " · " + fmt.format(new Date(n.time)));
        }
    }
    status.setText(rows.isEmpty() ? "没有符合条件的笔记" : "共 " + rows.size() + " 条笔记");
  }

  void edit(Note n) {
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(1);
    form.setPadding(dp(20), dp(8), dp(20), 0);
    EditText title = new EditText(this), body = new EditText(this);
    title.setHint("标题");
    title.setSingleLine();
    body.setHint("正文");
    body.setMinLines(3);
    Spinner cat = spinner(categories);
    form.addView(title);
    form.addView(body);
    form.addView(cat);
    if (n != null) {
      title.setText(n.title);
      body.setText(n.body);
      cat.setSelection(Math.max(0, Arrays.asList(categories).indexOf(n.category)));
    }
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle(n == null ? "新建笔记" : "编辑笔记")
            .setView(form)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create();
    d.show();
    d.getButton(-1)
        .setOnClickListener(
            v -> {
              String t = title.getText().toString().trim();
              if (t.isEmpty()) {
                title.setError("请输入标题");
                return;
              }
              ContentValues val = new ContentValues();
              val.put("title", t);
              val.put("note", body.getText().toString());
              val.put("category", cat.getSelectedItem().toString());
              val.put("modified", System.currentTimeMillis());
              if (n == null) getContentResolver().insert(CONTENT_URI, val);
              else
                getContentResolver()
                    .update(ContentUris.withAppendedId(CONTENT_URI, n.id), val, null, null);
              d.dismiss();
              refresh();
            });
  }
}

package com.example.android.notepad;

import static org.junit.Assert.*;

import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.Test;

public class CourseTest {
  UiDevice d;

  UiObject2 obj(String text) {
    UiObject2 item = d.wait(Until.findObject(By.text(text)), 5000);
    assertNotNull("未找到：" + text, item);
    return item;
  }

  void has(String text) {
    obj(text);
  }

  void hasPart(String part) {
    assertTrue(part, d.wait(Until.hasObject(By.textContains(part)), 5000));
  }

  void click(String s) {
    obj(s).click();
    d.waitForIdle();
  }

  void back() {
    d.pressBack();
    d.waitForIdle();
  }

  void input(int i, String s) {
    java.util.List<UiObject2> fields=d.wait(Until.findObjects(By.clazz("android.widget.EditText")), 5000);
    assertNotNull("编辑框没有出现", fields);
    fields.get(i).setText(s);
    d.waitForIdle();
  }

  @Test
  public void acceptance() throws Exception {
    d = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
    assertNotNull(launch);
    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    ctx.startActivity(launch);
    d.waitForIdle();
    String suffix = Long.toString(System.currentTimeMillis());
    String title = "复习" + suffix;
    click("新建笔记");
    has("保存");
    input(0, title);
    input(1, "LazyColumn" + suffix);
    java.util.List<UiObject2> spinners = d.findObjects(By.clazz("android.widget.Spinner"));
    spinners.get(spinners.size() - 1).click();
    click("学习");
    click("保存");
    hasPart(title);
    d.takeScreenshot(new java.io.File(ctx.getFilesDir(), "notes.png"));
    String second = "备忘" + suffix;
    click("新建笔记");
    has("保存");
    input(0, second);
    input(1, "第二条正文");
    click("保存");
    hasPart(second);
    assertTrue(d.findObject(By.textContains(second)).getVisibleCenter().y
        < d.findObject(By.textContains(title)).getVisibleCenter().y);
    d.findObjects(By.clazz("android.widget.Spinner")).get(1).click();
    click("最早修改");
    assertTrue(d.findObject(By.textContains(title)).getVisibleCenter().y
        < d.findObject(By.textContains(second)).getVisibleCenter().y);
    input(0, "LazyColumn" + suffix);
    hasPart(title);
    d.findObjects(By.clazz("android.widget.Spinner")).get(0).click();
    click("生活");
    has("没有符合条件的笔记");
    d.findObjects(By.clazz("android.widget.Spinner")).get(0).click();
    click("全部分类");
    hasPart(title);
    d.findObjects(By.clazz("android.widget.Spinner")).get(1).click();
    click("标题升序");
    hasPart(title);
    assertTrue(
        d.findObject(By.textContains(title))
            .getText()
            .matches("(?s).*学习 · [0-9]{4}-[0-9]{2}-[0-9]{2} [0-9:]{8}"));
    d.findObject(By.textContains(title)).longClick();
    click("删除");
    has("没有符合条件的笔记");
  }
}

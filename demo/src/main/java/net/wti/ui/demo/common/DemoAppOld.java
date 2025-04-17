package net.wti.ui.demo.common;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.wti.gdx.theme.raeleus.sgx.GdxThemeSgx;
import net.wti.gdx.theme.raeleus.sgx.TabbedPane;
import net.wti.ui.demo.api.DayOfWeek;
import net.wti.ui.demo.api.ModelRecurrence;
import net.wti.ui.demo.api.ModelTask;
import net.wti.ui.demo.ui.SettingsPanel;
import net.wti.ui.demo.ui.TaskTableOld;
import net.wti.ui.demo.ui.TaskViewOld;
import net.wti.ui.gdx.theme.GdxTheme;
import xapi.model.api.ModelList;

/// DemoApp:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 11:38
public class DemoAppOld  extends ApplicationAdapter {

    private Skin skin;
    private Stage stage;

    @Override
    public void create() {
        final GdxTheme theme = new GdxThemeSgx();
        skin = theme.getSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Texture texture = new Texture(Gdx.files.internal(theme.getAssetPath() + "/background.png"));
        Image background = new Image(texture);
        background.setFillParent(true);
        stage.addActor(background);

        TabbedPane root = new TabbedPane(theme.getSkin());
        root.setFillParent(true);
        if (stage.getWidth() > 400) {
            root.pad(10, 100, 10, 100);
        }

        TaskTableOld todos = new TaskTableOld(theme);
        todos.setHeader("To Do");

//        todos.setFillParent(true);
//        todos.setLayoutEnabled(true);


        TaskTableOld done = new TaskTableOld(theme);
//        done.setFillParent(true);
        done.setHeader("Done");

        // dailies
        final TaskViewOld firstItem;
        firstItem = todos.addTask(wakeUp());
        todos.addTask(cookBreakfast());
        todos.addTask(brushTeethMorning());
        todos.addTask(cookLunch());
        todos.addTask(cookDinner());
        todos.addTask(brushTeethNight());
        todos.addTask(bedTimeKids());
        todos.addTask(bedTime());

        // weeklies
        todos.addTask(doLaundry());
        todos.addTask(checkMail());

        // monthlies
        todos.addTask(payMortgage());
        todos.addTask(payBills());
        // one-offs
        todos.addTask(buyPotatoes());

        Table outer = new Table(skin);
        outer.setFillParent(true);
        outer.add(todos).expand().fill();

        root.addTab("To Do", outer);
        root.addTab("Done", done);
        // TODO: a settings panel.
        root.addTab("Settings", new SettingsPanel(theme));

        stage.addActor(root);
//        todos.layout();
        // TODO: remember last panel and switch+focus
        stage.setScrollFocus(todos);
    }

    private ModelTask payMortgage() {
        final ModelTask task = TaskFactory.create("Pay Mortgage", "Put money into mortgage account");
        task.recurrence().add(ModelRecurrence.monthly(1));
        return task;
    }

    private ModelTask checkMail() {
        final ModelTask task = TaskFactory.create("Check Mail", "Check the mail");
        task.recurrence().add(ModelRecurrence.biweekly(DayOfWeek.WEDNESDAY));
        return task;
    }

    private ModelTask payBills() {
        final ModelTask task = TaskFactory.create("Pay Bills", "Pay monthly bills");
        task.recurrence().add(ModelRecurrence.monthly(21));
        return task;
    }

    private static ModelTask doLaundry() {
        final ModelTask task = TaskFactory.create("Do Laundry", "Put away ALLL the clothes!");
        final ModelList<ModelRecurrence> recur = task.recurrence();
        recur.add(ModelRecurrence.biweekly(DayOfWeek.FRIDAY));
        recur.add(ModelRecurrence.biweekly(DayOfWeek.SATURDAY, 1));
        return task;
    }

    /// @return a {@link ModelTask} describing a simply daily "Cook Lunch" task at 12pm.
    private static ModelTask cookLunch() {
        final ModelTask task = TaskFactory.create("Cook Lunch", "Make lunch!");
        task.recurrence().add(ModelRecurrence.daily(12, 0));
        return task;
    }

    /// @return a {@link ModelTask} describing a simply daily "Cook Dinner" task at 5:30pm.
    private static ModelTask cookDinner() {
        final ModelTask task = TaskFactory.create("Cook Dinner", "Make a big meal!");
        task.recurrence().add(ModelRecurrence.daily(17, 30));
        return task;
    }
    ///  A task for cooking breakfast at a reasonable time.
    ///
    ///  My schedule is a bit complex; every Monday and Tuesday I have to be cooking by 7am.
    ///  From Wednesday through to Saturday, I have to be up by 9am.
    ///  Every second Sunday, I am up at either 9 or 7.
    ///
    ///  Thus, this task uses 6 weekly and 2 biweekly recurrences to handle "Cook Breakfast"
    ///
    ///  @return a task representing my rather complex breakfast schedule.
    private static ModelTask cookBreakfast() {
        ModelTask task;
        task = TaskFactory.create("Make Breakfast", "Cook / eat something");
        final ModelList<ModelRecurrence> recur = task.recurrence();
        specialBiWeekly(recur, 7, 0, 9, 30);
        return task;
    }
    private static ModelTask wakeUp() {
        ModelTask task;
        task = TaskFactory.create("Wake Up!", "Get outta bed!");
        task.setAlarmMinutes(10);
        final ModelList<ModelRecurrence> recur = task.recurrence();
        specialBiWeekly(recur, 6, 45, 9, 0);
        return task;
    }


    /// @return a {@link ModelTask} describing my bed time schedule.
    ///
    /// The schedule varies between 11pm and 1am, depending on whether I need to be up early or not.
    private static ModelTask bedTime() {
        ModelTask task;
        task = TaskFactory.create("Bed Time", "Go to sleep");

        final ModelList<ModelRecurrence> recur = task.recurrence();
        // 11pm and 1am
        specialBiWeekly(recur, 23, 0, 25, 0);
        return task;
    }

    /// @return a {@link ModelTask} describing my kids' bed time schedule.
    ///
    /// The schedule applies every sunday, monday, tuesday and every 2nd saturday.
    private static ModelTask bedTimeKids() {
        ModelTask task;
        task = TaskFactory.create("Kids in Bed", "Put kids in bed");

        task.setAlarmMinutes(30);
        final ModelList<ModelRecurrence> recur = task.recurrence();
        // 9:30 on the days I have my kids
        specialBiWeekly1(recur, 21, 30);
        return task;
    }

    private static ModelTask brushTeethMorning() {
        ModelTask task;
        task = TaskFactory.create("Morning brush", "Brush teeth / hair");
        final ModelList<ModelRecurrence> recur = task.recurrence();
        specialBiWeekly(recur, 7, 45, 9, 30);
        return task;
    }

    private static ModelTask brushTeethNight() {
        ModelTask task;
        task = TaskFactory.create("Nightly brush", "Brush + floss teeth");
        final ModelList<ModelRecurrence> recur = task.recurrence();
        specialBiWeekly(recur, 20, 0, 23, 55);
        return task;
    }

    private ModelTask buyPotatoes() {
        final ModelTask task = TaskFactory.create("Buy Potatoes", "");
        task.recurrence().add(ModelRecurrence.once(task.getBirth(), 3, 0, 0));
        return task;
    }

    private static void specialBiWeekly1(final ModelList<ModelRecurrence> recur, final int firstHour, final int firstMinute) {
        recur.add(ModelRecurrence.biweekly(DayOfWeek.SUNDAY, 0, firstHour, firstMinute));
        recur.add(ModelRecurrence.weekly(DayOfWeek.MONDAY,  firstHour, firstMinute));
        recur.add(ModelRecurrence.weekly(DayOfWeek.TUESDAY, firstHour, firstMinute));
    }
    private static void specialBiWeekly2(final ModelList<ModelRecurrence> recur, final int secondHour, final int secondMinute) {
        recur.add(ModelRecurrence.biweekly(DayOfWeek.SUNDAY, 1, secondHour, secondMinute));
        recur.add(ModelRecurrence.weekly(DayOfWeek.WEDNESDAY, secondHour, secondMinute));
        recur.add(ModelRecurrence.weekly(DayOfWeek.THURSDAY,  secondHour, secondMinute));
        recur.add(ModelRecurrence.weekly(DayOfWeek.FRIDAY,    secondHour, secondMinute));
        recur.add(ModelRecurrence.weekly(DayOfWeek.SATURDAY,  secondHour, secondMinute));
    }

    private static void specialBiWeekly(final ModelList<ModelRecurrence> recur, final int firstHour, final int firstMinute, final int secondHour, final int secondMinute) {
        specialBiWeekly1(recur, firstHour, firstMinute);
        specialBiWeekly2(recur, secondHour, secondMinute);
    }


    @Override
    public void render() {
        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glClear(GL31.GL_COLOR_BUFFER_BIT);

        stage.act();
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            dispose();
            create();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        skin.dispose();
        stage.dispose();
    }
}

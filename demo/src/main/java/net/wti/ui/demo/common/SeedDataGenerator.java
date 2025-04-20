package net.wti.ui.demo.common;

import net.wti.ui.demo.api.*;
import net.wti.ui.demo.ui.controller.TaskController;
import net.wti.ui.demo.ui.view.TaskTableActive;
import net.wti.ui.demo.ui.view.TaskTableComplete;
import net.wti.ui.demo.ui.view.TaskTableDefinitions;
import xapi.model.X_Model;
import xapi.model.api.ModelList;
import xapi.util.api.SuccessHandler;

/// SeedDataGenerator:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 20:55

/// SeedDataGenerator
///
/// Creates demo data from the *entire* DemoApp roadmap checklist.
/// 1. For every line item it persists a `ModelTaskDescription` and inserts it
///    into the **Library** table.
/// 2. If the item is *not* marked done, it clones the description into an
///    active `ModelTask`, persists it, and adds it to **Active**.
/// 3. If the item *is* done, it clones the task into a `ModelTaskCompletion`
///    with `CompletionStatus.COMPLETED` and inserts it into **Done**.
///
/// Uses only Java‑8 language features (no records, var, streams API is
/// allowable but avoided for widest compatibility).
public final class SeedDataGenerator {

    // ------------------------------------------------------------------ //
    // Hard‑coded checklist items                                         //
    // ------------------------------------------------------------------ //
    private static final String[][] CHECKLIST = {
            // title                      , description                                              , done?
            { "Implement Task Deferral"   , "Handle deferred scheduling and UI state update"        , "false" },
            { "Implement Task Cancellation", "Allow tasks to be canceled and removed from active list", "false" },
            { "Persist & Reload Tasks"    , "Serialize to JSON on app exit; reload on start"        , "false" },
            { "Animate Task Movement"     , "Polish UI transitions when tasks move between lists"   , "false" },
            { "Undo After Completion"     , "Snackbar‑style undo for five seconds"                  , "false" },
            { "Unit Tests via Spock"      , "Lifecycle + recurrence unit tests"                     , "false" },
            { "Recurrence Logic QA"       , "Weekly & bi‑weekly edge‑case verification"             , "false" },
            { "Toggle Expand/Collapse"    , "Click ACTIVE row to show full details"                 , "false" },
            { "Display Recurrence Info"   , "Day/time range in expanded view"                       , "false" },
            { "Hover Affordance"          , "Opacity change on expand/collapse hover"               , "false" },
            { "Improve Expanded Layout"   , "Clear spacing, labels, consistent padding"             , "false" },

            // completed items
            { "Mark Task Done"            , "Move ONCE task to done list"                           , "true"  },
            { "Reschedule Recurring"      , "Reinsert repeating task with next deadline"            , "true"  },
            { "Persist New Task"          , "Uses X_Model.persist"                                  , "true"  },
            { "Create Task UI"            , "TaskView + DeadlineView skeleton"                      , "true"  },
            { "Click Expand TaskView"     , "Make TaskView respond to user click"                   , "true"  },
            { "Show Recurrence Data"      , "Include day/time in expanded task view"                , "true"  },
            { "Style Expanded View"       , "Spaced layout, visible deadlines"                      , "true"  },
            { "Toggle Completed View"     , "Collapse/expand extra info for done tasks"             , "true"  }
    };

    // ------------------------------------------------------------------ //
    // Public entry point                                                 //
    // ------------------------------------------------------------------ //
    public static void seed(TaskController controller,
                            TaskTableDefinitions library,
                            TaskTableActive active,
                            TaskTableComplete done) {

        for (String[] row : CHECKLIST) {
            String title = row[0];
            String desc  = row[1];
            boolean isDone = Boolean.parseBoolean(row[2]);

            // 1) definition
            ModelTaskDescription def = X_Model.create(ModelTaskDescription.class)
                    .setName(title)
                    .setDescription(desc)
                    .setPriority(TaskPriority.medium)
                    .setBirth(System.currentTimeMillis());

            persist(def);
            library.addTask(def);

            // 2) clone to live task
            ModelTask task = X_Model.create(ModelTask.class);
            BasicModelTask.copyModel(def, task);
            task.setTaskSource(def.getKey());

            if (isDone) {
                // 3) convert to completion
                ModelTaskCompletion cmp = X_Model.create(ModelTaskCompletion.class);
                BasicModelTask.copyModel(task, cmp);
                cmp.setCompleted(System.currentTimeMillis());
                cmp.setSourceTask(task.getKey());
                cmp.setStatus(CompletionStatus.COMPLETED);

                persist(cmp);
                done.addTask(cmp);

            } else {
                controller.save(task);      // persists + injects into registry
                active.addTask(task);
            }
        }

        // human-generated, life-relevant tasks
        active.addTask(wakeUp());
        active.addTask(cookBreakfast());
        active.addTask(brushTeethMorning());
        active.addTask(cookLunch());
        active.addTask(cookDinner());
        active.addTask(brushTeethNight());
        active.addTask(bedTimeKids());
        active.addTask(bedTime());

        // weeklies
        active.addTask(doLaundry());
        active.addTask(checkMail());

        // monthlies
        active.addTask(payMortgage());
        active.addTask(payBills());
        // one-offs
        active.addTask(buyPotatoes());

    }


    private static ModelTask payMortgage() {
        final ModelTask task = TaskFactory.create("Pay Mortgage", "Put money into mortgage account");
        task.recurrence().add(ModelRecurrence.monthly(1));
        return task;
    }

    private static ModelTask checkMail() {
        final ModelTask task = TaskFactory.create("Check Mail", "Check the mail");
        task.recurrence().add(ModelRecurrence.biweekly(DayOfWeek.WEDNESDAY));
        return task;
    }

    private static ModelTask payBills() {
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

    private static ModelTask buyPotatoes() {
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


    private static void persist(BasicModelTask<?> m) {
        X_Model.persist(m, SuccessHandler.noop());
    }

    private SeedDataGenerator() {}
}
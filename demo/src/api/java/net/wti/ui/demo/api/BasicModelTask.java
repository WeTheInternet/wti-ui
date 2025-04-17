package net.wti.ui.demo.api;

import xapi.model.api.Model;

/// BasicModelTask:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/04/2025 @ 23:09
public interface BasicModelTask<Self extends BasicModelTask<Self>> extends Model {

    /// The name of the task at the time it was completed
    String getName();
    Self setName(String name);

    /// The description of the task at completion time
    String getDescription();
    Self setDescription(String description);

}

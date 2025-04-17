package net.wti.ui.api;

/// FieldType: an enum describing the various "primitive" form field types
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 23:55
public enum FieldType {
    text,
    integer,
    decimal,
    readonly,
    multiline,
    checkbox,
    radiobox,
    selectbox,
    date,
    time,
    datetime,
    duration,
    recurrence,
    rating,
    file,
    files,
    // this field is a special composite of multiple other fields
    composite
}


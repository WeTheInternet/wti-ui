package com.ray3k.sgx;

import com.badlogic.gdx.files.FileHandle;

/// FileListItem:
///
/// Adapted from theme example zip from [Raeleus blog](https://ray3k.wordpress.com/sgx-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 02:58
public class FileListItem {
    final FileHandle file;

    final String name;

    public FileListItem(FileHandle file) {
        this(file.name(), file);
    }

    public FileListItem(String name, FileHandle file){
        if(file.isDirectory()){
            name += "/";
        }
        this.name = name;
        this.file = file;
    }

    @Override
    public String toString() {
        return name;
    }
}
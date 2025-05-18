package com.coffeechat;

import android.net.Uri;

import java.io.File;

public class CameraOutput {
    public File photoFile;
    public Uri photoUri;

    public CameraOutput(File photoFile, Uri photoUri) {
        this.photoFile = photoFile;
        this.photoUri = photoUri;
    }
}
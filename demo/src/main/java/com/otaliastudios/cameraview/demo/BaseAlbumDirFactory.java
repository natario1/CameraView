package com.otaliastudios.cameraview.demo;

import android.os.Environment;

import java.io.File;

public final class BaseAlbumDirFactory extends AlbumStorageDirFactory {

	// Standard storage location for digital camera files
	private static final String CAMERA_DIR = "/Pictures/";


	@Override
	public File getAlbumStorageDir(String albumName) {
		return new File(
				Environment.getExternalStorageDirectory()
				+ CAMERA_DIR
				+ albumName
		);
	}
}

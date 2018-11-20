package motionlogger_app.bigdata.net.motionloggerapp;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetsUtils {
    private Context context;

    public AssetsUtils(Context context) {
        this.context = context;
    }

    private void copyFileOrDir(String path, String toPath) {
        AssetManager assetManager = context.getAssets();

        try {
            String assets[] = assetManager.list(path);

            if (assets.length == 0) {
                copyFile(path, toPath);
            } else {
                String fullPath = toPath + "/" + path;
                File dir = new File(fullPath);

                if (!dir.exists()) {
                    dir.mkdir();
                }

                for (String asset: assets) {
                    copyFileOrDir(path + "/" + asset, toPath);
                }
            }
        } catch (IOException ex) {}
    }

    public boolean copyAssetsFolder(String fromAssetPath, String toPath) {
        copyFileOrDir(fromAssetPath, toPath);
        return true;
    }

    private void copyFile(String filename, String toPath) {
        AssetManager assetManager = context.getAssets();

        InputStream in;
        OutputStream out;

        try {
            in = assetManager.open(filename);

            String newFileName = toPath+ "/" + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {}
    }
}

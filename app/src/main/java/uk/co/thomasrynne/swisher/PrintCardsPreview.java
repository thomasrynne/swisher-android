package uk.co.thomasrynne.swisher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.process.BitmapProcessor;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;


/**
 */
public class PrintCardsPreview extends Activity {

    private static final int width = 6 * 300;
    private static final int height = 4 * 300;
    private static final int margin = 40;
    private static final int square = (width - (margin * 4)) / 3;

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageView = new ImageView(this);
        setContentView(imageView);
        printCards();
    }

    public void printCards() {
//        final List<Uri> images = new LinkedList<Uri>();
//        Events.TracksStatus tracksStatus = EventBus.getDefault().getStickyEvent(Events.TracksStatus.class);
//        for (Events.TracksStatus.TrackGroup track : tracksStatus==null?new Events.TracksStatus.TrackGroup[]{} : tracksStatus.tracks) {
//            if (track.image != null && images.size() < 6) {
//                images.add(track.image);
//            }
//        }
//        final List<Bitmap> bitmaps = new LinkedList<Bitmap>();
//        for (Uri image : images) {
//            ImageLoader.getInstance().loadImage(
//                    image.toString(), new ImageSize(square, square),
//                    new DisplayImageOptions.Builder().postProcessor(new BitmapProcessor() {
//                        @Override
//                        public Bitmap process(Bitmap bitmap) {
//                            return Bitmap.createScaledBitmap(bitmap, square, square, false);
//                        }
//                    }).imageScaleType(ImageScaleType.NONE).build(),
//                    new ImageLoadingListener() {
//                        @Override
//                        public void onLoadingStarted(String s, View view) {
//                        }
//
//                        @Override
//                        public void onLoadingFailed(String s, View view, FailReason failReason) {
//                        }
//
//                        @Override
//                        public void onLoadingComplete(String s, View view, Bitmap bitmap) {
//                            bitmaps.add(bitmap);
//                            if (images.size() == bitmaps.size()) {
//                                try {
//                                    Uri image = createImage(bitmaps);
//                                    showImage(image);
//                                } catch (Exception e) {
//                                    throw new RuntimeException(e);
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void onLoadingCancelled(String s, View view) {
//                        }
//                    }
//            );
//        }
    }

    private void showImage(final Uri image) {
        imageView.setImageURI(image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/png");
                // Make sure you put example png image named myImage.png in your
                // directory
                share.putExtra(Intent.EXTRA_STREAM, image);

                startActivity(Intent.createChooser(share, "Share Image!"));

            }
        });
    }

    private static Uri createImage(List<Bitmap> images) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint white = new Paint();
        white.setColor(Color.WHITE);
        white.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, width, height, white);
        for (int i = 0; i < (Math.min(3, images.size())); i++) {
            Bitmap image = images.get(i);
            canvas.drawBitmap(image, margin + (i * (square + margin)), margin, null);
        }
        for (int i = 3; i < (Math.min(6, images.size())); i++) {
            Bitmap image = images.get(i);
            canvas.drawBitmap(image, margin + ((i-3) * (square + margin)), (margin * 2) + square, null);
        }


        String imagePath = Environment.getExternalStorageDirectory()
                + "/myImage.png";
        File imageFileToShare = new File(imagePath);
        bitmap.compress(Bitmap.CompressFormat.PNG, 1, new FileOutputStream(imageFileToShare));

        return Uri.fromFile(imageFileToShare);
    }
}

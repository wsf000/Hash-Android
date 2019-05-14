package com.fanjiee.thingsone.camera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.fanjiee.thingsone.FormatUtil;
import com.fanjiee.thingsone.R;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class CameraBCActivity extends Activity {


    private static final String TAG = CameraActivity.class.getSimpleName();
    private SurfaceView mSurfaceView;
    private String mCameraId;
    private Size mPreviewSize;
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private ImageReader mImageReader;
    private ImageView iv;
    private Button btn;
    private File file;
    private SurfaceHolder mSurfaceHolder;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mSurfaceView = (SurfaceView) findViewById(R.id.sfvSurfaceView);
        iv = (ImageView) findViewById(R.id.iv_image);
        btn = (Button) findViewById(R.id.btn);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPreView();


            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        intiCameraThread();

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceView.setZOrderMediaOverlay(true);
        mSurfaceHolder.addCallback(mSurfaceHolderCallback);

    }


    private void intiCameraThread() {
        mCameraThread = new HandlerThread("CameraSurfaceViewThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            setupCamera();
            openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupCamera() {
        //获取摄像头的管理者CameraManager
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            //遍历所有摄像头
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                //此处默认打开后置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                //获取StreamConfigurationMap，他是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
                mCameraId = cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //选择sizeMap中大于并且最接近width和height的size
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            Log.d(TAG, "mCameraId === " + mCameraId);
            cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            startPreView();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                camera.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                camera.close();
                mCameraDevice = null;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCameraDevice.close();

        mCameraThread.quitSafely();

    }

    //

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startPreView() {
        try {

            // 设置捕获请求为预览，这里还有拍照啊，录像等
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
          /*  if (surface != null) {
                Log.d(TAG, "SURFACE不为空");
                mCaptureRequestBuilder.addTarget(surface);
            } else {
                Log.d(TAG, "SURFACE为空");
            }*/

            //      就是在这里，通过这个set(key,value)方法，设置曝光啊，自动聚焦等参数！！ 如下举例：
            mImageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 1);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);
            // 这里一定分别add两个surface，一个Textureview的，一个ImageReader的，如果没add，会造成没摄像头预览，或者没有ImageReader的那个回调！！


            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureRequest = mCaptureRequestBuilder.build();
                    mCameraCaptureSession = session;
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 当有一张图片可用时会回调此方法，但有一点一定要注意：
     **/
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {

           /* Image image = imageReader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            Log.e("===数据", data.toString());
            image.close();*/


            /*Image image = imageReader.acquireLatestImage();
            // get image bytes
            ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
            final byte[] imageBytes = new byte[imageBuf.remaining()];
            Log.e("====================",imageBytes.toString());
            imageBuf.get(imageBytes);
            image.close();

            onPictureTaken(imageBytes);
*/
/*
            //获取每一帧
            Image image = imageReader.acquireNextImage() ;
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            try {
                Bitmap temp = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                Bitmap newBitmap = Bitmap.createBitmap(mPreviewSize.getWidth(),mPreviewSize.getWidth(),temp.getConfig());

                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                Matrix matrix = new Matrix();
                //图片镜像并旋转90度
                matrix.setScale(-1, 1);
                matrix.postTranslate(temp.getWidth(), 0);
                matrix.postRotate(90 ,temp.getWidth()/2,temp.getHeight()/2);
                matrix.postTranslate(0,(temp.getWidth()-temp.getHeight())/2);

                canvas.drawBitmap(temp, matrix , paint );

                File newFile = new File( Environment.getExternalStorageDirectory() , "345.jpg");
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile));
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
                temp.recycle();
                newBitmap.recycle();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close();
                byteBuffer.clear();
            }

            image.close();



        }*/


            Image image = imageReader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            final byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);//由缓冲区存入字节数组
            Log.e("===========000000000000000", bytes.toString());
            Bitmap temp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            final Bitmap newBitmap = Bitmap.createBitmap(mPreviewSize.getWidth(), mPreviewSize.getWidth(), temp.getConfig());
            //  bitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, true);


            if (newBitmap != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                iv.setImageBitmap(newBitmap);
                            }
                        });
                    }
                }).start();


            }
            //onPictureTaken(bytes);
            image.close();

        }

    };

    /**
     * Handle image processing
     */
    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            String imageStr = Base64.encodeToString(imageBytes, Base64.NO_WRAP | Base64.URL_SAFE);
            Log.d(TAG, "imageBase64:" + imageStr);

            final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            file = FormatUtil.saveBitmapFile(bitmap);


            OkGo.<String>post("http://192.168.3.105:9300/images/apiData")
                    .params("appKey", "01a118a0e4814b88afd1dbf8749a8317")
                    .params("appSecret", "e0e73e99fbcd45d18f2fc02f0681ddcd")
                    .params("code", "0")
                    .params("imgname", file)
                    .execute(new StringCallback() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            Log.e("==========数据", response.body());

                        }

                        @Override
                        public void onError(Response<String> response) {
                            super.onError(response);
                            Log.e("头像失败==================================", response.getRawCall().toString());
                        }
                    });


          /*  if (bitmap != null) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                iv.setImageBitmap(bitmap);
                            }
                        });
                    }
                }).start();
            }*/
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

}

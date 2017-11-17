package com.example.wordrecognition;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.WordSimple;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.crop.CropImageActivity;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
public class MainActivity extends AppCompatActivity implements OnResultListener<String> {
    private static final int CROP_IMAGE = -999;
    private static final String TAG = "WordRecognition";
    private CameraManager mCameraManager;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceViewHolder;
    private Handler mHandler;
    private String mCameraId;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private int mState;
    private CameraCaptureSession mSession;
    private ImageView open_album;
    private ImageView take_picture_bt;
    private Handler mainHandler;
    private ImageView img_show;
    private int pictureId;
    private final String wordRecognitionHost = "https://aip.baidubce.com/rest/2.0/ocr/v1/general";//文字识别通用url
    private final String apiKey = "Q8mGEfYzO4hdGDGMG2mS69j9";//API key
    private final String secretKey = "kSH89DLaLjd42VrFhhHkPdjGA83gHlVH";
    private final String accessTokenHost = "https://aip.baidubce.com/oauth/2.0/token?";
    private final int CHOOSE_PHOTO = 2;
    private String recognitionResult;
    private boolean hasGotToken = false;
    private boolean hasGotResult = false;
    private String filePath;
    private ImageView result_img;
    private boolean hasStopPreview = true;
    private Uri outputUri = null;
    private File outputFile = null;
    private String afterImagePath = null;
    private boolean isPhoto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("onCreate");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        //申请写入权限，暂时这样写，虽然很难看
        if(ActivityCompat.checkSelfPermission(MainActivity.this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},3);
        }
        initSurfaceView();//初始化SurfaceView
        img_show = (ImageView) findViewById(R.id.img_show);
        /**
         * 拍照按钮监听
         */
        take_picture_bt = (ImageView) findViewById(R.id.take_picture);
        take_picture_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPhoto){
                    mSurfaceView.setVisibility(View.VISIBLE);
                    img_show.setVisibility(View.GONE);
                    isPhoto = false;
                    return;
                }
                takePicture();
            }
        });
        //打开相册按钮
        open_album = (ImageView) findViewById(R.id.open_album);
        open_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 1);//第二个是请求码
                } else {
                    openAlbum();
                }
            }
        });
        result_img = (ImageView) findViewById(R.id.recognition_result);
        result_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasGotResult) {
                    //利用较少用的隐式Intent
                    Intent intent = new Intent("com.example.LAUNCH_RECOGNITION_RESULT");
                    intent.putExtra("result", recognitionResult);
                    startActivity(intent);
                    hasGotResult = false;
                } else {
                    Toast.makeText(MainActivity.this, "文字识别中...", Toast.LENGTH_SHORT).show();

                }
            }
        });

    }
    public void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");//选择照片后毁掉onActivityResult方法
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
//        startActivity(new Intent(this, DummyActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CHOOSE_PHOTO:
                String beforeImagePath = null;
                afterImagePath = Environment.getExternalStorageDirectory() + "/DCIM/Camera/WordRecognition_picture" + "_temp_" + ".jpg";
                outputFile = new File(afterImagePath);
                img_show.setImageURI(null);
                if (outputFile.exists()){
                    outputFile.delete();
                    try {
                        outputFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (resultCode == RESULT_OK){
                    if (Build.VERSION.SDK_INT > 19) {
                        beforeImagePath = handlerImgOnNewVersion(data);
                    } else {
                        beforeImagePath = handlerImgOnOldVersion(data);
                    }
                    Uri inputUri = Uri.fromFile(new File(beforeImagePath));
                    outputUri = Uri.fromFile(outputFile);
                    Crop.of(inputUri, outputUri).asSquare().start(this);
                }
                break;
            case Crop.REQUEST_CROP:
                String imgPath=null;
                if (resultCode == RESULT_OK) {
                    /*if (Build.VERSION.SDK_INT > 19) {
                        imgPath = handlerImgOnNewVersion(data);
                    } else {
                        imgPath = handlerImgOnOldVersion(data);
                    }*/
                    if (outputUri != null) {
//                        Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
//                        Bitmap bitmap = outputUri.
//                        img_show.setImageBitmap(bitmap);
                        img_show.setImageURI(outputUri);
                        mSurfaceView.setVisibility(View.GONE);
                        img_show.setVisibility(View.VISIBLE);
                        isPhoto = true;
                        System.out.println("imgPath" + imgPath);
                        if (!hasGotToken) {
                            OCR.getInstance().initWithToken(MainActivity.this, "24.9c4bcec34d389e9ace00432196b76266.2592000.1508329125.282335-10115903");//第二个参数为token，为加快识别速度，直接采用现成的token，周期为一个月
                            hasGotToken = true;
                        }
                        wordRecognition(afterImagePath);
                    } else {
                        Toast.makeText(this, "获取图片失败！", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
        }
    }

    private String handlerImgOnOldVersion(Intent data) {
        Uri uri = data.getData();
        String imgPath = getImagePath(uri, null);
        return imgPath;
    }
    private String handlerImgOnNewVersion(Intent data) {
        String imgPath = null;//选择的图片的路径
        Uri uri = data.getData();//选择图片的结果,即图片地址的封装，接下来对其进行解析
        if (DocumentsContract.isDocumentUri(this, uri)) {//判断是否是document类型,DocumentProvider
            String docId = DocumentsContract.getDocumentId(uri);
            switch (uri.getAuthority())//就是获取uri的最开头部分
            {
                case "com.android.providers.media.documents":
                    String id = docId.split(":")[1];//解析出数字格式的id
                    String selection = MediaStore.Images.Media._ID + "=" + id;
                    imgPath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                    break;
                case "com.android.providers.downloads.documents":
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                    imgPath = getImagePath(contentUri, null);
                    break;
                default:
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {//ContentProvider
            imgPath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {//FileProvider,相当于4.4之前的版本
            imgPath = uri.getPath();
        }
        return imgPath;
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    public void initSurfaceView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.mFirstSurfaceView);
        mSurfaceViewHolder = mSurfaceView.getHolder();//通过SurfaceViewHolder可以对SurfaceView进行管理
        mSurfaceViewHolder.addCallback(new SurfaceHolder.Callback() {
            //SurfaceView被成功创建
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCameraAndPreview();
                Log.d(TAG,"SUCCEED_CREATED");
            }

            //SurfaceView被销毁
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //释放camera
                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                Log.d(TAG,"SUCCEED_DESTROYED");

            }

            //SurfaceView内容发生改变
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }
        });
    }

    public void initCameraAndPreview() {
        HandlerThread handlerThread = new HandlerThread("My First Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());//用来处理ui线程的handler，即ui线程
        try {
            mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT;
            mImageReader = ImageReader.newInstance(mSurfaceView.getWidth(), mSurfaceView.getHeight(), ImageFormat.JPEG,/*maxImages*/7);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mainHandler);//这里必须传入mainHandler，因为涉及到了Ui操作
            mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, 2);
            } else {

                mCameraManager.openCamera(mCameraId, deviceStateCallback, mHandler);
            }
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //进行相片存储
            // mCameraDevice.close();
            byte[] imgBytes;
            imgBytes = getImagBytes(reader);//获取img的bytes数据格式
            filePath = savePicture(imgBytes);
            //采用api key和secret key来获取，当然也可以采用license来获取
            if (!hasGotToken) {
                OCR.getInstance().initWithToken(MainActivity.this, "24.999bbf542738bdf02b3cfdee9fddd8be.2592000.1512732685.282335-10115903");//第二个参数为token，为加快识别速度，直接采用现成的token，周期为一个月
                hasGotToken = true;
            }
            wordRecognition(filePath);
            //initAccessTokenWithAkSk();
            //initAccessTokenWithLicense();
        }
    };

    //获取图片的byte数据格式
    public byte[] getImagBytes(ImageReader reader) {
        Image image = reader.acquireNextImage();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];//buffer.capacity，返回buffer的容量
        buffer.get(bytes);//将image对象转化为byte，再转化为bitmap
        return bytes;
    }

    public void initAccessTokenWithAkSk() {
        OCR.getInstance().initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) {
                String token = result.getAccessToken();
                System.out.println("ABC:" + token);
                Toast.makeText(MainActivity.this, "获取token成功", Toast.LENGTH_SHORT).show();
                hasGotToken = true;
                //识别前需要判断token是否成功获取，英文token是变化的，需要实时获取
                wordRecognition(filePath);
            }

            @Override
            public void onError(OCRError error) {
                Toast.makeText(MainActivity.this, "获取token失败", Toast.LENGTH_SHORT).show();
                hasGotToken = false;
                error.printStackTrace();
            }
        }, this, apiKey, secretKey);
    }

    public void initAccessTokenWithLicense() {
        OCR.getInstance().initAccessToken(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
                String token = accessToken.getAccessToken();
                hasGotToken = true;
                wordRecognition(filePath);
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
            }
        }, getApplicationContext());
    }

    public void wordRecognition(String filePath) {
        GeneralParams param = new GeneralParams();
        param.setDetectDirection(true);
        param.setImageFile(new File(filePath));
        OCR.getInstance().recognizeGeneral(param, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult result) {
                StringBuilder sb = new StringBuilder();
                for (WordSimple wordSimple : result.getWordList()) {
                    WordSimple word = wordSimple;
                    sb.append(word.getWords());
                    //sb.append("\n");
                }
                recognitionResult = sb.toString();//记录识别结果
                hasGotResult = true;
                Toast.makeText(MainActivity.this, "文字已识别，点击按钮获取结果！", Toast.LENGTH_LONG).show();
                System.out.println("result:  " + sb);
//                throw new RuntimeException("WYY");
            }

            @Override
            public void onError(OCRError error) {
                System.out.println(error.getMessage());
            }
        });
    }

    public String getAccessToken() {
        String accessTokenUrl = accessTokenHost
                + "grant_type=client_credentials"
                + "&client_id=" + apiKey
                + "&client_secret=" + secretKey;
        try {
            URL realUrl = new URL(accessTokenUrl);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {
                System.err.println(key + "--->" + map.get(key));
            }
            // 定义 BufferedReader输入流来读取URL的响应
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String result = "";
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            /**
             * 返回结果示例
             */
            System.err.println("result:" + result);
            JSONObject jsonObject = new JSONObject(result);
            String access_token = jsonObject.getString("access_token");
            return access_token;
        } catch (Exception e) {
            System.err.printf("获取token失败！");
            e.printStackTrace(System.err);
        }
        return null;
    }
    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            try {
                takePreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
           if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Toast.makeText(MainActivity.this, "打开摄像头失败", Toast.LENGTH_SHORT).show();
        }
    };

    public void takePreview() throws CameraAccessException {
        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.addTarget(mSurfaceViewHolder.getSurface());
        mState = 0;
        mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceViewHolder.getSurface(), mImageReader.getSurface()), mSessionPreviewStateCallback, mHandler);
    }

    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mSession = session;
            //配置完毕开始预览
            try {
                /**
                 * 设置你需要配置的参数
                 */
                //自动对焦
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //打开闪光灯
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                //无限次的重复获取图像
                mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);//作者将这里改为了null存疑，应该存在的mSessionCaptureCallback
                hasStopPreview = false;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(MainActivity.this, "配置失败", Toast.LENGTH_SHORT).show();
        }
    };
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        //可以用来提示图片保存的位置
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            mSession = session;
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            mSession = session;
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            MainActivity.this.finish();
        }
    };

    //将照片存储在相机照片存储位置,这里采用bitmap方式保存
    public String savePicture(byte[] imgBytes) {
        pictureId = (int) (Math.random() * 1000);
        Log.d(TAG, pictureId + "");
        String imgPath = Environment.getExternalStorageDirectory() + "/DCIM/Camera/WordRecognition_picture" + pictureId + ".jpg";
        Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);//图像数据被转化为bitmap
        File outputImage = new File(imgPath);
        FileOutputStream outputStream = null;
        try {
            if (outputImage.exists()) {
                outputImage.delete();//存在就删除
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream = new FileOutputStream(outputImage);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);//第二个参数为压缩质量
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
//及时更新到系统相册
        MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStorageDirectory() + "//DCIM//Camera//WordRecogniton_picture" + pictureId + ".jpg"}, null, null);//"//"可以用File.separator代替
        return imgPath;
    }

    public void takePicture() {
        try {
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);//用来设置拍照请求的request
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(cameraCharacteristics, rotation));//使图片做顺时针旋转
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            if (!hasStopPreview) {
                mSession.stopRepeating();
                hasStopPreview = true;
            } else {
                mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
                hasStopPreview = false;
            }
            mSession.capture(mCaptureRequest, null, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "权限未获取！", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                if (grantResults.length > 0 && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        mCameraManager.openCamera(mCameraId, deviceStateCallback, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            case 3:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){

                }else{
                    Toast.makeText(this, "权限未获取，无法使用本应用！", Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            default:
        }
    }

    //获取图片应该旋转的角度，使图片竖直
    public int getOrientation(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
                return 90;
            case Surface.ROTATION_90:
                return 0;
            case Surface.ROTATION_180:
                return 270;
            case Surface.ROTATION_270:
                return 180;
            default:
                return 0;
        }
    }

    //获取图片应该旋转的角度，使图片竖直
    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // LENS_FACING相对于设备屏幕的方向,LENS_FACING_FRONT相机设备面向与设备屏幕相同的方向
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }

    public void onResult(String result) {
        recognitionResult = null;
        recognitionResult = result;
    }

    public void onError(OCRError error) {
        error.printStackTrace();
    }

    //释放资源待会处理
    @Override
    protected void onDestroy() {
        if(mCameraDevice!=null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (outputFile != null && outputFile.exists()){
            outputFile.delete();
        }
        super.onDestroy();
    }
}

package com.thundercomm.eBox.AI;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

import android.media.Image;
import android.util.Log;
import android.util.Pair;

import com.qualcomm.qti.snpe.FloatTensor;
import com.qualcomm.qti.snpe.NeuralNetwork;
import com.qualcomm.qti.snpe.SNPE;
import com.qualcomm.qti.snpe.Tensor;
import com.thundercomm.eBox.Config.GlobalConfig;
import com.thundercomm.eBox.Constants.Constants;
import com.thundercomm.eBox.Data.AgeGenderHelper;
import com.thundercomm.eBox.Data.DatabaseStatic;
import com.thundercomm.eBox.Jni;
import com.thundercomm.eBox.Model.RtspItemCollection;
import com.thundercomm.eBox.Utils.LogUtil;
import com.thundercomm.eBox.VIew.AgeGenderDetectFragment;
import com.thundercomm.eBox.VIew.PlayFragment;
import com.thundercomm.gateway.data.DeviceData;
import com.thundercomm.eBox.Data.ObjectData;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import org.bytedeco.opencv.presets.opencv_core;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import lombok.SneakyThrows;

/**
 * Age Gender Detector
 *
 * @Describe
 */
public class AgeGenderDectorTask {

    private static String TAG = "AgeGenderDectorTask";

    private HashMap<Integer, NeuralNetwork> mapAgeGendorDetector = new HashMap<Integer, NeuralNetwork>();

    private HashMap<Integer, DataInputFrame> inputFrameMap = new HashMap<Integer, DataInputFrame>();
    private Vector<AgeGenderDetectTaskThread> mAgeGenderPerTaskThreads = new Vector<AgeGenderDetectTaskThread>();

    private boolean istarting = false;
    private boolean isInit = false;
    private Application mContext;
    private ArrayList<PlayFragment> playFragments;
    private AgeGenderHelper mAgeGenderHelper;
    private SQLiteDatabase database = null;

    private int frameWidth;
    private int frameHeight;

    private static volatile AgeGenderDectorTask _instance;

    private AgeGenderDectorTask() {
    }

    public static AgeGenderDectorTask getAgeGenderDectorTask() {
        if (_instance == null) {
            synchronized (AgeGenderDectorTask.class) {
                if (_instance == null) {
                    _instance = new AgeGenderDectorTask();
                }
            }
        }
        return _instance;
    }

    public void init( Application context, Vector<Integer> idlist, ArrayList<PlayFragment> playFragments, int width, int height) {
        frameWidth = width;
        frameHeight = height;
        interrupThread();
        for (int i = 0; i < idlist.size(); i++) {
            if (getAgeGenderAlgorithmType(idlist.elementAt(i))) {
                DataInputFrame data = new DataInputFrame(idlist.elementAt(i));
                inputFrameMap.put(idlist.elementAt(i), data);
            }
        }
        mContext = context;
        istarting = true;
        isInit = true;
        this.playFragments = playFragments;
        for (int i = 0; i < idlist.size(); i++) {
            if (getAgeGenderAlgorithmType(idlist.elementAt(i))) {
                AgeGenderDetectTaskThread ageGenderTaskThread = new AgeGenderDetectTaskThread(idlist.elementAt(i));
                ageGenderTaskThread.start();
                mAgeGenderPerTaskThreads.add(ageGenderTaskThread);
            }
        }
    }

    private boolean getAgeGenderAlgorithmType(int id) {
        DeviceData deviceData = RtspItemCollection.getInstance().getDeviceList().get(id);
        boolean enable = Boolean.parseBoolean(RtspItemCollection.getInstance().getAttributesValue(deviceData, Constants.ENABLE_AGEGENDER_STR));
        return enable;
    }

    public void addImgById(int id, final Image img) {
        if (!inputFrameMap.containsKey(id)) {
            return;
        }

        DataInputFrame data = inputFrameMap.get(id);
        data.addImgById(img);
    }

    public void addBitmapById(int id, final Bitmap bmp, int w, int h) {
        if (!inputFrameMap.containsKey(id)) {
            return;
        }

        DataInputFrame data = inputFrameMap.get(id);
        data.org_w = w;
        data.org_h = h;
        data.addBitMapById(bmp);
    }

    public void addMatById(int id, final Mat img, int w, int h) {
        if (!inputFrameMap.containsKey(id)) {
            return;
        }

        DataInputFrame data = inputFrameMap.get(id);
        data.org_w = w;
        data.org_h = h;
        data.addMatById(img);
    }


    class AgeGenderDetectTaskThread extends Thread {
        private AgeGenderDetectFragment ageGenderDectorTask = null;
        private NeuralNetwork network = null;
        Map<String, FloatTensor> outputs = null;
        Map<String, FloatTensor> inputs = null;
        int alg_camid = -1;

        AgeGenderDetectTaskThread(int id) {
            alg_camid = id;
            if (!mapAgeGendorDetector.containsKey(alg_camid)) {
                try {
                    final SNPE.NeuralNetworkBuilder builder = new SNPE.NeuralNetworkBuilder(mContext)
                             .setOutputLayers("model_1/dense_1/Softmax", "model_1/dense_2/Softmax")
                            .setRuntimeOrder(NeuralNetwork.Runtime.CPU)
                            .setModel(new File( GlobalConfig.SAVE_PATH + "age-gender-05.dlc"));
                    network = builder.build();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mapAgeGendorDetector.put(alg_camid, network);
            } else {
                network = mapAgeGendorDetector.get(alg_camid);
            }
        }

        @SneakyThrows
        @Override
        public void run() {
            super.run();
            Jni.Affinity.bindToCpu(alg_camid % 4 + 4);
            ageGenderDectorTask = (AgeGenderDetectFragment) playFragments.get(alg_camid);
            DataInputFrame inputFrame = inputFrameMap.get(alg_camid);
            Mat rotateimage = new Mat(frameHeight, frameWidth, CvType.CV_8UC4);
            Mat resizeimage = new Mat(frameHeight, frameWidth, CvType.CV_8UC4);
            Mat frameBgrMat = new Mat(frameHeight, frameWidth, CvType.CV_8UC3);
            float[] matData = new float[frameHeight * frameWidth * 3];

            LogUtil.d("", "debug test start camid  " + alg_camid);
            Set<String> inputNames = network.getInputTensorsNames();
            final String inputLayer = inputNames.iterator().next();

            final FloatTensor tensor = network.createFloatTensor(
                    network.getInputTensorsShapes().get(inputLayer));
            while (istarting) {
                try {
                    inputFrame.updateFaceRectCache();
                    Mat mat = inputFrame.getMat();
                    if (!OPencvInit.isLoaderOpenCV() || mat == null) {
                        if (mat != null) mat.release();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    Core.flip(mat, rotateimage, 0);
                    FaceDet faceDet = new FaceDet();
                    Bitmap bitmap = matToBitmap(rotateimage);
                    Mat cron_img = null;
                    List<VisionDetRet> results = faceDet.detect(bitmap);
                    postNoResult(alg_camid);
                    Vector<ObjectData> mObjectVec = new Vector<>();
                    for (final VisionDetRet ret : results) {
                        ObjectData Dect = new ObjectData();
                        int rectLeft = ret.getLeft();
                        int rectTop = ret.getTop();
                        int rectRight = ret.getRight();
                        int rectBottom = ret.getBottom();
                        Dect.rect[0] = rectLeft;
                        Dect.rect[1] = rectTop;
                        Dect.rect[2] = rectRight - rectLeft;
                        Dect.rect[3] = rectBottom - rectTop;
                        cron_img  = new Mat(rotateimage, new Rect(rectLeft - 50,rectTop - 50,rectRight -rectLeft + 50,rectBottom - rectTop +50));
                        Imgproc.resize(cron_img, resizeimage, new Size(frameHeight, frameHeight));
                        Imgproc.cvtColor(resizeimage, frameBgrMat, Imgproc.COLOR_BGR2RGB);
                        frameBgrMat.convertTo(frameBgrMat, CvType.CV_32FC3);
                        frameBgrMat.get(0,0, matData);

                        if (mat != null) mat.release();
                        tensor.write(matData, 0, matData.length);
                        inputs = new HashMap<>();
                        inputs.put(inputLayer, tensor);
                        outputs = network.execute(inputs);

                        for (Map.Entry<String, FloatTensor> output : outputs.entrySet()) {
                            FloatTensor outputTensor = output.getValue();
                            final float[] array = new float[outputTensor.getSize()];
                            outputTensor.read(array, 0, array.length);
                            float num = 0;
                            if (output.getKey().equals("model_1/dense_2/Softmax:0")) {
                                for (int i = 0; i < array.length; i++) {
                                    num = num + i * array[i];
                                }
                                Dect.age = (int)num;
                            } else {
                                Dect.gender = array[0] > array[1] ? "F" : "M";
                            }
                            if (array[0] >= 0.7f || array[0] <= 0.3f) {
                                insertOrUpdateDatabase(Dect.age, array[0] <= array[1]);
                                mObjectVec.add(Dect);
                            }
                        }
                    }
                     if (mObjectVec.size() != 0) postObjectDetectResult(alg_camid, mObjectVec);

                } catch (final Exception e) {
                    e.printStackTrace();
                    LogUtil.e(TAG, "Exception!");
                }
            }
            releaseTensors(inputs, outputs);
        }

        private void insertDatabase(int age, boolean isMale, String sDate) {
            if(mAgeGenderHelper == null) {
                mAgeGenderHelper = new AgeGenderHelper(mContext);
            }
            database = mAgeGenderHelper.getWritableDatabase();

            ContentValues cV = new ContentValues();
            for(int i = 0; i < 10; i++) {
                if (age / 10 == i || age == 100) {
                    cV.put(DatabaseStatic.Age_Gender[i][0], isMale ? 1 : 0);
                    cV.put(DatabaseStatic.Age_Gender[i][1], isMale ? 0 : 1);
                } else {
                    cV.put(DatabaseStatic.Age_Gender[i][0], 0);
                    cV.put(DatabaseStatic.Age_Gender[i][1], 0);
                }
            }
            cV.put(DatabaseStatic.Date, sDate);
            database.insert(DatabaseStatic.TABLE_NAME, null, cV);

        }

        private void updateDatabase(String ageGender, int count, String date) {
            if(mAgeGenderHelper == null) {
                mAgeGenderHelper = new AgeGenderHelper(mContext);
            }
            database = mAgeGenderHelper.getWritableDatabase();

            ContentValues cV = new ContentValues();
            cV.put(ageGender, count + 1);
            database.update(DatabaseStatic.TABLE_NAME, cV,
                    DatabaseStatic.Date + "= ?", new String[]{date});
        }


        private void insertOrUpdateDatabase(int age, boolean isMale)
        {
            if(mAgeGenderHelper == null) {
                mAgeGenderHelper = new AgeGenderHelper(mContext);
            }
            database = mAgeGenderHelper.getWritableDatabase();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date(System.currentTimeMillis());
            simpleDateFormat.format(date);
            String sDate = simpleDateFormat.format(date);
            Cursor cursor = database.rawQuery("select * from " + DatabaseStatic.TABLE_NAME +
                    " where "+ DatabaseStatic.Date + "=?", new String[] {sDate});

            if(cursor.moveToFirst()) {
                int age_index = ((age / 10) == 10) ? 9 : (age / 10);
                String ageGender = DatabaseStatic.Age_Gender[age_index][isMale ? 0 : 1];
                int count = cursor.getInt(age_index * 2 + (isMale ? 0 : 1) + 1);
                updateDatabase(ageGender, count, sDate);
            } else {
                insertDatabase(age, isMale, sDate);
            }

            cursor.close();
        }


        private final void releaseTensors(Map<String, ? extends Tensor>... tensorMaps) {
            for (Map<String, ? extends Tensor> tensorMap: tensorMaps) {
                for (Tensor tensor: tensorMap.values()) {
                    tensor.release();
                }
            }
        }

        private void postNoResult(int id) {
            if (ageGenderDectorTask != null) {
                ageGenderDectorTask.OnClean();
            }
        }

        private void postObjectDetectResult(int id, Vector<ObjectData> mObjectVec) {
            if (ageGenderDectorTask != null) {
                ageGenderDectorTask.OndrawFace(mObjectVec);
            }
        }
    }

    //Mat to Bitmap
    public static Bitmap matToBitmap(Mat mat) {
        Bitmap resultBitmap = null;
        if (mat != null) {
            resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            if (resultBitmap != null)
                Utils.matToBitmap(mat, resultBitmap);
        }
        return resultBitmap;
    }

    public void closeService() {

        isInit = false;
        istarting = false;

        System.gc();
        System.gc();
    }

    private void interrupThread() {
        for (AgeGenderDetectTaskThread objectPerTaskThread : this.mAgeGenderPerTaskThreads) {
            if (objectPerTaskThread != null && !objectPerTaskThread.isInterrupted()) {
                objectPerTaskThread.interrupt();
            }
        }
        mapAgeGendorDetector.clear();
    }

    public boolean isIstarting() {
        return isInit;
    }
}

package com.example.kanbara.avoidobstacle;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AvoidObstacleView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final int GOAL_HEIGHT = 300;
    private static final int START_HEIGHT = 300;

    private static final int JUMP_HEIGHT = START_HEIGHT - 30;

    private static final int OUT_WIDTH = 50;
    private static final int DROID_POS = OUT_WIDTH + 50;

    private int mWidth;
    private int mHeight;

    private boolean mIsGoal = false;
    private boolean mIsGone = false;

    private boolean mIsAttached;
    private Thread mThread;

    private SurfaceHolder mHolder;
    private Canvas mCanvas = null;
    private Paint mPaint = null;
    private Path mGoalZone;
    private Path mStartZone;
    private Path mOutZoneL;
    private Path moutZoneR;
    private Region mRegionGoalZone;
    private Region mRegionStartZone;
    private Region mRegionOutZoneL;
    private Region mRegionOutZoneR;

    private Region mRegionWholeScreen;

    private long startTime;
    private long endTime;

    // ドロイド君用のビットマップ
    private Bitmap mBitmapDroid;
    // ドロイド君クラス
    private Droid mDroid;

    // 障害物用のビットマップ
    private Bitmap mBitmapObstacle;
    // 障害物のクラス
    private Obstacle mObstacle;

    // 障害物のリスト
    private List<Obstacle> mObstacleList = new ArrayList<Obstacle>(20);

    // 乱数
    private Random mRond;

    public AvoidObstacleView(Context context) {
        super(context);
        // SurfaceHolder の取得
        mHolder = getHolder();
        // SurfaceView イベントの通知先の指定(自身のクラス)
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);

        mWidth = getWidth();
        mHeight = getHeight();

        Resources rsc = getResources();
        // ランチャーのドロイド君の画像 Bitmap を生成
        mBitmapDroid = BitmapFactory.decodeResource(rsc, R.mipmap.ic_launcher);
        // 障害物の画像 Bitmap を生成
        mBitmapObstacle = BitmapFactory.decodeResource(rsc, R.mipmap.rock);

        // 盤面のゾーン決め
        zoneDcide();

        // 乱数準備
        mRond = new Random();

        newDroid();
        newObstacle();

        mIsAttached = true;
        mThread = new Thread(this);
        mThread.start();
    }

    @Override
    public void run() {
        while (mIsAttached) {
            drawGameBoard();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(mRegionStartZone.contains((int)event.getX(), (int)event.getY())) {
                    // ドロイド君と障害物を出す
                    newDroid();
                    newObstacle();
                }

                break;
            default:
                break;
        }
        return true;
    }

    private void zoneDcide() {
        mRegionWholeScreen = new Region(0, 0, mWidth, mHeight);
        mGoalZone = new Path();
        mGoalZone.addRect(OUT_WIDTH, 0, mWidth - OUT_WIDTH, GOAL_HEIGHT, Path.Direction.CW);
        mRegionGoalZone = new Region();
        mRegionGoalZone.setPath(mGoalZone, mRegionWholeScreen);

        mStartZone = new Path();
        mStartZone.addRect(OUT_WIDTH, mHeight - START_HEIGHT, mWidth - OUT_WIDTH, mHeight, Path.Direction.CW);
        mRegionStartZone = new Region();
        mRegionStartZone.setPath(mStartZone, mRegionWholeScreen);

        mOutZoneL = new Path();
        mOutZoneL.addRect(0, 0, OUT_WIDTH, mHeight, Path.Direction.CW);
        mRegionOutZoneL = new Region();
        mRegionOutZoneL.setPath(mOutZoneL, mRegionWholeScreen);

        moutZoneR = new Path();
        moutZoneR.addRect(mWidth - OUT_WIDTH, 0, mWidth, mHeight, Path.Direction.CW);
        mRegionOutZoneR = new Region();
        mRegionOutZoneR.setPath(moutZoneR, mRegionWholeScreen);
    }

    public void drawGameBoard() {
        // ボールを落としたか、障害物に衝突したか、あるいはゴールしたとき
        if ((mIsGone) || (mIsGoal)) {
            return;
        }

        mDroid.move(MainActivity.role, MainActivity.pitch);
        if (mDroid.getBottom() > mHeight) {
            mDroid.setLocate(mDroid.getLeft(), (mHeight - JUMP_HEIGHT));
        }

        try {
            for (Obstacle obstacle : mObstacleList) {
                if (obstacle != null) {
                    obstacle.move();
                }
            }
            mCanvas = getHolder().lockCanvas();
            mCanvas.drawColor(Color.LTGRAY);

            mPaint.setColor(Color.MAGENTA);
            mCanvas.drawPath(mGoalZone, mPaint);
            mPaint.setColor(Color.GRAY);
            mCanvas.drawPath(mStartZone, mPaint);
            mPaint.setColor(Color.BLACK);
            mCanvas.drawPath(mOutZoneL, mPaint);
            mCanvas.drawPath(moutZoneR, mPaint);

            mPaint.setColor(Color.BLACK);
            mPaint.setTextSize(50);

            // Goal 文字列
            mCanvas.drawText(getResources().getString(R.string.goal), (int)mWidth / 2 - 50, 100, mPaint);
            mCanvas.drawText(getResources().getString(R.string.start), (int)mWidth / 2 -50, mHeight - 50 ,mPaint);

            if (mRegionOutZoneL.contains(mDroid.getCenterX(), mDroid.getCenterY())) {
                mIsGone = true;
            }
            if (mRegionOutZoneR.contains(mDroid.getCenterX(), mDroid.getCenterY())) {
                mIsGone = true;
            }
            if (mRegionGoalZone.contains(mDroid.getCenterX(), mDroid.getCenterY())) {
                mIsGoal = true;

                // ゴールした
                String msg = goaled();
                mPaint.setColor(Color.WHITE);
                mCanvas.drawText(msg, OUT_WIDTH + 10, GOAL_HEIGHT - 100, mPaint);
            }
            // 隕石はスタートゾーンにかかると消える
            for (Obstacle obstacle : mObstacleList) {
                if (mRegionStartZone.contains(obstacle.getLeft(), obstacle.getBottom())) {
                    obstacle.setLocate(obstacle.getLeft(), 0);
                }
            }
            if (!mIsGoal) {
                for (Obstacle obstacle : mObstacleList) {
                    if (mDroid.collisionCheck(obstacle)) {
                        String msg = getResources().getString(R.string.collision);
                        mPaint.setColor(Color.WHITE);
                        mCanvas.drawText(msg, OUT_WIDTH + 10, GOAL_HEIGHT - 100, mPaint);
                        mIsGone = true;
                    }
                }
            }

            if (!((mIsGone) || (mIsGoal))) {
                mPaint.setColor(Color.DKGRAY);
                for (Obstacle obstacle : mObstacleList) {
                    mCanvas.drawBitmap(mBitmapObstacle, obstacle.getLeft(), obstacle.getTop(), null);
                }

                mCanvas.drawBitmap(mBitmapDroid, mDroid.getLeft(), mDroid.getTop(), null);
            }
            getHolder().unlockCanvasAndPost(mCanvas);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String goaled() {
        endTime = System.currentTimeMillis();
        // 経過時間
        long erapsedTime = endTime - startTime;
        int secTime = (int)(erapsedTime/1000);
        return ("Goal! " + secTime + "秒");

    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Bitmap リソースをメモリから開放する
        if (mBitmapDroid != null) {
            mBitmapDroid.recycle();
            mBitmapDroid = null;
        }
        if (mBitmapObstacle != null) {
            mBitmapObstacle.recycle();
            mBitmapObstacle = null;
        }
        mIsAttached = false;
        while (mThread.isAlive());

    }

    private void newDroid() {
        mDroid = new Droid(DROID_POS, mHeight - JUMP_HEIGHT, mBitmapDroid.getWidth(), mBitmapDroid.getHeight());
        Log.v("Width",":"+mBitmapDroid.getWidth());
        Log.v("Height",":"+mBitmapDroid.getHeight());

        mIsGoal = false;
        mIsGone = false;
        startTime = System.currentTimeMillis();
    }

    private void newObstacle() {
        Obstacle obstacle;

        mObstacleList.clear();

        for (int i = 0; i < 20; i++) {
            // left 座標を乱数で求める
            int left = mRond.nextInt(mWidth - (OUT_WIDTH * 2 + mBitmapObstacle.getWidth())) + OUT_WIDTH;
            // top 座標を乱数で求める
            int top = mRond.nextInt(mHeight - mBitmapObstacle.getHeight() * 2);
            // 1 から 3 の乱数生成
            int speed = mRond.nextInt(3) + 1;
            obstacle = new Obstacle(left, top, mBitmapObstacle.getWidth(), mBitmapObstacle.getHeight(), speed);
            mObstacleList.add(obstacle);
        }
    }

}

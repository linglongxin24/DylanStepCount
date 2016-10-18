package cn.bluemobi.dylan.step.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.text.DecimalFormat;

import cn.bluemobi.dylan.step.R;

/**
 * Created by admin on 2016/5/26.
 * 显示步数的圆弧
 */
public class CustomCircleView extends View {
    private static final int DEFAULT_MIN_WIDTH = 200; //View默认大小
    //圆弧的宽度
    private static final float DEFAULT_BORDER_WIDTH = 38f;
    //小原点的半径
    private int DEFAULT_LITLE_WIDTH = dipToPx( 5 );
    private ValueAnimator progressAnimator;
    private float textSize = dipToPx(15);
    private float BIM_textSize = dipToPx(16);

    private float Number_textSiz=0;
    private int padding=dipToPx(10);//默认胖和瘦距离上面圆环的距离
    private int marging=(int)(Math.max(DEFAULT_BORDER_WIDTH,textSize));
    private float currentAngle=0;
    private String PANG_NUMBER="";
    private String PANG_NUMBER_TEXT="";
    //当前的步数
    private float maxCount=1;
    private float currentCount=0;
    private float startAngle = 500;//开始绘制的角度
    private float lastAngle=260;//最后绘制的角度
    private float defaultLastAngle=260;//黄色圆弧默认的弧度


    private int aniSpeed = 1000;//设置滚动的速度

    DecimalFormat df = new DecimalFormat("0.0");//格式化小数，不足的补0
    //分段颜色
    private int[] colors = new int[]{getResources().getColor(R.color.red),
            getResources().getColor(R.color.red),
            getResources().getColor(R.color.red),
            getResources().getColor(R.color.red),
            getResources().getColor(R.color.red)
    };
    /**
     * 填充色主要参数：
     colors[]
     positions[]
     即每个position定义一个color值，注意position是一个相对位置，其值应该在0.0到1.0之间。
     18.5/24.0/28.0/35.0
     */
    private float positions[]=new float[colors.length];
    //分割区域的数值
    private double position_line[] = new double[]{0,18.5,24.0,28.0,35.0};


    public CustomCircleView(Context context) {
        super( context );


    }

    public CustomCircleView(Context context, AttributeSet attrs) {
        super( context, attrs );
    }

    public CustomCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super( context, attrs, defStyleAttr );
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw( canvas );
//        canvas.drawCircle( 550,600,400,paint );
//        RectF oval=new RectF();                       //RectF对象
//        oval.left=100;                              //左边
//        oval.top=400;                                   //上边
//        oval.right=400;                             //右边
//        oval.bottom=700;                                //下边
//        canvas.drawArc(oval, 501, 260, false, paint);    //绘制圆弧

//1------绘制默认黄色的圆弧
        float r = Math.min( getHeight() / 2, getWidth() / 2 );//半径
        Paint paintDefalt = new Paint();
        paintDefalt.setColor( getResources().getColor( R.color.yellow ) );
        //设置笔刷的样式 Paint.Cap.Round ,Cap.SQUARE等分别为圆形、方形
        paintDefalt.setStrokeCap( Paint.Cap.SQUARE );
        float borderWidth = DEFAULT_BORDER_WIDTH;//圆弧宽度

        float centerX = (getHeight() - 2 * marging) / 2;
//        RectF oval1=new RectF(marging,marging,getWidth()-marging,getHeight()-marging);
        RectF oval1 = new RectF( marging, marging, 2 * centerX + marging, 2 * centerX + marging );
        /**
         * Paint.Style.FILL    :填充内部
         Paint.Style.FILL_AND_STROKE  ：填充内部和描边
         Paint.Style.STROKE  ：仅描边
         */

        paintDefalt.setStrokeJoin( Paint.Join.ROUND );
        paintDefalt.setStrokeCap( Paint.Cap.ROUND );//圆角弧度
        paintDefalt.setStyle( Paint.Style.STROKE );//设置填充样式
        paintDefalt.setAntiAlias( true );//抗锯齿功能
        paintDefalt.setStrokeWidth( borderWidth );//设置画笔宽度
//        该类是第二个参数是角度的开始，第三个参数是多少度
//        canvas.drawCircle(getWidth() / 2, getHeight() / 2, r, paintCircle);//画圆,圆心在中心位置,半径为长宽小者的一半

        /**
         * drawArc(RectF oval, float startAngle, float sweepAngle, boolean useCenter, Paint paint)//画弧，
         参数一是RectF对象，一个矩形区域椭圆形的界限用于定义在形状、大小、电弧，
         参数二是起始角(度)在电弧的开始，
         参数三扫描角(度)开始顺时针测量的，参数四是如果这是真的话,包括椭圆中心的电弧,并关闭它,如果它是假这将是一个弧线,参数五是Paint对象；
         */
        //绘制默认黄色的圆弧
        canvas.drawArc( oval1, startAngle, defaultLastAngle, false, paintDefalt );//小弧形
//        参数一为渐变起初点坐标x位置，参数二为y轴位置，参数三和四分辨对应渐变终点，最后参数为平铺方式，这里设置为镜像.

        //2-----绘制当前进度的圆弧
        Paint paintCurrent = new Paint();
        paintCurrent.setStrokeJoin( Paint.Join.ROUND );
        paintCurrent.setStrokeCap( Paint.Cap.ROUND );//圆角弧度
        paintCurrent.setStyle( Paint.Style.STROKE );//设置填充样式
        paintCurrent.setAntiAlias( true );//抗锯齿功能
        paintCurrent.setStrokeWidth( borderWidth );//设置画笔宽度
        paintCurrent.setColor( getResources().getColor( R.color.red ) );//设置画笔颜色
        //设置笔刷的样式 Paint.Cap.Round ,Cap.SQUARE等分别为圆形、方形
      //  paintCurrent.setStrokeCap( Paint.Cap.SQUARE );
        /**
         * static final Shader.TileMode CLAMP: 边缘拉伸.
         static final Shader.TileMode MIRROR：在水平方向和垂直方向交替景象, 两个相邻图像间没有缝隙.
         Static final Shader.TillMode REPETA：在水平方向和垂直方向重复摆放,两个相邻图像间有缝隙缝隙.
         */

        LinearGradient lg = new LinearGradient( 0, 0, 100, 100, colors, positions, Shader.TileMode.MIRROR );  //渐变颜色
        // 创建SweepGradient对象
        // 第一个,第二个参数中心坐标
        // 后面的参数与线性渲染相同
        //18.5/24.0/28.0/35.0
        positions[0] = Float.parseFloat( df.format( position_line[0] / maxCount ) );
        positions[1] = Float.parseFloat( df.format( position_line[1] / maxCount ) );
        positions[2] = Float.parseFloat( df.format( position_line[2] / maxCount ) );
        positions[3] = Float.parseFloat( df.format( position_line[3] / maxCount ) );
        positions[4] = Float.parseFloat( df.format( position_line[4] / maxCount ) );

        SweepGradient sweepGradient = new SweepGradient( centerX + padding, centerX + padding, colors, positions );
        Matrix matrix = new Matrix();
        matrix.setRotate( 130, centerX, centerX );//加上旋转还是很有必要的，每次最右边总是有一部分多余了,不太美观,也可以不加
        sweepGradient.setLocalMatrix( matrix );
        //设置变色
        //paintCurrent.setShader( sweepGradient );

//        canvas.drawArc(oval1, 180, currentAngle, false, paintCurrent);//小弧形
        //当前进度
        canvas.drawArc( oval1, startAngle, currentAngle, false, paintCurrent );
        //3----文字
        //内容显示文字
        Paint vTextPaint = new Paint();
        vTextPaint.setTextSize( textSize );

        vTextPaint.setAntiAlias( true );//抗锯齿功能
//        vTextPaint.setStrokeWidth((float) 3.0);
        vTextPaint.setTextAlign( Paint.Align.CENTER );

        //TODO 等待修改
//        canvas.drawText( "开始", marging, centerX + marging + textSize + padding, vTextPaint );
//        canvas.drawText( "结束", getWidth() - marging, centerX + marging + textSize + padding, vTextPaint );
        //圆环中心的文字
        vTextPaint.setTextSize( BIM_textSize );
        vTextPaint.setColor(getResources().getColor( R.color.grey ));
        canvas.drawText( "步数",centerX + marging, (int) ((centerX + marging) / 3 * 3.5 + textSize), vTextPaint );

        //跑步所占比例
        /**
         * 常用的字体风格名称还有：
         * Typeface.BOLD //粗体
         * Typeface.BOLD_ITALIC //粗斜体
         * Typeface.ITALIC //斜体
         * Typeface.NORMAL //常规
         */
        vTextPaint.setTextSize( Number_textSiz );
        Typeface font = Typeface.create( Typeface.SANS_SERIF, Typeface.NORMAL );
        vTextPaint.setTypeface( font );
        vTextPaint.setColor(getResources().getColor( R.color.red ));
        canvas.drawText( PANG_NUMBER_TEXT,centerX + marging, (int) ((centerX + marging) / 3 * 2.8 + textSize), vTextPaint );


        //4---绘制圆弧上的小圆球--根据currentAngle
        /**
         * Paint.Style.FILL    :填充内部
         Paint.Style.FILL_AND_STROKE  ：填充内部和描边
         Paint.Style.STROKE  ：仅描边
         */
        canvas.translate( getWidth() / 2, getHeight() / 2 );//这时候的画布已经移动到了中心位置
        Paint paintCircle = new Paint();
        paintCircle.setStyle( Paint.Style.FILL );//设置填充样式
        paintCircle.setAntiAlias( true );//抗锯齿功能
        paintCircle.setColor(getResources().getColor( R.color.red ) );
//        paintCircle.setStrokeWidth(borderLitalWidth);//设置画笔宽度

//            canvas.drawCircle((float)(centerX+padding-centerX*Math.cos(currentAngle)), (float)(centerX+padding-centerX*Math.sin(currentAngle)), DEFAULT_LITLE_WIDTH, paintCircle);//画圆,圆心在中心位置,半径为长宽小者的一半
//        canvas.drawCircle(0, 0, DEFAULT_LITLE_WIDTH, paintCircle);//这时候的画布已经移动到了中心位置
        /**
         * 第一个参数为正则顺时针，否则逆时针
         * 后面两个参数是圆心
         * 画布的旋转一定要在，画图形之前进行旋转
         */
//        canvas.rotate(-currentAngle, centerX+padding, centerX+padding);//以圆心旋转的
//        canvas.drawCircle(padding,centerX+padding,DEFAULT_LITLE_WIDTH,paintCircle);
//        canvas.rotate(currentAngle, centerX+padding, centerX+padding);//以圆心旋转的

        //canvas.rotate( currentAngle );
        //canvas.drawCircle( -centerX, 0, DEFAULT_LITLE_WIDTH, paintCircle );
        //canvas.rotate( -currentAngle );


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension( measure( widthMeasureSpec, true ), measure( heightMeasureSpec, false ) );
    }

    /**
     * @param origin
     * @param isWidth
     * @return
     */
    private int measure(int origin, boolean isWidth) {
        int result = DEFAULT_MIN_WIDTH;
        int specMode = MeasureSpec.getMode( origin );//得到模式
        int specSize = MeasureSpec.getSize( origin );//得到尺寸
        switch (specMode) {
            //EXACTLY是精确尺寸，当我们将控件的layout_width或layout_height指定为具体数值时如andorid:layout_width="50dip"，或者为FILL_PARENT是，都是控件大小已经确定的情况，都是精确尺寸。
            case MeasureSpec.EXACTLY:
                //AT_MOST是最大尺寸，当控件的layout_width或layout_height指定为WRAP_CONTENT时，控件大小一般随着控件的子空间或内容进行变化，此时控件尺寸只要不超过父控件允许的最大尺寸即可
            case MeasureSpec.AT_MOST:
                result = specSize;
                if (isWidth) {
//                    widthForUnspecified = result;
                } else {
//                    heightForUnspecified = result;
                }
                break;
            //UNSPECIFIED是未指定尺寸，这种情况不多，一般都是父控件是AdapterView，通过measure方法传入的模式。
            case MeasureSpec.UNSPECIFIED:
            default:
                result = Math.min( result, specSize );
                if (isWidth) {//宽或高未指定的情况下，可以由另一端推算出来 - -如果两边都没指定就用默认值
//                    result = (int) (heightForUnspecified * BODY_WIDTH_HEIGHT_SCALE);
                } else {
//                    result = (int) (widthForUnspecified / BODY_WIDTH_HEIGHT_SCALE);
                }
                if (result == 0) {
                    result = DEFAULT_MIN_WIDTH;
                }

                break;
        }
            return result;
        }
        /**
         * dip 转换成px
         * @param dip
         * @return
         */

    private int dipToPx(float dip) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dip * density + 0.5f * (dip >= 0 ? 1 : -1));
    }

    /**
     * 所走的步数进度
     * @param totalStepNum // 设置的步数
     * @param currentCounts // 所走步数
     */
    public void setCurrentCount(int totalStepNum,int currentCounts) {
        PANG_NUMBER_TEXT=currentCounts+"";//要显示的步数的设置
        if(currentCounts>totalStepNum){
            currentCounts=totalStepNum;
        }
        float scale=(float) currentCounts/totalStepNum;//所走步数占用总共步数的百分比
        float degree=scale*defaultLastAngle; //换算成弧度
        //this.currentCount = currentCounts > maxCount ? maxCount : currentCounts;
        // float curren=Float.parseFloat(df.format(currentCounts/maxCount));//返回的是String类型的
        // float last_angle=curren>1?1:curren;//最后要到达的角度
        float last_angle=degree;//最后要到达的角度
        PANG_NUMBER=currentCounts+"";//步数的设置
        currentAngle=last_angle;
//        currentAngle = lastAngle;//保存最后绘制的位置
        postInvalidate();
//        invalidate();
//        setAnimation(lastAngle, last_angle, aniSpeed);
    }
    /**
     * 为进度设置动画
     * @param last
     * @param current
     */
    private void setAnimation(float last, float current, int length) {
        progressAnimator = ValueAnimator.ofFloat(last, current);
//        Log.e("last=====",last+"");
//        Log.e("current=====",current+"");
        progressAnimator.setDuration(length);
        progressAnimator.setTarget(currentAngle);
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                currentAngle= (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        progressAnimator.start();
    }


    int[] sizeTable = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999,
            999999999, Integer.MAX_VALUE };

    /**
     * 获取整数类型位数
     * @param x
     * @return
     */
    private int sizeOfInt(int x) {
        for (int i = 0;; i++)
            if (x <= sizeTable[i])
                return i + 1;
    }

    /**
     * 设置文本大小
     * @param num
     */
    public void setTextSize(int num){
        int textSize=sizeOfInt(num);
        if(textSize<=4){
            Number_textSiz=dipToPx(50);
        }else if(textSize>4&&textSize<=6){
            Number_textSiz=dipToPx(40);
        }else if(textSize>6&&textSize<=8){
            Number_textSiz=dipToPx(35);
        }else if(textSize>8){
            Number_textSiz=dipToPx(28);
        }
    }


}


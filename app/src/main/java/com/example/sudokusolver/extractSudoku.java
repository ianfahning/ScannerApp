package com.example.sudokusolver;

import android.graphics.Bitmap;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.opencv.core.CvType.CV_8UC1;

public class extractSudoku {

    public extractSudoku(){

    }

    public int[][] extract(Mat sudoku, ImageView view,Bitmap bitSudoku){
        int[][] sudokuNumbers = new int[9][9];
        Mat presudoku = preProccess(sudoku.clone());

        int count=0;
        int max=-1;

        Point maxPt = null;
        Mat mask = new Mat();
        Mat test = new Mat();
        for(int y=0;y<presudoku.size().height;y++)
        {

            for(int x=0;x<presudoku.size().width;x++)
            {
                if(presudoku.get(y,x)[0]>=128)
                {
                    int area = Imgproc.floodFill(presudoku,mask, new Point(x,y), new Scalar(100));

                    if(area>max)
                    {
                        maxPt = new Point(x,y);
                        max = area;
                    }


                }
            }

        }
        Imgproc.floodFill(presudoku, mask,maxPt, new Scalar(255));
        for(int y=0;y<presudoku.size().height;y++)
        {
            for(int x=0;x<presudoku.size().width;x++)
            {
                System.out.println();
                if(presudoku.get(y,x)[0]==100 && x!=maxPt.x && y!=maxPt.y)
                {
                    Imgproc.floodFill(presudoku, mask,new Point(x,y), new Scalar(0));
                }
            }
        }
        double[] val = new double[]{0,1,0,1,1,1,0,1,0};
        Mat kernel = new Mat(3,3,CvType.CV_8U,new Scalar(val));
        Imgproc.erode(presudoku, presudoku, kernel);
        Mat lines = new Mat();
        Imgproc.HoughLines(presudoku,lines,1,Math.PI/180,200);
        for(int i=0;i<lines.rows();i++) {
            drawLine(lines.row(i), presudoku, new Scalar(128));
        }
        /*mergeLines(lines, presudoku); //merge neighboring lines
        Mat undistorted = null;
        undistorted = warpImage(lines, presudoku.clone(),undistorted,sudoku.clone());
        System.out.println(undistorted.height());
        System.out.println(undistorted.width());*/
        bitSudoku = Bitmap.createBitmap(bitSudoku, 0, 0, 500, 500);
        Utils.matToBitmap(presudoku,bitSudoku);
        view.setImageBitmap(bitSudoku);
        return sudokuNumbers;
    }

    private Mat warpImage(Mat lines,Mat sudoku,Mat undistorted,Mat original){
        // Now detect the lines on extremes
        double[] val1 = new double[]{1000,1000};
        double[] val2 = new double[]{-1000,-1000};
        double[] topEdge = new double[]{1000,1000};    double topYIntercept=100000, topXIntercept=0;
        double[] bottomEdge = new double[]{-1000,-1000};        double bottomYIntercept=0, bottomXIntercept=0;
        double[] leftEdge = new double[]{1000,1000};    double leftXIntercept=100000, leftYIntercept=0;
        double[] rightEdge = new double[]{-1000,-1000};        double rightXIntercept=0, rightYIntercept=0;
        for(int i=0;i<lines.size().width;i++) {
            double[] current = lines.get(0,i);
            double p=current[0];
            double theta=current[1];
            if(p==0 && theta==-100)
                continue;
            double xIntercept, yIntercept;
            xIntercept = p/Math.cos(theta);
            yIntercept = p/(Math.cos(theta)*Math.sin(theta));
            if(theta>Math.PI*80/180 && theta<Math.PI*100/180)
            {
                if(p<topEdge[0])
                    topEdge = current;

                if(p>bottomEdge[0])
                    bottomEdge = current;
            } else if(theta<Math.PI*10/180 || theta>Math.PI*170/180) {
                if(xIntercept>rightXIntercept)
                {
                    rightEdge = current;
                    rightXIntercept = xIntercept;
                }
                else if(xIntercept<=leftXIntercept)
                {
                    leftEdge = current;
                    leftXIntercept = xIntercept;
                }
            }
        }
        Point left1 = new Point();
        Point left2 = new Point();
        Point right1 = new Point();
        Point right2 = new Point();
        Point bottom1 = new Point();
        Point bottom2 = new Point();
        Point top1 = new Point();
        Point top2 = new Point();

        double height=sudoku.size().height;

        double width=sudoku.size().width;

        if(leftEdge[1]!=0)
        {
            left1.x=0;        left1.y=leftEdge[0]/Math.sin(leftEdge[1]);
            left2.x=width;    left2.y=-left2.x/Math.tan(leftEdge[1]) + left1.y;
        }
        else
        {
            left1.y=0;        left1.x=leftEdge[0]/Math.cos(leftEdge[1]);
            left2.y=height;    left2.x=left1.x - height*Math.tan(leftEdge[1]);

        }

        if(rightEdge[1]!=0)
        {
            right1.x=0;        right1.y=rightEdge[0]/Math.sin(rightEdge[1]);
            right2.x=width;    right2.y=-right2.x/Math.tan(rightEdge[1]) + right1.y;
        }
        else
        {
            right1.y=0;        right1.x=rightEdge[0]/Math.cos(rightEdge[1]);
            right2.y=height;    right2.x=right1.x - height*Math.tan(rightEdge[1]);

        }

        bottom1.x=0;    bottom1.y=bottomEdge[0]/Math.sin(bottomEdge[1]);

        bottom2.x=width;bottom2.y=-bottom2.x/Math.tan(bottomEdge[1]) + bottom1.y;

        top1.x=0;        top1.y=topEdge[0]/Math.sin(topEdge[1]);
        top2.x=width;    top2.y=-top2.x/Math.tan(topEdge[1]) + top1.y;
        // Next, we find the intersection of  these four lines
        double leftA = left2.y-left1.y;
        double leftB = left1.x-left2.x;

        double leftC = leftA*left1.x + leftB*left1.y;

        double rightA = right2.y-right1.y;
        double rightB = right1.x-right2.x;

        double rightC = rightA*right1.x + rightB*right1.y;

        double topA = top2.y-top1.y;
        double topB = top1.x-top2.x;

        double topC = topA*top1.x + topB*top1.y;

        double bottomA = bottom2.y-bottom1.y;
        double bottomB = bottom1.x-bottom2.x;

        double bottomC = bottomA*bottom1.x + bottomB*bottom1.y;

        // Intersection of left and top
        double detTopLeft = leftA*topB - leftB*topA;

        Point ptTopLeft = new Point((topB*leftC - leftB*topC)/detTopLeft, (leftA*topC - topA*leftC)/detTopLeft);

        // Intersection of top and right
        double detTopRight = rightA*topB - rightB*topA;

        Point ptTopRight = new Point((topB*rightC-rightB*topC)/detTopRight, (rightA*topC-topA*rightC)/detTopRight);

        // Intersection of right and bottom
        double detBottomRight = rightA*bottomB - rightB*bottomA;
        Point ptBottomRight = new Point((bottomB*rightC-rightB*bottomC)/detBottomRight, (rightA*bottomC-bottomA*rightC)/detBottomRight);// Intersection of bottom and left
        double detBottomLeft = leftA*bottomB-leftB*bottomA;
        Point ptBottomLeft = new Point((bottomB*leftC-leftB*bottomC)/detBottomLeft, (leftA*bottomC-bottomA*leftC)/detBottomLeft);
        double maxLength = (ptBottomLeft.x - ptBottomRight.x)*(ptBottomLeft.x-ptBottomRight.x) + (ptBottomLeft.y-ptBottomRight.y)*(ptBottomLeft.y-ptBottomRight.y);
        double temp = (ptTopRight.x-ptBottomRight.x)*(ptTopRight.x-ptBottomRight.x) + (ptTopRight.y-ptBottomRight.y)*(ptTopRight.y-ptBottomRight.y);

        if(temp>maxLength) maxLength = temp;

        temp = (ptTopRight.x-ptTopLeft.x)*(ptTopRight.x-ptTopLeft.x) + (ptTopRight.y-ptTopLeft.y)*(ptTopRight.y-ptTopLeft.y);

        if(temp>maxLength) maxLength = temp;

        temp = (ptBottomLeft.x-ptTopLeft.x)*(ptBottomLeft.x-ptTopLeft.x) + (ptBottomLeft.y-ptTopLeft.y)*(ptBottomLeft.y-ptTopLeft.y);

        if(temp>maxLength) maxLength = temp;

        maxLength = Math.sqrt((double)maxLength);
        List<Point> source = new ArrayList<Point>();
        List<Point> dest = new ArrayList<Point>();
        source.add(ptTopLeft);
        source.add(ptTopRight);
        source.add(ptBottomRight);
        source.add(ptBottomLeft);
        dest.add(new Point(0,0));
        dest.add(new Point(maxLength-1, 0));
        dest.add(new Point(maxLength-1, maxLength-1));
        dest.add(new Point(0, maxLength-1));
        //TODO all points are the same except bottom right
        System.out.println(ptTopLeft.x + " " + ptTopLeft.y);
        System.out.println(ptTopRight.x + " " + ptTopRight.y);
        System.out.println(ptBottomRight.x + " " + ptBottomRight.y);
        System.out.println(ptBottomLeft.x + " " + ptBottomLeft.y);
        Mat src = Converters.vector_Point2f_to_Mat(source);
        Mat dst = Converters.vector_Point2f_to_Mat(dest);
        Mat undistorted1 = new Mat((int) maxLength,(int) maxLength, CV_8UC1);
        Size size = new Size(maxLength, maxLength);
        Imgproc.warpPerspective(original, undistorted1, Imgproc.getPerspectiveTransform(src, dst), size);
        return undistorted1;
    }

    private void mergeLines(Mat lines,Mat sudoku){
        for(int x=0;x<lines.size().width;x++) {
            double[] current = lines.get(0,x);
            System.out.println(lines.get(0,x));
            if(current[0] == 0 && current[1] == -100) continue;
            double p1 = current[0];
            double theta1 = current[0];
            Point pt1current = new Point();
            Point pt2current = new Point();
            if(theta1>Math.PI*45/180 && theta1<Math.PI*135/180) {
                pt1current.x=0;

                pt1current.y = p1/Math.sin(theta1);

                pt2current.x=sudoku.size().width;
                pt2current.y=-pt2current.x/Math.tan(theta1) + p1/Math.sin(theta1);
            }else{
                pt1current.y=0;

                pt1current.x=p1/Math.cos(theta1);

                pt2current.y=sudoku.size().height;
                pt2current.x=-pt2current.y/Math.tan(theta1) + p1/Math.cos(theta1);
            }
            for(int y=0;y<lines.size().width;y++) {
                double[] pos = lines.get(0,y);
                if (current == pos)continue;
                if (Math.abs((pos)[0] - (current)[0])<20 && Math.abs((pos)[1] - (current)[1])<
                Math.PI * 10 / 180)
                {
                    double p = pos[0];
                    double theta = pos[1];
                    Point pt1 = new Point();
                    Point pt2 = new Point();
                    if (pos[1]>Math.PI * 45 / 180 && pos[1]<Math.PI * 135 / 180)
                    {
                        pt1.x = 0;
                        pt1.y = p / Math.sin(theta);
                        pt2.x = sudoku.size().width;
                        pt2.y = -pt2.x / Math.tan(theta) + p / Math.sin(theta);
                    }  else {
                        pt1.y = 0;
                        pt1.x = p / Math.cos(theta);
                        pt2.y = sudoku.size().height;
                        pt2.x = -pt2.y / Math.tan(theta) + p / Math.cos(theta);
                    }
                    if (((double) (pt1.x - pt1current.x) * (pt1.x - pt1current.x) + (pt1.y - pt1current.y) * (pt1.y - pt1current.y) < 64 * 64) &&
                            ((double) (pt2.x - pt2current.x) * (pt2.x - pt2current.x) + (pt2.y - pt2current.y) * (pt2.y - pt2current.y) < 64 * 64)) {
                        // Merge the two
                        current[0] =(current[0] + pos[0])/2;

                        current[1] = (current[1] + pos[1])/2;

                        pos[0]=0;
                        pos[1]=-100;
                    }
                }
            }
        }
    }

    private Mat preProccess(Mat sudoku){
        Size sz = new Size(500,500);
        Imgproc.resize( sudoku, sudoku, sz );
        Imgproc.cvtColor(sudoku,sudoku,Imgproc.COLOR_BGR2GRAY);
        org.opencv.core.Size s = new Size(11,11);
        Imgproc.GaussianBlur(sudoku,sudoku,s,0);
        Imgproc.adaptiveThreshold(sudoku,sudoku,255,Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY,5,2);

        Core.bitwise_not(sudoku,sudoku);
        double[] val = new double[]{0,1,0,1,1,1,0,1,0};
        Mat kernel = new Mat(3,3,CvType.CV_8U,new Scalar(val));
        Imgproc.dilate(sudoku,sudoku,kernel);
        return sudoku;
    }

    private void drawLine(Mat line, Mat img, Scalar rgb ) {
        System.out.println(line.toString());
        if(line.get(0,0)[1] !=0) {
            float m = (float) (-1/Math.tan(line.get(0,1)[0]));

            float c = (float) (line.get(0,0)[0]/Math.sin(line.get(0,1)[0]));

            Imgproc.line(img, new Point(0, c), new Point(img.size().width, m*img.size().width+c), rgb);
        } else {
            Imgproc.line(img, new Point(line.get(0,0)[0], 0), new Point(line.get(0,0)[0], img.size().height), rgb);
        }

    }


}

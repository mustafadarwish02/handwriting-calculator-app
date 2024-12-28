package com.example.hwc;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private Interpreter tflite;
    private TextView resultTextView;
    private Button calculateButton, clearButton;
    private static final String TAG = "MainActivity";

    // ترتيب الفئات حسب الأولوية
    private static final String[] LABELS = {
            "div", "eight",  "five", "four", "minus", "nine", "one",
            "plus",
            "seven", "six", "three", "times", "two", "zero"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingView = findViewById(R.id.drawingView);
        resultTextView = findViewById(R.id.resultTextView);
        calculateButton = findViewById(R.id.calculateButton);
        clearButton = findViewById(R.id.clearButton);

        // تحميل النموذج
        loadModel();

        // عند الضغط على زر الحساب
        calculateButton.setOnClickListener(v -> {
            Bitmap bitmap = drawingView.getBitmap();
            try {
                // حساب النتيجة
                String result = predictAndCalculate(bitmap);

                // عرض المعادلة والنتيجة في TextView
                resultTextView.setText(result);  // المعادلة المحسوبة هنا

                // إظهار الـ TextView الذي يحتوي على النتيجة
                resultTextView.setVisibility(TextView.VISIBLE);  // جعل النص مرئيًا

                // مسح الرسم بعد الحساب
                drawingView.clear();  // مسح الرسم من الـ DrawingView

            } catch (Exception e) {
                Toast.makeText(this, "Error during prediction or calculation", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error during prediction or calculation: ", e);
            }
        });

        // عند الضغط على زر مسح
        clearButton.setOnClickListener(v -> {
            // إخفاء النتيجة
            resultTextView.setVisibility(TextView.GONE);

            // إعادة تمكين الرسم
            drawingView.setEnabled(true);

            // مسح الرسم من الـ DrawingView
            drawingView.clear();
        });
    }

    // تحميل النموذج
    private void loadModel() {
        try {
            Interpreter.Options options = new Interpreter.Options();
            tflite = new Interpreter(loadModelFile(), options);
            Log.d(TAG, "Model loaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error loading model: ", e);
        }
    }

    // تحميل النموذج من ملف assets
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("HRC2.tflite");
        FileInputStream inputStream = fileDescriptor.createInputStream();
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    // تحويل الصورة إلى ByteBuffer
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int size = 224;  // فرضًا نستخدم 224x224 كما في التدريب
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // تغيير حجم الصورة مع الحفاظ على النسب الأصلية
        float ratio = Math.min((float) size / width, (float) size / height);  // العثور على النسبة الأصغر
        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        // إضافة padding (فراغات) إذا كانت الصورة أصغر من الحجم المطلوب
        Bitmap paddedBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawColor(Color.WHITE); // لون الخلفية يكون أبيض (أو أي لون آخر)
        canvas.drawBitmap(resizedBitmap, (size - newWidth) / 2, (size - newHeight) / 2, null);

        // إنشاء ByteBuffer بحجم (224, 224, 3)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * size * size * 3);  // 3 لقنوات RGB
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[size * size];
        paddedBitmap.getPixels(intValues, 0, size, 0, 0, size, size);

        // تعبئة ByteBuffer بالقيم عائمة (0-1) لكل قناة RGB
        for (int i = 0; i < intValues.length; i++) {
            final int val = intValues[i];
            float r = ((val >> 16) & 0xFF) / 255.0f;  // R
            float g = ((val >> 8) & 0xFF) / 255.0f;   // G
            float b = (val & 0xFF) / 255.0f;          // B

            // إضافة القيم إلى ByteBuffer
            byteBuffer.putFloat(r);
            byteBuffer.putFloat(g);
            byteBuffer.putFloat(b);
        }

        return byteBuffer;
    }

    // دالة للتنبؤ
    private String predict(Bitmap bitmap) {
        ByteBuffer input = convertBitmapToByteBuffer(bitmap);  // تحويل الصورة إلى ByteBuffer
        float[][] output = new float[1][14];  // فرضًا أن النموذج يتوقع 14 فئة
        tflite.run(input, output);  // إرسال البيانات إلى النموذج

        // ترتيب النتائج بناءً على الاحتمالات
        float[] result = output[0];
        Integer[] indices = new Integer[result.length];
        for (int i = 0; i < result.length; i++) {
            indices[i] = i;
        }

        // ترتيب الأرقام بناءً على القيم المتوقعة
        Arrays.sort(indices, (i1, i2) -> Float.compare(result[i2], result[i1]));

        // إعادة الفئة المتنبأ بها بناءً على الترتيب المحدد
        return LABELS[indices[0]];  // العودة إلى أعلى نتيجة (بناءً على الترتيب الخاص بك)
    }

    // تقسيم الصورة إلى أجزاء
    private Bitmap[] splitBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap part1 = Bitmap.createBitmap(bitmap, 0, 0, width / 3, height);
        Bitmap part2 = Bitmap.createBitmap(bitmap, width / 3, 0, width / 3, height);
        Bitmap part3 = Bitmap.createBitmap(bitmap, 2 * width / 3, 0, width / 3, height);

        return new Bitmap[]{part1, part2, part3};
    }

    // دالة للتنبؤ وحساب المعادلة
    private String predictAndCalculate(Bitmap bitmap) {
        Bitmap[] parts = splitBitmap(bitmap);
        String num1 = predict(parts[0]);
        String operator = predict(parts[1]);
        String num2 = predict(parts[2]);

        // تسجيل القيم المتنبأ بها
        Log.d(TAG, "Prediction for num1: " + num1);
        Log.d(TAG, "Prediction for operator: " + operator);
        Log.d(TAG, "Prediction for num2: " + num2);

        // تحويل القيم المتنبأ بها إلى أرقام ورموز رياضية
        String number1 = strToSymbol(num1);  // الآن سيتم تحويل الرموز إلى قيم رياضية
        String operatorSymbol = strToSymbol(operator);  // تحويل الكلمة إلى رمز رياضي
        String number2 = strToSymbol(num2);

        // حساب المعادلة
        double result = calculateResult(number1, operatorSymbol, number2);
        return number1 + " " + operatorSymbol + " " + number2 + " = " + result;
    }

    // تحويل القيم النصية إلى رموز رياضية
    private String strToSymbol(String str) {
        switch (str) {
            case "div":
                return "/";
            case "eight":
                return "8";
            case "five":
                return "5";
            case "four":
                return "4";
            case "minus":
                return "-";
            case "nine":
                return "9";
            case "one":
                return "1";
            case "plus":
                return "+";
            case "seven":
                return "7";
            case "six":
                return "6";
            case "three":
                return "3";
            case "times":
                return "*";
            case "two":
                return "2";
            case "zero":
                return "0";
            default:
                return "";
        }
    }

    // دالة لحساب المعادلة
    private double calculateResult(String num1, String operator, String num2) {
        double n1 = Double.parseDouble(num1);
        double n2 = Double.parseDouble(num2);
        switch (operator) {
            case "+":
                return n1 + n2;
            case "-":
                return n1 - n2;
            case "*":
                return n1 * n2;
            case "/":
                if (n2 != 0) return n1 / n2;
                else return Double.NaN;  // حالة القسمة على صفر
            default:
                return 0;
        }
    }
}

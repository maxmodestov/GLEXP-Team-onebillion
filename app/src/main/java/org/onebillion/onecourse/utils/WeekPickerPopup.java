package org.onebillion.onecourse.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import org.onebillion.onecourse.mainui.MainActivity;

import static java.lang.Math.max;

public class WeekPickerPopup {
    public static void showDialog() {
        showDialog(new OnCloseListener() {
            @Override
            public void onClose() {
            }
        });
    }

    static void showDialog(final OnCloseListener listener) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.mainActivity);
        final EditText input = new EditText(MainActivity.mainActivity);
        input.setText("" + max(1, TimeProvider.getCurrentDayNumber() / 7));
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        alert.setView(input);
        alert.setTitle("Select Week");
        alert.setCancelable(false);
        alert.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String dayStr = (input.getText()).toString();
                int week = Integer.parseInt(dayStr);
                TimeProvider.setDay(week * 7);
                listener.onClose();
            }
        });
        alert.setNeutralButton("Next Week", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TimeProvider.setDay((TimeProvider.getCurrentDayNumber() / 7 + 1) * 7);
                listener.onClose();
            }
        });
        alert.show();
    }

    public interface OnCloseListener {
        void onClose();
    }
}
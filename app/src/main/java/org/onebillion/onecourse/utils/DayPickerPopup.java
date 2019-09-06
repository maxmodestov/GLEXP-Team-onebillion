package org.onebillion.onecourse.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import org.onebillion.onecourse.mainui.MainActivity;

import static java.lang.Math.max;

public class DayPickerPopup {
    public static void showDialog() {
        showDialog(() -> {
        });
    }

    static void showDialog(final OnCloseListener listener) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.mainActivity);
        final EditText input = new EditText(MainActivity.mainActivity);
        input.setText("" + max(1, TimeProvider.getCurrentDayNumber()));
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        alert.setView(input);
        alert.setTitle("Select Day");
        alert.setCancelable(false);
        alert.setPositiveButton("Proceed", (dialog, which) -> {
            String dayStr = (input.getText()).toString();
            int day = Integer.parseInt(dayStr);
            TimeProvider.setDay(day);
            listener.onClose();
        });
        alert.setNeutralButton("Next Day", (dialog, which) -> {
            TimeProvider.setDay(TimeProvider.getCurrentDayNumber() + 1);
            listener.onClose();
        });
        alert.show();
    }

    public interface OnCloseListener {
        void onClose();
    }
}
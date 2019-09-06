package org.onebillion.onecourse.utils;

import android.app.AlertDialog;

import org.onebillion.onecourse.mainui.MainActivity;

public class SyllabusPickerPopup {
    public static void showDialog() {
        showDialog(() -> {
        });
    }

    static void showDialog(final OnCloseListener listener) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.mainActivity);
        alert.setTitle("Choose Syllabus");
        alert.setCancelable(false);
        alert.setPositiveButton("PP1", (dialog, which) -> {
            TimeProvider.setDay(1);
            listener.onClose();
        });
        alert.setNegativeButton("PP2", (dialog, which) -> {
            TimeProvider.setDay(8);
            listener.onClose();
        });
        alert.setNeutralButton("Teacher", (dialog, which) -> {
            TimeProvider.setDay(15);
            listener.onClose();
        });
        alert.show();
    }

    public interface OnCloseListener {
        void onClose();
    }
}
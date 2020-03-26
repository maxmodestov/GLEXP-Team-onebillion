package org.onebillion.onecourse.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;

import org.onebillion.onecourse.mainui.MainActivity;

public class SyllabusPickerPopup {

    static void showDialog(final OnClickListener teacher, final OnCloseListener listener) {

        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.instance);
        alert.setTitle("Choose Syllabus");
        alert.setCancelable(false);
        alert.setPositiveButton("PP1", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TimeProvider.setDay(1);
                listener.onClose();
            }
        });
        alert.setNegativeButton("PP2", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TimeProvider.setDay(8);
                listener.onClose();
            }
        });
        alert.setNeutralButton("Teacher", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TimeProvider.setDay(15);
                teacher.onClick();
                listener.onClose();
            }
        });
        alert.show();
    }

    public interface OnCloseListener {
        void onClose();
    }

    public interface OnClickListener {
        void onClick();
    }
}
package com.whalts.android.appsharing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.provider.MediaStore;
import android.view.View;

import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SharePDFFile implements Runnable{
    public static final int PDFTYPE = 1001;
    public static final int IMAGETYPE = 2002;
    private NestedScrollView z;
    private Context context;
    private OutputStream os;
    private FileUriListener listener;
    private int fileType;
    public SharePDFFile(FileUriListener context, NestedScrollView u) {
        this.z = u;
        this.context = (Context) context;
        this.listener = context;
    }

    public void createFile(int filetype) {
        this.fileType = filetype;
        new Thread(this).start();
    }




    @Override
    public void run() {

        // Create a shiny new (but blank) PDF document in memory
        // We want it to optionally be printable, so add PrintAttributes
        // and use a PrintedPdfDocument. Simpler: new PdfDocument().
        if (fileType == PDFTYPE) {
            int totalHeight = z.getChildAt(0).getHeight();// parent view height
            int totalWidth = z.getChildAt(0).getWidth();// parent view width
            PrintAttributes printAttrs = new PrintAttributes.Builder().
                    setColorMode(PrintAttributes.COLOR_MODE_COLOR).
                    setMediaSize(PrintAttributes.MediaSize.NA_LETTER).
                    setResolution(new PrintAttributes.Resolution("zooey", context.PRINT_SERVICE, totalWidth, totalHeight)).
                    setMinMargins(PrintAttributes.Margins.NO_MARGINS).
                    build();
            PdfDocument document = new PrintedPdfDocument(context, printAttrs);

            // crate a page description
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(totalWidth, totalHeight, 1).create();

            // create a new page from the PageInfo
            PdfDocument.Page page = document.startPage(pageInfo);

            // repaint the user's text into the page
            z.draw(page.getCanvas());

            // do final processing of the page
            document.finishPage(page);

            // Here you could add more pages in a longer doc app, but you'd have
            // to handle page-breaking yourself in e.g., write your own word processor...

            // Now write the PDF document to a file; it actually needs to be a file
            // since the Share mechanism can't accept a byte[]. though it can
            // accept a String/CharSequence. Meh.
            try {
                File pdfDirPath = new File(context.getFilesDir(), "pdfs");
                pdfDirPath.mkdirs();
                File file = new File(pdfDirPath, "pdfsend.pdf");
                Uri contentUri = FileProvider.getUriForFile(context, "com.whalts.android.appsharing.fileprovider", file);
                os = new FileOutputStream(file);
                document.writeTo(os);
                document.close();
                os.close();
                listener.onCompleteSave(contentUri);
                //shareDocument(contentUri);
            } catch (IOException e) {
                throw new RuntimeException("Error generating file", e);
            }
        }else{
            int totalHeight = z.getChildAt(0).getHeight();// parent view height
            int totalWidth = z.getChildAt(0).getWidth();// parent view width
            Bitmap b = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            z.draw(canvas);

            try {
                File pdfDirPath = new File(context.getFilesDir(), "img");
                pdfDirPath.mkdirs();
                File file = new File(pdfDirPath, "imgsend.jpg");
                Uri contentUri = FileProvider.getUriForFile(context, "com.whalts.android.appsharing.fileprovider", file);
                FileOutputStream fos = new FileOutputStream(file);
                b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
                MediaStore.Images.Media.insertImage(context.getContentResolver(), b,
                        "Screen", "screen");
                listener.onCompleteSave(contentUri);
                //shareDocument(contentUri);
            } catch (IOException e) {
                throw new RuntimeException("Error generating file", e);
            }
        }
    }

    interface FileUriListener{
        void onCompleteSave(Uri uri);
    }
}

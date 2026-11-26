package pt.qpetiscos.agenda;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;
public class ReportProvider extends ContentProvider{
public boolean onCreate(){return true;}
private File file(Uri u){String n=u.getLastPathSegment();if(n==null||!n.matches("QPetiscos-[0-9-]+\\.(pdf|xlsx|csv)"))throw new IllegalArgumentException();return new File(new File(getContext().getCacheDir(),"reports"),n);}
public String getType(Uri u){String n=file(u).getName();return n.endsWith(".pdf")?"application/pdf":n.endsWith(".xlsx")?"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":"text/csv";}
public ParcelFileDescriptor openFile(Uri u,String m)throws FileNotFoundException{if(!"r".equals(m))throw new SecurityException();return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY);}
public Cursor query(Uri u,String[] p,String s,String[] a,String sort){File f=file(u);String[] cols=p==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:p;MatrixCursor c=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?f.getName():OpenableColumns.SIZE.equals(cols[i])?f.length():null;c.addRow(row);return c;}
public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}